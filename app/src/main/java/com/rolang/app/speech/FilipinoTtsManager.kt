package com.rolang.app.speech

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.rolang.app.diagnostics.DiagnosticLogger
import java.io.Closeable
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PronunciationAvailability {
    INITIALIZING,
    READY,
    MISSING_DATA,
    UNSUPPORTED,
    ERROR,
    CLOSED,
}

enum class SpeechQueueMode {
    FLUSH,
    ADD,
}

data class SpeechItem(val id: String, val text: String)

interface PronunciationController {
    fun refresh()

    fun speak(text: String, id: String, queueMode: SpeechQueueMode = SpeechQueueMode.FLUSH)

    fun speakSequence(groupId: String, items: List<SpeechItem>)

    fun replay()

    fun stop()

    fun setSpeechRate(value: Float)

    fun setPitch(value: Float)

    fun openVoiceDataSettings()
}

data class PronunciationState(
    val availability: PronunciationAvailability = PronunciationAvailability.INITIALIZING,
    val activeId: String? = null,
    val activeGroupId: String? = null,
    val lastGroupId: String? = null,
    val speechRate: Float = 0.85f,
    val pitch: Float = 1f,
    val voiceName: String? = null,
    val engineName: String? = null,
    val filipinoVoiceCount: Int = 0,
    val availableLanguageCount: Int = 0,
    val message: String = "Menyiapkan suara Filipino…",
) {
    val ready
        get() = availability == PronunciationAvailability.READY

    val speaking
        get() = activeGroupId != null
}

internal data class FilipinoVoiceOption(
    val name: String,
    val language: String,
    val country: String,
    val networkRequired: Boolean,
    val quality: Int,
    val latency: Int,
)

internal fun isFilipinoLanguage(code: String) = code.equals("fil", true) || code.equals("tl", true)

internal fun availabilityFromLanguageStatus(status: Int): PronunciationAvailability? =
    when (status) {
        TextToSpeech.LANG_MISSING_DATA -> PronunciationAvailability.MISSING_DATA
        TextToSpeech.LANG_NOT_SUPPORTED -> PronunciationAvailability.UNSUPPORTED
        else -> null
    }

internal fun preferredFilipinoVoice(
    options: Collection<FilipinoVoiceOption>
): FilipinoVoiceOption? =
    options
        .filter { isFilipinoLanguage(it.language) }
        .sortedWith(
            compareBy<FilipinoVoiceOption>(
                { it.networkRequired },
                { !it.country.equals("PH", true) },
                { -it.quality },
                { it.latency },
                { it.name },
            )
        )
        .firstOrNull()

internal fun verifiedOfflineFilipinoVoice(
    options: Collection<FilipinoVoiceOption>,
    selected: FilipinoVoiceOption?,
): FilipinoVoiceOption? =
    preferredFilipinoVoice(options.filterNot { it.networkRequired })
        ?: selected?.takeIf { isFilipinoLanguage(it.language) && !it.networkRequired }

/** One lifecycle-owned native TTS instance shared by every pronunciation control. */
class FilipinoTtsManager(context: Context) : PronunciationController, Closeable {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val preferredLocale = Locale("fil", "PH")
    private val _state = MutableStateFlow(PronunciationState())
    val state = _state.asStateFlow()
    private var tts: TextToSpeech? = null
    private var closed = false
    private var initialization = 0L
    private var session = 0L
    private var utterance = 0L

    private data class Pending(val controlId: String, val groupId: String, val session: Long)

    private val pending = mutableMapOf<String, Pending>()
    private var lastBatch: Pair<String, List<SpeechItem>>? = null

    init {
        initialize()
    }

    override fun refresh() = onMain {
        if (!closed && _state.value.availability != PronunciationAvailability.READY) initialize()
    }

