package com.rolang.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.NavigateBefore
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rolang.app.LearningState
import com.rolang.app.data.Story

private val lessonSections = listOf("Tujuan", "Kosakata", "Tata bahasa", "Contoh", "Latihan")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LessonScreen(
    lesson: Story,
    state: LearningState,
    lessonNumber: Int,
    onBack: () -> Unit,
    onComplete: (Int, Int, Map<String, String>) -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
) {
    var section by rememberSaveable(lesson.id) { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Pelajaran $lessonNumber") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali ke daftar pelajaran") }
        }, actions = {
            IconButton(onClick = { onPrevious?.invoke() }, enabled = onPrevious != null) {
                Icon(Icons.AutoMirrored.Outlined.NavigateBefore, "Pelajaran sebelumnya")
            }
            IconButton(onClick = { onNext?.invoke() }, enabled = onNext != null) {
                Icon(Icons.AutoMirrored.Outlined.NavigateNext, "Pelajaran berikutnya")
            }
        })
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(lessonSections[section], style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("${section + 1} / ${lessonSections.size}", style = MaterialTheme.typography.labelLarge)
            }
            LinearProgressIndicator(progress = { (section + 1f) / lessonSections.size }, Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                lessonSections.forEachIndexed { index, title ->
                    FilterChip(section == index, { section = index }, label = { Text(title) })
                }
            }
        }
        AnimatedContent(section, modifier = Modifier.weight(1f), label = "lesson-section") { page ->
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                when (page) {
                    0 -> item { LessonOverview(lesson, state) }
                    1 -> item { VocabularySection(lesson, state) }
                    2 -> item { GrammarSection(lesson, state) }
                    3 -> itemsIndexed(lesson.sentences) { index, _ -> ExampleCard(lesson, state, index) }
                    else -> item {
                        ExerciseSession(lesson, state, onComplete) {
                            onNext?.invoke() ?: onBack()
                        }
                    }
                }
            }
        }
        if (section < lessonSections.lastIndex) {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = { section-- }, enabled = section > 0) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null); Spacer(Modifier.width(6.dp)); Text("Sebelumnya")
                    }
                    Button(onClick = { section++ }) {
                        Text("Berikutnya: ${lessonSections[section + 1]}"); Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonOverview(lesson: Story, state: LearningState) {
    Column(Modifier.widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Image(painterResource(illustrationResource(lesson.image)), null, Modifier.fillMaxWidth().height(150.dp), contentScale = ContentScale.Fit)
        Text(lesson.title(state.preferences.target), style = MaterialTheme.typography.headlineMedium)
        Text(lesson.summary(state.preferences.target), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Flag, null); Spacer(Modifier.width(8.dp)); Text("Tujuan pembelajaran", style = MaterialTheme.typography.titleMedium)
                }
                lesson.objectives.forEach { Text("• ${it.getValue(state.preferences.native.code)}") }
                Text("Selesaikan kelima bagian dan latihan akhir untuk menuntaskan pelajaran ini.",
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VocabularySection(lesson: Story, state: LearningState) {
    val target = lesson.focus(state.preferences.target)
    val native = lesson.focus(state.preferences.native)
    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Kosakata utama", "Pelajari ungkapan inti sebelum membaca contoh.")
        target.forEachIndexed { index, word ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(word, style = MaterialTheme.typography.titleMedium)
                        Text(native.getOrNull(index).orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.RecordVoiceOver, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun GrammarSection(lesson: Story, state: LearningState) {
    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("Tata bahasa dan penggunaan", "Penjelasan singkat yang dapat langsung dipakai.")
        lesson.pronunciation[state.preferences.native.code]?.takeIf(String::isNotBlank)?.let { guide ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(20.dp)) { DetailSection("Panduan pelafalan", guide) }
            }
        }
        lesson.notes.forEach { note ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(20.dp)) { DetailSection(note.title(state.preferences.native), note.text(state.preferences.native)) }
            }
        }
    }
}

@Composable
private fun ExampleCard(lesson: Story, state: LearningState, index: Int) {
    Card(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CONTOH ${index + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            Text(lesson.sentence(index, state.preferences.target), style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            Text(lesson.sentence(index, state.preferences.native), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
