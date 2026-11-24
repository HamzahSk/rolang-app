package com.rolang.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rolang.app.data.*
import com.rolang.app.diagnostics.DiagnosticLogger
import com.rolang.app.domain.*
import com.rolang.app.network.AppServices
import com.rolang.app.speech.FilipinoTtsManager
import com.rolang.app.speech.PronunciationController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject

data class LearningState(
    val preferences: LearningPreferences = LearningPreferences(),
    val stories: List<Story> = emptyList(),
    val vocabulary: List<VocabularyEntity> = emptyList(),
    val readings: List<ReadingEntity> = emptyList(),
    val exerciseResults: List<ExerciseResultEntity> = emptyList(),
    val reviews: List<ReviewEntity> = emptyList(),
    val ready: Boolean = false,
    val error: String? = null,
    val catalogLoading: Boolean = false,
    val catalogError: String? = null,
    val catalogStatus: CatalogStatus = CatalogStatus.IDLE,
    val downloads: List<StoryDownloadEntity> = emptyList(),
) {
    val courseWords get() = vocabulary.filter {
        it.language == preferences.target.code && it.reference == preferences.native.code
    }
    val courseReadings get() = readings.filter { it.language == preferences.target.code }
    val courseResults get() = exerciseResults.filter { it.language == preferences.target.code }
    val lessons get() = stories.filter { it.kind == UnitKind.LESSON }
    val readingStories get() = stories.filter { it.kind != UnitKind.LESSON }
    val lessonReadings get() = courseReadings.filter { reading -> lessons.any { it.id == reading.storyId } }
    val storyReadings get() = courseReadings.filter { reading -> readingStories.any { it.id == reading.storyId } }
    fun contentCompleted(id: String) = courseResults.any { it.contentId == id && it.completed } ||
        courseReadings.any { it.storyId == id && it.completed }
    val courseReviews get() = reviews.filter {
        it.language == preferences.target.code && it.reference == preferences.native.code
    }
    fun wordStatus(storyId: String, word: String) = courseWords.firstOrNull {
        it.storyId == storyId && it.word == Words.key(word)
    }?.learningStatus() ?: VocabularyStatus.NEW
    fun due(now: Long) = courseWords.filter { !it.mastered && it.dueAt <= now }.sortedBy { it.dueAt }
}

data class Quiz(
    val word: VocabularyEntity,
    val mode: PracticeMode,
    val prompt: String,
    val expected: String,
    val choices: List<String>,
    val typed: Boolean,
    val response: String = "",
    val revealed: Boolean = false,
    val correct: Boolean = false,
)

class LearningViewModel(application: Application) : AndroidViewModel(application) {
    private val services = AppServices.get(application)
    private val repository = services.repository
    private val preferences = services.preferences
    private val tts = FilipinoTtsManager(application)
    val pronunciation: PronunciationController = tts
    val pronunciationState = tts.state
    private val ready = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now = _now.asStateFlow()
    private val _quiz = MutableStateFlow<Quiz?>(null)
    val quiz = _quiz.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private var languageGeneration = 0
    private val contentBase = combine(preferences.preferences,
        combine(services.stories.catalog, repository.dao.stories()) { catalog, cached ->
            withContext(Dispatchers.Default) {
                val snapshots = cached.associateBy { it.id }
                catalog.mapNotNull { item ->
                    val snapshot = snapshots[item.id]?.let { entity ->
                        runCatching { Corpus.story(entity) }.onFailure {
                            DiagnosticLogger.error("LOCAL_DATA", "Cached content ${entity.id} is invalid; using catalog metadata.", it)
                        }.getOrNull()
                    }
                    snapshot ?: runCatching {
                        JSONObject(item.content).let { json ->
                            Corpus.story(StoryEntity(item.id, json.getString("family"), json.getString("category"), json.getString("level"), item.content))
                        }
                    }.onFailure {
                        DiagnosticLogger.error("LOCAL_DATA", "Catalog item ${item.id} is invalid and was skipped.", it)
                    }.getOrNull()
                }.sortedBy { it.order }
            }
        },
        repository.dao.vocabulary(), repository.dao.readings(), repository.dao.reviews()) { prefs, stories, words, reading, reviews ->
        LearningState(preferences = prefs, stories = stories, vocabulary = words, readings = reading, reviews = reviews)
    }
    private val content = combine(contentBase, repository.dao.exerciseResults()) { base, results ->
        base.copy(exerciseResults = results)
    }
    private val library = combine(content, services.stories.catalogState, services.stories.downloads) { content, catalog, downloads ->
        content.copy(catalogLoading = catalog.loading, catalogError = catalog.error, catalogStatus = catalog.status, downloads = downloads)
    }
    val state = combine(library, ready, error) { content, ready, error -> content.copy(ready = ready, error = error) }
        .catch {
            DiagnosticLogger.error("STATE", "The local learning-state stream failed.", it)
            emit(LearningState(error = "Data belajar lokal tidak dapat dimuat. Mulai ulang aplikasi untuk mencoba lagi."))
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LearningState())

