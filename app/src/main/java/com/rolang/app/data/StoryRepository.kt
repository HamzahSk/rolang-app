package com.rolang.app.data

import androidx.room.withTransaction
import com.rolang.app.diagnostics.DiagnosticLogger
import com.rolang.app.network.ApiException
import com.rolang.app.network.ApiResponseException
import com.rolang.app.network.ServerConfigurationException
import com.rolang.app.network.StoryRemoteSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class ContentChanged : IOException("Cerita berubah saat diunduh. Percobaan ulang akan memakai versi terbaru.")
enum class CatalogStatus { IDLE, LOADING, READY, EMPTY, OFFLINE, SERVER_ERROR, INVALID_RESPONSE, CONFIGURATION_ERROR }
data class CatalogState(val loading: Boolean = false, val error: String? = null,
    val status: CatalogStatus = if (loading) CatalogStatus.LOADING else CatalogStatus.IDLE)

class StoryRepository(private val database: LearningDatabase, private val remote: StoryRemoteSource) {
    private val local = StoryLocalSource(database)
    val dao = local.dao
    private val catalogLock = Mutex()
    private val locks = ConcurrentHashMap<String, Mutex>()
    private fun lock(id: String) = locks.getOrPut(id) { Mutex() }
    private val _catalogState = MutableStateFlow(CatalogState())
    val catalogState = _catalogState.asStateFlow()
    val catalog = dao.catalog()
    val downloads = dao.downloads()

    suspend fun refreshCatalog() = catalogLock.withLock {
        withContext(Dispatchers.IO) {
            _catalogState.value = CatalogState(loading = true, status = CatalogStatus.LOADING)
            try {
                val seen = mutableSetOf<String>()
                val catalog = mutableListOf<StoryCatalogEntity>()
                var after: String? = null
                var pages = 0
                do {
                    val response = remote.catalog(after)
                    val array = response.getJSONArray("stories")
                    require(array.length() <= 100) { "The server returned too many catalog entries." }
                    require(++pages <= 100) { "The server returned too many catalog pages." }
                    val items = (0 until array.length()).map { index ->
                        val item = array.getJSONObject(index)
                        val id = item.getString("id")
                        require(id.matches(Regex("[a-zA-Z0-9_-]{1,80}")) && seen.add(id))
                        // Catalogs persist metadata only, even when talking to an older server.
                        val metadata = JSONObject(item.toString())
                        listOf("sentences", "vocabulary", "exercises", "notes", "focus", "speakers", "objectives", "pronunciation").forEach(metadata::remove)
                        require(item.has("version") || item.has("updatedAt"))
                        Corpus.story(StoryEntity(id, item.getString("family"), item.getString("category"), item.getString("level"), metadata.toString()))
                        StoryCatalogEntity(id, metadata.toString())
                    }
                    catalog.addAll(items)
                    val next = if (response.isNull("next")) null else response.getString("next")
                    require(next == null || next != after && (after == null || next > after!!))
                    after = next
                } while (after != null)
                database.withTransaction {
                    dao.cacheCatalog(catalog)
                    dao.cacheCatalog(dao.allCatalog().filter { it.id !in seen }.map { it.copy(available = false) })
                }
                val status = if (seen.isEmpty()) CatalogStatus.EMPTY else CatalogStatus.READY
                _catalogState.value = CatalogState(status = status)
                DiagnosticLogger.info("CATALOG", "Catalog refresh completed with ${seen.size} published items across $pages page(s).")
            } catch (cancelled: CancellationException) { _catalogState.value = CatalogState(); throw cancelled }
            catch (error: Exception) {
                _catalogState.value = failureState(error)
                DiagnosticLogger.error("CATALOG", "Catalog refresh failed.", error)
                throw error
            }
        }
    }

    private fun failureState(error: Exception): CatalogState = when (error) {
        is ServerConfigurationException, is IllegalStateException -> CatalogState(
            error = "Konfigurasi server tidak dapat digunakan. Materi tersimpan tetap tersedia luring.",
            status = CatalogStatus.CONFIGURATION_ERROR)
        is ApiException -> CatalogState(error = "Server mengembalikan kesalahan HTTP ${error.status}. Materi tersimpan tetap aman.",
            status = CatalogStatus.SERVER_ERROR)
        is ApiResponseException, is org.json.JSONException, is IllegalArgumentException -> CatalogState(
            error = "Respons server tidak dapat dibaca dengan aman. Materi tersimpan tetap tersedia.",
            status = CatalogStatus.INVALID_RESPONSE)
        is IOException -> CatalogState(error = "Tidak dapat terhubung ke internet. Materi tersimpan tetap tersedia luring.",
            status = CatalogStatus.OFFLINE)
        else -> CatalogState(error = "Pustaka daring sedang tidak tersedia. Materi tersimpan tetap tersedia luring.",
            status = CatalogStatus.SERVER_ERROR)
    }

