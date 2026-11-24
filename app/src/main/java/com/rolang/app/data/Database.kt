package com.rolang.app.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rolang.app.domain.Schedule
import com.rolang.app.domain.VocabularyStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val family: String,
    val category: String,
    val level: String,
    val content: String,
    @ColumnInfo(defaultValue = "0") val remote: Boolean = false,
    @ColumnInfo(defaultValue = "1") val available: Boolean = true,
)

@Entity(tableName = "story_catalog")
data class StoryCatalogEntity(@PrimaryKey val id: String, val content: String,
    @ColumnInfo(defaultValue = "1") val available: Boolean = true)

@Entity(tableName = "chapter_cache", primaryKeys = ["storyId", "chapterId"])
data class ChapterCacheEntity(val storyId: String, val chapterId: String, val version: String, val content: String)

@Entity(tableName = "story_downloads")
data class StoryDownloadEntity(@PrimaryKey val storyId: String, val status: String = "QUEUED",
    val completed: Int = 0, val total: Int = 0, val version: String = "", val error: String? = null,
    val requestId: String = UUID.randomUUID().toString())

@Entity(tableName = "lexicon", primaryKeys = ["language", "word"])
data class LexemeEntity(val language: String, val word: String, val content: String)

@Entity(tableName = "story_lexicon", primaryKeys = ["storyId", "language", "word"])
data class StoryLexemeEntity(val storyId: String, val language: String, val word: String, val content: String)

@Entity(tableName = "vocabulary", indices = [Index(value = ["language", "reference", "word", "storyId"], unique = true)])
data class VocabularyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val language: String,
    val reference: String,
    val word: String,
    val translation: String,
    val meaning: String,
    val partOfSpeech: String,
    val explanation: String,
    val example: String,
    val exampleTranslation: String,
    val storyId: String,
    val storyTitle: String,
    val category: String,
    val createdAt: Long,
    val dueAt: Long,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val ease: Double = 2.5,
    val lapses: Int = 0,
    val mastered: Boolean = false,
    val lastReviewedAt: Long = 0,
    @ColumnInfo(defaultValue = "'LEARNING'") val status: String = VocabularyStatus.LEARNING.name,
    @ColumnInfo(defaultValue = "0") val statusChangedAt: Long = 0,
) {
    fun learningStatus() = if (mastered) VocabularyStatus.KNOWN else VocabularyStatus.valueOf(status)
    fun schedule() = Schedule(repetitions, intervalDays, ease, dueAt, lapses, mastered)
    fun scheduled(schedule: Schedule, now: Long) = copy(
        dueAt = schedule.dueAt, repetitions = schedule.repetitions, intervalDays = schedule.intervalDays,
        ease = schedule.ease, lapses = schedule.lapses, mastered = schedule.mastered, lastReviewedAt = now,
    )
}

@Entity(tableName = "reading", primaryKeys = ["storyId", "language"])
data class ReadingEntity(val storyId: String, val language: String, val sentence: Int = 0, val completed: Boolean = false)

@Entity(tableName = "exercise_results", primaryKeys = ["contentId", "language"])
data class ExerciseResultEntity(
    val contentId: String,
    val language: String,
    val contentType: String,
    val attempts: Int,
    val bestScore: Int,
    val latestScore: Int,
    val totalQuestions: Int,
    val answersJson: String,
    val completed: Boolean,
    val updatedAt: Long,
)

@Entity(tableName = "reviews", indices = [Index(value = ["syncKey"], unique = true)])
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vocabularyId: Long,
    val language: String,
    val reference: String,
    val reviewedAt: Long,
    val correct: Boolean,
    val rating: String,
    @ColumnInfo(defaultValue = "''") val syncKey: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "''") val vocabularyKey: String = "",
)

/** Kept only so existing version-6 databases open without a destructive migration. */
@Entity(tableName = "pending_changes", primaryKeys = ["kind", "key"])
data class PendingChange(val kind: String, val key: String, val operationId: String = UUID.randomUUID().toString(),
    val data: String? = null, val deleted: Boolean = false)

/** Legacy schema compatibility; the app no longer reads or writes synchronization state. */
@Entity(tableName = "sync_state")
data class SyncState(@PrimaryKey val id: Int = 1, val owner: String? = null,
    val cursor: Long = 0, val bootstrapped: Boolean = false)

