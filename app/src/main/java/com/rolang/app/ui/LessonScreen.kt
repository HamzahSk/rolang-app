package com.rolang.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.NavigateBefore
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.automirrored.outlined.Toc
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
        TopAppBar(
            title = { Text("Pelajaran $lessonNumber") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali ke daftar pelajaran")
                }
            },
            actions = {
                IconButton(onClick = { onPrevious?.invoke() }, enabled = onPrevious != null) {
                    Icon(Icons.AutoMirrored.Outlined.NavigateBefore, "Pelajaran sebelumnya")
                }
                IconButton(onClick = { onNext?.invoke() }, enabled = onNext != null) {
                    Icon(Icons.AutoMirrored.Outlined.NavigateNext, "Pelajaran berikutnya")
                }
            },
        )
        if (sections.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Isi pelajaran belum tersedia luring.")
            }
            return@Column
        }
        LessonSectionNavigator(sections, section) { section = it }
        AnimatedContent(section, modifier = Modifier.weight(1f), label = "textbook-section") { page
            ->
            val current = sections[page]
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (page == 0) item { LessonHeader(lesson, state, viewModel) }
                item { SectionHeader(current) }
                itemsIndexed(current.blocks) { index, block ->
                    LessonBlockCard(lesson, current, block, index, viewModel)
                }
                if (page == sections.lastIndex)
                    item {
                        Card(
                            Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                        ) {
                            Column(
                                Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    if (completed) "Pelajaran selesai" else "Selesaikan pelajaran",
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    "Pastikan Anda telah mengerjakan latihan asli pada bagian di atas."
                                )
                                Button(
                                    onClick = {
                                        onComplete(1, 1, mapOf("textbook" to "completed"))
                                    },
                                    enabled = !completed,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Outlined.CheckCircle, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (completed) "Kemajuan tersimpan" else "Tandai selesai")
                                }
                                if (completed && onNext != null)
                                    OutlinedButton(onClick = onNext, Modifier.fillMaxWidth()) {
                                        Text("Pelajaran berikutnya")
                                        Spacer(Modifier.width(8.dp))
                                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
                                    }
                            }
                        }
                    }
            }
        }
        Surface(tonalElevation = 3.dp) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                OutlinedButton(onClick = { section-- }, enabled = section > 0) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Bagian sebelumnya")
                }
                Button(onClick = { section++ }, enabled = section < sections.lastIndex) {
                    Text("Bagian berikutnya")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
                }
            }
        }
    }
}

@Composable
internal fun LessonSectionNavigator(
    sections: List<LessonSection>,
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    var sectionMenu by remember { mutableStateOf(false) }
    val sectionListState = rememberLazyListState()
    LaunchedEffect(selected) { sectionListState.animateScrollToItem(selected) }
    Column(
        Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box {
                TextButton(
                    onClick = { sectionMenu = true },
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Toc, null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        sectionLabel(sections[selected].type),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Icon(Icons.Outlined.ArrowDropDown, null)
                }
                DropdownMenu(expanded = sectionMenu, onDismissRequest = { sectionMenu = false }) {
                    sections.forEachIndexed { index, item ->
                        DropdownMenuItem(
                            text = { Text("${index + 1}. ${item.title.indonesian}") },
                            leadingIcon = {
                                if (index == selected) Icon(Icons.Outlined.Check, null)
                            },
                            onClick = {
                                onSelected(index)
                                sectionMenu = false
                            },
                        )
                    }
                }
            }
            Text("${selected + 1} / ${sections.size}", style = MaterialTheme.typography.labelLarge)
        }
        LinearProgressIndicator(
            progress = { (selected + 1f) / sections.size },
            Modifier.fillMaxWidth(),
        )
        LazyRow(state = sectionListState, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(sections) { index, item ->
                FilterChip(
                    selected == index,
                    { onSelected(index) },
                    label = { Text(item.title.indonesian, maxLines = 1) },
                )
            }
        }
    }
}

