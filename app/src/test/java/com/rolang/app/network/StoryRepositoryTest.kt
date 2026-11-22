package com.rolang.app.network

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rolang.app.TestCorpus
import com.rolang.app.data.*
import com.rolang.app.domain.Language
import com.rolang.app.domain.Words
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class StoryRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: LearningDatabase
    private lateinit var server: MockWebServer
    private lateinit var repository: StoryRepository
    private lateinit var detail: JSONObject
    private lateinit var chapters: List<JSONObject>
    private val detailCalls = AtomicInteger()
    private val chapterCalls = AtomicInteger()
    @Volatile private var offline = false
    @Volatile private var failSecond = false
    @Volatile private var delayChapter = false
    private val name = "story-cache-test.db"

    @Before fun setup() {
        context.deleteDatabase(name)
        database = Room.databaseBuilder(context, LearningDatabase::class.java, name).build()
        server = MockWebServer()
        val corpus = TestCorpus.json(context)
        detail = JSONObject(corpus.getJSONArray("stories").getJSONObject(0).toString())
            .put("version", "1").put("updatedAt", "2026-01-01T00:00:00Z").put("vocabulary", corpus.getJSONArray("lexicon"))
        val original = detail.getJSONArray("sentences")
        chapters = (0..1).map { index -> JSONObject().put("id", "chapter$index").put("storyId", detail.getString("id"))
            .put("title", detail.getJSONObject("title")).put("version", "chapter-$index-v1").put("storyVersion", "1")
            .put("sentences", JSONArray((index * 2 until index * 2 + 2).map { original.getJSONObject(it) }))
            .put("vocabulary", corpus.getJSONArray("lexicon")) }
        updateMetadata()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (offline) return MockResponse().setResponseCode(503).setBody("{}")
                val path = request.path.orEmpty()
                return when {
                    path.startsWith("/api/v1/stories?") -> json(JSONObject().put("stories", JSONArray().put(JSONObject(StoryRepository.metadata(detail))))
                        .put("next", JSONObject.NULL))
                    path.contains("/chapters/") -> {
                        chapterCalls.incrementAndGet()
                        val index = path.substringAfterLast('/').removePrefix("chapter").toInt()
                        if (failSecond && index == 1) MockResponse().setResponseCode(503).setBody("{}")
                        else json(chapters[index]).apply { if (delayChapter) setBodyDelay(1, java.util.concurrent.TimeUnit.SECONDS) }
                    }
                    else -> {
                        detailCalls.incrementAndGet()
                        if (request.getHeader("If-None-Match") == "\"${detail.getString("version")}\"") MockResponse().setResponseCode(304)
                        else json(detail)
                    }
                }
            }
        }
        server.start()
        reopenRepository()
    }
    private fun json(value: JSONObject) = MockResponse().setHeader("Content-Type", "application/json").setBody(value.toString())
    private fun updateMetadata() {
        detail.put("chapters", JSONArray(chapters.mapIndexed { index, c -> JSONObject().put("id", c.getString("id"))
            .put("title", c.getJSONObject("title")).put("version", c.getString("version")).put("order", index + 1)
            .put("sentenceCount", c.getJSONArray("sentences").length()) }))
    }
    private fun reopenRepository() {
        repository = StoryRepository(database, StoryRemoteSource {
            ServerConfig(serverBaseUrl = server.url("/").toString(), onlineEnabled = true, requestTimeoutMs = 3000)
        })
    }
    @After fun close() { server.shutdown(); database.close(); context.deleteDatabase(name) }

    @Test fun catalogsStayLightweightAndOpeningAutomaticallyPersistsTextDictionaryAndExercises() = runBlocking {
        repository.refreshCatalog()
        assertEquals(0, database.dao().storyCount())
        assertEquals(0, chapterCalls.get())
        assertFalse(JSONObject(repository.catalog.first().single().content).has("sentences"))
        val story = repository.open(detail.getString("id"))
        assertEquals(4, story.sentences.size)
        assertEquals(2, story.chapters.size)
        assertEquals(6, story.exercises.size)
        val firstDownloads = chapterCalls.get()
        repository.open(story.id)
        assertEquals(firstDownloads, chapterCalls.get())
        assertEquals(2, detailCalls.get())
        val learning = LearningRepository(database)
        val word = Words.tokens(story.sentence(0, Language.FILIPINO)).first().text
        assertTrue(learning.lookup(story, 0, word, Language.FILIPINO, Language.INDONESIAN).translation.isNotBlank())
        database.close()
        database = Room.databaseBuilder(context, LearningDatabase::class.java, name).build()
        reopenRepository()
        offline = true
        assertEquals(story, repository.open(story.id))
        assertNotNull(repository.cachedChapter(story.id, "chapter0"))
        try { repository.refreshCatalog(); fail() } catch (_: ApiException) { }
        assertEquals(1, repository.catalog.first().size)
    }

    @Test fun interruptedManualDownloadsReuseSavedChaptersAcrossRestartAndCanBeRemovedWithoutLosingLearning() = runBlocking {
        val id = detail.getString("id")
        val queued = repository.queueDownload(id)
        assertEquals(queued.requestId, repository.queueDownload(id).requestId)
        failSecond = true
        try { repository.refresh(id, queued.requestId); fail() } catch (_: ApiException) { }
        repository.failDownload(id, queued.requestId, true)
        assertEquals(1, database.dao().download(id)!!.completed)
        assertNull(repository.cached(id))
        database.close()
        database = Room.databaseBuilder(context, LearningDatabase::class.java, name).build()
        reopenRepository()
        failSecond = false
        val story = repository.refresh(id, queued.requestId)
        assertEquals(3, chapterCalls.get()) // first chapter was fetched only once
        assertEquals("COMPLETE", database.dao().download(id)!!.status)
        val learning = LearningRepository(database)
        val token = Words.tokens(story.sentence(0, Language.FILIPINO)).first().text
        learning.save(story, learning.lookup(story, 0, token, Language.FILIPINO, Language.INDONESIAN), Language.FILIPINO, Language.INDONESIAN, 1000)
        learning.reading(story, Language.FILIPINO, 1, true)
        offline = true
        assertEquals(story, repository.open(id))
        assertEquals("COMPLETE", database.dao().download(id)!!.status)
        repository.remove(id)
        assertNull(repository.cached(id))
        assertNull(repository.cachedChapter(id, "chapter0"))
        assertNull(database.dao().download(id))
        assertEquals(1, database.dao().allVocabulary().size)
        assertTrue(database.dao().allReadings().single().completed)
        assertEquals(1, database.dao().allCatalog().size)
    }

    @Test fun serverUpdatesReuseUnchangedChaptersAndInvalidOrMixedRevisionsKeepTheOldSnapshot() = runBlocking {
        val id = detail.getString("id")
        val first = repository.open(id)
        detail.put("version", "2")
        chapters = chapters.mapIndexed { index, chapter -> JSONObject(chapter.toString()).put("storyVersion", "2").apply {
            if (index == 1) put("version", "chapter-1-v2").getJSONArray("sentences").getJSONObject(0).put("fil", first.sentence(0, Language.FILIPINO))
        } }
        updateMetadata()
        repository.refreshCatalog()
        repository.refreshUpdatedCaches()
        val latest = repository.cached(id)!!
        assertNotEquals(first.sentences, latest.sentences)
        assertEquals(3, chapterCalls.get())
        detail.put("version", "3")
        chapters[1].put("version", "chapter-1-v3").put("storyVersion", "4")
        updateMetadata()
        assertEquals(latest, repository.open(id))
        assertEquals("2", StoryRepository.version(JSONObject(database.dao().cachedStory(id)!!.content)))
        chapters[1].put("storyVersion", "3").put("vocabulary", JSONArray())
        assertEquals(latest, repository.open(id))
        assertEquals("2", StoryRepository.version(JSONObject(database.dao().cachedStory(id)!!.content)))
    }

    @Test fun cancellationAndConcurrentRequestsCannotCompleteDuplicateDownloadGenerations() = runBlocking {
        val id = detail.getString("id")
        val request = repository.queueDownload(id)
        delayChapter = true
        val downloading = launch(Dispatchers.IO) { repository.refresh(id, request.requestId) }
        withTimeout(5000) { while (chapterCalls.get() == 0) delay(10) }
        repository.cancelDownload(id)
        downloading.cancelAndJoin()
        assertEquals("CANCELLED", database.dao().download(id)!!.status)
        assertNull(repository.cached(id))
        delayChapter = false
        val retry = repository.queueDownload(id)
        assertNotEquals(request.requestId, retry.requestId)
        coroutineScope { awaitAll(async { repository.refresh(id, retry.requestId) }, async { repository.open(id) }) }
        assertEquals("COMPLETE", database.dao().download(id)!!.status)
        assertTrue(chapterCalls.get() <= 3)
    }

    @Test fun chapterSpecificExercisesSpeakersAndGuidesRemainAvailableOffline() = runBlocking {
        val existing = detail.getJSONArray("exercises").getJSONObject(0)
        val extra = JSONObject(existing.toString()).put("id", "chapter_extra")
        chapters[0].put("exercises", JSONArray().put(existing)).put("speakers", JSONArray().put("Maya").put("Joel"))
        chapters[1].put("exercises", JSONArray().put(extra)).put("pronunciation", JSONObject().put("id", "Panduan tambahan").put("fil", "Dagdag na gabay"))
        val story = repository.open(detail.getString("id"))
        assertEquals(7, story.exercises.size)
        assertEquals(listOf("Maya", "Joel", null, null), story.speakers)
        assertTrue(story.pronunciation.getValue("id").contains("Panduan tambahan"))
        offline = true
        assertEquals(story, repository.open(story.id))
    }

    @Test fun unpublishedStoriesKeepTheirCompleteOfflineSnapshot() = runBlocking {
        val id = detail.getString("id")
        val request = repository.queueDownload(id)
        val cached = repository.refresh(id, request.requestId)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = if (request.path!!.contains("?limit="))
                json(JSONObject().put("stories", JSONArray()).put("next", JSONObject.NULL))
                else MockResponse().setResponseCode(404).setBody("{}")
        }
        repository.refreshCatalog()
        assertFalse(database.dao().allCatalog().single().available)
        assertEquals(1, repository.catalog.first().size)
        assertEquals(cached, repository.open(id))
        assertEquals("COMPLETE", database.dao().download(id)!!.status)
    }

    @Test fun freshInitializationSeedsBundledLibraryAndEmptyServerDoesNotRemoveIt() = runBlocking {
        LearningRepository(database).initialize(context)
        assertEquals(21, database.dao().storyCount())
        assertEquals(21, database.dao().allCatalog().size)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = json(JSONObject().put("stories", JSONArray()).put("next", JSONObject.NULL))
        }
        repository.refreshCatalog()
        assertFalse(repository.catalogState.value.loading)
        assertNull(repository.catalogState.value.error)
        assertEquals(CatalogStatus.EMPTY, repository.catalogState.value.status)
        assertEquals(21, repository.catalog.first().size)
    }

    @Test fun invalidCatalogResponsesBecomeRecoverableStateAndNeverReplaceCachedMetadata() = runBlocking {
        repository.refreshCatalog()
        val before = database.dao().allCatalog()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = json(JSONObject().put("stories", "invalid").put("next", JSONObject.NULL))
        }
        runCatching { repository.refreshCatalog() }
        assertEquals(CatalogStatus.INVALID_RESPONSE, repository.catalogState.value.status)
        assertNotNull(repository.catalogState.value.error)
        assertEquals(before, database.dao().allCatalog())
    }
}
