package com.rolang.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.rolang.app.LearningState
import com.rolang.app.data.VocabularyEntity
import com.rolang.app.domain.VocabularyStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    state: LearningState,
    now: Long,
    mastery: (VocabularyEntity) -> Unit,
    delete: (VocabularyEntity) -> Unit,
    status: (VocabularyEntity, VocabularyStatus) -> Unit,
    stories: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Semua kata") }
    var group by rememberSaveable { mutableStateOf("Cerita") }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var remove by remember { mutableStateOf<VocabularyEntity?>(null) }
    val words =
        state.courseWords.filter {
            (filter == "Semua kata" || it.learningStatus().label == filter) &&
                (it.word.contains(query, true) ||
                    it.translation.contains(query, true) ||
                    it.storyTitle.contains(query, true))
        }
    LazyColumn(
        modifier = Modifier.testTag("vocabulary-list"),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            SectionTitle(
                "Kosakata tersimpan",
                "${state.preferences.target.label} → ${state.preferences.native.label}",
            )
        }
        item { PronunciationAvailabilityNotice() }
        item {
            OutlinedTextField(
                query,
                { query = it },
                Modifier.fillMaxWidth(),
                label = { Text("Cari kosakata") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
            )
        }
        item {
            Column {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf("Semua kata") + VocabularyStatus.entries.map { it.label }).forEach {
                        item ->
                        FilterChip(filter == item, { filter = item }, label = { Text(item) })
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Kelompokkan", style = MaterialTheme.typography.labelLarge)
                    listOf("Cerita", "Kategori").forEach { item ->
                        FilterChip(group == item, { group = item }, label = { Text(item) })
                    }
                }
            }
        }
        if (state.courseWords.isEmpty())
            item {
                InfoCard(
                    "Koleksi dimulai dari cerita",
                    "Ketuk kata dalam bacaan, lihat terjemahannya, lalu simpan untuk latihan.",
                    "Lihat cerita",
                    stories,
                )
            }
        else if (words.isEmpty())
            item { InfoCard("Kata tidak ditemukan", "Coba pencarian atau filter lain.") }
        words
            .groupBy { if (group == "Cerita") it.storyTitle else it.category }
            .forEach { (title, grouped) ->
                item(key = "group:$title") {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                items(grouped, key = { it.id }) { word ->
                    Card(
                        onClick = { selectedId = word.id },
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(word.word, style = MaterialTheme.typography.titleLarge)
                                VocabularyStatusLabel(word.learningStatus())
                                Text(word.translation)
                                Text(
                                    if (word.mastered) "Dikuasai"
                                    else if (word.dueAt <= now) "Siap ditinjau"
                                    else "Tinjauan berikutnya: ${dueDate(word.dueAt)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            PronunciationButton(word.word, "vocabulary-word-${word.id}", word.word)
                            IconButton(onClick = { mastery(word) }) {
                                Icon(
                                    if (word.mastered) Icons.Outlined.CheckCircle
                                    else Icons.Outlined.RadioButtonUnchecked,
                                    if (word.mastered) "Tandai ${word.word} sebagai dipelajari"
                                    else "Tandai ${word.word} sebagai dikuasai",
                                )
                            }
                        }
                    }
                }
            }
    }
    val selected = state.courseWords.firstOrNull { it.id == selectedId }
    if (selected != null)
        ModalBottomSheet(onDismissRequest = { selectedId = null }) {
            LazyColumn(
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            SectionTitle(
                                selected.word,
                                "${selected.translation} · ${selected.partOfSpeech}",
                            )
                        }
                        PronunciationButton(
                            selected.word,
                            "vocabulary-detail-${selected.id}",
                            selected.word,
                        )
                        IconButton(onClick = { selectedId = null }) {
                            Icon(Icons.Outlined.Close, "Tutup rincian kosakata")
                        }
                    }
                }
                item { DetailSection("Makna", selected.meaning) }
                item { DetailSection("Dalam konteks", selected.explanation) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DetailSection("Contoh", selected.example)
                            PronunciationButton(
                                selected.example,
                                "vocabulary-example-${selected.id}",
                                "contoh ${selected.word}",
                            )
                        }
                        Text(
                            selected.exampleTranslation,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item {
                    DetailSection(
                        "Dari ${selected.storyTitle}",
                        "${selected.category} · ${selected.repetitions} pengulangan berhasil · ${selected.lapses} kesalahan",
                    )
                }
                item {
                    VocabularyStatusSelector(selected.learningStatus()) { status(selected, it) }
                }
                item {
                    Button(onClick = { mastery(selected) }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (selected.mastered) "Kembalikan ke pembelajaran"
                            else "Tandai sebagai dikuasai"
                        )
                    }
                    TextButton(onClick = { remove = selected }) {
                        Text("Hapus kata", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    remove?.let { word ->
        AlertDialog(
            onDismissRequest = { remove = null },
            title = { Text("Hapus ${word.word}?") },
            text = {
                Text(
                    "Kata tersimpan dan jadwal tinjauannya akan dihapus. Kata dapat disimpan lagi dari cerita."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        delete(word)
                        remove = null
                        selectedId = null
                    }
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = { TextButton(onClick = { remove = null }) { Text("Simpan") } },
        )
    }
}

fun dueDate(millis: Long): String =
    DateTimeFormatter.ofPattern("MMM d · HH:mm")
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
