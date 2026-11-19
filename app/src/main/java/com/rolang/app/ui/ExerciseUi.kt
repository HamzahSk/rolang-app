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
                Text("Practice complete", style = MaterialTheme.typography.headlineMedium)
                Text("$score of ${story.exercises.size} correct", style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(progress = { if (story.exercises.isEmpty()) 0f else score.toFloat() / story.exercises.size }, Modifier.fillMaxWidth())
                Text(when {
                    score == story.exercises.size -> "Excellent work. You understood every question."
                    score * 2 >= story.exercises.size -> "Good progress. Review the explanations, then try again for mastery."
                    else -> "Review the learning sections and retry when you’re ready."
                })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { answers = emptyMap(); checked = emptySet(); summary = false }) {
                        Icon(Icons.Outlined.Replay, null); Spacer(Modifier.width(6.dp)); Text("Retry")
                    }
                    Button(onClick = onContinue, Modifier.weight(1f)) { Text("Continue") }
                }
            }
        }
        return
    }
    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("Final practice", "Answer every question. Feedback appears immediately after each check.")
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
            Text("Finish and save score")
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
            Text("QUESTION ${index + 1} · ${exercise.type.label()}", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary)
            Text(exercise.question(language), style = MaterialTheme.typography.titleMedium)
            when (exercise.type) {
                ExerciseType.MULTIPLE_CHOICE, ExerciseType.GRAMMAR -> exercise.choices(language).forEach { choice ->
                    FilterChip(selected = response == choice, onClick = { if (!checked) onAnswer(choice) }, enabled = !checked,
                        label = { Text(choice) }, modifier = Modifier.fillMaxWidth())
                }
                ExerciseType.SENTENCE_CONSTRUCTION -> {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                        Text(response.ifBlank { "Build the sentence below" }, Modifier.fillMaxWidth().padding(14.dp))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        exercise.tokens(language).forEach { token -> AssistChip(onClick = {
                            if (!checked) onAnswer(listOf(response, token).filter(String::isNotBlank).joinToString(" "))
                        }, label = { Text(token) }, enabled = !checked) }
                    }
                    TextButton(onClick = { onAnswer("") }, enabled = !checked && response.isNotBlank()) {
                        Icon(Icons.Outlined.Refresh, null); Text("Clear")
                    }
                }
                else -> OutlinedTextField(response, { if (!checked) onAnswer(it) }, Modifier.fillMaxWidth(),
                    enabled = !checked, singleLine = exercise.type != ExerciseType.COMPREHENSION,
                    label = { Text(if (exercise.type == ExerciseType.FILL_BLANK) "Missing word" else "Your answer") })
            }
            if (!checked) Button(onClick = onCheck, enabled = response.isNotBlank()) { Text("Check answer") }
            AnimatedVisibility(checked) {
                val container = if (correct) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                val foreground = if (correct) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                Surface(color = container, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (correct) "Correct" else "Not quite", fontWeight = FontWeight.Bold, color = foreground)
                        if (!correct) Text("Correct answer: ${exercise.answer(language)}", color = foreground)
                        Text(exercise.explanation(language), color = foreground)
                    }
                }
            }
        }
    }
}

private fun ExerciseType.label() = when (this) {
    ExerciseType.MULTIPLE_CHOICE -> "Multiple choice"
    ExerciseType.VOCABULARY_TRANSLATION -> "Vocabulary"
    ExerciseType.FILL_BLANK -> "Fill in the blank"
    ExerciseType.GRAMMAR -> "Grammar"
    ExerciseType.SENTENCE_CONSTRUCTION -> "Build a sentence"
    ExerciseType.COMPREHENSION -> "Comprehension"
}
