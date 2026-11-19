package com.rolang.app.network

import com.rolang.app.TestCorpus

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rolang.app.data.LearningDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @Test fun upgradingVersionOnePreservesStoriesReadingAndReviews() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val schema = JSONObject(File("schemas/com.rolang.app.data.LearningDatabase/1.json").readText()).getJSONObject("database")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { sqlite ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                sqlite.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.getJSONArray("indices")
                for (i in 0 until indices.length()) sqlite.execSQL(indices.getJSONObject(i).getString("createSql")
                    .replace("\u0024{TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (index in 0 until setup.length()) sqlite.execSQL(setup.getString(index))
            sqlite.execSQL("INSERT INTO stories VALUES('existing_story','family','Nature','BEGINNER','{}')")
            sqlite.execSQL("INSERT INTO reading VALUES('existing_story','en',2,1)")
            sqlite.execSQL("INSERT INTO reviews VALUES(1,42,'en','id',1000,1,'GOOD')")
            sqlite.version = 1
        }
        val upgraded = Room.databaseBuilder(context, LearningDatabase::class.java, name)
            .addMigrations(LearningDatabase.MIGRATION_1_2, LearningDatabase.MIGRATION_2_3,
                LearningDatabase.MIGRATION_3_4, LearningDatabase.MIGRATION_4_5).build()
        try {
            assertEquals(1, upgraded.dao().storyCount())
            assertTrue(upgraded.dao().allReadings().single().completed)
            val review = upgraded.dao().allReviews().single()
            assertEquals(42L, review.vocabularyId)
            UUID.fromString(review.syncKey)
            assertFalse(upgraded.dao().allStories().single().remote)
            assertTrue(upgraded.dao().allStories().single().available)
            assertTrue(upgraded.dao().pending().isEmpty())
            assertTrue(upgraded.dao().exerciseResults().first().isEmpty())
        } finally { upgraded.close(); context.deleteDatabase(name) }
    }

    @Test fun upgradingVersionTwoMapsExistingMasteryWithoutLosingPendingChanges() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-status-test.db"
        context.deleteDatabase(name)
        val schema = JSONObject(File("schemas/com.rolang.app.data.LearningDatabase/2.json").readText()).getJSONObject("database")
        val operationId = UUID.randomUUID().toString()
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { sqlite ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                sqlite.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.getJSONArray("indices")
                for (i in 0 until indices.length()) sqlite.execSQL(indices.getJSONObject(i).getString("createSql")
                    .replace("\u0024{TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (index in 0 until setup.length()) sqlite.execSQL(setup.getString(index))
            for ((word, repetitions, mastered) in listOf(Triple("new", 0, 0), Triple("almost", 3, 0), Triple("known", 5, 1))) {
                sqlite.execSQL("""INSERT INTO vocabulary(id,language,reference,word,translation,meaning,partOfSpeech,
                    explanation,example,exampleTranslation,storyId,storyTitle,category,createdAt,dueAt,repetitions,
                    intervalDays,ease,lapses,mastered,lastReviewedAt)
                    VALUES(NULL,'en','id',?,'translation','meaning','noun','notes','example','translation',
                    'story','Story','Nature',1000,2000,?,21,2.5,0,?,1000)""", arrayOf(word, repetitions, mastered))
            }
            sqlite.execSQL("INSERT INTO pending_changes VALUES('vocabulary','en:id:story:known',?,'{}',0)", arrayOf(operationId))
            sqlite.execSQL("INSERT INTO reading VALUES('story','en',2,1)")
            sqlite.version = 2
        }
        val upgraded = Room.databaseBuilder(context, LearningDatabase::class.java, name)
            .addMigrations(LearningDatabase.MIGRATION_2_3, LearningDatabase.MIGRATION_3_4, LearningDatabase.MIGRATION_4_5).build()
        try {
            val words = upgraded.dao().allVocabulary().associateBy { it.word }
            assertEquals("LEARNING", words.getValue("new").status)
            assertEquals("ALMOST_MASTERED", words.getValue("almost").status)
            assertEquals("KNOWN", words.getValue("known").status)
            assertTrue(words.getValue("known").mastered)
            assertEquals(2000L, words.getValue("almost").dueAt)
            assertEquals(0L, words.getValue("known").statusChangedAt)
            assertEquals(operationId, upgraded.dao().pending().single().operationId)
            assertTrue(upgraded.dao().allReadings().single().completed)
        } finally { upgraded.close(); context.deleteDatabase(name) }
    }
    @Test fun upgradingVersionFourPreservesContentScoresAndPendingLearningEdits() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-content-test.db"
        context.deleteDatabase(name)
        val schema = JSONObject(File("schemas/com.rolang.app.data.LearningDatabase/4.json").readText()).getJSONObject("database")
        val fixture = TestCorpus.load(context).first.first()
        val operationId = UUID.randomUUID().toString()
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { sqlite ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                sqlite.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.getJSONArray("indices")
                for (i in 0 until indices.length()) sqlite.execSQL(indices.getJSONObject(i).getString("createSql")
                    .replace("\u0024{TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (index in 0 until setup.length()) sqlite.execSQL(setup.getString(index))
            sqlite.execSQL("INSERT INTO stories VALUES(?,?,?,?,?,0,1)", arrayOf(fixture.id, fixture.family, fixture.category, fixture.level, fixture.content))
            sqlite.execSQL("INSERT INTO exercise_results VALUES(?,'en','LESSON',2,5,4,6,'{}',1,1000)", arrayOf(fixture.id))
            sqlite.execSQL("INSERT INTO reading VALUES(?,'en',2,1)", arrayOf(fixture.id))
            sqlite.execSQL("INSERT INTO pending_changes VALUES('progress',?,?,?,0)", arrayOf("${fixture.id}:en", operationId, "{}"))
            sqlite.version = 4
        }
        val upgraded = Room.databaseBuilder(context, LearningDatabase::class.java, name)
            .addMigrations(LearningDatabase.MIGRATION_4_5).build()
        try {
            com.rolang.app.data.LearningRepository(upgraded).initialize(context)
            assertEquals(fixture.content, upgraded.dao().allStories().single().content)
            assertEquals(fixture.content, upgraded.dao().allCatalog().single().content)
            assertEquals(5, upgraded.dao().exerciseResults().first().single().bestScore)
            assertTrue(upgraded.dao().allReadings().single().completed)
            assertEquals(operationId, upgraded.dao().pending().single().operationId)
        } finally { upgraded.close(); context.deleteDatabase(name) }
    }

}
