package com.rolang.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rolang.app.LearningState
import com.rolang.app.data.Story
import com.rolang.app.domain.Level
import java.util.concurrent.TimeUnit

@Composable
fun HomeScreen(
    state: LearningState, now: Long, lessons: () -> Unit, stories: () -> Unit,
    review: () -> Unit, learn: (String) -> Unit, read: (String) -> Unit,
) {
    val prefs = state.preferences
    val lessonItems = state.lessons
    val completedLessons = state.lessons.count { state.contentCompleted(it.id) }
    val completedStories = state.readingStories.count { state.contentCompleted(it.id) }
    val nextLesson = lessonItems.firstOrNull { lesson ->
        !state.contentCompleted(lesson.id)
    } ?: lessonItems.lastOrNull()
    val due = state.due(now).size
    val upcoming = state.courseWords.count { !it.mastered && it.dueAt in (now + 1)..(now + TimeUnit.DAYS.toMillis(7)) }
    val dayStart = now - now % TimeUnit.DAYS.toMillis(1)
    val activity = state.courseReviews.count { it.reviewedAt >= dayStart } + state.courseResults.count { it.updatedAt >= dayStart }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Ro-Lang", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text("${prefs.target.label} · ${prefs.level.label}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(onClick = {}, label = { Text("Offline ready") },
                    leadingIcon = { Icon(Icons.Outlined.OfflinePin, null, Modifier.size(18.dp)) })
            }
        }
        item { SectionTitle("Keep your momentum", "A clear next step, with reading practice alongside your course.") }
        nextLesson?.let { lesson -> item {
            val unitNumber = lessonItems.map { it.family }.distinct().indexOf(lesson.family) + 1
            Card(onClick = { learn(lesson.id) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.School, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp)); Text("CONTINUE LEARNING", style = MaterialTheme.typography.labelLarge)
                    }
                    Text(lesson.title(prefs.target), style = MaterialTheme.typography.headlineMedium)
                    Text("Unit $unitNumber · ${lesson.family.replaceFirstChar(Char::uppercase)} · Next lesson",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(progress = { if (lessonItems.isEmpty()) 0f else completedLessons.toFloat() / lessonItems.size }, Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$completedLessons of ${lessonItems.size} lessons")
                        Text("Resume", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("$completedLessons/${lessonItems.size}", "Lessons", Modifier.weight(1f))
            StatTile("$completedStories/${state.readingStories.size}", "Stories", Modifier.weight(1f))
        } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("${state.courseWords.size}", "Words saved", Modifier.weight(1f)); StatTile("$activity", "Today", Modifier.weight(1f))
        } }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Review forecast", style = MaterialTheme.typography.titleLarge)
                    Text(if (due > 0) "$due ready now · $upcoming coming this week" else "You’re caught up · $upcoming coming this week")
                    Button(onClick = if (due > 0) review else lessons, Modifier.fillMaxWidth()) {
                        Text(if (due > 0) "Review due vocabulary" else "Browse lessons")
                    }
                }
            }
        }
        item { SectionTitle("Choose your practice") }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PracticeCard("Lessons", "Guided course", Icons.Outlined.School, lessons, Modifier.weight(1f))
            PracticeCard("Stories", "Reading room", Icons.AutoMirrored.Outlined.MenuBook, stories, Modifier.weight(1f))
        } }
        state.readingStories.firstOrNull { story -> state.storyReadings.any { it.storyId == story.id && !it.completed } }?.let { story -> item {
            TextButton(onClick = { read(story.id) }) {
                Text("Continue reading “${story.title(prefs.target)}”"); Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
            }
        } }
    }
}

