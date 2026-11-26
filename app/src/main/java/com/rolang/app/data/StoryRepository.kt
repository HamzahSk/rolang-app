package com.rolang.app.data

import android.content.Context
import androidx.room.withTransaction
import com.rolang.app.diagnostics.DiagnosticLogger
import com.rolang.app.network.ContentHttpException
import com.rolang.app.network.ContentValidationException
import com.rolang.app.network.GitHubContentSource
import com.rolang.app.domain.Language
import com.rolang.app.domain.Words
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

enum class CatalogStatus { IDLE, LOADING, READY, EMPTY, OFFLINE, HTTP_ERROR, INVALID_RESPONSE }
data class CatalogState(val loading: Boolean = false, val error: String? = null,
    val status: CatalogStatus = if (loading) CatalogStatus.LOADING else CatalogStatus.IDLE)

class StoryRepository(
    private val context: Context,
    private val database: LearningDatabase,
    private val remote: GitHubContentSource = GitHubContentSource(context),
) {
    val dao = database.dao()
    val catalog = dao.catalog()
    val downloads = dao.downloads()
    private val catalogLock = Mutex()
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val _catalogState = MutableStateFlow(CatalogState())
    val catalogState = _catalogState.asStateFlow()

    private fun lock(id: String) = locks.getOrPut(id) { Mutex() }
    private fun parent(path: String) = path.substringBeforeLast('/', "").let { if (it.isBlank()) "" else "$it/" }
    private fun join(base: String, child: String): String {
        require(!child.startsWith('/') && child.split('/').none { it == ".." })
        return base + child
    }

    suspend fun refreshCatalog() = catalogLock.withLock {
        _catalogState.value = CatalogState(loading = true, status = CatalogStatus.LOADING)
        try {
            val manifest = remote.json("manifest.json").json
            require(manifest.getInt("schemaVersion") == 1)
            val lessonRef = manifest.getJSONObject("lessons")
            val lessonIndexPath = lessonRef.getString("index")
            val lessonIndex = remote.json(lessonIndexPath, lessonRef.getString("sha256")).json
            require(lessonIndex.getInt("schemaVersion") == 1)
            val catalog = mutableListOf<StoryCatalogEntity>()
            val ids = mutableSetOf<String>()
            val courses = lessonIndex.getJSONArray("courses")
            for (courseIndex in 0 until courses.length()) {
                val courseRef = courses.getJSONObject(courseIndex)
                val coursePath = join(parent(lessonIndexPath), courseRef.getString("path"))
                val course = remote.json(coursePath, courseRef.getString("sha256")).json
                require(course.getInt("schemaVersion") == 1)
                val pair = course.getJSONObject("languagePair")
                require(pair.getString("reference") == "id" && pair.getString("target") == "fil")
                val courseDir = parent(coursePath)
                val units = course.getJSONArray("units")
                val unitMetadata = (0 until units.length()).associate { index ->
                    val unit = units.getJSONObject(index)
                    unit.getString("id") to unit
                }
                val lessons = course.getJSONArray("lessons")
                for (index in 0 until lessons.length()) {
                    val lesson = lessons.getJSONObject(index)
                    val id = lesson.getString("id")
                    require(id.matches(Regex("[a-z0-9-]{1,100}")) && ids.add(id))
                    val unit = checkNotNull(unitMetadata[lesson.getString("unitId")])
                    val unitTitle = unit.getJSONObject("title")
                    val contentPath = join(courseDir, lesson.getString("path"))
                    val firstInUnit = unit.getJSONArray("lessons").optString(0) == id
                    val lessonHash = lesson.getString("sha256")
                    val unitHash = if (firstInUnit) unit.getString("sha256") else ""
                    val metadata = JSONObject()
                        .put("schemaVersion", 1)
                        .put("id", id)
                        .put("courseId", course.getString("id"))
                        .put("family", lesson.getString("unitId"))
                        .put("category", unitTitle.getString("id"))
                        .put("level", course.getString("level"))
                        .put("title", lesson.getJSONObject("title"))
                        .put("summary", lesson.getJSONObject("summary"))
                        .put("objectives", lesson.optJSONArray("objectives") ?: JSONArray())
                        .put("order", lesson.getInt("order"))
                        .put("kind", UnitKind.LESSON.name)
                        .put("image", lesson.optJSONArray("assets")?.optString(0).orEmpty())
                        .put("contentHash", listOf(lessonHash, unitHash).filter(String::isNotBlank).joinToString(":"))
                        .put("lessonHash", lessonHash)
                        .put("contentPath", contentPath)
                        .put("unitIntroductionPath", if (firstInUnit) join(courseDir, unit.getString("path")) else "")
                        .put("unitIntroductionHash", unitHash)
                        .put("assetBaseUrl", remote.resolve(courseDir).toString())
                        .put("courseTitle", course.getJSONObject("title"))
                        .put("courseDescription", course.getJSONObject("description"))
                        .put("unitTitle", unitTitle)
                        .put("assets", lesson.optJSONArray("assets") ?: JSONArray())
                    Corpus.story(StoryEntity(id, lesson.getString("unitId"), unitTitle.getString("id"),
                        course.getString("level"), metadata.toString(), remote = true))
                    catalog += StoryCatalogEntity(id, metadata.toString())
                }
            }
            val storyRef = manifest.getJSONObject("stories")
            val storyIndexPath = storyRef.getString("index")
            val storyIndex = remote.json(storyIndexPath, storyRef.getString("sha256")).json
            require(storyIndex.getInt("schemaVersion") == 1)
            val storyPair = storyIndex.getJSONObject("languagePair")
            require(storyPair.getString("reference") == "id" && storyPair.getString("target") == "fil")
            val storyDir = parent(storyIndexPath)
            val collectionId = storyIndex.getString("id")
            val collectionTitle = storyIndex.getJSONObject("title")
            val collectionDescription = storyIndex.getJSONObject("description")
            val stories = storyIndex.getJSONArray("stories")
            for (index in 0 until stories.length()) {
                val story = stories.getJSONObject(index)
                val id = story.getString("id")
                require(id.matches(Regex("[a-z0-9-]{1,100}")) && ids.add(id))
                val contentPath = join(storyDir, story.getString("path"))
                val contentHash = story.getString("sha256")
                val assets = story.optJSONArray("assets") ?: JSONArray()
                val metadata = JSONObject()
                    .put("schemaVersion", 1)
                    .put("id", id)
                    .put("collectionId", collectionId)
                    .put("family", story.getString("family"))
                    .put("category", story.getString("category"))
                    .put("level", story.getString("level"))
                    .put("title", story.getJSONObject("title"))
                    .put("summary", story.getJSONObject("summary"))
                    .put("objectives", JSONArray())
                    .put("order", story.getInt("order"))
                    .put("sourceOrder", story.optInt("sourceOrder", story.getInt("order")))
                    .put("sentenceCount", story.getInt("sentenceCount"))
                    .put("kind", UnitKind.STORY.name)
                    .put("image", story.optString("image", assets.optString(0)))
                    .put("contentHash", contentHash)
                    .put("storyHash", contentHash)
                    .put("contentPath", contentPath)
                    .put("assetBaseUrl", remote.resolve(parent(contentPath)).toString())
                    .put("courseTitle", collectionTitle)
                    .put("courseDescription", collectionDescription)
                    .put("assets", assets)
                Corpus.story(StoryEntity(id, story.getString("family"), story.getString("category"),
                    story.getString("level"), metadata.toString(), remote = true))
                catalog += StoryCatalogEntity(id, metadata.toString())
            }
            database.withTransaction {
                val obsoleteRemote = dao.allCatalog().filter { item ->
                    item.id !in ids && runCatching {
                        JSONObject(item.content).optString("contentPath").isNotBlank()
                    }.getOrDefault(false)
                }.map { it.id }
                if (obsoleteRemote.isNotEmpty()) dao.hideCatalog(obsoleteRemote)
                dao.cacheCatalog(catalog)
            }
            _catalogState.value = CatalogState(status = if (catalog.isEmpty()) CatalogStatus.EMPTY else CatalogStatus.READY)
            DiagnosticLogger.info("GITHUB_CONTENT", "Loaded ${catalog.size} static lesson and story entries from GitHub Raw.")
        } catch (cancelled: CancellationException) {
            _catalogState.value = CatalogState()
            throw cancelled
        } catch (error: Exception) {
            _catalogState.value = failureState(error)
            DiagnosticLogger.error("GITHUB_CONTENT", "Static lesson catalog refresh failed; cached content was retained.", error)
            throw error
        }
    }

    private fun failureState(error: Exception) = when (error) {
        is ContentHttpException -> CatalogState(
            error = "GitHub mengembalikan HTTP ${error.status}. Pelajaran tersimpan tetap tersedia.",
            status = CatalogStatus.HTTP_ERROR,
        )
        is ContentValidationException, is JSONException, is IllegalArgumentException, is IllegalStateException -> CatalogState(
            error = "Katalog GitHub tidak valid. Salinan terakhir yang aman tetap digunakan.",
            status = CatalogStatus.INVALID_RESPONSE,
        )
        is IOException -> CatalogState(
            error = "GitHub tidak dapat dijangkau. Pelajaran tersimpan tetap tersedia luring.",
            status = CatalogStatus.OFFLINE,
        )
        else -> CatalogState(
            error = "Pustaka GitHub sedang tidak tersedia. Pelajaran tersimpan tetap aman.",
            status = CatalogStatus.HTTP_ERROR,
        )
    }

    suspend fun cached(id: String): Story? = withContext(Dispatchers.IO) {
        dao.cachedStory(id)?.let(Corpus::story)
    }

    suspend fun open(id: String): Story = lock(id).withLock {
        val cached = cached(id)
        val metadata = dao.catalogStory(id)?.let { JSONObject(it.content) }
        if (metadata == null || metadata.optString("contentPath").isBlank()) {
            return@withLock cached ?: throw IOException("Materi lokal tidak ditemukan")
        }
        val expectedHash = metadata.getString("contentHash")
        val kind = runCatching { UnitKind.valueOf(metadata.optString("kind", UnitKind.STORY.name)) }
            .getOrDefault(UnitKind.STORY)
        if (cached?.contentHash == expectedHash && when (kind) {
                UnitKind.LESSON -> cached.lessonSections.isNotEmpty()
                else -> cached.sentences.isNotEmpty()
            }) return@withLock cached
        try {
            val documentHash = if (kind == UnitKind.LESSON) metadata.getString("lessonHash") else metadata.getString("storyHash")
            val document = remote.json(metadata.getString("contentPath"), documentHash).json
            require(document.getInt("schemaVersion") == 1 && document.getString("id") == id)
            if (kind == UnitKind.LESSON) {
                require(document.getJSONArray("sections").length() > 0)
                val unitPath = metadata.optString("unitIntroductionPath")
                if (unitPath.isNotBlank()) {
                    val unit = remote.json(unitPath, metadata.getString("unitIntroductionHash")).json
                    require(unit.getInt("schemaVersion") == 1 && unit.getString("courseId") == document.getString("courseId"))
                    val mergedSections = JSONArray()
                    unit.getJSONArray("sections").let { sections ->
                        (0 until sections.length()).forEach { mergedSections.put(sections.get(it)) }
                    }
                    document.getJSONArray("sections").let { sections ->
                        (0 until sections.length()).forEach { mergedSections.put(sections.get(it)) }
                    }
                    document.put("sections", mergedSections)
                    val assets = linkedSetOf<String>()
                    for (source in listOf(unit.optJSONArray("assets"), document.optJSONArray("assets"))) {
                        if (source != null) (0 until source.length()).forEach { assets += source.getString(it) }
                    }
                    document.put("assets", JSONArray(assets.toList()))
                }
            } else {
                require(document.getJSONArray("sentences").length() > 0)
                require(((document.optJSONArray("questions") ?: document.optJSONArray("exercises"))?.length() ?: 0) > 0)
            }
            listOf("family", "category", "kind", "contentHash", "contentPath", "assetBaseUrl",
                "courseTitle", "courseDescription", "unitTitle", "collectionId").forEach { field ->
                if (metadata.has(field)) document.put(field, metadata.get(field))
            }
            document.put("image", metadata.optString("image"))
            val entity = StoryEntity(id, metadata.getString("family"), metadata.getString("category"),
                metadata.getString("level"), document.toString(), remote = true, available = true)
            val parsed = Corpus.story(entity)
            if (kind == UnitKind.LESSON) require(parsed.lessonSections.isNotEmpty())
            else require(parsed.sentences.isNotEmpty() && parsed.exercises.isNotEmpty())
            val storyLexicon = if (kind == UnitKind.LESSON) emptyList() else document.optJSONArray("vocabulary")?.let { words ->
                (0 until words.length()).map { index ->
                    val word = words.getJSONObject(index)
                    val language = Language.from(word.getString("language"))
                    require(language == Language.FILIPINO)
                    listOf("translations", "meanings", "notes").forEach { field ->
                        val values = word.getJSONObject(field)
                        require(values.getString("id").isNotBlank() && values.getString("fil").isNotBlank())
                    }
                    StoryLexemeEntity(id, language.code, Words.key(word.getString("word")), word.toString())
                }
            }.orEmpty()
            database.withTransaction {
                dao.seedStories(listOf(entity))
                if (kind != UnitKind.LESSON) {
                    dao.removeStoryLexicon(id)
                    dao.cacheStoryLexicon(storyLexicon)
                }
            }
            parsed
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { cached ?: throw error }
    }

    suspend fun saveOffline(id: String) {
        val story = open(id)
        lock(id).withLock {
        require(story.contentPath.isNotBlank())
        val requestId = java.util.UUID.randomUUID().toString()
        dao.saveDownload(StoryDownloadEntity(id, status = "DOWNLOADING", total = story.assets.size,
            version = story.contentHash, requestId = requestId))
        try {
            story.assets.forEachIndexed { index, asset ->
                val url = resolveAssetUrl(story, asset)
                val bytes = remote.bytes(url)
                val target = assetFile(story.id, asset)
                target.parentFile?.mkdirs()
                val temporary = File(target.parentFile, "${target.name}.part")
                temporary.writeBytes(bytes)
                check(temporary.renameTo(target) || run { temporary.copyTo(target, overwrite = true); temporary.delete() })
                dao.saveDownload(StoryDownloadEntity(id, status = "DOWNLOADING", completed = index + 1,
                    total = story.assets.size, version = story.contentHash, requestId = requestId))
            }
            dao.saveDownload(StoryDownloadEntity(id, status = "COMPLETE", completed = story.assets.size,
                total = story.assets.size, version = story.contentHash, requestId = requestId))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            val current = dao.download(id)
            dao.saveDownload((current ?: StoryDownloadEntity(id, requestId = requestId)).copy(
                status = "FAILED", error = "Sebagian aset belum tersimpan. Materi lama tetap aman."))
            throw error
        }
        }
    }

    suspend fun removeOffline(id: String) = withContext(Dispatchers.IO) {
        assetRoot(id).deleteRecursively()
        dao.removeDownload(id)
    }

    fun assetModel(story: Story, relative: String): Any {
        val file = assetFile(story.id, relative)
        return if (file.isFile) file else resolveAssetUrl(story, relative).toString()
    }

    private fun resolveAssetUrl(story: Story, relative: String) =
        checkNotNull(story.assetBaseUrl.toHttpUrlOrNull()?.resolve(relative)) { "Invalid lesson asset URL" }

    private fun assetRoot(id: String) = File(context.filesDir, "lesson-assets/$id")
    private fun assetFile(id: String, relative: String): File {
        val clean = relative.replace('\\', '/')
        require(clean.isNotBlank() && !clean.startsWith('/') && clean.split('/').none { it == ".." })
        val root = assetRoot(id).canonicalFile
        val file = File(root, clean).canonicalFile
        require(file.path.startsWith(root.path + File.separator))
        return file
    }
}
