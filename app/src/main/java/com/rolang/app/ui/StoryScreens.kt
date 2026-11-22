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
import com.rolang.app.LearningViewModel
import com.rolang.app.LearningState
import com.rolang.app.data.Story
import com.rolang.app.domain.Level
import java.util.concurrent.TimeUnit

private fun moduleTitle(id: String) = mapOf(
    "foundations" to "Dasar bahasa", "greetings" to "Salam dan perkenalan", "time" to "Angka dan waktu",
    "routine" to "Kegiatan sehari-hari", "family" to "Keluarga", "school" to "Sekolah",
    "grammar" to "Tata bahasa", "food" to "Makanan dan belanja", "travel" to "Tempat dan arah",
    "friends" to "Persahabatan", "review" to "Tinjauan akhir",
)[id] ?: id

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
                    Text("Indonesia → Tagalog · A1", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(onClick = {}, label = { Text("${state.stories.count { it.sentences.isNotEmpty() }} tersimpan luring") },
                    leadingIcon = { Icon(Icons.Outlined.OfflinePin, null, Modifier.size(18.dp)) })
            }
        }
        item { LibraryStatus(state, null) }
        item { SectionTitle("Lanjutkan kemajuan", "Langkah berikutnya yang jelas beserta latihan membaca.") }
        nextLesson?.let { lesson -> item {
            val unitNumber = lessonItems.map { it.family }.distinct().indexOf(lesson.family) + 1
            Card(onClick = { learn(lesson.id) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.School, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp)); Text("LANJUTKAN BELAJAR", style = MaterialTheme.typography.labelLarge)
                    }
                    Text(lesson.title(prefs.native), style = MaterialTheme.typography.headlineMedium)
                    Text("Unit $unitNumber · ${moduleTitle(lesson.family)} · Pelajaran berikutnya",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(progress = { if (lessonItems.isEmpty()) 0f else completedLessons.toFloat() / lessonItems.size }, Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$completedLessons dari ${lessonItems.size} pelajaran")
                        Text("Lanjutkan", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("$completedLessons/${lessonItems.size}", "Pelajaran", Modifier.weight(1f))
            StatTile("$completedStories/${state.readingStories.size}", "Cerita", Modifier.weight(1f))
        } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("${state.courseWords.size}", "Kata tersimpan", Modifier.weight(1f)); StatTile("$activity", "Hari ini", Modifier.weight(1f))
        } }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Jadwal tinjauan", style = MaterialTheme.typography.titleLarge)
                    Text(if (due > 0) "$due siap sekarang · $upcoming minggu ini" else "Semua selesai · $upcoming minggu ini")
                    Button(onClick = if (due > 0) review else lessons, Modifier.fillMaxWidth()) {
                        Text(if (due > 0) "Tinjau kosakata" else "Lihat pelajaran")
                    }
                }
            }
        }
        item { SectionTitle("Pilih latihan") }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PracticeCard("Pelajaran", "Kursus terpandu", Icons.Outlined.School, lessons, Modifier.weight(1f))
            PracticeCard("Cerita", "Ruang baca", Icons.AutoMirrored.Outlined.MenuBook, stories, Modifier.weight(1f))
        } }
        state.readingStories.firstOrNull { story -> state.storyReadings.any { it.storyId == story.id && !it.completed } }?.let { story -> item {
            TextButton(onClick = { read(story.id) }) {
                Text("Lanjut membaca “${story.title(prefs.native)}”"); Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
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
fun LessonsScreen(state: LearningState, viewModel: LearningViewModel? = null, open: (String) -> Unit) {
    val lessons = state.lessons
    val completed = lessons.filter { state.contentCompleted(it.id) }.map { it.id }.toSet()
    val groups = lessons.groupBy { it.family }
    LazyColumn(Modifier.testTag("lessons-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { LibraryStatus(state, viewModel) }
        item { SectionTitle("Pelajaran Tagalog A1", "Kurikulum bertahap dengan tujuan, latihan, dan urutan yang jelas.") }
        item { LinearProgressIndicator(progress = { if (lessons.isEmpty()) 0f else completed.size.toFloat() / lessons.size }, modifier = Modifier.fillMaxWidth()) }
        groups.entries.forEachIndexed { unitIndex, (family, unitLessons) ->
            item(key = "unit:$family") { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("UNIT ${unitIndex + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Text(moduleTitle(family), style = MaterialTheme.typography.titleLarge)
                Text(unitLessons.joinToString(" · ") { it.category }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
            items(unitLessons, key = { it.id }) { lesson ->
                val number = lessons.indexOf(lesson) + 1
                val prerequisite = lessons.getOrNull(number - 2)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LessonCard(lesson, number, lesson.id in completed, prerequisite?.takeUnless { it.id in completed }, state, open)
                    viewModel?.let { DownloadControls(lesson, state, it) }
                }
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
                    if (complete) Icon(Icons.Outlined.CheckCircle, "Selesai", tint = MaterialTheme.colorScheme.primary)
                    else Text("$number", style = MaterialTheme.typography.titleLarge)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Pelajaran $number · ${lesson.category}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Text(lesson.title(state.preferences.native), style = MaterialTheme.typography.titleMedium)
                Text(lesson.summary(state.preferences.native), maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                when {
                    result != null -> Text("Nilai terbaik ${result.bestScore}/${result.totalQuestions} · ${result.attempts} percobaan", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    prerequisite != null -> Text("Disarankan lebih dahulu: ${prerequisite.title(state.preferences.native)}", style = MaterialTheme.typography.bodyMedium)
                    else -> Text("Siap dimulai", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoriesScreen(state: LearningState, viewModel: LearningViewModel? = null, read: (String) -> Unit) {
    var savedOnly by rememberSaveable { mutableStateOf(false) }
    var format by rememberSaveable { mutableStateOf("Semua format") }
    var topic by rememberSaveable { mutableStateOf("Semua topik") }
    var level by rememberSaveable { mutableStateOf("Semua tingkat") }
    var query by rememberSaveable { mutableStateOf("") }
    val stories = state.readingStories
    val formats = listOf("Semua format") + stories.map { it.kind.label }.distinct()
    val topics = listOf("Semua topik") + stories.map { it.category }.distinct()
    val levels = listOf("Semua tingkat") + stories.map { it.level.label }.distinct()
    val filtered = stories.filter { story ->
        (!savedOnly || story.sentences.isNotEmpty()) &&
        (format == "Semua format" || story.kind.label == format) &&
        (topic == "Semua topik" || story.category == topic) &&
        (level == "Semua tingkat" || story.level.label == level) &&
        (story.title(state.preferences.native).contains(query, true) || story.summary(state.preferences.native).contains(query, true)) }
    LazyColumn(Modifier.testTag("stories-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { LibraryStatus(state, viewModel) }
        item { FilterChip(savedOnly, { savedOnly = !savedOnly }, label = { Text("Tersimpan luring") }) }
        item { SectionTitle("Cerita", "Latihan membaca mandiri berdasarkan tingkat dan topik.") }
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Cari cerita") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }) }
        item { FilterGroup("Tingkat", levels, level) { level = it } }
        item { FilterGroup("Format", formats, format) { format = it } }
        item { FilterGroup("Topik", topics, topic) { topic = it } }
        item { Text("${filtered.size} bacaan", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
        if (filtered.isEmpty()) item { InfoCard("Cerita tidak ditemukan", "Coba judul atau topik lain.") }
        items(filtered, key = { it.id }) { story -> StoryCard(story, state, viewModel, read) }
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
fun StoryCard(story: Story, state: LearningState, viewModel: LearningViewModel? = null, read: (String) -> Unit) {
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
                Text(story.title(state.preferences.native), style = MaterialTheme.typography.titleLarge)
                Text(story.summary(state.preferences.native), color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { when {
                    state.contentCompleted(story.id) -> 1f; reading == null -> 0f; else -> ((reading.sentence + 1f) / story.sentenceCount.coerceAtLeast(1)).coerceIn(0f, 1f)
                } }, Modifier.fillMaxWidth())
                viewModel?.let { DownloadControls(story, state, it) }
                Text(when { result?.completed == true -> "Selesai · terbaik ${result.bestScore}/${result.totalQuestions}"
                    state.contentCompleted(story.id) -> "Selesai · latihan tersedia"
                    reading != null -> "Lanjut membaca"; else -> "Mulai cerita" },
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