    private fun initialize() = onMain {
        if (closed) return@onMain
        val currentInitialization = ++initialization
        tts?.runCatching {
            stop()
            shutdown()
        }
        tts = null
        pending.clear()
        session++
        _state.value =
            _state.value.copy(
                availability = PronunciationAvailability.INITIALIZING,
                activeId = null,
                activeGroupId = null,
                voiceName = null,
                message = "Menyiapkan suara Filipino…",
            )
        try {
            val created =
                TextToSpeech(appContext) { status ->
                    main.post { finishInitialization(status, currentInitialization) }
                }
            if (!closed && currentInitialization == initialization) tts = created
            else created.shutdown()
        } catch (error: Exception) {
            failInitialization("Mesin Text-to-Speech Android tidak dapat dimulai.", error)
        }
    }

    private fun finishInitialization(status: Int, currentInitialization: Long) {
        if (closed || currentInitialization != initialization) return
        val engine = tts
        if (engine == null) {
            main.post { finishInitialization(status, currentInitialization) }
            return
        }
        if (status != TextToSpeech.SUCCESS) {
            failInitialization("Mesin Text-to-Speech Android gagal diinisialisasi.")
            return
        }
        engine.setOnUtteranceProgressListener(listener)
        engine.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        val availableLanguages =
            runCatching { engine.availableLanguages.orEmpty() }.getOrDefault(emptySet())
        val availability =
            runCatching { engine.isLanguageAvailable(preferredLocale) }
                .getOrElse { TextToSpeech.LANG_NOT_SUPPORTED }
        val engineName = runCatching { engine.defaultEngine }.getOrNull()
        when (availabilityFromLanguageStatus(availability)) {
            PronunciationAvailability.MISSING_DATA -> {
                unavailable(
                    PronunciationAvailability.MISSING_DATA,
                    "Data suara Filipino belum terpasang. Pasang suara Filipino (Filipina) melalui pengaturan Text-to-Speech Android.",
                    engineName,
                    availableLanguages.size,
                )
                return
            }
            PronunciationAvailability.UNSUPPORTED -> {
                unavailable(
                    PronunciationAvailability.UNSUPPORTED,
                    "Mesin Text-to-Speech di perangkat ini tidak mendukung Filipino. Coba pasang atau pilih mesin TTS yang menyediakan Filipino (Filipina).",
                    engineName,
                    availableLanguages.size,
                )
                return
            }
            else -> Unit
        }
        if (
            availability < TextToSpeech.LANG_AVAILABLE ||
                engine.setLanguage(preferredLocale) < TextToSpeech.LANG_AVAILABLE
        ) {
            unavailable(
                PronunciationAvailability.UNSUPPORTED,
                "Suara Filipino tidak didukung oleh mesin Text-to-Speech yang aktif.",
                engineName,
                availableLanguages.size,
            )
            return
        }
        val voices = runCatching { engine.voices.orEmpty() }.getOrDefault(emptySet())
        val options =
            voices.map { voice ->
                FilipinoVoiceOption(
                    voice.name,
                    voice.locale.language,
                    voice.locale.country,
                    voice.isNetworkConnectionRequired,
                    voice.quality,
                    voice.latency,
                )
            }
        val selectedBefore = runCatching { engine.voice }.getOrNull()
        val selectedOption =
            selectedBefore?.let { voice ->
                FilipinoVoiceOption(
                    voice.name,
                    voice.locale.language,
                    voice.locale.country,
                    voice.isNetworkConnectionRequired,
                    voice.quality,
                    voice.latency,
                )
            }
        val preferred = verifiedOfflineFilipinoVoice(options, selectedOption)
        voices
            .firstOrNull { it.name == preferred?.name }
            ?.let { voice -> runCatching { engine.setVoice(voice) } }
        val selected = runCatching { engine.voice }.getOrNull()
        val verified =
            selected
                ?.let { voice ->
                    FilipinoVoiceOption(
                        voice.name,
                        voice.locale.language,
                        voice.locale.country,
                        voice.isNetworkConnectionRequired,
                        voice.quality,
                        voice.latency,
                    )
                }
                ?.takeIf { isFilipinoLanguage(it.language) && !it.networkRequired }
        if (verified == null) {
            unavailable(
                PronunciationAvailability.MISSING_DATA,
                "Mesin TTS tidak memilih suara Filipino offline. Tidak ada pengucapan bahasa lain yang akan digunakan sebagai pengganti.",
                engineName,
                availableLanguages.size,
                options.count { isFilipinoLanguage(it.language) },
            )
            return
        }
        if (
            engine.setSpeechRate(_state.value.speechRate) != TextToSpeech.SUCCESS ||
                engine.setPitch(_state.value.pitch) != TextToSpeech.SUCCESS
        ) {
            failInitialization("Pengaturan suara Filipino tidak dapat diterapkan.")
            return
        }
        _state.value =
            _state.value.copy(
                availability = PronunciationAvailability.READY,
                voiceName = verified.name,
                engineName = engineName,
                filipinoVoiceCount = maxOf(options.count { isFilipinoLanguage(it.language) }, 1),
                availableLanguageCount = availableLanguages.size,
                message = "Suara Filipino offline siap digunakan.",
            )
    }