@Composable
private fun LessonHeader(lesson: Story, state: LearningState, viewModel: LearningViewModel) {
    Column(
        Modifier.widthIn(max = 760.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            lesson.title(state.preferences.native),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            "${lesson.unitTitle(state.preferences.native)} · ${lesson.category}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PronunciationAvailabilityNotice()
        DownloadControls(lesson, state, viewModel)
    }
}

@Composable
private fun SectionHeader(section: LessonSection) {
    val color =
        when (section.type) {
            LessonSectionType.VOCABULARY -> MaterialTheme.colorScheme.tertiaryContainer
            LessonSectionType.DIALOGUE -> MaterialTheme.colorScheme.secondaryContainer
            LessonSectionType.EXERCISES,
            LessonSectionType.ACTIVITY,
            LessonSectionType.PRACTICE -> MaterialTheme.colorScheme.primaryContainer
            LessonSectionType.EXAMPLES ->
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
            LessonSectionType.SUMMARY -> MaterialTheme.colorScheme.tertiaryContainer
            LessonSectionType.LEARNING_MATERIAL,
            LessonSectionType.GRAMMAR -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceContainer
        }
    Surface(
        Modifier.widthIn(max = 760.dp).fillMaxWidth(),
        color = color,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                sectionLabel(section.type),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
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
        "image" ->
            SubcomposeAsyncImage(
                model = viewModel.assetModel(lesson, block.path),
                contentDescription = null,
                modifier =
                    Modifier.widthIn(max = 760.dp)
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 420.dp),
                contentScale = ContentScale.Fit,
                loading = {
                    Box(
                        Modifier.fillMaxWidth().height(160.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                },
                error = {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Outlined.BrokenImage, null)
                            Text("Ilustrasi belum tersedia. Materi teks tetap dapat dipelajari.")
                        }
                    }
                },
                success = { SubcomposeAsyncImageContent() },
            )
        "vocabulary" -> VocabularyBlockCard(lesson, section, block, index)
        "dialogue" -> DialogueBlockCard(lesson, section, block, index)
        "example" -> ExampleBlockCard(lesson, section, block, index)
        "exercise" -> ExerciseBlockCard(lesson, section, block, index)
        "list" -> ListBlockCard(block)
        "table" ->
            Card(
                Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = sectionContainer(section.type)),
            ) {
                LessonRows(block.rows, "${lesson.id}-${section.id}-$index", Modifier.padding(16.dp))
            }
        else ->
            block.text?.let { text ->
                if (block.style == "heading") {
                    Text(
                        text.indonesian,
                        Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else
                    Card(
                        Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(containerColor = sectionContainer(section.type)),
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            LessonTextContent(text, "${lesson.id}-${section.id}-$index")
                        }
                    }
            }
    }
}

@Composable
private fun VocabularyBlockCard(
    lesson: Story,
    section: LessonSection,
    block: LessonBlock,
    index: Int,
) {
    ElevatedCard(Modifier.widthIn(max = 760.dp).fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            block.title?.let {
                Text(
                    it.indonesian,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            block.entries.forEachIndexed { entryIndex, entry ->
                if (entryIndex > 0) HorizontalDivider()
                val tagalog = entry.term.tagalog.ifBlank { entry.term.sourceText }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            tagalog,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(entry.meaning.indonesian, style = MaterialTheme.typography.bodyLarge)
                    }
                    PronunciationButton(
                        tagalog,
                        "${lesson.id}-${section.id}-$index-vocab-$entryIndex",
                        "kosakata Tagalog",
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogueBlockCard(
    lesson: Story,
    section: LessonSection,
    block: LessonBlock,
    index: Int,
) {
    val speakers = block.turns.map { it.speaker }.distinct()
    Card(
        Modifier.widthIn(max = 760.dp).fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            block.turns.forEachIndexed { turnIndex, turn ->
                val alternate = speakers.indexOf(turn.speaker).let { it >= 0 && it % 2 == 1 }
                Row(Modifier.fillMaxWidth()) {
                    if (alternate) Spacer(Modifier.weight(0.12f))
                    Surface(
                        Modifier.weight(0.88f),
                        color =
                            if (alternate) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 1.dp,
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                turn.speaker,
                                style = MaterialTheme.typography.labelLarge,
                                color =
                                    if (alternate) MaterialTheme.colorScheme.onTertiaryContainer
                                    else MaterialTheme.colorScheme.primary,
                            )
                            LessonTextContent(
                                turn.text,
                                "${lesson.id}-${section.id}-$index-dialogue-$turnIndex",
                            )
                        }
                    }
                    if (!alternate) Spacer(Modifier.weight(0.12f))
                }
            }
        }
    }
}

@Composable
private fun ExampleBlockCard(
    lesson: Story,
    section: LessonSection,
    block: LessonBlock,
    index: Int,
) {
    Card(
        Modifier.widthIn(max = 760.dp).fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
            ),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lightbulb, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text("Contoh", style = MaterialTheme.typography.labelLarge)
            }
            block.items.forEachIndexed { itemIndex, item ->
                if (itemIndex > 0) HorizontalDivider()
                LessonTextContent(item, "${lesson.id}-${section.id}-$index-example-$itemIndex")
            }
            if (block.rows.isNotEmpty())
                LessonRows(block.rows, "${lesson.id}-${section.id}-$index-example")
        }
    }
}