@Composable
private fun PracticeCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                         onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } }
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun LessonsScreen(state: LearningState, open: (String) -> Unit) {
    val lessons = state.lessons
    val completed = lessons.filter { state.contentCompleted(it.id) }.map { it.id }.toSet()
    val groups = lessons.groupBy { it.family }
    LazyColumn(Modifier.testTag("lessons-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("Tagalog A1 lessons", "A guided curriculum with clear goals, practice, and prerequisites.") }
        item { LinearProgressIndicator(progress = { if (lessons.isEmpty()) 0f else completed.size.toFloat() / lessons.size }, modifier = Modifier.fillMaxWidth()) }
        groups.entries.forEachIndexed { unitIndex, (family, unitLessons) ->
            item(key = "unit:$family") { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("UNIT ${unitIndex + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Text(family.replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.titleLarge)
                Text(unitLessons.joinToString(" · ") { it.category }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
            items(unitLessons, key = { it.id }) { lesson ->
                val number = lessons.indexOf(lesson) + 1
                val prerequisite = lessons.getOrNull(number - 2)
                LessonCard(lesson, number, lesson.id in completed, prerequisite?.takeUnless { it.id in completed }, state, open)
            }
        }
    }
}

@Composable
private fun LessonCard(lesson: Story, number: Int, complete: Boolean, prerequisite: Story?, state: LearningState, open: (String) -> Unit) {
    val result = state.courseResults.firstOrNull { it.contentId == lesson.id }
    Card(onClick = { open(lesson.id) }, modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(16.dp), color = if (complete) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    if (complete) Icon(Icons.Outlined.CheckCircle, "Completed", tint = MaterialTheme.colorScheme.primary)
                    else Text("$number", style = MaterialTheme.typography.titleLarge)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Lesson $number · ${lesson.category}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Text(lesson.title(state.preferences.target), style = MaterialTheme.typography.titleMedium)
                Text(lesson.summary(state.preferences.target), maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                when {
                    result != null -> Text("Best score ${result.bestScore}/${result.totalQuestions} · ${result.attempts} attempt${if (result.attempts == 1) "" else "s"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    prerequisite != null -> Text("Recommended first: ${prerequisite.title(state.preferences.target)}", style = MaterialTheme.typography.bodyMedium)
                    else -> Text("Ready to start", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoriesScreen(state: LearningState, read: (String) -> Unit) {
    var format by rememberSaveable { mutableStateOf("All formats") }
    var topic by rememberSaveable { mutableStateOf("All topics") }
    var level by rememberSaveable { mutableStateOf("All levels") }
    var query by rememberSaveable { mutableStateOf("") }
    val stories = state.readingStories
    val formats = listOf("All formats") + stories.map { it.kind.label }.distinct()
    val topics = listOf("All topics") + stories.map { it.category }.distinct()
    val levels = listOf("All levels") + stories.map { it.level.label }.distinct()
    val filtered = stories.filter { story ->
        (format == "All formats" || story.kind.label == format) &&
        (topic == "All topics" || story.category == topic) &&
        (level == "All levels" || story.level.label == level) &&
        (story.title(state.preferences.target).contains(query, true) || story.summary(state.preferences.target).contains(query, true)) }
    LazyColumn(Modifier.testTag("stories-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { SectionTitle("Stories", "Independent reading practice by level, genre, and topic.") }
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Search stories") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }) }
        item { FilterGroup("Level", levels, level) { level = it } }
        item { FilterGroup("Format", formats, format) { format = it } }
        item { FilterGroup("Topic", topics, topic) { topic = it } }
        item { Text("${filtered.size} offline readings", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
        if (filtered.isEmpty()) item { InfoCard("No stories found", "Try another title, genre, or topic.") }
        items(filtered, key = { it.id }) { story -> StoryCard(story, state, read) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(label: String, values: List<String>, selected: String, choose: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { value -> FilterChip(selected == value, { choose(value) }, label = { Text(value) }) }
        }
    }
}

@Composable
fun StoryCard(story: Story, state: LearningState, read: (String) -> Unit) {
    val reading = state.storyReadings.firstOrNull { it.storyId == story.id }
    val result = state.courseResults.firstOrNull { it.contentId == story.id }
    Card(onClick = { read(story.id) }, modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(illustrationResource(story.image)), null, Modifier.fillMaxWidth().height(150.dp).padding(top = 12.dp), contentScale = ContentScale.Fit)
            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${story.kind.label.uppercase()} · ${story.category.uppercase()}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    DifficultyBadge(story.level)
                }
                Text(story.title(state.preferences.target), style = MaterialTheme.typography.titleLarge)
                Text(story.summary(state.preferences.target), color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { when {
                    state.contentCompleted(story.id) -> 1f; reading == null -> 0f; else -> (reading.sentence + 1f) / story.sentences.size
                } }, Modifier.fillMaxWidth())
                Text(when { result?.completed == true -> "Completed · best ${result.bestScore}/${result.totalQuestions}"
                    state.contentCompleted(story.id) -> "Completed · practice available"
                    reading != null -> "Continue reading"; else -> "Start story" },
                    color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun DifficultyBadge(level: Level) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(50)) {
        Text(level.label.substringBefore(" ·"), Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelLarge)
    }
}
