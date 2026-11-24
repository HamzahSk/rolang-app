package com.rolang.app.network

import android.content.Context
import android.annotation.SuppressLint
import androidx.room.Room
import com.rolang.app.data.*

class AppServices private constructor(private val context: Context) {
    val database = Room.databaseBuilder(context, LearningDatabase::class.java, "ro-lang.db")
        .addMigrations(LearningDatabase.MIGRATION_1_2, LearningDatabase.MIGRATION_2_3,
            LearningDatabase.MIGRATION_3_4, LearningDatabase.MIGRATION_4_5,
            LearningDatabase.MIGRATION_5_6).build()
    val repository = LearningRepository(database)
    val stories = StoryRepository(context, database)
    val preferences = PreferencesRepository(context.learningPreferences, database)
    suspend fun downloadLesson(id: String) = stories.saveOffline(id)
    suspend fun removeLessonDownload(id: String) = stories.removeOffline(id)
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile private var instance: AppServices? = null
        fun get(context: Context): AppServices = instance ?: synchronized(this) {
            instance ?: AppServices(context.applicationContext).also { instance = it }
        }
    }
}