    override fun speak(text: String, id: String, queueMode: SpeechQueueMode) {
        val item = SpeechItem(id, text)
        onMain {
            if (queueMode == SpeechQueueMode.ADD && _state.value.activeGroupId != null) append(item)
            else play(id, listOf(item), remember = true)
        }
    }

    override fun speakSequence(groupId: String, items: List<SpeechItem>) = onMain {
        play(groupId, items, remember = true)
    }

    override fun replay() = onMain {
        lastBatch?.let { (group, items) -> play(group, items, remember = false) }
    }

    override fun stop() = onMain { stopInternal() }

    override fun setSpeechRate(value: Float) = onMain {
        val rate = value.coerceIn(0.5f, 1.2f)
        val applied = tts?.takeIf { _state.value.ready }?.setSpeechRate(rate)
        _state.value =
            _state.value.copy(
                speechRate = rate,
                message =
                    if (applied == TextToSpeech.ERROR) "Kecepatan suara belum dapat diterapkan."
                    else _state.value.message,
            )
    }

    override fun setPitch(value: Float) = onMain {
        val pitch = value.coerceIn(0.8f, 1.2f)
        val applied = tts?.takeIf { _state.value.ready }?.setPitch(pitch)
        _state.value =
            _state.value.copy(
                pitch = pitch,
                message =
                    if (applied == TextToSpeech.ERROR) "Nada suara belum dapat diterapkan."
                    else _state.value.message,
            )
    }

    override fun openVoiceDataSettings() = onMain {
        val intents =
            listOf(
                Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
                Intent("com.android.settings.TTS_SETTINGS"),
                Intent(Settings.ACTION_SETTINGS),
            )
        val opened =
            intents.any { intent ->
                runCatching {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        appContext.startActivity(intent)
                    }
                    .isSuccess
            }
        if (!opened)
            _state.value =
                _state.value.copy(
                    message =
                        "Buka Pengaturan Android → Aksesibilitas atau Sistem → Text-to-Speech, lalu unduh suara Filipino (Filipina)."
                )
    }

    private fun play(groupId: String, source: List<SpeechItem>, remember: Boolean) {
        val engine = tts
        val items =
            source.filter {
                it.text.isNotBlank() && it.text.length <= TextToSpeech.getMaxSpeechInputLength()
            }
        if (!_state.value.ready || engine == null) {
            _state.value =
                _state.value.copy(
                    message =
                        when (_state.value.availability) {
                            PronunciationAvailability.MISSING_DATA ->
                                "Pasang data suara Filipino melalui pengaturan Text-to-Speech Android terlebih dahulu."
                            PronunciationAvailability.UNSUPPORTED ->
                                "Perangkat ini belum menyediakan suara Filipino."
                            else -> "Suara Filipino belum siap. Coba lagi sebentar."
                        }
                )
            return
        }
        if (items.isEmpty()) {
            _state.value = _state.value.copy(message = "Teks Tagalog ini tidak dapat dibacakan.")
            return
        }
        stopInternal(clearLast = false)
        session++
        val currentSession = session
        if (remember) lastBatch = groupId to items
        _state.value =
            _state.value.copy(activeId = null, activeGroupId = groupId, lastGroupId = groupId)
        for ((index, item) in items.withIndex()) {
            val token = "rolang-${currentSession}-${utterance++}"
            pending[token] = Pending(item.id, groupId, currentSession)
            val result =
                engine.speak(
                    item.text,
                    if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                    Bundle(),
                    token,
                )
            if (result != TextToSpeech.SUCCESS) {
                playbackError("Pelafalan Tagalog tidak dapat dimulai.")
                return
            }
        }
    }

