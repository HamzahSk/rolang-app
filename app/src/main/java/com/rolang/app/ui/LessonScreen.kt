package com.rolang.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.rolang.app.LearningState
import com.rolang.app.LearningViewModel
import com.rolang.app.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    lesson: Story,
    state: LearningState,
    viewModel: LearningViewModel,
    lessonNumber: Int,
    onBack: () -> Unit,
    onComplete: (Int, Int, Map<String, String>) -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
) {
    val sections = lesson.lessonSections
    var section by rememberSaveable(lesson.id) { mutableIntStateOf(0) }
    val completed = state.contentCompleted(lesson.id)
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
        if (sections.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Isi pelajaran belum tersedia luring.") }
            return@Column
        }
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(sectionLabel(sections[section].type), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text("${section + 1} / ${sections.size}", style = MaterialTheme.typography.labelLarge)
            }
            LinearProgressIndicator(progress = { (section + 1f) / sections.size }, Modifier.fillMaxWidth())
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(sections) { index, item ->
                    FilterChip(section == index, { section = index }, label = {
                        Text(item.title.indonesian, maxLines = 1)
                    })
                }
            }
        }
        AnimatedContent(section, modifier = Modifier.weight(1f), label = "textbook-section") { page ->
            val current = sections[page]
            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                if (page == 0) item { LessonHeader(lesson, state, viewModel) }
                item { SectionHeader(current) }
                itemsIndexed(current.blocks) { index, block ->
                    LessonBlockCard(lesson, current, block, index, viewModel)
                }
                if (page == sections.lastIndex) item {
                    Card(Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(if (completed) "Pelajaran selesai" else "Selesaikan pelajaran", style = MaterialTheme.typography.titleLarge)
                            Text("Pastikan Anda telah mengerjakan latihan asli pada bagian di atas.")
                            Button(onClick = { onComplete(1, 1, mapOf("textbook" to "completed")) },
                                enabled = !completed, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Outlined.CheckCircle, null); Spacer(Modifier.width(8.dp))
                                Text(if (completed) "Kemajuan tersimpan" else "Tandai selesai")
                            }
                            if (completed && onNext != null) OutlinedButton(onClick = onNext, Modifier.fillMaxWidth()) {
                                Text("Pelajaran berikutnya"); Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
                            }
                        }
                    }
                }
            }
        }
        Surface(tonalElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = { section-- }, enabled = section > 0) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, null); Spacer(Modifier.width(6.dp)); Text("Bagian sebelumnya")
                }
                Button(onClick = { section++ }, enabled = section < sections.lastIndex) {
                    Text("Bagian berikutnya"); Spacer(Modifier.width(6.dp)); Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
                }
            }
        }
    }
}

@Composable
private fun LessonHeader(lesson: Story, state: LearningState, viewModel: LearningViewModel) {
    Column(Modifier.widthIn(max = 760.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(lesson.title(state.preferences.native), style = MaterialTheme.typography.headlineMedium)
        Text("${lesson.unitTitle(state.preferences.native)} · ${lesson.category}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        PronunciationAvailabilityNotice()
        if (lesson.objectives.isNotEmpty()) Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Flag, null); Spacer(Modifier.width(8.dp)); Text("Tujuan pembelajaran", style = MaterialTheme.typography.titleMedium)
                }
                lesson.objectives.forEach { objective -> Text(objective[state.preferences.native.code].orEmpty()) }
            }
        }
        DownloadControls(lesson, state, viewModel)
    }
}

@Composable
private fun SectionHeader(section: LessonSection) {
    val color = when (section.type) {
        LessonSectionType.VOCABULARY -> MaterialTheme.colorScheme.tertiaryContainer
        LessonSectionType.DIALOGUE -> MaterialTheme.colorScheme.secondaryContainer
        LessonSectionType.ACTIVITY, LessonSectionType.PRACTICE -> MaterialTheme.colorScheme.primaryContainer
        LessonSectionType.GRAMMAR -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    Surface(Modifier.widthIn(max = 760.dp).fillMaxWidth(), color = color, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(sectionLabel(section.type), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(section.title.indonesian, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun LessonBlockCard(
    lesson: Story,
    section: LessonSection,
    block: LessonBlock,
    index: Int,
    viewModel: LearningViewModel,
) {
    when (block.type) {
        "image" -> SubcomposeAsyncImage(
            model = viewModel.assetModel(lesson, block.path),
            contentDescription = null,
            modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth().heightIn(min = 140.dp, max = 420.dp),
            contentScale = ContentScale.Fit,
            loading = { Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } },
            error = {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.BrokenImage, null)
                        Text("Ilustrasi belum tersedia. Materi teks tetap dapat dipelajari.")
                    }
                }
            },
            success = { SubcomposeAsyncImageContent() },
        )
        "table" -> Card(Modifier.widthIn(max = 760.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = sectionContainer(section.type))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                block.rows.forEachIndexed { rowIndex, row ->
                    if (rowIndex > 0) HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.cells.forEachIndexed { cellIndex, cell -> LessonTextContent(
                            cell, "${lesson.id}-${section.id}-$index-$rowIndex-$cellIndex")
                        }
                    }
                }
            }
        }
        else -> block.text?.let { text ->
            if (block.style == "heading") {
                Text(text.indonesian, Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            } else Card(Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = sectionContainer(section.type))) {
                Column(Modifier.padding(18.dp)) { LessonTextContent(text, "${lesson.id}-${section.id}-$index") }
            }
        }
    }
}

@Composable
private fun LessonTextContent(text: LessonText, speechId: String) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        if (text.sourceLanguage == "fil" || text.tagalog.isNotBlank()) {
            val spoken = text.tagalog.ifBlank { text.sourceText }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text.sourceText, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                PronunciationButton(spoken, speechId, "teks Tagalog")
            }
            if (text.indonesian != text.sourceText) Text(text.indonesian, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else Text(text.indonesian, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun sectionContainer(type: LessonSectionType) = when (type) {
    LessonSectionType.VOCABULARY -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
    LessonSectionType.DIALOGUE -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    LessonSectionType.ACTIVITY, LessonSectionType.PRACTICE -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    else -> MaterialTheme.colorScheme.surfaceContainerLow
}

private fun sectionLabel(type: LessonSectionType) = when (type) {
    LessonSectionType.OVERVIEW -> "Gambaran umum"
    LessonSectionType.VOCABULARY -> "Kosakata"
    LessonSectionType.DIALOGUE -> "Dialog"
    LessonSectionType.ACTIVITY -> "Aktivitas"
    LessonSectionType.GRAMMAR -> "Tata bahasa"
    LessonSectionType.PRACTICE -> "Latihan"
    LessonSectionType.CONTENT -> "Materi"
}