@Dao
interface LearningDao {
    @Query("SELECT * FROM story_catalog WHERE available=1 ORDER BY id")
    fun catalog(): Flow<List<StoryCatalogEntity>>
    @Query("SELECT * FROM story_catalog")
    suspend fun allCatalog(): List<StoryCatalogEntity>
    @Query("SELECT * FROM story_catalog WHERE id=:id")
    suspend fun catalogStory(id: String): StoryCatalogEntity?
    @Query("UPDATE story_catalog SET available=0 WHERE id IN (:ids)")
    suspend fun hideCatalog(ids: List<String>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheCatalog(items: List<StoryCatalogEntity>)
    @Query("SELECT * FROM stories WHERE id=:id")
    suspend fun cachedStory(id: String): StoryEntity?
    @Query("DELETE FROM stories WHERE id=:id")
    suspend fun removeStoryCache(id: String)
    @Query("DELETE FROM story_lexicon WHERE storyId=:id")
    suspend fun removeStoryLexicon(id: String)
    @Query("SELECT * FROM chapter_cache WHERE storyId=:id AND chapterId=:chapterId")
    suspend fun cachedChapter(id: String, chapterId: String): ChapterCacheEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheChapter(chapter: ChapterCacheEntity)
    @Query("DELETE FROM chapter_cache WHERE storyId=:id")
    suspend fun removeChapters(id: String)
    @Query("DELETE FROM chapter_cache WHERE storyId=:id AND chapterId NOT IN (:ids)")
    suspend fun pruneChapters(id: String, ids: List<String>)
    @Query("SELECT * FROM story_downloads")
    fun downloads(): Flow<List<StoryDownloadEntity>>
    @Query("SELECT * FROM story_downloads WHERE storyId=:id")
    suspend fun download(id: String): StoryDownloadEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDownload(download: StoryDownloadEntity)
    @Query("DELETE FROM story_downloads WHERE storyId=:id")
    suspend fun removeDownload(id: String)
    @Query("SELECT * FROM stories WHERE available = 1 ORDER BY family, level")
    fun stories(): Flow<List<StoryEntity>>
    @Query("SELECT * FROM stories")
    suspend fun allStories(): List<StoryEntity>
    @Query("UPDATE stories SET available = 0 WHERE remote = 0 AND id NOT IN (:ids)")
    suspend fun hideObsoleteBundledStories(ids: List<String>)
    @Query("UPDATE stories SET available = 0 WHERE remote = 0 AND id IN (:ids)")
    suspend fun hideStories(ids: List<String>)
    @Query("SELECT COUNT(*) FROM stories")
    suspend fun storyCount(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun seedStories(stories: List<StoryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun seedLexicon(words: List<LexemeEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheStoryLexicon(words: List<StoryLexemeEntity>)
    @Query("SELECT * FROM story_lexicon WHERE storyId=:storyId AND language=:language AND word=:word")
    suspend fun storyLexeme(storyId: String, language: String, word: String): StoryLexemeEntity?
    @Query("SELECT * FROM lexicon WHERE language = :language AND word = :word")
    suspend fun lexeme(language: String, word: String): LexemeEntity?
    @Query("SELECT * FROM lexicon WHERE language = :language")
    suspend fun lexemes(language: String): List<LexemeEntity>
    @Query("SELECT * FROM vocabulary ORDER BY createdAt DESC")
    fun vocabulary(): Flow<List<VocabularyEntity>>
    @Query("SELECT * FROM vocabulary WHERE id = :id")
    suspend fun vocabularyById(id: Long): VocabularyEntity?
    @Query("SELECT * FROM vocabulary WHERE language=:language AND reference=:reference AND storyId=:storyId AND word=:word")
    suspend fun vocabularyByKey(language: String, reference: String, storyId: String, word: String): VocabularyEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun saveWord(word: VocabularyEntity): Long
    @Update
    suspend fun updateWord(word: VocabularyEntity)
    @Query("DELETE FROM vocabulary WHERE id = :id")
    suspend fun deleteWord(id: Long)
    @Query("SELECT * FROM vocabulary")
    suspend fun allVocabulary(): List<VocabularyEntity>
    @Query("SELECT * FROM reading")
    fun readings(): Flow<List<ReadingEntity>>
    @Query("SELECT * FROM reading WHERE storyId = :storyId AND language = :language")
    suspend fun reading(storyId: String, language: String): ReadingEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReading(reading: ReadingEntity)
    @Query("SELECT * FROM reading")
    suspend fun allReadings(): List<ReadingEntity>
    @Query("DELETE FROM reading WHERE storyId=:storyId AND language=:language")
    suspend fun deleteReading(storyId: String, language: String)
    @Query("SELECT * FROM exercise_results")
    fun exerciseResults(): Flow<List<ExerciseResultEntity>>
    @Query("SELECT * FROM exercise_results WHERE contentId=:contentId AND language=:language")
    suspend fun exerciseResult(contentId: String, language: String): ExerciseResultEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveExerciseResult(result: ExerciseResultEntity)
    @Query("SELECT * FROM reviews ORDER BY reviewedAt DESC")
    fun reviews(): Flow<List<ReviewEntity>>
    @Insert
    suspend fun logReview(review: ReviewEntity)
    @Query("SELECT * FROM reviews")
    suspend fun allReviews(): List<ReviewEntity>
    @Query("SELECT * FROM reviews WHERE syncKey=:key")
    suspend fun reviewByKey(key: String): ReviewEntity?
    @Query("UPDATE reviews SET vocabularyId=:id WHERE vocabularyKey=:key")
    suspend fun linkReviews(key: String, id: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun importReview(review: ReviewEntity)
    @Query("DELETE FROM reviews WHERE syncKey=:key")
    suspend fun deleteReview(key: String)
    /** Migration verification/read-only access to legacy rows; no new changes are enqueued. */
    @Query("SELECT * FROM pending_changes ORDER BY kind, `key` LIMIT :limit")
    suspend fun pending(limit: Int = 100): List<PendingChange>
}

@Database(entities = [StoryEntity::class, StoryCatalogEntity::class, ChapterCacheEntity::class, StoryDownloadEntity::class, LexemeEntity::class, StoryLexemeEntity::class, VocabularyEntity::class,
    ReadingEntity::class, ExerciseResultEntity::class, ReviewEntity::class, PendingChange::class, SyncState::class], version = 6, exportSchema = true)
abstract class LearningDatabase : RoomDatabase() {
    abstract fun dao(): LearningDao
    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Merge any early `fil` rows before canonicalizing the former `tl` course.
                db.execSQL("""UPDATE vocabulary SET
                    repetitions=MAX(repetitions, COALESCE((SELECT MAX(old.repetitions) FROM vocabulary old
                        WHERE old.language='tl' AND old.reference='id' AND old.storyId=vocabulary.storyId AND old.word=vocabulary.word), repetitions)),
                    intervalDays=MAX(intervalDays, COALESCE((SELECT MAX(old.intervalDays) FROM vocabulary old
                        WHERE old.language='tl' AND old.reference='id' AND old.storyId=vocabulary.storyId AND old.word=vocabulary.word), intervalDays)),
                    lapses=MAX(lapses, COALESCE((SELECT MAX(old.lapses) FROM vocabulary old
                        WHERE old.language='tl' AND old.reference='id' AND old.storyId=vocabulary.storyId AND old.word=vocabulary.word), lapses)),
                    lastReviewedAt=MAX(lastReviewedAt, COALESCE((SELECT MAX(old.lastReviewedAt) FROM vocabulary old
                        WHERE old.language='tl' AND old.reference='id' AND old.storyId=vocabulary.storyId AND old.word=vocabulary.word), lastReviewedAt)),
                    statusChangedAt=MAX(statusChangedAt, COALESCE((SELECT MAX(old.statusChangedAt) FROM vocabulary old
                        WHERE old.language='tl' AND old.reference='id' AND old.storyId=vocabulary.storyId AND old.word=vocabulary.word), statusChangedAt)),
                    dueAt=MIN(dueAt, COALESCE((SELECT MIN(old.dueAt) FROM vocabulary old
                        WHERE old.language='tl' AND old.reference='id' AND old.storyId=vocabulary.storyId AND old.word=vocabulary.word), dueAt)),
                    mastered=CASE WHEN mastered=1 OR EXISTS(SELECT 1 FROM vocabulary old WHERE old.language='tl'
                        AND old.reference='id' AND old.storyId=vocabulary.storyId AND old.word=vocabulary.word AND old.mastered=1) THEN 1 ELSE 0 END,
                    status=CASE
                        WHEN mastered=1 OR EXISTS(SELECT 1 FROM vocabulary old WHERE old.language='tl' AND old.reference='id'
                            AND old.storyId=vocabulary.storyId AND old.word=vocabulary.word AND (old.mastered=1 OR old.status='KNOWN')) THEN 'KNOWN'
                        WHEN status='ALMOST_MASTERED' OR EXISTS(SELECT 1 FROM vocabulary old WHERE old.language='tl' AND old.reference='id'
                            AND old.storyId=vocabulary.storyId AND old.word=vocabulary.word AND old.status='ALMOST_MASTERED') THEN 'ALMOST_MASTERED'
                        WHEN status='LEARNING' OR EXISTS(SELECT 1 FROM vocabulary old WHERE old.language='tl' AND old.reference='id'
                            AND old.storyId=vocabulary.storyId AND old.word=vocabulary.word AND old.status='LEARNING') THEN 'LEARNING'
                        ELSE 'NEW' END
                    WHERE language='fil' AND reference='id'""")
                db.execSQL("""UPDATE reviews SET vocabularyId=COALESCE((SELECT current.id FROM vocabulary legacy
                    JOIN vocabulary current ON current.language='fil' AND current.reference='id'
                        AND current.storyId=legacy.storyId AND current.word=legacy.word
                    WHERE legacy.id=reviews.vocabularyId AND legacy.language='tl' AND legacy.reference='id'), vocabularyId)""")
                db.execSQL("""DELETE FROM vocabulary WHERE language='tl' AND reference='id' AND EXISTS(
                    SELECT 1 FROM vocabulary current WHERE current.language='fil' AND current.reference='id'
                        AND current.storyId=vocabulary.storyId AND current.word=vocabulary.word)""")
                db.execSQL("UPDATE vocabulary SET language='fil' WHERE language='tl' AND reference='id'")

                db.execSQL("""UPDATE reading SET
                    sentence=MAX(sentence, COALESCE((SELECT legacy.sentence FROM reading legacy
                        WHERE legacy.storyId=reading.storyId AND legacy.language='tl'), sentence)),
                    completed=CASE WHEN completed=1 OR EXISTS(SELECT 1 FROM reading legacy
                        WHERE legacy.storyId=reading.storyId AND legacy.language='tl' AND legacy.completed=1) THEN 1 ELSE 0 END
                    WHERE language='fil'""")
                db.execSQL("DELETE FROM reading WHERE language='tl' AND EXISTS(SELECT 1 FROM reading current WHERE current.storyId=reading.storyId AND current.language='fil')")
                db.execSQL("UPDATE reading SET language='fil' WHERE language='tl'")

                db.execSQL("""UPDATE exercise_results SET
                    attempts=MAX(attempts, COALESCE((SELECT legacy.attempts FROM exercise_results legacy
                        WHERE legacy.contentId=exercise_results.contentId AND legacy.language='tl'), attempts)),
                    bestScore=MAX(bestScore, COALESCE((SELECT legacy.bestScore FROM exercise_results legacy
                        WHERE legacy.contentId=exercise_results.contentId AND legacy.language='tl'), bestScore)),
                    completed=CASE WHEN completed=1 OR EXISTS(SELECT 1 FROM exercise_results legacy
                        WHERE legacy.contentId=exercise_results.contentId AND legacy.language='tl' AND legacy.completed=1) THEN 1 ELSE 0 END,
                    updatedAt=MAX(updatedAt, COALESCE((SELECT legacy.updatedAt FROM exercise_results legacy
                        WHERE legacy.contentId=exercise_results.contentId AND legacy.language='tl'), updatedAt))
                    WHERE language='fil'""")
                db.execSQL("DELETE FROM exercise_results WHERE language='tl' AND EXISTS(SELECT 1 FROM exercise_results current WHERE current.contentId=exercise_results.contentId AND current.language='fil')")
                db.execSQL("UPDATE exercise_results SET language='fil' WHERE language='tl'")

                db.execSQL("UPDATE reviews SET language='fil' WHERE language='tl' AND reference='id'")
                db.execSQL("UPDATE reviews SET vocabularyKey=REPLACE(vocabularyKey, 'tl:id:', 'fil:id:') WHERE vocabularyKey LIKE 'tl:id:%'")

                db.execSQL("DELETE FROM pending_changes WHERE kind='vocabulary' AND `key` LIKE 'tl:id:%' AND EXISTS(SELECT 1 FROM pending_changes current WHERE current.kind='vocabulary' AND current.`key`=REPLACE(pending_changes.`key`, 'tl:id:', 'fil:id:'))")
                db.execSQL("UPDATE pending_changes SET `key`=REPLACE(`key`, 'tl:id:', 'fil:id:') WHERE kind='vocabulary' AND `key` LIKE 'tl:id:%'")
                db.execSQL("DELETE FROM pending_changes WHERE kind='progress' AND `key` LIKE '%:tl' AND EXISTS(SELECT 1 FROM pending_changes current WHERE current.kind='progress' AND current.`key`=SUBSTR(pending_changes.`key`, 1, LENGTH(pending_changes.`key`)-2) || 'fil')")
                db.execSQL("UPDATE pending_changes SET `key`=SUBSTR(`key`, 1, LENGTH(`key`)-2) || 'fil' WHERE kind='progress' AND `key` LIKE '%:tl'")
                db.execSQL("""UPDATE pending_changes SET data=REPLACE(REPLACE(REPLACE(data,
                    '"language":"tl"', '"language":"fil"'),
                    '"target":"tl"', '"target":"fil"'),
                    '"native":"en"', '"native":"id"') WHERE data IS NOT NULL""")
                db.execSQL("UPDATE pending_changes SET data=REPLACE(data, 'tl:id:', 'fil:id:') WHERE data IS NOT NULL")

                db.execSQL("DELETE FROM lexicon WHERE language IN ('en','id')")
                db.execSQL("DELETE FROM lexicon WHERE language='tl' AND EXISTS(SELECT 1 FROM lexicon current WHERE current.language='fil' AND current.word=lexicon.word)")
                db.execSQL("UPDATE lexicon SET language='fil' WHERE language='tl'")
                db.execSQL("DELETE FROM story_lexicon WHERE language IN ('en','id')")
                db.execSQL("DELETE FROM story_lexicon WHERE language='tl' AND EXISTS(SELECT 1 FROM story_lexicon current WHERE current.storyId=story_lexicon.storyId AND current.language='fil' AND current.word=story_lexicon.word)")
                db.execSQL("UPDATE story_lexicon SET language='fil' WHERE language='tl'")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS story_catalog (id TEXT NOT NULL, content TEXT NOT NULL, available INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(id))")
                db.execSQL("INSERT INTO story_catalog(id,content,available) SELECT id,content,available FROM stories")
                db.execSQL("CREATE TABLE IF NOT EXISTS chapter_cache (storyId TEXT NOT NULL, chapterId TEXT NOT NULL, version TEXT NOT NULL, content TEXT NOT NULL, PRIMARY KEY(storyId,chapterId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS story_downloads (storyId TEXT NOT NULL, status TEXT NOT NULL, completed INTEGER NOT NULL, total INTEGER NOT NULL, version TEXT NOT NULL, error TEXT, requestId TEXT NOT NULL, PRIMARY KEY(storyId))")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS exercise_results (
                    contentId TEXT NOT NULL, language TEXT NOT NULL, contentType TEXT NOT NULL,
                    attempts INTEGER NOT NULL, bestScore INTEGER NOT NULL, latestScore INTEGER NOT NULL,
                    totalQuestions INTEGER NOT NULL, answersJson TEXT NOT NULL, completed INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL, PRIMARY KEY(contentId, language))""")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vocabulary ADD COLUMN status TEXT NOT NULL DEFAULT 'LEARNING'")
                db.execSQL("ALTER TABLE vocabulary ADD COLUMN statusChangedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE vocabulary SET status=CASE WHEN mastered=1 THEN 'KNOWN' WHEN repetitions>=3 THEN 'ALMOST_MASTERED' ELSE 'LEARNING' END")
            }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stories ADD COLUMN remote INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE stories ADD COLUMN available INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE reviews ADD COLUMN syncKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reviews ADD COLUMN vocabularyKey TEXT NOT NULL DEFAULT ''")
                db.query("SELECT id FROM reviews").use { cursor ->
                    while (cursor.moveToNext()) db.execSQL("UPDATE reviews SET syncKey=? WHERE id=?",
                        arrayOf(UUID.randomUUID().toString(), cursor.getLong(0)))
                }
                db.execSQL("UPDATE reviews SET vocabularyKey=COALESCE((SELECT language || ':' || reference || ':' || storyId || ':' || word FROM vocabulary WHERE vocabulary.id=reviews.vocabularyId), 'removed:' || syncKey)")
                db.execSQL("CREATE UNIQUE INDEX index_reviews_syncKey ON reviews(syncKey)")
                db.execSQL("CREATE TABLE pending_changes (kind TEXT NOT NULL, `key` TEXT NOT NULL, operationId TEXT NOT NULL, data TEXT, deleted INTEGER NOT NULL, PRIMARY KEY(kind, `key`))")
                db.execSQL("CREATE TABLE story_lexicon (storyId TEXT NOT NULL, language TEXT NOT NULL, word TEXT NOT NULL, content TEXT NOT NULL, PRIMARY KEY(storyId, language, word))")
                db.execSQL("CREATE TABLE sync_state (id INTEGER NOT NULL, owner TEXT, cursor INTEGER NOT NULL, bootstrapped INTEGER NOT NULL, PRIMARY KEY(id))")
            }
        }
    }
}
