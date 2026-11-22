package com.rolang.app.network

import com.rolang.app.TestCorpus

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rolang.app.data.*
import com.rolang.app.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Optional full Android client -> actual Node API -> PostgreSQL contract check. */
@RunWith(AndroidJUnit4::class)
class LiveServerTest {
    @Test fun twoDevicesExchangeVocabularyReadingReviewsAndDeletionsThroughTheActualBackend() = runBlocking {
        val origin = System.getProperty("rolang.integration.url")
        assumeTrue("Supply -ProlangIntegrationUrl for the dedicated local test server", origin != null)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = ServerConfig(serverBaseUrl = origin!!, onlineEnabled = true)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val databases = (1..2).map { Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java).build() }
        val files = (1..2).map { File(context.cacheDir, "live-${UUID.randomUUID()}.preferences_pb") }
        fun sessions() = object : Sessions {
            var value: AccountSession? = null
            override fun read() = value
            override fun save(session: AccountSession) { value = session }
            override fun clear() { value = null }
        }
        val engines = databases.mapIndexed { i, db -> SyncEngine(context, db,
            PreferencesRepository(PreferenceDataStoreFactory.create(scope = scope) { files[i] }), sessions()) { config } }
        try {
            databases.forEach { TestCorpus.seed(it.dao(), context) }
            val email = "android.${UUID.randomUUID()}@example.test"
            engines[0].signIn(email, "test-fixture-password", "Android contract test")
            engines[1].signIn(email, "test-fixture-password")
            val repository = LearningRepository(databases[0])
            val story = Corpus.story(repository.dao.stories().first().first())
            val token = Words.tokens(story.sentence(0, Language.FILIPINO)).first().text
            repository.save(story, repository.lookup(story, 0, token, Language.FILIPINO, Language.INDONESIAN),
                Language.FILIPINO, Language.INDONESIAN, System.currentTimeMillis())
            val word = repository.dao.allVocabulary().single()
            repository.reading(story, Language.FILIPINO, 1, true)
            assertTrue(repository.review(word, Rating.GOOD, true, System.currentTimeMillis() + 1))
            assertTrue(engines[0].synchronize())
            assertTrue(engines[1].synchronize())
            val secondWord = databases[1].dao().allVocabulary().single()
            assertEquals(word.word, secondWord.word)
            assertEquals(1, secondWord.repetitions)
            assertEquals(VocabularyStatus.LEARNING, secondWord.learningStatus())
            assertTrue(databases[1].dao().allReadings().single().completed)
            assertEquals(1, databases[1].dao().allReviews().size)
            assertTrue(databases[0].dao().pending().isEmpty())
            for (status in VocabularyStatus.entries) {
                val changedAt = System.currentTimeMillis()
                repository.status(word.id, status, changedAt)
                assertTrue(engines[0].synchronize())
                assertTrue(engines[1].synchronize())
                val remote = databases[1].dao().allVocabulary().single()
                assertEquals(status, remote.learningStatus())
                assertEquals(status == VocabularyStatus.KNOWN, remote.mastered)
                assertTrue(remote.statusChangedAt >= changedAt)
                assertEquals(1, databases[1].dao().allReviews().size)
            }
            repository.delete(word.id)
            engines[0].synchronize(); engines[1].synchronize()
            assertTrue(databases[1].dao().allVocabulary().isEmpty())
            assertEquals(1, databases[1].dao().allReviews().size)
            engines.forEach { it.signOut() }
        } finally {
            databases.forEach { it.close() }
            scope.coroutineContext.job.cancelAndJoin()
            files.forEach { it.delete() }
        }
    }
    @Test fun everyMigratedStoryCanBeReadAndReopenedOfflineThroughTheActualApi() = runBlocking {
        val origin = System.getProperty("rolang.integration.url")
        assumeTrue("Supply a disposable server with npm run import:corpus", origin != null)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "live-content-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, LearningDatabase::class.java, name).build()
        val config = ServerConfig(serverBaseUrl = origin!!, onlineEnabled = true, storyPageSize = 3)
        var stories = StoryRepository(db, StoryRemoteSource { config })
        try {
            stories.refreshCatalog()
            assertEquals(21, db.dao().allCatalog().size)
            assertEquals(0, db.dao().storyCount())
            val original = TestCorpus.load(context).first.map(Corpus::story)
            for (expected in original) {
                val actual = stories.open(expected.id)
                assertEquals(expected.sentences, actual.sentences)
                assertEquals(expected.exercises, actual.exercises.take(expected.exercises.size))
                assertTrue(actual.notes.containsAll(expected.notes))
                for (language in listOf("id", "fil"))
                    assertTrue(actual.focus.getValue(language).containsAll(expected.focus.getValue(language)))
                for ((language, guide) in expected.pronunciation)
                    assertTrue(actual.pronunciation.getValue(language).contains(guide))
                assertEquals(expected.speakers, actual.speakers)
            }
            assertEquals(21, db.dao().storyCount())
            db.close()
            db = Room.databaseBuilder(context, LearningDatabase::class.java, name).build()
            stories = StoryRepository(db, StoryRemoteSource { config.copy(onlineEnabled = false) })
            for (expected in original) assertEquals(expected.sentences, stories.open(expected.id).sentences)
        } finally { db.close(); context.deleteDatabase(name) }
    }

}
