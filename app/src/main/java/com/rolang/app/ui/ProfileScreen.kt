package com.rolang.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.rolang.app.LearningState
import com.rolang.app.diagnostics.DiagnosticLogger
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ProfileScreen(
    state: LearningState,
    font: (Float) -> Unit,
    dark: (Boolean) -> Unit,
    footer: @Composable () -> Unit = {},
) {
    val prefs = state.preferences
    val reviews = state.courseReviews
    val mastered = state.courseWords.count { it.mastered }
    val completedLessons = state.lessons.count { state.contentCompleted(it.id) }
    val completedStories = state.readingStories.count { state.contentCompleted(it.id) }
    val dates =
        reviews
            .map {
                Instant.ofEpochMilli(it.reviewedAt).atZone(ZoneId.systemDefault()).toLocalDate()
            }
            .toSet()
    var cursor = if (LocalDate.now() in dates) LocalDate.now() else LocalDate.now().minusDays(1)
    var streak = 0
    while (cursor in dates) {
        streak++
        cursor = cursor.minusDays(1)
    }
    LazyColumn(
        modifier = Modifier.testTag("profile-content"),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            SectionTitle("Perjalanan belajar", "Langkah kecil membentuk kemajuan yang bertahan.")
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("$streak", "Hari beruntun", Modifier.weight(1f))
                StatTile(
                    if (reviews.isEmpty()) "—"
                    else "${reviews.count { it.correct } * 100 / reviews.size}%",
                    "Ketepatan",
                    Modifier.weight(1f),
                )
            }
        }
        item {
            Card {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "$mastered dari ${state.courseWords.size} kata dikuasai",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (state.courseWords.isEmpty()) 0f
                            else mastered.toFloat() / state.courseWords.size
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Pelajaran selesai: $completedLessons · Cerita selesai: $completedStories")
                    Text(
                        "Tinjauan tercatat: ${reviews.size}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            InfoCard(
                "Kursus tetap: Indonesia → Tagalog A1",
                "Bahasa Indonesia digunakan untuk antarmuka, petunjuk, terjemahan, dan penjelasan. Tagalog adalah satu-satunya bahasa yang dipelajari.",
            )
        }
        item { PronunciationSettingsCard() }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ukuran teks bacaan", style = MaterialTheme.typography.titleMedium)
                var size by remember(prefs.fontSize) { mutableFloatStateOf(prefs.fontSize) }
                Text("${size.toInt()} sp · Sesuaikan agar nyaman dibaca")
                Slider(
                    value = size,
                    onValueChange = { size = it },
                    valueRange = 16f..28f,
                    steps = 5,
                    onValueChangeFinished = { font(size) },
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Tampilan gelap")
                    Switch(checked = prefs.dark, onCheckedChange = dark)
                }
            }
        }
        item { footer() }
        item {
            InfoCard(
                "Data belajar tetap di perangkat",
                "Kemajuan, kosakata, jadwal tinjauan, dan hasil latihan disimpan hanya di Room lokal. Pelajaran dapat disimpan beserta gambarnya untuk penggunaan luring.",
            )
        }
        item { Text("Ro-Lang · Versi 1.0", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
fun DiagnosticLogCard() {
    val context = LocalContext.current
    Card {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Diagnostik", style = MaterialTheme.typography.titleLarge)
            Text(
                "Kesalahan aplikasi dan permintaan GitHub disimpan di ${DiagnosticLogger.displayPath(context)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = {
                    if (!DiagnosticLogger.share(context))
                        Toast.makeText(
                                context,
                                "Log diagnostik belum dapat diekspor.",
                                Toast.LENGTH_LONG,
                            )
                            .show()
                }
            ) {
                Text("Ekspor log diagnostik")
            }
        }
    }
}
