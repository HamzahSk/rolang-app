package com.rolang.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.rolang.app.LearningState
import com.rolang.app.domain.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(state: LearningState, target: (Language) -> Unit, native: (Language) -> Unit, level: (Level) -> Unit,
                  font: (Float) -> Unit, dark: (Boolean) -> Unit, account: @Composable () -> Unit = {}) {
    val prefs = state.preferences
    val reviews = state.courseReviews
    val mastered = state.courseWords.count { it.mastered }
    val completedLessons = state.lessons.count { state.contentCompleted(it.id) }
    val completedStories = state.readingStories.count { state.contentCompleted(it.id) }
    val dates = reviews.map { Instant.ofEpochMilli(it.reviewedAt).atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()
    var cursor = if (LocalDate.now() in dates) LocalDate.now() else LocalDate.now().minusDays(1)
    var streak = 0
    while (cursor in dates) { streak++; cursor = cursor.minusDays(1) }
    LazyColumn(modifier = Modifier.testTag("profile-content"), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { SectionTitle("Perjalanan belajar", "Langkah kecil membentuk kemajuan yang bertahan.") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("$streak", "Hari beruntun", Modifier.weight(1f))
                StatTile(if (reviews.isEmpty()) "—" else "${reviews.count { it.correct } * 100 / reviews.size}%", "Ketepatan", Modifier.weight(1f))
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("$mastered dari ${state.courseWords.size} kata dikuasai", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(progress = { if (state.courseWords.isEmpty()) 0f else mastered.toFloat() / state.courseWords.size }, modifier = Modifier.fillMaxWidth())
                    Text("Pelajaran selesai: $completedLessons · Cerita selesai: $completedStories")
                    Text("Tinjauan tercatat: ${reviews.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            LanguagePicker("Saya mempelajari", prefs.target, target)
        }
        item {
            LanguagePicker("Terjemahkan ke", prefs.native, native)
            Text("Kosakata dan tinjauan dipisahkan untuk setiap pasangan bahasa.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { InfoCard("Kursus saat ini: Tagalog A1", "Kursus ini berfokus pada dasar lengkap untuk pemula berbahasa Indonesia.") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ukuran teks bacaan", style = MaterialTheme.typography.titleMedium)
                var size by remember(prefs.fontSize) { mutableFloatStateOf(prefs.fontSize) }
                Text("${size.toInt()} sp · Sesuaikan agar nyaman dibaca")
                Slider(value = size, onValueChange = { size = it }, valueRange = 16f..28f, steps = 5,
                    onValueChangeFinished = { font(size) })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Tampilan gelap")
                    Switch(checked = prefs.dark, onCheckedChange = dark)
                }
            }
        }
        item { account() }
        item { InfoCard("Pustaka selalu tersedia", "Cerita, terjemahan, kata tersimpan, kemajuan, dan tinjauan disimpan di perangkat. Ro-Lang tetap dapat dibaca luring dan dapat disinkronkan saat daring.") }
        item { Text("Ro-Lang · Versi 1.0", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguagePicker(title: String, selected: Language, choose: (Language) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Language.entries.forEach { language -> FilterChip(selected == language, { choose(language) }, label = { Text(language.label) }) }
        }
    }
}
