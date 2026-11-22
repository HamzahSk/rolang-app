package com.rolang.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rolang.app.LearningState
import com.rolang.app.data.Exercise
import com.rolang.app.data.ExerciseType
import com.rolang.app.data.Story
import com.rolang.app.domain.Words

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseSession(
    story: Story,
    state: LearningState,
    onComplete: (Int, Int, Map<String, String>) -> Unit,
    onContinue: () -> Unit,
) {
    var answers by remember(story.id) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var checked by remember(story.id) { mutableStateOf<Set<String>>(emptySet()) }
    var summary by remember(story.id) { mutableStateOf(false) }
    val score = story.exercises.count { exercise ->
        val language = if (exercise.type == ExerciseType.FILL_BLANK ||
            exercise.type == ExerciseType.SENTENCE_CONSTRUCTION) state.preferences.target else state.preferences.native
        exercise.id in checked && Words.matches(answers[exercise.id].orEmpty(), exercise.answer(language))
    }
    if (summary) {
        Card(Modifier.widthIn(max = 720.dp).fillMaxWidth().testTag("exercise-summary"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Latihan selesai", style = MaterialTheme.typography.headlineMedium)
                Text("$score dari ${story.exercises.size} jawaban benar", style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(progress = { if (story.exercises.isEmpty()) 0f else score.toFloat() / story.exercises.size }, Modifier.fillMaxWidth())
                Text(when {
                    score == story.exercises.size -> "Sangat baik. Semua pertanyaan sudah dipahami."
                    score * 2 >= story.exercises.size -> "Kemajuan bagus. Tinjau penjelasan, lalu coba lagi."
                    else -> "Pelajari kembali materinya, lalu ulangi latihan saat siap."
                })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { answers = emptyMap(); checked = emptySet(); summary = false }) {
                        Icon(Icons.Outlined.Replay, null); Spacer(Modifier.width(6.dp)); Text("Ulangi")
                    }
                    Button(onClick = onContinue, Modifier.weight(1f)) { Text("Lanjut") }
                }
            }
        }
        return
    }
    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("Latihan akhir", "Jawab semua pertanyaan. Umpan balik muncul setelah jawaban diperiksa.")
        PronunciationAvailabilityNotice()
        story.exercises.forEachIndexed { index, exercise ->
            val language = if (exercise.type == ExerciseType.FILL_BLANK ||
                exercise.type == ExerciseType.SENTENCE_CONSTRUCTION) state.preferences.target else state.preferences.native
            ExerciseQuestion(index, exercise, answers[exercise.id].orEmpty(), exercise.id in checked, language,
                onAnswer = { answers = answers + (exercise.id to it) },
                onCheck = { checked = checked + exercise.id })
        }
        Button(onClick = {
            onComplete(score, story.exercises.size, answers)
            summary = true
        }, enabled = checked.size == story.exercises.size, modifier = Modifier.fillMaxWidth().testTag("finish-exercises")) {
            Text("Selesai dan simpan nilai")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseQuestion(
    index: Int,
    exercise: Exercise,
    response: String,
    checked: Boolean,
    language: com.rolang.app.domain.Language,
    onAnswer: (String) -> Unit,
    onCheck: () -> Unit,
) {
    val correct = Words.matches(response, exercise.answer(language))
    Card(Modifier.fillMaxWidth().testTag("exercise-${exercise.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("SOAL ${index + 1} · ${exercise.type.label()}", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary)
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(exercise.question(language), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                PronunciationButton(exercise.spoken(com.rolang.app.domain.Language.FILIPINO),
                    "exercise-${exercise.id}", "latihan ${index + 1}")
            }
            when (exercise.type) {
                ExerciseType.MULTIPLE_CHOICE, ExerciseType.GRAMMAR -> exercise.choices(language).forEach { choice ->
                    FilterChip(selected = response == choice, onClick = { if (!checked) onAnswer(choice) }, enabled = !checked,
                        label = { Text(choice) }, modifier = Modifier.fillMaxWidth())
                }
                ExerciseType.SENTENCE_CONSTRUCTION -> {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                        Text(response.ifBlank { "Susun kalimat di bawah ini" }, Modifier.fillMaxWidth().padding(14.dp))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        exercise.tokens(language).forEach { token -> AssistChip(onClick = {
                            if (!checked) onAnswer(listOf(response, token).filter(String::isNotBlank).joinToString(" "))
                        }, label = { Text(token) }, enabled = !checked) }
                    }
                    TextButton(onClick = { onAnswer("") }, enabled = !checked && response.isNotBlank()) {
                        Icon(Icons.Outlined.Refresh, null); Text("Hapus")
                    }
                }
                else -> OutlinedTextField(response, { if (!checked) onAnswer(it) }, Modifier.fillMaxWidth(),
                    enabled = !checked, singleLine = exercise.type != ExerciseType.COMPREHENSION,
                    label = { Text(if (exercise.type == ExerciseType.FILL_BLANK) "Kata yang hilang" else "Jawaban Anda") })
            }
            if (!checked) Button(onClick = onCheck, enabled = response.isNotBlank()) { Text("Periksa jawaban") }
            AnimatedVisibility(checked) {
                val container = if (correct) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                val foreground = if (correct) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                Surface(color = container, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (correct) "Benar" else "Belum tepat", fontWeight = FontWeight.Bold, color = foreground)
                        if (!correct) Text("Jawaban benar: ${exercise.answer(language)}", color = foreground)
                        Text(exercise.explanation(language), color = foreground)
                    }
                }
            }
        }
    }
}

private fun ExerciseType.label() = when (this) {
    ExerciseType.MULTIPLE_CHOICE -> "Pilihan ganda"
    ExerciseType.VOCABULARY_TRANSLATION -> "Kosakata"
    ExerciseType.FILL_BLANK -> "Isi bagian kosong"
    ExerciseType.GRAMMAR -> "Tata bahasa"
    ExerciseType.SENTENCE_CONSTRUCTION -> "Susun kalimat"
    ExerciseType.COMPREHENSION -> "Pemahaman"
}
