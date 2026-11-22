package com.rolang.app.network

import android.content.Context
import android.annotation.SuppressLint
import androidx.room.Room
import com.rolang.app.data.*
import com.rolang.app.diagnostics.DiagnosticLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class OnlineState(val enabled: Boolean = false, val signedIn: Boolean = false,
    val displayName: String = "", val busy: Boolean = false, val message: String = "Fitur daring dinonaktifkan.")

class AppServices private constructor(private val context: Context) {
    val database = Room.databaseBuilder(context, LearningDatabase::class.java, "ro-lang.db")
        .addMigrations(LearningDatabase.MIGRATION_1_2, LearningDatabase.MIGRATION_2_3,
            LearningDatabase.MIGRATION_3_4, LearningDatabase.MIGRATION_4_5,
            LearningDatabase.MIGRATION_5_6).build()
    val repository = LearningRepository(database)
    val stories = StoryRepository(database, StoryRemoteSource { ServerConfig.load(context) })
    val preferences = PreferencesRepository(context.learningPreferences, database)
    private val sessions = SessionStore(context)
    private val engine = SyncEngine(context, database, preferences, sessions, storyRepository = stories)
    private val status = MutableStateFlow(OnlineState())
    val online = status.asStateFlow()

    suspend fun configure() = withContext(Dispatchers.IO) {
        val config = try { ServerConfig.load(context) } catch (error: Exception) {
            DiagnosticLogger.error("CONFIG", "Online services could not load their configuration.", error)
            status.value = OnlineState(message = "Konfigurasi daring tidak tersedia. Materi tersimpan tetap dapat digunakan.")
            return@withContext
        }
        val account = runCatching { sessions.read() }.onFailure {
            DiagnosticLogger.error("SESSION", "The saved account state could not be read; continuing signed out.", it)
        }.getOrNull()
        status.value = OnlineState(config.onlineEnabled, account != null, account?.displayName.orEmpty(),
            message = if (!config.onlineEnabled) "Fitur daring dinonaktifkan."
                else if (account == null) "Masuk untuk menyinkronkan data belajar." else "Perubahan lokal tersimpan dan siap disinkronkan.")
        try {
            SyncScheduler.configure(context, config)
            if (config.onlineEnabled) SyncScheduler.enqueue(context)
        } catch (error: Exception) {
            // Foreground requests must remain available even if WorkManager cannot be scheduled.
            DiagnosticLogger.error("SCHEDULER", "Background synchronization could not be scheduled.", error)
        }
    }
    suspend fun downloadStory(id: String) {
        val download = stories.queueDownload(id)
        try { StoryDownloadScheduler.enqueue(context, id, download.requestId) }
        catch (error: Exception) {
            stories.failDownload(id, download.requestId, retry = false)
            throw error
        }
    }
    suspend fun cancelDownload(id: String) {
        stories.cancelDownload(id)
        runCatching { StoryDownloadScheduler.cancel(context, id) }
            .onFailure { DiagnosticLogger.error("SCHEDULER", "The story download worker for $id could not be cancelled.", it) }
    }
    suspend fun removeDownload(id: String) {
        cancelDownload(id)
        stories.remove(id)
    }
    fun schedulePending() {
        val config = runCatching { ServerConfig.load(context) }.onFailure {
            DiagnosticLogger.error("CONFIG", "A pending-change sync could not load server configuration.", it)
        }.getOrNull() ?: return
        if (config.onlineEnabled && config.automaticSyncEnabled) runCatching { SyncScheduler.enqueue(context) }
            .onFailure { DiagnosticLogger.error("SCHEDULER", "A pending-change sync could not be scheduled.", it) }
    }
    suspend fun authenticate(email: String, password: String, name: String?) {
        status.update { it.copy(busy = true) }
        try {
            val account = engine.signIn(email, password, name)
            status.update { it.copy(signedIn = true, displayName = account.displayName, message = "Berhasil masuk. Menyinkronkan pustaka…") }
            synchronize()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { DiagnosticLogger.error("AUTH", "Authentication failed.", error); failure(error) }
        finally { status.update { it.copy(busy = false) } }
    }
    suspend fun logout() {
        status.update { it.copy(busy = true) }
        try { engine.signOut() }
        catch (cancelled: CancellationException) { throw cancelled }
        finally { status.update { it.copy(signedIn = false, displayName = "", busy = false,
            message = "Anda telah keluar. Data belajar tetap tersimpan di perangkat ini.") } }
    }
    suspend fun synchronize(includeLearning: Boolean = true): Boolean {
        status.update { it.copy(busy = true) }
        try {
            repository.initialize(context)
            val complete = engine.synchronize(includeLearning)
            status.update { it.copy(message = if (complete) "Pustaka sudah mutakhir." else "Masih ada perubahan yang menunggu sinkronisasi.") }
            return complete
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { DiagnosticLogger.error("SYNC", "Online synchronization failed.", error); failure(error); throw error }
        finally { status.update { it.copy(busy = false) } }
    }
    private fun failure(error: Exception) {
        val message = when {
            error is AccountMismatch -> "Pustaka ini terikat ke akun lain. Masuk dengan akun semula."
            error is ApiException && error.status == 401 -> "Masuk kembali untuk menyinkronkan. Data belajar tetap tersimpan secara lokal."
            error is ApiException && error.status == 429 -> "Terlalu banyak percobaan masuk. Coba lagi nanti."
            error is ApiException && error.status == 400 -> "Periksa data akun. Gunakan email yang valid dan kata sandi 12–128 karakter."
            error is ApiException && error.code == "ALREADY_EXISTS" -> "Akun tersebut sudah ada. Masuk untuk melanjutkan."
            else -> "Server tidak tersedia. Pustaka lokal dan perubahan tertunda tetap tersimpan."
        }
        val signedIn = runCatching { sessions.read() != null }.onFailure {
            DiagnosticLogger.error("SESSION", "The account state could not be checked after an online failure.", it)
        }.getOrDefault(false)
        status.update { it.copy(signedIn = signedIn, message = message) }
    }
    companion object {
        // The instance only stores applicationContext and shares Room with workers.
        @SuppressLint("StaticFieldLeak")
        @Volatile private var instance: AppServices? = null
        fun get(context: Context): AppServices = instance ?: synchronized(this) {
            instance ?: AppServices(context.applicationContext).also { instance = it }
        }
    }
}
