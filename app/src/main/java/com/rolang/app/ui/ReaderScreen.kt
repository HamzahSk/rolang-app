package com.rolang.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rolang.app.LearningState
import com.rolang.app.LearningViewModel
import com.rolang.app.data.Story
import com.rolang.app.data.WordDetail
import com.rolang.app.domain.Language
import com.rolang.app.domain.Words
import com.rolang.app.domain.VocabularyStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged

private data class SelectedWord(val text: String, val paragraph: Int)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderScreen(story: Story, state: LearningState, viewModel: LearningViewModel, onBack: () -> Unit) {
    val chapterScope = rememberCoroutineScope()
    val prefs = state.preferences
    var readingCode by rememberSaveable(story.id) { mutableStateOf(Language.FILIPINO.code) }
    val readingLanguage = Language.from(readingCode)
    val translationLanguage = if (readingLanguage == Language.FILIPINO) Language.INDONESIAN else Language.FILIPINO
    val savedReading = state.readings.firstOrNull { it.storyId == story.id && it.language == readingLanguage.code }
    val listState = rememberSaveable(story.id, readingLanguage.code, saver = LazyListState.Saver) {
        val paragraph = (savedReading?.sentence ?: 0).coerceIn(story.sentences.indices)
        LazyListState(if (paragraph == 0) 0 else paragraph + 1)
    }
    val index by remember(story.sentences.size, listState) {
        derivedStateOf {
            ((listState.layoutInfo.visibleItemsInfo.lastOrNull { it.index in 1..story.sentences.size }?.index
                ?: listState.firstVisibleItemIndex) - 1).coerceIn(story.sentences.indices)
        }
    }
    var translated by rememberSaveable(story.id, readingLanguage.code) { mutableStateOf(false) }
    var selected by remember(story.id, readingLanguage, prefs.target, prefs.native) { mutableStateOf<SelectedWord?>(null) }
    var detail by remember(selected) { mutableStateOf<WordDetail?>(null) }
    var lookupError by remember(selected) { mutableStateOf(false) }
    val textColor = MaterialTheme.colorScheme.onSurface
    val statuses = state.courseWords.filter { it.storyId == story.id }.associate { it.word to it.learningStatus() }
    LaunchedEffect(story.id, readingLanguage, story.sentences.size, listState) {
        // Clamp restored positions when a downloaded story has been shortened.
        if (listState.firstVisibleItemIndex > story.sentences.size) {
            listState.scrollToItem(story.sentences.size)
        }
        snapshotFlow { (listState.firstVisibleItemIndex - 1).coerceIn(story.sentences.indices) }
            .distinctUntilChanged().collect { paragraph ->
                viewModel.reading(story, readingLanguage, paragraph, savedReading?.completed == true)
            }
    }
    LaunchedEffect(selected, prefs.target, prefs.native) {
        selected?.let { word ->
            try { detail = viewModel.lookup(story, word.paragraph, word.text, prefs) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { lookupError = true }
        }
    }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Ruang baca", style = MaterialTheme.typography.titleMedium) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali ke daftar cerita") } },
            actions = {
                IconButton(onClick = { viewModel.toggleBookmark(story.id) }) {
                    Icon(if (story.id in prefs.bookmarkedStories) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        if (story.id in prefs.bookmarkedStories) "Hapus tanda cerita" else "Tandai cerita")
                }
            })
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Paragraf ${index + 1} dari ${story.sentences.size}", style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(progress = { (index + 1f) / story.sentences.size }, modifier = Modifier.fillMaxWidth())
        }
        LazyColumn(Modifier.weight(1f).testTag("reader-content"), state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            item(key = "heading") {
                Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    StoryIllustration(story, viewModel, Modifier.fillMaxWidth().height(150.dp), ContentScale.Fit)
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(story.title(prefs.native), style = MaterialTheme.typography.headlineMedium)
                        Text("${story.kind.label} · ${story.category} · ${story.level.label}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        Text(story.summary(prefs.native), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 1.dp) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    FilterChip(readingLanguage == Language.FILIPINO, { readingCode = Language.FILIPINO.code }, label = { Text("Tagalog") })
                                    FilterChip(readingLanguage == Language.INDONESIAN, { readingCode = Language.INDONESIAN.code }, label = { Text("Indonesia") })
                                }
                                if (readingLanguage == Language.FILIPINO) PronunciationSequenceControls("story-${story.id}",
                                    story.sentences.indices.map { story.sentence(it, Language.FILIPINO) },
                                    { "story-sentence-${story.id}-$it" }, "Putar seluruh cerita", compact = true)
                            }
                            TextButton(onClick = { translated = !translated }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                                Icon(Icons.Outlined.Translate, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (translated) "Sembunyikan terjemahan" else "Tampilkan terjemahan")
                            }
                            Text(if (readingLanguage == Language.FILIPINO) "Ketuk kata Tagalog untuk melihat artinya."
                                else "Gunakan mode Tagalog untuk membuka rincian kosakata.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (readingLanguage == Language.FILIPINO) PronunciationAvailabilityNotice()
                    DownloadControls(story, state, viewModel, compact = true)
                    if (story.chapters.size > 1) {
                        Text("Bab", style = MaterialTheme.typography.titleMedium)
                        story.chapters.forEach { chapter -> TextButton(onClick = {
                            chapterScope.launch { listState.scrollToItem(chapter.start + 1) }
                        }) { Text(chapter.title.getValue(prefs.native.code)) } }
                    }
                    VocabularyLegend(Modifier.fillMaxWidth())
                }
            }
            itemsIndexed(story.sentences, key = { paragraph, _ -> "paragraph:$paragraph" }) { paragraph, _ ->
                val text = story.sentence(paragraph, readingLanguage)
                val annotated = remember(story.id, paragraph, readingLanguage, text, textColor, statuses) {
                    if (readingLanguage == Language.INDONESIAN) AnnotatedString(text)
                    else buildAnnotatedString {
                            append(text)
                            Words.tokens(text).forEach { token ->
                                addLink(LinkAnnotation.Clickable(token.text,
                                    styles = TextLinkStyles(style = readerWordStyle(
                                        statuses[Words.key(token.text)] ?: VocabularyStatus.NEW, textColor)),
                                    linkInteractionListener = { selected = SelectedWord(token.text, paragraph) }), token.start, token.end)
                            }
                        }
                }
                Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(story.speakers.getOrNull(paragraph) ?: "Paragraf ${paragraph + 1}",
                            Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (readingLanguage == Language.FILIPINO) PronunciationButton(text,
                            "story-sentence-${story.id}-$paragraph", "${story.kind.label.lowercase()} ${paragraph + 1}")
                    }
                    Text(annotated, Modifier.fillMaxWidth().testTag("story-paragraph-$paragraph"),
                        style = TextStyle(fontFamily = FontFamily.Serif, fontSize = prefs.fontSize.sp,
                            lineHeight = (prefs.fontSize * 1.85).sp))
                    AnimatedVisibility(translated) {
                        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Text(story.sentence(paragraph, translationLanguage), Modifier.padding(14.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            if (story.objectives.isNotEmpty()) item(key = "objectives") {
                Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tujuan pembelajaran", style = MaterialTheme.typography.titleLarge)
                    story.objectives.forEach { Text("• ${it.getValue(prefs.native.code)}") }
                }
            }
            if (story.pronunciation[prefs.native.code]?.isNotBlank() == true) item(key = "pronunciation") {
                Card(Modifier.widthIn(max = 680.dp).fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(20.dp)) {
                        DetailSection("Panduan pelafalan", story.pronunciation.getValue(prefs.native.code))
                    }
                }
            }
            itemsIndexed(story.notes, key = { index, _ -> "note:$index" }) { _, note ->
                Card(Modifier.widthIn(max = 680.dp).fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(20.dp)) { DetailSection(note.title(prefs.native), note.text(prefs.native)) }
                }
            }
            if (story.focus(prefs.target).isNotEmpty()) item(key = "focus") {
                Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Kosakata utama", style = MaterialTheme.typography.titleLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        story.focus(prefs.target).forEach { expression ->
                            val status = state.wordStatus(story.id, expression)
                            val style = vocabularyStyle(status, textColor)
                            FilterChip(false, {
                                val paragraph = story.sentences.indexOfFirst {
                                    it.getValue(prefs.target.code).contains(expression, ignoreCase = true)
                                }.coerceAtLeast(0)
                                selected = SelectedWord(expression, paragraph)
                            }, label = { Text(expression) }, colors = FilterChipDefaults.filterChipColors(
                                containerColor = style.background, labelColor = style.color))
                        }
                    }
                }
            }
            item(key = "exercises") {
                ExerciseSession(story, state, { score, total, answers ->
                    viewModel.completeExercises(story, score, total, answers)
                }, onBack)
            }
        }
    }
    if (selected != null) ModalBottomSheet(
        onDismissRequest = { selected = null },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        val word = detail
        LazyColumn(modifier = Modifier.fillMaxWidth().testTag("word-details"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(selected!!.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary)
                    PronunciationButton(selected!!.text, "reader-word-${story.id}-${selected!!.paragraph}-${selected!!.text}", selected!!.text)
                    IconButton(onClick = { selected = null }) { Icon(Icons.Outlined.Close, "Tutup rincian kata") }
                }
            }
            if (word == null) item {
                if (lookupError) Text("Terjemahan ini belum dapat dibuka. Tutup lalu ketuk katanya untuk mencoba lagi.")
                else CircularProgressIndicator()
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Makna utama", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(word.translation, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        Text("${word.partOfSpeech} · ${prefs.target.label} → ${prefs.native.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item { SheetSection("Makna", word.meaning) }
                item { SheetSection("Pelafalan dan konteks", word.explanation) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Contoh", style = MaterialTheme.typography.titleMedium)
                        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            shape = MaterialTheme.shapes.medium) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(word.example, Modifier.weight(1f).padding(start = 14.dp, top = 12.dp, bottom = 12.dp),
                                    style = MaterialTheme.typography.bodyLarge)
                                PronunciationButton(word.example, "reader-example-${story.id}-${selected!!.paragraph}-${word.word}",
                                    "contoh ${word.word}")
                            }
                        }
                        Text(word.exampleTranslation, Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    VocabularyStatusSelector(state.wordStatus(story.id, word.word), scrollable = true) {
                        viewModel.status(story, word, prefs, it)
                    }
                }
            }
            if (word != null) item {
                val saved = state.courseWords.any { it.storyId == story.id && it.word == Words.key(word.word) }
                Button(onClick = { viewModel.save(story, word, prefs) }, enabled = !saved,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Icon(Icons.Outlined.BookmarkAdd, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (saved) "Tersimpan di kosakata" else "Simpan kata")
                }
            }
        }
    }
}

@Composable
private fun SheetSection(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DetailSection(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Keep word links readable across line breaks without painting every token as a box. */
private fun readerWordStyle(status: VocabularyStatus, textColor: Color): SpanStyle {
    val tint = vocabularyStyle(status, textColor).background
    return SpanStyle(
        color = textColor,
        background = if (status == VocabularyStatus.NEW || status == VocabularyStatus.KNOWN)
            Color.Transparent else tint.copy(alpha = 0.32f),
        textDecoration = if (status == VocabularyStatus.NEW) TextDecoration.Underline else null,
    )
}
