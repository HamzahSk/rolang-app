package com.rolang.app

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rolang.app.data.*
import com.rolang.app.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

/** Runs on both a device and Robolectric with the same repository assertions. */
@RunWith(AndroidJUnit4::class)
class OfflineLearningTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: LearningDatabase
    private lateinit var repository: LearningRepository
    private val dbName = "offline-test.db"

    @Before fun setup() {
        context.deleteDatabase(dbName)
        reopen()
    }
    private fun reopen() {
        database = Room.databaseBuilder(context, LearningDatabase::class.java, dbName).build()
        repository = LearningRepository(database)
    }
    @After fun close() { database.close(); context.deleteDatabase(dbName) }

    @Test fun corpusCoversEveryWordAtEveryLevelAndLanguage() = runBlocking {
        TestCorpus.seed(repository.dao, context)
        TestCorpus.seed(repository.dao, context)
        val stories = repository.dao.stories().first().map(Corpus::story)
        assertEquals(21, stories.size)
        assertEquals((1..21).toList(), stories.sortedBy { it.order }.map { it.order })
        assertEquals(setOf(UnitKind.LESSON, UnitKind.DIALOGUE, UnitKind.STORY), stories.map { it.kind }.toSet())
        assertTrue(stories.all { it.level == Level.BEGINNER })
        val lessonTypes = setOf(ExerciseType.MULTIPLE_CHOICE, ExerciseType.VOCABULARY_TRANSLATION,
            ExerciseType.FILL_BLANK, ExerciseType.GRAMMAR, ExerciseType.SENTENCE_CONSTRUCTION)
        assertTrue(stories.filter { it.kind == UnitKind.LESSON }.all { story ->
            lessonTypes.all { required -> story.exercises.any { it.type == required } }
        })
        assertTrue(stories.filter { it.kind != UnitKind.LESSON }.all { story ->
            story.exercises.any { it.type == ExerciseType.COMPREHENSION } &&
                story.exercises.any { it.type == ExerciseType.MULTIPLE_CHOICE }
        })
        for (story in stories) {
            val target = Language.FILIPINO
            val native = Language.INDONESIAN
            for (index in story.sentences.indices) for (token in Words.tokens(story.sentence(index, target))) {
                val detail = repository.lookup(story, index, token.text, target, native)
                assertTrue(detail.translation.isNotBlank())
                assertTrue(detail.meaning.isNotBlank())
                assertTrue(detail.explanation.isNotBlank())
                assertEquals(story.sentence(index, native), detail.exampleTranslation)
            }
        }
    }

    @Test fun vocabularyReadingAndReviewSurviveReopeningAndCannotBeDoubleSubmitted() = runBlocking {
        TestCorpus.seed(repository.dao, context)
        val story = Corpus.story(repository.dao.stories().first().first())
        val target = Language.FILIPINO
        val native = Language.INDONESIAN
        val token = Words.tokens(story.sentence(0, target)).first().text
        val detail = repository.lookup(story, 0, token, target, native)
        val now = System.currentTimeMillis()
        assertTrue(repository.save(story, detail, target, native, now))
        assertFalse(repository.save(story, detail, target, native, now))
        repository.reading(story, target, 2, true)
        repository.reading(story, target, 1, false)
        repository.exerciseResult(story, target, 4, 5, mapOf("question" to "answer"), now)
        val original = repository.dao.vocabulary().first().first { it.reference == native.code }
        assertTrue(repository.review(original, Rating.GOOD, true, now + 1))
        assertFalse(repository.review(original, Rating.EASY, true, now + 2))
        database.close()
        reopen()
        val saved = repository.dao.vocabularyById(original.id)!!
        assertEquals(1, saved.repetitions)
        assertEquals(now + 1 + SpacedRepetition.DAY, saved.dueAt)
        assertEquals(detail.example, saved.example)
        assertEquals(1, repository.dao.reviews().first().size)
        assertEquals(1, repository.dao.vocabulary().first().size)
        assertTrue(repository.dao.readings().first().single().completed)
        assertEquals(story.sentences.lastIndex, repository.dao.readings().first().single().sentence)
        val result = repository.dao.exerciseResults().first().single()
        assertEquals(1, result.attempts)
        assertEquals(4, result.bestScore)
        assertTrue(result.completed)
        repository.mastery(saved.id, true, now + 3)
        assertFalse(repository.review(repository.dao.vocabularyById(saved.id)!!, Rating.GOOD, true, now + 4))
        repository.mastery(saved.id, false, now + 5)
        val relearn = repository.dao.vocabularyById(saved.id)!!
        assertTrue(repository.review(relearn, Rating.EASY, false, now + 6))
        assertEquals(0, repository.dao.vocabularyById(saved.id)!!.repetitions)
        assertEquals(now + 6 + 600_000, repository.dao.vocabularyById(saved.id)!!.dueAt)
        assertEquals("AGAIN", repository.dao.reviews().first().first().rating)
    }

    @Test fun coursePairStaysFixedWhileDisplayPreferencesPersist() = runBlocking {
        val file = File(context.cacheDir, "preferences-test.preferences_pb")
        file.delete()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope) { file }
        val preferences = PreferencesRepository(store)
        preferences.restore(LearningPreferences(Language.INDONESIAN, Language.FILIPINO, Level.ADVANCED, 26f, true,
            setOf("book1-story-01-the-lion-and-the-rabbit")))
        scope.coroutineContext.job.cancelAndJoin()
        val secondScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val restored = PreferencesRepository(PreferenceDataStoreFactory.create(scope = secondScope) { file }).preferences.first()
            assertEquals(Language.FILIPINO, restored.target)
            assertEquals(Language.INDONESIAN, restored.native)
            assertEquals(Level.BEGINNER, restored.level)
            assertEquals(26f, restored.fontSize)
            assertTrue(restored.dark)
            assertEquals(setOf("book1-story-01-the-lion-and-the-rabbit"), restored.bookmarkedStories)
        } finally { secondScope.coroutineContext.job.cancelAndJoin(); file.delete() }
    }

    @Test fun fourStatusesPersistProgressAndRejectReviewsAfterManualEdits() = runBlocking {
        TestCorpus.seed(repository.dao, context)
        val story = Corpus.story(repository.dao.stories().first().first())
        val detail = repository.lookup(story, 0, Words.tokens(story.sentence(0, Language.FILIPINO)).first().text,
            Language.FILIPINO, Language.INDONESIAN)
        repository.status(story, detail, Language.FILIPINO, Language.INDONESIAN, VocabularyStatus.NEW, 1000)
        var word = repository.dao.allVocabulary().single()
        assertEquals(VocabularyStatus.NEW, word.learningStatus())
        val expected = listOf(VocabularyStatus.LEARNING, VocabularyStatus.LEARNING,
            VocabularyStatus.ALMOST_MASTERED, VocabularyStatus.ALMOST_MASTERED, VocabularyStatus.KNOWN)
        for (status in expected) {
            assertTrue(repository.review(word, Rating.GOOD, true, word.dueAt))
            word = repository.dao.vocabularyById(word.id)!!
            assertEquals(status, word.learningStatus())
        }
        assertTrue(word.mastered)
        assertFalse(repository.review(word, Rating.GOOD, true, word.dueAt))
        repository.status(word.id, VocabularyStatus.ALMOST_MASTERED, word.dueAt)
        val beforeManualEdit = repository.dao.vocabularyById(word.id)!!
        repository.status(word.id, VocabularyStatus.LEARNING, word.dueAt)
        assertFalse(repository.review(beforeManualEdit, Rating.GOOD, true, word.dueAt))
        word = repository.dao.vocabularyById(word.id)!!
        repository.status(word.id, VocabularyStatus.ALMOST_MASTERED, word.dueAt)
        word = repository.dao.vocabularyById(word.id)!!
        assertTrue(repository.review(word, Rating.GOOD, true, word.dueAt))
        word = repository.dao.vocabularyById(word.id)!!
        assertEquals(VocabularyStatus.ALMOST_MASTERED, word.learningStatus())
        val failedAt = word.dueAt
        assertTrue(repository.review(word, Rating.EASY, false, failedAt))
        word = repository.dao.vocabularyById(word.id)!!
        assertEquals(VocabularyStatus.LEARNING, word.learningStatus())
        assertEquals(failedAt + 600_000, word.dueAt)
        assertEquals(1, word.lapses)
        assertEquals(7, repository.dao.allReviews().size)
        database.close()
        reopen()
        assertEquals(word, repository.dao.vocabularyById(word.id))
    }
}
