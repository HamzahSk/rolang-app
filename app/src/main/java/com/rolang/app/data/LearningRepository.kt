package com.rolang.app.data

import android.content.Context
import androidx.room.withTransaction
import com.rolang.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LearningRepository(private val database: LearningDatabase) {
    val dao = database.dao()
    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        val root = context.assets.open("corpus.json").bufferedReader().use { JSONObject(it.readText()) }
        val (bundledStories, bundledWords) = Corpus.parse(root)
        database.withTransaction {
            val currentStories = dao.allStories().associateBy { it.id }
            val currentCatalog = dao.allCatalog().associateBy { it.id }
            dao.seedStories(bundledStories.filter { currentStories[it.id]?.remote != true })
            // Replace legacy cached entries so obsolete English localization
            // fields do not survive after the focused corpus is installed.
            dao.seedLexicon(bundledWords)
            dao.cacheCatalog(bundledStories.map { story ->
                if (currentStories[story.id]?.remote == true) currentCatalog[story.id]
                    ?: StoryCatalogEntity(story.id, story.content)
                else StoryCatalogEntity(story.id, story.content)
            })
            dao.hideObsoleteBundledStories(bundledStories.map { it.id })
        }
    }
    suspend fun lookup(story: Story, sentence: Int, word: String, target: Language, native: Language): WordDetail {
        val cached = dao.storyLexeme(story.id, target.code, Words.key(word))
        val lexeme = cached?.let { LexemeEntity(it.language, it.word, it.content) }
            ?: checkNotNull(dao.lexeme(target.code, Words.key(word))) { "Translation missing from local cache" }
        return Corpus.detail(lexeme, word, native, story.sentence(sentence, target), story.sentence(sentence, native))
    }
    suspend fun lookup(story: Story, word: String, target: Language, native: Language): WordDetail {
        val key = Words.key(word)
        val index = story.sentences.indexOfFirst { sentence ->
            sentence.getValue(target.code).lowercase().contains(key)
        }.coerceAtLeast(0)
        return lookup(story, index, word, target, native)
    }
    suspend fun save(story: Story, detail: WordDetail, target: Language, native: Language, now: Long): Boolean =
        database.withTransaction {
            val word = VocabularyEntity(language = target.code, reference = native.code,
                word = Words.key(detail.word), translation = detail.translation, meaning = detail.meaning,
                partOfSpeech = detail.partOfSpeech, explanation = detail.explanation, example = detail.example,
                exampleTranslation = detail.exampleTranslation, storyId = story.id, storyTitle = story.title(native),
                category = story.category, createdAt = now, dueAt = now)
            val saved = dao.saveWord(word) != -1L
            if (saved) dao.enqueue(SyncCodec.vocabulary(word))
            saved
        }

    /** Check the snapshot inside the transaction so a repeated submission cannot advance the schedule twice. */
    suspend fun review(snapshot: VocabularyEntity, rating: Rating, correct: Boolean, now: Long): Boolean =
        database.withTransaction {
            val current = dao.vocabularyById(snapshot.id) ?: return@withTransaction false
            if (current != snapshot ||
                current.mastered || current.dueAt > now) return@withTransaction false
            val effective = if (correct) rating else Rating.AGAIN
            val next = SpacedRepetition.next(current.schedule(), effective, now)
            val updated = current.scheduled(next, now).copy(
                status = SpacedRepetition.statusAfterReview(current.learningStatus(), next, effective).name,
                statusChangedAt = maxOf(now, current.statusChangedAt + 1))
            dao.updateWord(updated)
            dao.enqueue(SyncCodec.vocabulary(updated))
            val review = ReviewEntity(vocabularyId = current.id, language = current.language,
                reference = current.reference, reviewedAt = now, correct = correct, rating = effective.name,
                vocabularyKey = SyncCodec.key(current))
            dao.logReview(review)
            dao.enqueue(SyncCodec.review(review, SyncCodec.key(current)))
            true
        }
    suspend fun status(id: Long, status: VocabularyStatus, now: Long) = database.withTransaction {
        val current = dao.vocabularyById(id) ?: return@withTransaction
        val reset = status == VocabularyStatus.NEW || status == VocabularyStatus.LEARNING
        val updated = current.copy(status = status.name, mastered = status == VocabularyStatus.KNOWN, dueAt = now,
            repetitions = if (reset) 0 else current.repetitions,
            intervalDays = if (reset) 0 else current.intervalDays,
            statusChangedAt = maxOf(now, current.statusChangedAt + 1))
        dao.updateWord(updated)
        dao.enqueue(SyncCodec.vocabulary(updated))
    }
    suspend fun mastery(id: Long, mastered: Boolean, now: Long) =
        status(id, if (mastered) VocabularyStatus.KNOWN else VocabularyStatus.LEARNING, now)
    suspend fun status(story: Story, detail: WordDetail, target: Language, native: Language,
                       status: VocabularyStatus, now: Long) = database.withTransaction {
        save(story, detail, target, native, now)
        val current = checkNotNull(dao.vocabularyByKey(target.code, native.code, story.id, Words.key(detail.word)))
        status(current.id, status, now)
    }
    suspend fun delete(id: Long) = database.withTransaction {
        val current = dao.vocabularyById(id) ?: return@withTransaction
        dao.deleteWord(id)
        dao.enqueue(PendingChange("vocabulary", SyncCodec.key(current), deleted = true))
    }
    suspend fun reading(story: Story, language: Language, sentence: Int, completed: Boolean) = database.withTransaction {
        val previous = dao.reading(story.id, language.code)
        val reading = ReadingEntity(story.id, language.code, sentence.coerceIn(story.sentences.indices),
            completed || previous?.completed == true)
        dao.saveReading(reading)
        dao.enqueue(SyncCodec.progress(reading))
    }
    suspend fun exerciseResult(
        story: Story,
        language: Language,
        score: Int,
        total: Int,
        answers: Map<String, String>,
        now: Long,
    ) = database.withTransaction {
        val previous = dao.exerciseResult(story.id, language.code)
        val result = ExerciseResultEntity(
            contentId = story.id,
            language = language.code,
            contentType = if (story.kind == UnitKind.LESSON) "LESSON" else "STORY",
            attempts = (previous?.attempts ?: 0) + 1,
            bestScore = maxOf(previous?.bestScore ?: 0, score),
            latestScore = score,
            totalQuestions = total,
            answersJson = JSONObject(answers).toString(),
            completed = true,
            updatedAt = now,
        )
        dao.saveExerciseResult(result)
        val reading = ReadingEntity(story.id, language.code, story.sentences.lastIndex, true)
        dao.saveReading(reading)
        dao.enqueue(SyncCodec.progress(reading))
    }
}