    init {
        viewModelScope.launch {
            try {
                repository.initialize(application)
                preferences.ensureSupportedCourse()
                preferences.preferences.first()
                ready.value = true
                refreshCatalog()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                DiagnosticLogger.error("STARTUP", "The local learning library could not be initialized.", failure)
                error.value = "Pustaka luring tidak dapat disiapkan. Mulai ulang aplikasi untuk mencoba lagi."
            }
        }
        viewModelScope.launch { while (isActive) { _now.value = System.currentTimeMillis(); delay(30_000) } }
        viewModelScope.launch {
            try {
                preferences.preferences.map { it.target to it.native }.distinctUntilChanged().drop(1).collect {
                    languageGeneration++
                    _quiz.value = null
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { DiagnosticLogger.error("PREFERENCES", "Preference observation stopped unexpectedly.", failure) }
        }
    }
    fun refreshCatalog() { viewModelScope.launch {
        try { services.stories.refreshCatalog() } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Repository state and persistent diagnostics expose the failure. */ }
    } }
    suspend fun cachedStory(id: String) = try { services.stories.cached(id) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { DiagnosticLogger.error("STORY", "Cached story $id could not be read.", error); null }
    suspend fun openStory(id: String) = try { services.stories.open(id) } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { DiagnosticLogger.error("STORY", "Story $id could not be opened.", error); throw error }
    fun downloadLesson(id: String) { safeAction("Pelajaran belum dapat disimpan luring. Silakan coba lagi.") {
        services.downloadLesson(id); _message.value = "Pelajaran dan gambar tersimpan untuk akses luring"
    } }
    fun removeLessonDownload(id: String) { safeAction("Unduhan belum dapat dihapus. Silakan coba lagi.") {
        services.removeLessonDownload(id); _message.value = "Aset luring dihapus; kemajuan belajar tetap tersimpan"
    } }
    fun assetModel(story: Story, path: String): Any = services.stories.assetModel(story, path)
    fun clearMessage() { _message.value = null }
    private fun work(action: suspend () -> Unit) = viewModelScope.launch {
        try { action() } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            DiagnosticLogger.error("ACTION", "A local learning change failed.", failure)
            _message.value = "Perubahan belum dapat disimpan. Silakan coba lagi."
        }
    }
    private fun safeAction(message: String, action: suspend () -> Unit) = viewModelScope.launch {
        try { action() } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            DiagnosticLogger.error("ACTION", message, failure)
            _message.value = message
        }
    }
    fun font(size: Float) { work { preferences.font(size) } }
    fun dark(value: Boolean) { work { preferences.dark(value) } }
    suspend fun lookup(story: Story, index: Int, word: String, prefs: LearningPreferences) = try {
        repository.lookup(story, index, word, prefs.target, prefs.native)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (failure: Exception) { DiagnosticLogger.error("LOOKUP", "A word lookup failed in story ${story.id}.", failure); throw failure }
    suspend fun lookup(story: Story, word: String, prefs: LearningPreferences) = try {
        repository.lookup(story, word, prefs.target, prefs.native)
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (failure: Exception) { DiagnosticLogger.error("LOOKUP", "A vocabulary lookup failed in story ${story.id}.", failure); throw failure }
    fun save(story: Story, word: WordDetail, prefs: LearningPreferences) { work {
        val saved = repository.save(story, word, prefs.target, prefs.native, System.currentTimeMillis())
        _now.value = System.currentTimeMillis()
        _message.value = if (saved) "Tersimpan di kosakata · siap ditinjau" else "Sudah tersimpan dari cerita ini"
    } }
    fun mastery(word: VocabularyEntity) { work {
        repository.mastery(word.id, !word.mastered, System.currentTimeMillis())
        _now.value = System.currentTimeMillis()
    } }
    fun status(word: VocabularyEntity, status: VocabularyStatus) { work {
        repository.status(word.id, status, System.currentTimeMillis())
        _now.value = System.currentTimeMillis()
    } }
    fun status(story: Story, word: WordDetail, prefs: LearningPreferences, status: VocabularyStatus) { work {
        repository.status(story, word, prefs.target, prefs.native, status, System.currentTimeMillis())
        _now.value = System.currentTimeMillis()
    } }
    fun delete(word: VocabularyEntity) { work { repository.delete(word.id); _message.value = "Kata dihapus" } }
    fun reading(story: Story, language: Language, index: Int, completed: Boolean) { work {
        repository.reading(story, language, index, completed)
        if (completed) _message.value = "Cerita selesai · bagus"
    } }
    fun completeExercises(story: Story, score: Int, total: Int, answers: Map<String, String>) { work {
        repository.exerciseResult(story, state.value.preferences.target, score, total, answers, System.currentTimeMillis())
        _message.value = "$score dari $total benar · kemajuan tersimpan"
    } }
    fun startQuiz(mode: PracticeMode) {
        if (_busy.value) return
        val word = state.value.due(System.currentTimeMillis()).firstOrNull() ?: return
        val generation = languageGeneration
        _busy.value = true
        work {
            try {
                val lexemes = repository.dao.lexemes(word.language).map { JSONObject(it.content) }
                val distractors = lexemes
                    .filter { it.getString("pos") == word.partOfSpeech }
                    .map { it.getJSONObject("translations").getString(word.reference) }
                    .filter { !Words.matches(it, word.translation) }.distinct().shuffled().take(3)
                val source = lexemes.firstOrNull { Words.key(it.getString("word")) == word.word }
                val practice = source?.optJSONObject("practice")
                val reference = Language.from(word.reference)
                val contextual = word.example.replace(Regex("(?iu)(?<![\\p{L}\\p{M}])${Regex.escape(word.word)}(?![\\p{L}\\p{M}])"), "____")
                val effective = if (mode == PracticeMode.GRAMMAR && practice == null) PracticeMode.CONTEXT else mode
                val prompt = when (effective) {
                    PracticeMode.RECOGNITION, PracticeMode.RECALL -> word.word
                    PracticeMode.REVERSE -> word.translation
                    PracticeMode.CONTEXT -> contextual
                    PracticeMode.GRAMMAR -> practice!!.getJSONObject("prompt").getString(reference.code)
                }
                val expected = if (effective in setOf(PracticeMode.RECOGNITION, PracticeMode.RECALL)) word.translation else word.word
                val choices = when (effective) {
                    PracticeMode.RECOGNITION -> (distractors + expected).shuffled()
                    PracticeMode.GRAMMAR -> practice?.optJSONArray("choices")?.let { array ->
                        (0 until array.length()).map(array::getString)
                    }.orEmpty().ifEmpty { listOf(word.word) }
                    else -> emptyList()
                }
                val prefs = state.value.preferences
                if (generation == languageGeneration && prefs.target.code == word.language && prefs.native.code == word.reference) {
                    _quiz.value = Quiz(word, effective, prompt, expected, choices,
                        effective !in setOf(PracticeMode.RECOGNITION, PracticeMode.GRAMMAR))
                }
            } finally { _busy.value = false }
        }
    }
    fun answer(value: String) { _quiz.value = _quiz.value?.takeUnless { it.revealed }?.copy(response = value) ?: _quiz.value }
    fun reveal() {
        val quiz = _quiz.value ?: return
        if (!quiz.revealed && quiz.response.isNotBlank()) _quiz.value = quiz.copy(revealed = true,
            correct = Words.matches(quiz.response, quiz.expected))
    }
    fun rate(rating: Rating) {
        val question = _quiz.value ?: return
        if (!question.revealed || _busy.value) return
        _busy.value = true
        work {
            try {
                val saved = repository.review(question.word, rating, question.correct, System.currentTimeMillis())
                _quiz.value = null
                _now.value = System.currentTimeMillis()
                _message.value = if (saved) "Tinjauan tersimpan" else "Kata ini belum perlu ditinjau lagi"
            } finally { _busy.value = false }
        }
    }
    fun exitQuiz() { if (!_busy.value) _quiz.value = null }

    override fun onCleared() {
        tts.close()
        super.onCleared()
    }
}