@Composable
private fun ExerciseBlockCard(
    lesson: Story,
    section: LessonSection,
    block: LessonBlock,
    index: Int,
) {
    var response by rememberSaveable(lesson.id, section.id, index) { mutableStateOf("") }
    val container =
        when (block.role) {
            "answer" -> MaterialTheme.colorScheme.tertiaryContainer
            "question" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        }
    Card(
        Modifier.widthIn(max = 760.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when (block.role) {
                        "question" -> Icons.Outlined.EditNote
                        "answer" -> Icons.Outlined.Key
                        "worksheet" -> Icons.Outlined.GridOn
                        else -> Icons.AutoMirrored.Outlined.Assignment
                    },
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(exerciseRoleLabel(block.role), style = MaterialTheme.typography.labelLarge)
            }
            block.text?.let { LessonTextContent(it, "${lesson.id}-${section.id}-$index-exercise") }
            if (block.rows.isNotEmpty())
                LessonRows(block.rows, "${lesson.id}-${section.id}-$index-worksheet")
            if (block.role == "question")
                OutlinedTextField(
                    value = response,
                    onValueChange = { response = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Jawaban Anda") },
                    minLines = 2,
                )
        }
    }
}

@Composable
private fun ListBlockCard(block: LessonBlock) {
    val objectives = block.style == "objectives"
    Card(
        Modifier.widthIn(max = 760.dp).fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (objectives) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow
            ),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (objectives) Icons.Outlined.Flag else Icons.AutoMirrored.Outlined.Toc, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (objectives) "Tujuan pembelajaran" else "Isi pelajaran",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            block.items.forEach { item ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("•", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        item.indonesian.removePrefix("•").trim(),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonRows(rows: List<LessonRow>, speechPrefix: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) HorizontalDivider()
            if (row.cells.size == 1) {
                LessonTextContent(row.cells.first(), "$speechPrefix-$rowIndex-0")
            } else
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.cells.forEachIndexed { cellIndex, cell ->
                        Surface(
                            Modifier.width(240.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Box(Modifier.padding(12.dp)) {
                                LessonTextContent(cell, "$speechPrefix-$rowIndex-$cellIndex")
                            }
                        }
                    }
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
                Text(spoken, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                PronunciationButton(spoken, speechId, "teks Tagalog")
            }
            if (text.indonesian != spoken)
                Text(text.indonesian, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                text.indonesian.split(Regex("\\n\\s*\\n")).filter(String::isNotBlank).forEach {
                    Text(it.trim(), style = MaterialTheme.typography.bodyLarge)
                }
            }
    }
}

@Composable
private fun sectionContainer(type: LessonSectionType) =
    when (type) {
        LessonSectionType.VOCABULARY ->
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        LessonSectionType.DIALOGUE ->
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        LessonSectionType.EXAMPLES ->
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        LessonSectionType.EXERCISES,
        LessonSectionType.ACTIVITY,
        LessonSectionType.PRACTICE -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        LessonSectionType.SUMMARY -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

private fun sectionLabel(type: LessonSectionType) =
    when (type) {
        LessonSectionType.INTRODUCTION -> "Pengantar"
        LessonSectionType.LEARNING_MATERIAL -> "Materi"
        LessonSectionType.OVERVIEW -> "Gambaran umum"
        LessonSectionType.VOCABULARY -> "Kosakata"
        LessonSectionType.EXAMPLES -> "Contoh"
        LessonSectionType.DIALOGUE -> "Dialog"
        LessonSectionType.EXERCISES -> "Latihan"
        LessonSectionType.SUMMARY -> "Ringkasan"
        LessonSectionType.ACTIVITY -> "Aktivitas"
        LessonSectionType.GRAMMAR -> "Tata bahasa"
        LessonSectionType.PRACTICE -> "Latihan"
        LessonSectionType.CONTENT -> "Materi"
    }

private fun exerciseRoleLabel(role: String) =
    when (role) {
        "question" -> "Pertanyaan"
        "answer" -> "Kunci jawaban"
        "worksheet" -> "Lembar latihan"
        "instruction" -> "Petunjuk"
        else -> "Latihan"
    }
