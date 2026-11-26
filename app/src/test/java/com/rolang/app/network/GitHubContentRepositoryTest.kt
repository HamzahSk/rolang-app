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
    private val storyId = "book1-story-01-test-rabbit"

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
        context.filesDir.resolve("lesson-assets/$storyId").deleteRecursively()
    }

    @Test fun catalogAndLessonLoadFromGitHubAndRemainReadableAfterHttpFailure() = runBlocking {
        repository.refreshCatalog()
        assertEquals(1, database.dao().allCatalog().count { it.id == lessonId && it.available })
        val loaded = repository.open(lessonId)
        assertEquals("Hari Pertama Kelas", loaded.title(com.rolang.app.domain.Language.INDONESIAN))
        assertEquals(2, loaded.lessonSections.size)
        val semantic = loaded.lessonSections.last().blocks
        assertEquals("Magandang umaga.", semantic.first { it.type == "vocabulary" }.entries.single().term.sourceText)
        assertEquals("Ana", semantic.first { it.type == "dialogue" }.turns.single().speaker)
        assertEquals("question", semantic.first { it.type == "exercise" }.role)
        assertEquals("Magandang umaga.", semantic.first { it.type == "example" }.items.single().sourceText)
        val story = repository.open(storyId)
        assertEquals("The Test Rabbit", story.title(com.rolang.app.domain.Language.INDONESIAN))
        assertEquals(2, story.sentences.size)
        assertEquals(1, story.exercises.size)
        assertEquals("Kelinci itu selamat", story.exercises.single().answer(com.rolang.app.domain.Language.INDONESIAN))
        assertNotNull(database.dao().storyLexeme(storyId, "fil", "kuneho"))

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
        val story = repository.open(storyId)
        repository.saveOffline(storyId)
        assertEquals("COMPLETE", database.dao().download(storyId)!!.status)
        assertTrue(repository.assetModel(story, "assets/rabbit.jpg") is java.io.File)

        repository.removeOffline(lessonId)
        responses["/lesson/elementary-tagalog-1/assets/picture.jpg"] = MockResponse().setResponseCode(404)
        runCatching { repository.saveOffline(lessonId) }.onSuccess { fail("Missing asset must fail safely") }
        assertEquals("FAILED", database.dao().download(lessonId)!!.status)
        assertNotNull(repository.cached(lessonId))
    }

    private fun installValidContent() {
        val block = JSONObject().put("type", "text")
            .put("text", text("Ini adalah penjelasan.", "Magandang umaga."))
        val vocabulary = JSONObject().put("type", "vocabulary").put("entries", JSONArray().put(JSONObject()
            .put("term", text("Selamat pagi", "Magandang umaga."))
            .put("meaning", text("Selamat pagi", "Good morning"))))
        val dialogue = JSONObject().put("type", "dialogue").put("turns", JSONArray().put(JSONObject()
            .put("speaker", "Ana").put("speakerText", text("Ana", "Ana:"))
            .put("text", text("Selamat pagi", "Magandang umaga."))))
        val exercise = JSONObject().put("type", "exercise").put("role", "question")
            .put("text", text("Apa sapaan ini?", "Ano ang bati?"))
        val example = JSONObject().put("type", "example")
            .put("items", JSONArray().put(text("Selamat pagi", "Magandang umaga.")))
        val list = JSONObject().put("type", "list").put("style", "objectives")
            .put("items", JSONArray().put(text("Menyapa teman", "Bumati sa kaibigan")))
        val section = JSONObject().put("id", "overview").put("type", "INTRODUCTION")
            .put("title", text("Gambaran umum", "Pangkalahatang-ideya"))
            .put("blocks", JSONArray().put(block).put(vocabulary).put(dialogue).put(exercise).put(example).put(list))
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
        val storyQuestionId = "$storyId-q1"
        val storyDocument = JSONObject()
            .put("schemaVersion", 1)
            .put("id", storyId)
            .put("collectionId", "4000-essential-english-words-book-1")
            .put("sourceOrder", 1)
            .put("family", "4000-essential-english-words-book-1")
            .put("category", "Fabel")
            .put("level", "BEGINNER")
            .put("order", 1)
            .put("kind", "STORY")
            .put("image", "assets/rabbit.jpg")
            .put("title", localized("The Test Rabbit", "The Test Rabbit"))
            .put("summary", localized("Kelinci mencari jalan pulang", "Hinahanap ng kuneho ang daan pauwi"))
            .put("sentences", JSONArray()
                .put(localized("Seekor kelinci tersesat.", "Naliligaw ang isang kuneho."))
                .put(localized("Kelinci itu selamat.", "Nakaligtas ang kuneho.")))
            .put("sentenceCount", 2)
            .put("speakers", JSONArray().put(JSONObject.NULL).put(JSONObject.NULL))
            .put("questions", JSONArray().put(JSONObject()
                .put("id", storyQuestionId).put("type", "MULTIPLE_CHOICE")
                .put("prompt", localized("Apa yang terjadi?", "Ano ang nangyari?"))
                .put("choices", JSONObject()
                    .put("id", JSONArray().put("Kelinci itu selamat").put("Kelinci itu tidur"))
                    .put("fil", JSONArray().put("Nakaligtas ang kuneho").put("Natulog ang kuneho")))
                .put("speech", JSONObject().put("fil", "Ano ang nangyari?"))))
            .put("answers", JSONArray().put(JSONObject()
                .put("questionId", storyQuestionId)
                .put("answer", localized("Kelinci itu selamat", "Nakaligtas ang kuneho"))
                .put("explanation", localized("Jawaban ada di akhir.", "Nasa wakas ang sagot."))))
            .put("vocabulary", JSONArray().put(JSONObject()
                .put("language", "fil").put("word", "kuneho")
                .put("translations", localized("kelinci", "kuneho"))
                .put("meanings", localized("kelinci", "Hayop na kuneho."))
                .put("pos", "pangngalan").put("pronunciation", "kuneho")
                .put("notes", localized("Kata cerita.", "Salita sa kuwento."))))
            .put("assets", JSONArray().put("assets/rabbit.jpg"))
        val storyBytes = storyDocument.toString().toByteArray()
        val storyIndex = JSONObject()
            .put("schemaVersion", 1)
            .put("id", "4000-essential-english-words-book-1")
            .put("languagePair", JSONObject().put("reference", "id").put("target", "fil"))
            .put("title", localized("Book 1", "Book 1"))
            .put("description", localized("Cerita", "Mga kuwento"))
            .put("storyCount", 1)
            .put("stories", JSONArray().put(JSONObject()
                .put("id", storyId).put("collectionId", "4000-essential-english-words-book-1")
                .put("family", "4000-essential-english-words-book-1").put("category", "Fabel")
                .put("level", "BEGINNER").put("order", 1).put("sourceOrder", 1)
                .put("path", "4000-essential-english-words-book-1/01-test-rabbit.json")
                .put("sha256", GitHubContentSource.sha256(storyBytes)).put("sentenceCount", 2)
                .put("title", localized("The Test Rabbit", "The Test Rabbit"))
                .put("summary", localized("Kelinci mencari jalan pulang", "Hinahanap ng kuneho ang daan pauwi"))
                .put("image", "assets/rabbit.jpg").put("assets", JSONArray().put("assets/rabbit.jpg"))))
        val storyIndexBytes = storyIndex.toString().toByteArray()
        val manifest = JSONObject().put("schemaVersion", 1)
            .put("lessons", JSONObject().put("index", "lesson/index.json").put("sha256", GitHubContentSource.sha256(indexBytes)))
            .put("stories", JSONObject().put("index", "storie/index.json").put("sha256", GitHubContentSource.sha256(storyIndexBytes)))
        responses["/manifest.json"] = json(manifest)
        responses["/lesson/index.json"] = MockResponse().setBody(String(indexBytes))
        responses["/lesson/elementary-tagalog-1/index.json"] = MockResponse().setBody(String(courseBytes))
        responses["/lesson/elementary-tagalog-1/units/01.json"] = MockResponse().setBody(String(unitBytes))
        responses["/lesson/elementary-tagalog-1/01.json"] = MockResponse().setBody(String(lessonBytes))
        responses["/lesson/elementary-tagalog-1/assets/picture.jpg"] = MockResponse().setBody("image-bytes")
        responses["/storie/index.json"] = MockResponse().setBody(String(storyIndexBytes))
        responses["/storie/4000-essential-english-words-book-1/01-test-rabbit.json"] = MockResponse().setBody(String(storyBytes))
        responses["/storie/4000-essential-english-words-book-1/assets/rabbit.jpg"] = MockResponse().setBody("rabbit-image")
    }

    private fun localized(id: String, fil: String) = JSONObject().put("id", id).put("fil", fil)
    private fun text(id: String, fil: String) = JSONObject().put("id", id).put("sourceLanguage", "fil")
        .put("sourceText", fil).put("tagalog", fil)
    private fun json(value: JSONObject) = MockResponse().setHeader("Content-Type", "application/json").setBody(value.toString())
}
