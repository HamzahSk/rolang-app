package com.rolang.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.rolang.app.LearningState
import com.rolang.app.Quiz
import com.rolang.app.domain.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewScreen(
    state: LearningState,
    now: Long,
    quiz: Quiz?,
    busy: Boolean,
    start: (PracticeMode) -> Unit,
    answer: (String) -> Unit,
    reveal: () -> Unit,
    rate: (Rating) -> Unit,
    exit: () -> Unit,
    stories: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(PracticeMode.RECOGNITION) }
    val due = state.due(now)
    BackHandler(quiz != null) { exit() }
    LazyColumn(
        modifier = Modifier.testTag("review-content"),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            SectionTitle("Kuatkan ingatan kata", "Latihan singkat membantu mengingat lebih lama.")
        }
        item { PronunciationAvailabilityNotice() }
        if (quiz == null) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("${due.size}", "Siap ditinjau", Modifier.weight(1f))
                    StatTile(
                        "${state.courseWords.count { it.mastered }}",
                        "Dikuasai",
                        Modifier.weight(1f),
                    )
                }
            }
            if (due.isEmpty())
                item {
                    val next = state.courseWords.filter { !it.mastered }.minByOrNull { it.dueAt }
                    InfoCard(
                        if (state.courseWords.isEmpty()) "Simpan kata pertama"
                        else "Semua tinjauan selesai",
                        if (state.courseWords.isEmpty())
                            "Kata tersimpan akan menjadi soal di sini. Mulailah dengan membaca cerita."
                        else if (next != null)
                            "Tinjauan berikutnya: ${dueDate(next.dueAt)}. Bacalah cerita baru sambil menunggu."
                        else "Semua kata sudah dikuasai. Teruslah membaca untuk menambah koleksi.",
                        "Lihat cerita",
                        stories,
                    )
                }
            else {
                item { ListeningPracticeCard(due.first()) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Pilih latihan", style = MaterialTheme.typography.titleMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PracticeMode.entries.forEach { value ->
                                FilterChip(
                                    mode == value,
                                    { mode = value },
                                    label = { Text(value.label) },
                                )
                            }
                        }
                        Button(
                            onClick = { start(mode) },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Latih kata berikutnya")
                        }
                    }
                }
            }
            item {
                InfoCard(
                    "Jadwal yang menyesuaikan",
                    "Kata tersimpan langsung siap ditinjau. Jawaban benar menaikkan penguasaan dan memperpanjang jarak tinjauan. Jawaban salah kembali dalam 10 menit.",
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.testTag("quiz-question"),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            quiz.mode.label.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Text(
                                quiz.prompt,
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            val spoken =
                                when (quiz.mode) {
                                    PracticeMode.RECOGNITION,
                                    PracticeMode.RECALL -> quiz.word.word
                                    PracticeMode.CONTEXT -> quiz.word.example
                                    else -> ""
                                }
                            if (spoken.isNotBlank())
                                PronunciationButton(
                                    spoken,
                                    "flashcard-prompt-${quiz.word.id}",
                                    quiz.word.word,
                                )
                        }
                        VocabularyStatusLabel(quiz.word.learningStatus())
                        Text("${quiz.word.partOfSpeech} · ${quiz.word.storyTitle}")
                    }
                }
            }
            if (!quiz.revealed) {
                if (quiz.typed)
                    item {
                        OutlinedTextField(
                            quiz.response,
                            answer,
                            Modifier.fillMaxWidth(),
                            label = { Text("Terjemahan Anda") },
                            singleLine = true,
                        )
                    }
                else
                    quiz.choices.forEach { option ->
                        item(key = option) {
                            FilterChip(
                                quiz.response == option,
                                { answer(option) },
                                label = { Text(option, Modifier.padding(8.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                item {
                    Button(
                        onClick = reveal,
                        enabled = quiz.response.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Periksa jawaban")
                    }
                }
            } else {
                item {
                    InfoCard(
                        if (quiz.correct) "Jawaban diingat" else "Kata ini perlu ditinjau lagi",
                        "Jawaban: ${quiz.expected}\n\n${quiz.word.word} → ${quiz.word.translation}\n\n${quiz.word.example}\n\n${quiz.word.exampleTranslation}",
                    )
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        PronunciationButton(
                            quiz.word.word,
                            "flashcard-word-${quiz.word.id}",
                            quiz.word.word,
                        )
                        Text("Dengarkan kata")
                        PronunciationButton(
                            quiz.word.example,
                            "flashcard-example-${quiz.word.id}",
                            "contoh ${quiz.word.word}",
                        )
                        Text("Dengarkan contoh")
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            if (quiz.correct) "Seberapa mudah?" else "Latih lagi dalam 10 menit.",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val ratings = if (quiz.correct) Rating.entries else listOf(Rating.AGAIN)
                            ratings.forEach { rating ->
                                val next = SpacedRepetition.next(quiz.word.schedule(), rating, now)
                                FilledTonalButton(onClick = { rate(rating) }, enabled = !busy) {
                                    val label =
                                        when (rating) {
                                            Rating.AGAIN -> "Ulangi"
                                            Rating.HARD -> "Sulit"
                                            Rating.GOOD -> "Baik"
                                            Rating.EASY -> "Mudah"
                                        }
                                    Text(
                                        "$label · ${if (rating == Rating.AGAIN) "10 menit" else "${next.intervalDays} hari"}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { TextButton(onClick = exit, enabled = !busy) { Text("Keluar dari latihan") } }
        }
    }
}
