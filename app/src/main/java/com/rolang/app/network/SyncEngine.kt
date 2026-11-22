package com.rolang.app.network

import android.content.Context
import androidx.room.withTransaction
import com.rolang.app.data.*
import com.rolang.app.domain.Language
import com.rolang.app.domain.Level
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

class AccountMismatch : IOException("This local library belongs to another account.")

class SyncEngine(
    private val context: Context, private val database: LearningDatabase,
    private val preferences: PreferencesRepository, private val sessions: Sessions,
    storyRepository: StoryRepository? = null,
    private val configuration: () -> ServerConfig = { ServerConfig.load(context) },
) {
    private val stories = storyRepository ?: StoryRepository(database, StoryRemoteSource(configuration))
    private val mutex = Mutex()
    private val dao = database.dao()
    suspend fun signIn(email: String, password: String, name: String? = null): AccountSession = mutex.withLock {
        withContext(Dispatchers.IO) {
            val config = configuration()
            check(config.onlineEnabled) { "Online features are disabled." }
            val body = JSONObject().put("email", email.trim()).put("password", password)
            if (name != null) body.put("displayName", name.trim())
            val response = ApiClient(config).request(if (name == null) "auth/login" else "auth/register", "POST", body)
            val user = response.getJSONObject("user")
            val session = AccountSession(user.getString("id"), user.getString("displayName"),
                response.getString("token"), response.getString("expiresAt"))
            val state = dao.syncState() ?: SyncState()
            if (state.owner != null && state.owner != session.userId) {
                runCatching { ApiClient(config).request("auth/logout", "POST", token = session.token) }
                throw AccountMismatch()
            }
            dao.saveSyncState(state.copy(owner = session.userId))
            sessions.save(session)
            session
        }
    }
    suspend fun signOut() = mutex.withLock {
        val previous = sessions.read()
        withContext(Dispatchers.IO) { sessions.clear() }
        if (previous != null) runCatching {
            ApiClient(configuration()).request("auth/logout", "POST", token = previous.token)
        }
    }

    suspend fun synchronize(includeLearning: Boolean = true): Boolean = mutex.withLock {
        withContext(Dispatchers.IO) {
            val config = configuration()
            if (!config.onlineEnabled) return@withContext true
            val api = ApiClient(config)
            // Public stories are available without registration.
            stories.refreshCatalog()
            stories.refreshUpdatedCaches()
            if (!includeLearning) return@withContext true
            val session = sessions.read() ?: return@withContext true
            val state = dao.syncState() ?: SyncState()
            if (state.owner != session.userId) throw AccountMismatch()
            bootstrap(state)
            try {
                // Bound each worker run; durable queue/cursor continue on retry.
                repeat(20) {
                    val current = checkNotNull(dao.syncState())
                    val batch = dao.pendingForSupportedCourse()
                    val operations = JSONArray()
                    batch.forEach { change -> operations.put(JSONObject().put("id", change.operationId)
                        .put("kind", change.kind).put("key", change.key).put("deleted", change.deleted)
                        .put("data", change.data?.let(::JSONObject) ?: JSONObject.NULL)) }
                    val response = api.request("me/sync", "POST", JSONObject().put("cursor", current.cursor)
                        .put("operations", operations), session.token)
                    applyResponse(response, batch)
                    if (!response.getBoolean("hasMore") && dao.pendingForSupportedCourse().isEmpty()) return@withContext true
                }
                false
            } catch (error: ApiException) {
                if (error.status == 401) sessions.clear()
                if (error.code == "CURSOR_INVALID") dao.saveSyncState(checkNotNull(dao.syncState()).copy(cursor = 0))
                throw error
            }
        }
    }

    private suspend fun bootstrap(state: SyncState) {
        if (state.bootstrapped) return
        val prefs = preferences.preferences.first()
        database.withTransaction {
            suspend fun queue(change: PendingChange) {
                if (dao.pendingFor(change.kind, change.key) == null) dao.enqueue(change)
            }
            val words = dao.allVocabulary().filter { it.language == "fil" && it.reference == "id" }
            words.forEach { queue(SyncCodec.vocabulary(it)) }
            dao.allReadings().filter { it.language == "fil" }.forEach { queue(SyncCodec.progress(it)) }
            dao.allReviews().filter { it.language == "fil" && it.reference == "id" }.forEach { review ->
                val word = words.firstOrNull { it.id == review.vocabularyId }
                // Removed words retain their review statistics even on a second device.
                queue(SyncCodec.review(review, review.vocabularyKey.ifEmpty { word?.let(SyncCodec::key) ?: "removed:${review.syncKey}" }))
            }
            // A fresh install should download server preferences before uploading
            // defaults. Explicit local edits already have a queued profile record.
            if (prefs != LearningPreferences()) queue(SyncCodec.preferences(prefs))
            dao.saveSyncState(state.copy(bootstrapped = true))
        }
    }

    internal suspend fun applyResponse(response: JSONObject, batch: List<PendingChange>) {
        val acknowledged = response.getJSONArray("acknowledged")
        val allowed = batch.map { it.operationId }.toSet()
        val records = response.getJSONArray("records")
        database.withTransaction {
            for (index in 0 until acknowledged.length()) {
                val id = acknowledged.getString(index)
                require(id in allowed)
                // If the user edited this record while uploading, its newer ID
                // remains queued and its local data wins until it is uploaded.
                dao.acknowledge(id)
            }
            val sorted = (0 until records.length()).map(records::getJSONObject)
                .sortedBy { if (it.getString("kind") == "vocabulary") 0 else 1 }
            for (record in sorted) {
                val kind = record.getString("kind")
                val deleted = record.getBoolean("deleted")
                val normalized = normalizeRecord(kind, record.getString("key"), deleted,
                    if (deleted) null else record.getJSONObject("data")) ?: continue
                val key = normalized.first
                if (dao.pendingFor(kind, key) != null) continue
                val data = normalized.second
                when (kind) {
                    "vocabulary" -> {
                        val parts = key.split(':', limit = 4)
                        require(parts.size == 4)
                        val old = dao.vocabularyByKey(parts[0], parts[1], parts[2], parts[3])
                        if (deleted) { if (old != null) dao.deleteWord(old.id) }
                        else {
                            val word = SyncCodec.word(checkNotNull(data), old?.id ?: 0)
                            require(SyncCodec.key(word) == key)
                            val localId = if (old == null) dao.saveWord(word) else { dao.updateWord(word); old.id }
                            dao.linkReviews(key, localId)
                        }
                    }
                    "progress" -> {
                        val parts = key.split(':')
                        require(parts.size == 2)
                        if (deleted) dao.deleteReading(parts[0], parts[1])
                        else {
                            val json = checkNotNull(data)
                            require("${json.getString("storyId")}:${json.getString("language")}" == key)
                            val old = dao.reading(parts[0], parts[1])
                            dao.saveReading(ReadingEntity(parts[0], parts[1], json.getInt("sentence"),
                                json.getBoolean("completed") || old?.completed == true))
                        }
                    }
                    "review" -> {
                        UUID.fromString(key)
                        if (deleted) dao.deleteReview(key) else {
                            val json = checkNotNull(data)
                            val parts = json.getString("vocabularyKey").split(':', limit = 4)
                            val word = if (parts.size == 4) dao.vocabularyByKey(parts[0], parts[1], parts[2], parts[3]) else null
                            val old = dao.reviewByKey(key)
                            dao.importReview(ReviewEntity(id = old?.id ?: 0, vocabularyId = word?.id ?: old?.vocabularyId ?: 0,
                                language = json.getString("language"), reference = json.getString("reference"),
                                reviewedAt = json.getLong("reviewedAt"), correct = json.getBoolean("correct"),
                                rating = json.getString("rating"), syncKey = key, vocabularyKey = json.getString("vocabularyKey")))
                        }
                    }
                    "profile" -> if (!deleted) {
                        val json = checkNotNull(data)
                        preferences.restore(LearningPreferences(Language.from(json.getString("target")),
                            Language.from(json.getString("native")), Level.valueOf(json.getString("level")),
                            json.getDouble("fontSize").toFloat(), json.getBoolean("dark")))
                    }
                    else -> throw IOException("Unsupported learning record.")
                }
            }
            val state = checkNotNull(dao.syncState())
            val cursor = response.getLong("cursor")
            require(cursor >= state.cursor)
            dao.saveSyncState(state.copy(cursor = cursor))
        }
    }

    /** Old synchronized `tl` course records remain usable, while unrelated pairs stay untouched on the server. */
    private fun normalizeRecord(kind: String, key: String, deleted: Boolean, source: JSONObject?): Pair<String, JSONObject?>? {
        val data = source?.let { JSONObject(it.toString()) }
        return when (kind) {
            "vocabulary" -> {
                val parts = key.split(':', limit = 4)
                if (parts.size != 4 || parts[0] !in setOf("tl", "fil") || parts[1] != "id") null
                else {
                    data?.put("language", "fil")?.put("reference", "id")
                    listOf("fil", "id", parts[2], parts[3]).joinToString(":") to data
                }
            }
            "progress" -> {
                val parts = key.split(':')
                if (parts.size != 2 || parts[1] !in setOf("tl", "fil")) null
                else {
                    data?.put("language", "fil")
                    "${parts[0]}:fil" to data
                }
            }
            "review" -> {
                if (deleted) key to null
                else if (data == null || data.optString("language") !in setOf("tl", "fil") || data.optString("reference") != "id") null
                else {
                    data.put("language", "fil").put("reference", "id")
                    data.put("vocabularyKey", data.getString("vocabularyKey").replaceFirst("tl:id:", "fil:id:"))
                    key to data
                }
            }
            "profile" -> {
                data?.put("target", "fil")?.put("native", "id")?.put("level", Level.BEGINNER.name)
                key to data
            }
            else -> key to data
        }
    }

}
