package com.rolang.app.network

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.rolang.app.data.*
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GitHubContentRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: LearningDatabase
    private lateinit var server: MockWebServer
    private lateinit var repository: StoryRepository
    private val responses = linkedMapOf<String, MockResponse>()
    private val lessonId = "elementary-tagalog-1-lesson-01"

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java)
            .allowMainThreadQueries().build()
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                responses[request.path.orEmpty().substringBefore('?')] ?: MockResponse().setResponseCode(404)
        }
        server.start()
        repository = StoryRepository(context, database,
            GitHubContentSource(context, server.url("/"), OkHttpClient.Builder().build()))
        installValidContent()
    }

    @After fun close() {
        server.shutdown()
        database.close()
        context.filesDir.resolve("lesson-assets/$lessonId").deleteRecursively()
    }

    @Test fun catalogAndLessonLoadFromGitHubAndRemainReadableAfterHttpFailure() = runBlocking {
        repository.refreshCatalog()
        assertEquals(1, database.dao().allCatalog().count { it.id == lessonId && it.available })
        val loaded = repository.open(lessonId)
        assertEquals("Hari Pertama Kelas", loaded.title(com.rolang.app.domain.Language.INDONESIAN))
        assertEquals(2, loaded.lessonSections.size)

        val metadata = JSONObject(database.dao().catalogStory(lessonId)!!.content)
            .put("contentHash", "0".repeat(64))
        database.dao().cacheCatalog(listOf(StoryCatalogEntity(lessonId, metadata.toString())))
        responses["/lesson/elementary-tagalog-1/01.json"] = MockResponse().setResponseCode(503)
        assertEquals(loaded.title(com.rolang.app.domain.Language.INDONESIAN), repository.open(lessonId).title(com.rolang.app.domain.Language.INDONESIAN))
        assertNotNull(database.dao().cachedStory(lessonId))
    }

    @Test fun malformedRemoteCatalogNeverReplacesTheLastValidCatalog() = runBlocking {
        repository.refreshCatalog()
        val before = database.dao().catalogStory(lessonId)
        val broken = "{not-json".toByteArray()
        val manifest = JSONObject()
            .put("schemaVersion", 1)
            .put("lessons", JSONObject().put("index", "lesson/index.json").put("sha256", GitHubContentSource.sha256(broken)))
        responses["/manifest.json"] = json(manifest)
        responses["/lesson/index.json"] = MockResponse().setBody(String(broken))
        runCatching { repository.refreshCatalog() }.onSuccess { fail("Malformed JSON must fail") }
        assertEquals(before, database.dao().catalogStory(lessonId))
        assertEquals(CatalogStatus.INVALID_RESPONSE, repository.catalogState.value.status)
    }

    @Test fun saveOfflineDownloadsEveryAssetAndMissingGitHubAssetIsRecoverable() = runBlocking {
        repository.refreshCatalog()
        val lesson = repository.open(lessonId)
        repository.saveOffline(lessonId)
        assertEquals("COMPLETE", database.dao().download(lessonId)!!.status)
        assertTrue(repository.assetModel(lesson, "assets/picture.jpg") is java.io.File)

        repository.removeOffline(lessonId)
        responses["/lesson/elementary-tagalog-1/assets/picture.jpg"] = MockResponse().setResponseCode(404)
        runCatching { repository.saveOffline(lessonId) }.onSuccess { fail("Missing asset must fail safely") }
        assertEquals("FAILED", database.dao().download(lessonId)!!.status)
        assertNotNull(repository.cached(lessonId))
    }

    private fun installValidContent() {
        val block = JSONObject().put("type", "text")
            .put("text", text("Ini adalah penjelasan.", "Magandang umaga."))
        val section = JSONObject().put("id", "overview").put("type", "OVERVIEW")
            .put("title", text("Gambaran umum", "Pangkalahatang-ideya"))
            .put("blocks", JSONArray().put(block))
        val lesson = JSONObject()
            .put("schemaVersion", 1)
            .put("id", lessonId)
            .put("courseId", "elementary-tagalog-1")
            .put("unitId", "unit-01-self")
            .put("order", 1)
            .put("level", "BEGINNER")
            .put("title", localized("Hari Pertama Kelas", "Unang Araw ng Klase"))
            .put("summary", localized("Belajar sapaan kelas", "Mga bati sa klase"))
            .put("objectives", JSONArray().put(text("Menyapa teman", "Bumati sa kaibigan")))
            .put("sentences", JSONArray().put(localized("Magandang umaga", "Magandang umaga")))
            .put("assets", JSONArray().put("assets/picture.jpg"))
            .put("sections", JSONArray().put(section))
        val lessonBytes = lesson.toString().toByteArray()
        val unitDocument = JSONObject()
            .put("schemaVersion", 1)
            .put("id", "unit-01-self")
            .put("courseId", "elementary-tagalog-1")
            .put("title", localized("Tentang Diri", "Tungkol sa Sarili"))
            .put("assets", JSONArray())
            .put("sections", JSONArray().put(JSONObject()
                .put("id", "unit-overview").put("type", "OVERVIEW")
                .put("title", text("Pengantar unit", "Tungkol sa Sarili"))
                .put("blocks", JSONArray().put(block))))
        val unitBytes = unitDocument.toString().toByteArray()
        val course = JSONObject()
            .put("schemaVersion", 1)
            .put("id", "elementary-tagalog-1")
            .put("level", "BEGINNER")
            .put("languagePair", JSONObject().put("reference", "id").put("target", "fil"))
            .put("title", localized("Elementary Tagalog 1", "Elementary Tagalog 1"))
            .put("description", localized("Kursus buku teks", "Kursong aklat"))
            .put("units", JSONArray().put(JSONObject().put("id", "unit-01-self")
                .put("title", localized("Tentang Diri", "Tungkol sa Sarili"))
                .put("path", "units/01.json").put("sha256", GitHubContentSource.sha256(unitBytes))
                .put("lessons", JSONArray().put(lessonId))))
            .put("lessons", JSONArray().put(JSONObject()
                .put("id", lessonId).put("unitId", "unit-01-self").put("order", 1).put("path", "01.json")
                .put("sha256", GitHubContentSource.sha256(lessonBytes))
                .put("title", localized("Hari Pertama Kelas", "Unang Araw ng Klase"))
                .put("summary", localized("Belajar sapaan kelas", "Mga bati sa klase"))
                .put("objectives", JSONArray().put(text("Menyapa teman", "Bumati sa kaibigan")))
                .put("assets", JSONArray().put("assets/picture.jpg"))))
        val courseBytes = course.toString().toByteArray()
        val index = JSONObject().put("schemaVersion", 1).put("courses", JSONArray().put(JSONObject()
            .put("id", "elementary-tagalog-1").put("path", "elementary-tagalog-1/index.json")
            .put("sha256", GitHubContentSource.sha256(courseBytes))))
        val indexBytes = index.toString().toByteArray()
        val manifest = JSONObject().put("schemaVersion", 1)
            .put("lessons", JSONObject().put("index", "lesson/index.json").put("sha256", GitHubContentSource.sha256(indexBytes)))
        responses["/manifest.json"] = json(manifest)
        responses["/lesson/index.json"] = MockResponse().setBody(String(indexBytes))
        responses["/lesson/elementary-tagalog-1/index.json"] = MockResponse().setBody(String(courseBytes))
        responses["/lesson/elementary-tagalog-1/units/01.json"] = MockResponse().setBody(String(unitBytes))
        responses["/lesson/elementary-tagalog-1/01.json"] = MockResponse().setBody(String(lessonBytes))
        responses["/lesson/elementary-tagalog-1/assets/picture.jpg"] = MockResponse().setBody("image-bytes")
    }

    private fun localized(id: String, fil: String) = JSONObject().put("id", id).put("fil", fil)
    private fun text(id: String, fil: String) = JSONObject().put("id", id).put("sourceLanguage", "fil")
        .put("sourceText", fil).put("tagalog", fil)
    private fun json(value: JSONObject) = MockResponse().setHeader("Content-Type", "application/json").setBody(value.toString())
}