    private fun append(item: SpeechItem) {
        val engine = tts ?: return
        if (
            !_state.value.ready ||
                item.text.isBlank() ||
                item.text.length > TextToSpeech.getMaxSpeechInputLength()
        )
            return
        val group = _state.value.activeGroupId ?: item.id
        val token = "rolang-${session}-${utterance++}"
        pending[token] = Pending(item.id, group, session)
        if (
            engine.speak(item.text, TextToSpeech.QUEUE_ADD, Bundle(), token) != TextToSpeech.SUCCESS
        ) {
            playbackError("Pelafalan tambahan tidak dapat dimasukkan ke antrean.")
        }
    }

    private fun stopInternal(clearLast: Boolean = false) {
        runCatching { tts?.stop() }
        session++
        pending.clear()
        if (clearLast) lastBatch = null
        _state.value = _state.value.copy(activeId = null, activeGroupId = null)
    }

    private fun playbackError(message: String, error: Throwable? = null) {
        DiagnosticLogger.error("TTS", message, error)
        runCatching { tts?.stop() }
        session++
        pending.clear()
        _state.value = _state.value.copy(activeId = null, activeGroupId = null, message = message)
    }

    private fun unavailable(
        availability: PronunciationAvailability,
        message: String,
        engineName: String?,
        languageCount: Int,
        voiceCount: Int = 0,
    ) {
        releaseEngine()
        _state.value =
            _state.value.copy(
                availability = availability,
                activeId = null,
                activeGroupId = null,
                engineName = engineName,
                availableLanguageCount = languageCount,
                filipinoVoiceCount = voiceCount,
                voiceName = null,
                message = message,
            )
    }

    private fun failInitialization(message: String, error: Throwable? = null) {
        DiagnosticLogger.error("TTS", message, error)
        releaseEngine()
        _state.value =
            _state.value.copy(
                availability = PronunciationAvailability.ERROR,
                activeId = null,
                activeGroupId = null,
                voiceName = null,
                message = message,
            )
    }

    private val listener =
        object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                main.post {
                    val item = pending[utteranceId] ?: return@post
                    if (item.session == session)
                        _state.value =
                            _state.value.copy(
                                activeId = item.controlId,
                                activeGroupId = item.groupId,
                            )
                }
            }

            override fun onDone(utteranceId: String) {
                main.post { complete(utteranceId) }
            }

            @Deprecated("Android calls the error-code overload on supported engines")
            override fun onError(utteranceId: String) {
                main.post { playbackError("Pelafalan Tagalog gagal diputar.") }
            }

            override fun onError(utteranceId: String, errorCode: Int) {
                main.post { playbackError("Pelafalan Tagalog gagal diputar (kode $errorCode).") }
            }

            override fun onStop(utteranceId: String, interrupted: Boolean) {
                main.post { complete(utteranceId) }
            }
        }

    private fun complete(utteranceId: String) {
        val item = pending.remove(utteranceId) ?: return
        if (item.session != session) return
        if (pending.values.none { it.session == session }) {
            _state.value = _state.value.copy(activeId = null, activeGroupId = null)
        } else if (_state.value.activeId == item.controlId) {
            _state.value = _state.value.copy(activeId = null)
        }
    }

    private fun onMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else main.post(action)
    }

    private fun releaseEngine() {
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
    }

    override fun close() = onMain {
        if (closed) return@onMain
        closed = true
        initialization++
        stopInternal(clearLast = true)
        releaseEngine()
        _state.value =
            _state.value.copy(
                availability = PronunciationAvailability.CLOSED,
                activeId = null,
                activeGroupId = null,
                message = "Text-to-Speech telah dihentikan.",
            )
    }
}
