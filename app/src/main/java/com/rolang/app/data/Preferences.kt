package com.rolang.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.withTransaction
import com.rolang.app.domain.Language
import com.rolang.app.domain.Level
import com.rolang.app.domain.Courses
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.io.IOException

val Context.learningPreferences by preferencesDataStore(name = "learning_preferences")
data class LearningPreferences(
    val target: Language = Language.FILIPINO,
    val native: Language = Language.INDONESIAN,
    val level: Level = Level.BEGINNER,
    val fontSize: Float = 20f,
    val dark: Boolean = false,
)

class PreferencesRepository(private val store: DataStore<Preferences>, private val database: LearningDatabase? = null) {
    private val target = stringPreferencesKey("target")
    private val native = stringPreferencesKey("native")
    private val level = stringPreferencesKey("level")
    private val font = floatPreferencesKey("font")
    private val dark = booleanPreferencesKey("dark")
    val preferences = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map {
        LearningPreferences(Courses.INDONESIAN_TO_FILIPINO.target, Courses.INDONESIAN_TO_FILIPINO.reference,
            Level.BEGINNER,
            (it[font] ?: 20f).coerceIn(16f, 28f), it[dark] ?: false)
    }
    private suspend fun change(update: (MutablePreferences) -> Unit) {
        if (database == null) store.edit { update(it) }
        else database.withTransaction {
            // Use the same lock order as incoming profile sync. A user edit
            // cannot be overwritten before its pending operation is visible.
            store.edit { update(it) }
            database.dao().enqueue(SyncCodec.preferences(preferences.first()))
        }
    }
    suspend fun ensureSupportedCourse() {
        val saved = store.data.first()
        val course = Courses.INDONESIAN_TO_FILIPINO
        if (saved[target] != course.target.code || saved[native] != course.reference.code || saved[level] != Level.BEGINNER.name) change {
            it[target] = course.target.code
            it[native] = course.reference.code
            it[level] = Level.BEGINNER.name
        }
    }
    suspend fun font(value: Float) = change { it[font] = value.coerceIn(16f, 28f) }
    suspend fun dark(value: Boolean) = change { it[dark] = value }
    suspend fun restore(value: LearningPreferences) = store.edit {
        it[target] = Courses.INDONESIAN_TO_FILIPINO.target.code
        it[native] = Courses.INDONESIAN_TO_FILIPINO.reference.code
        it[level] = Level.BEGINNER.name
        it[font] = value.fontSize; it[dark] = value.dark
    }
}