    /** UI calls cached() first, so validation and synchronization never delay an existing reader. */
    suspend fun cached(id: String): Story? = local.story(id)?.let(Corpus::story)
    suspend fun cachedChapter(id: String, chapterId: String): Story? = withContext(Dispatchers.IO) {
        val metadata = dao.catalogStory(id)?.let { JSONObject(it.content) } ?: return@withContext null
        val chapter = dao.cachedChapter(id, chapterId)?.let { JSONObject(it.content) } ?: return@withContext null
        val content = JSONObject(metadata.toString())
        listOf("sentences", "speakers", "objectives", "notes", "exercises", "focus", "pronunciation").forEach { key ->
            if (chapter.has(key)) content.put(key, chapter.get(key))
        }
        content.put("chapters", JSONArray().put(JSONObject().put("id", chapterId).put("title", chapter.getJSONObject("title"))
            .put("sentenceCount", chapter.getJSONArray("sentences").length())))
        content.put("sentenceCount", chapter.getJSONArray("sentences").length())
        Corpus.story(StoryEntity(id, metadata.getString("family"), metadata.getString("category"), metadata.getString("level"), content.toString()))
    }
    suspend fun open(id: String): Story = try { refresh(id) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { cached(id) ?: throw error }

    suspend fun refresh(id: String, requestId: String? = null): Story = lock(id).withLock {
        withContext(Dispatchers.IO) {
            val previous = local.story(id)
            val oldVersion = previous?.let { runCatching { version(JSONObject(it.content)) }.getOrNull() }
            val response = remote.detail(id, oldVersion)
            val detail = if (response.optBoolean("notModified")) {
                checkNotNull(previous) { "Server returned an empty uncached story." }
                JSONObject(previous.content)
            } else response
            require(detail.getString("id") == id)
            val revision = version(detail)
            val chapters = detail.optJSONArray("chapters")
            val total = chapters?.length() ?: 1
            require(total in 1..100)
            suspend fun progress(completed: Int, status: String = "DOWNLOADING") {
                if (requestId == null) return
                database.withTransaction {
                    val current = dao.download(id) ?: throw CancellationException("Download removed")
                    if (current.requestId != requestId || current.status in setOf("CANCELLED", "FAILED")) throw CancellationException("Download cancelled")
                    dao.saveDownload(current.copy(status = status, completed = completed, total = total, version = revision, error = null))
                }
            }
            progress(0)
            if (previous != null && oldVersion == revision) {
                progress(total, "COMPLETE")
                return@withContext Corpus.story(previous)
            }
            val complete = JSONObject(detail.toString())
            val chapterIds = mutableListOf<String>()
            if (chapters != null) {
                val sentences = JSONArray()
                val speakers = JSONArray()
                val exercises = JSONArray()
                val notes = JSONArray()
                val objectives = JSONArray()
                val lexicon = linkedMapOf<String, JSONObject>()
                val focus = linkedMapOf<String, MutableList<String>>()
                val pronunciation = linkedMapOf<String, MutableList<String>>()
                // Keep original dictionaries and all learning fields (including practice metadata).
                detail.optJSONArray("vocabulary")?.let { words -> (0 until words.length()).forEach {
                    val w = words.getJSONObject(it); lexicon["${w.getString("language")}:${w.getString("word")}"] = w
                } }
                for (index in 0 until chapters.length()) {
                    val meta = chapters.getJSONObject(index)
                    val chapterId = meta.getString("id")
                    require(chapterId.matches(Regex("[a-zA-Z0-9_-]{1,80}")) && chapterId !in chapterIds)
                    chapterIds.add(chapterId)
                    val existing = dao.cachedChapter(id, chapterId)
                    val current = if (existing != null && existing.version == meta.getString("version")) JSONObject(existing.content)
                        else remote.chapter(id, chapterId).also { chapter ->
                            require(chapter.getString("storyId") == id && chapter.getString("id") == chapterId)
                            if (chapter.getString("storyVersion") != revision || chapter.getString("version") != meta.getString("version")) throw ContentChanged()
                            // Validate an individual chapter before durable resumable storage.
                            val check = JSONObject(detail.toString())
                            listOf("sentences", "exercises", "focus", "chapters", "sentenceCount").forEach(check::remove)
                            listOf("sentences", "exercises", "focus").forEach { key -> if (chapter.has(key)) check.put(key, chapter.get(key)) }
                            check.put("sentenceCount", chapter.getJSONArray("sentences").length())
                            check.put("vocabulary", chapter.getJSONArray("vocabulary"))
                            Corpus.parse(JSONObject().put("stories", JSONArray().put(check)).put("lexicon", chapter.getJSONArray("vocabulary")))
                            dao.cacheChapter(ChapterCacheEntity(id, chapterId, chapter.getString("version"), chapter.toString()))
                        }
                    fun append(field: String, target: JSONArray) { current.optJSONArray(field)?.let { array ->
                        (0 until array.length()).forEach { target.put(array.get(it)) }
                    } }
                    append("sentences", sentences)
                    val aligned = current.optJSONArray("speakers")
                    (0 until current.getJSONArray("sentences").length()).forEach { speakers.put(aligned?.opt(it) ?: JSONObject.NULL) }
                    append("exercises", exercises); append("notes", notes); append("objectives", objectives)
                    current.getJSONArray("vocabulary").let { words -> (0 until words.length()).forEach {
                        val w = words.getJSONObject(it); lexicon["${w.getString("language")}:${w.getString("word")}"] = w
                    } }
                    for (code in listOf("id", "fil")) {
                        val values = current.optJSONObject("focus")?.let { localized ->
                            localized.optJSONArray(code) ?: if (code == "fil") localized.optJSONArray("tl") else null
                        }
                        values?.let {
                            (0 until it.length()).forEach { index -> focus.getOrPut(code) { mutableListOf() }.add(it.getString(index)) }
                        }
                        current.optJSONObject("pronunciation")?.let { localized ->
                            localized.optString(code).ifBlank { if (code == "fil") localized.optString("tl") else "" }
                        }?.takeIf { it.isNotBlank() }?.let {
                            pronunciation.getOrPut(code) { mutableListOf() }.add(it)
                        }
                    }
                    progress(index + 1)
                }
                complete.put("sentences", sentences).put("speakers", if (detail.optJSONArray("speakers")?.length() == 0 && (0 until speakers.length()).all(speakers::isNull)) JSONArray() else speakers).put("vocabulary", JSONArray(lexicon.values.toList()))
                    .put("sentenceCount", sentences.length())
                fun merged(field: String, additions: JSONArray): JSONArray {
                    val result = JSONArray()
                    val seen = mutableSetOf<String>()
                    for (source in listOfNotNull(detail.optJSONArray(field), additions)) for (index in 0 until source.length()) {
                        val value = source.getJSONObject(index)
                        val key = if (field == "exercises") value.getString("id") else value.toString()
                        if (seen.add(key)) result.put(value)
                    }
                    return result
                }
                complete.put("exercises", merged("exercises", exercises)).put("notes", merged("notes", notes))
                    .put("objectives", merged("objectives", objectives))
                val combinedFocus = JSONObject()
                val combinedPronunciation = JSONObject()
                for (code in listOf("id", "fil")) {
                    val rootFocus = detail.optJSONObject("focus")?.let { localized ->
                        localized.optJSONArray(code) ?: if (code == "fil") localized.optJSONArray("tl") else null
                    }?.let { array -> (0 until array.length()).map(array::getString) }.orEmpty()
                    combinedFocus.put(code, JSONArray((rootFocus + focus[code].orEmpty()).distinct()))
                    val rootGuide = detail.optJSONObject("pronunciation")?.let { localized ->
                        localized.optString(code).ifBlank { if (code == "fil") localized.optString("tl") else "" }
                    }.orEmpty()
                    combinedPronunciation.put(code, (listOf(rootGuide) + pronunciation[code].orEmpty()).filter { it.isNotBlank() }.distinct().joinToString("\n\n"))
                }
                complete.put("focus", combinedFocus).put("pronunciation", combinedPronunciation)

            }
            // No partially downloaded revision replaces the previous complete reading snapshot.
            local.store(id, complete, chapterIds)
            dao.cacheCatalog(listOf(StoryCatalogEntity(id, metadata(complete))))
            if (requestId == null) database.withTransaction {
                val download = dao.download(id)
                if (download?.status == "COMPLETE") dao.saveDownload(download.copy(version = revision, completed = total, total = total))
            }
            progress(total, "COMPLETE")
            Corpus.story(checkNotNull(local.story(id)))
        }
    }
    suspend fun refreshUpdatedCaches() {
        val catalog = dao.allCatalog().filter { it.available }.associateBy { it.id }
        for (cached in dao.allStories()) {
            val latest = catalog[cached.id] ?: continue
            if (runCatching { version(JSONObject(cached.content)) }.getOrNull() != version(JSONObject(latest.content))) {
                refresh(cached.id)

            }
        }
    }
    suspend fun queueDownload(id: String): StoryDownloadEntity = database.withTransaction {
        val previous = dao.download(id)
        if (previous?.status in setOf("QUEUED", "DOWNLOADING", "RETRYING")) return@withTransaction previous!!
        StoryDownloadEntity(id).also { dao.saveDownload(it) }
    }
    suspend fun failDownload(id: String, requestId: String, retry: Boolean) = database.withTransaction {
        val current = dao.download(id) ?: return@withTransaction
        if (current.requestId == requestId && current.status != "CANCELLED") dao.saveDownload(current.copy(
            status = if (retry) "RETRYING" else "FAILED", error = "Unduhan terhenti. Bab yang tersimpan akan dipakai kembali saat mencoba ulang."))
    }
    suspend fun cancelDownload(id: String) = database.withTransaction {
        dao.download(id)?.let { dao.saveDownload(it.copy(status = "CANCELLED", error = null)) }
    }
    suspend fun remove(id: String) = lock(id).withLock { local.remove(id) }
    companion object {
        fun version(json: JSONObject): String = json.optString("version").ifBlank { json.getString("updatedAt") }
        fun metadata(json: JSONObject): String = JSONObject(json.toString()).apply {
            listOf("sentences", "vocabulary", "speakers", "exercises", "notes", "focus", "objectives", "pronunciation").forEach(::remove)
        }.toString()
    }
}
