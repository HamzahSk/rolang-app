package com.rolang.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.rolang.app.data.VocabularyEntity
import com.rolang.app.speech.*

val LocalPronunciationState = staticCompositionLocalOf { PronunciationState(
    availability = PronunciationAvailability.ERROR,
    message = "Kontrol pelafalan belum tersedia.",
) }

private object NoPronunciationController : PronunciationController {
    override fun refresh() = Unit
    override fun speak(text: String, id: String, queueMode: SpeechQueueMode) = Unit
    override fun speakSequence(groupId: String, items: List<SpeechItem>) = Unit
    override fun replay() = Unit
    override fun stop() = Unit
    override fun setSpeechRate(value: Float) = Unit
    override fun setPitch(value: Float) = Unit
    override fun openVoiceDataSettings() = Unit
}

val LocalPronunciationController = staticCompositionLocalOf<PronunciationController> { NoPronunciationController }

@Composable
fun PronunciationButton(
    text: String,
    id: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val state = LocalPronunciationState.current
    val controller = LocalPronunciationController.current
    val active = state.activeId == id
    val replay = !state.speaking && state.lastGroupId == id
    val transition = rememberInfiniteTransition(label = "pronunciation-pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "speaker-scale",
    )
    IconButton(
        onClick = { if (active) controller.stop() else controller.speak(text, id) },
        enabled = text.isNotBlank(),
        modifier = modifier.graphicsLayer {
            scaleX = if (active) pulse else 1f
            scaleY = if (active) pulse else 1f
        },
    ) {
        Icon(
            when {
                active -> Icons.Outlined.StopCircle
                replay -> Icons.Outlined.Replay
                else -> Icons.AutoMirrored.Outlined.VolumeUp
            },
            when {
                active -> "Hentikan pelafalan $label"
                replay -> "Putar ulang pelafalan $label"
                else -> "Putar pelafalan $label"
            },
            tint = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun PronunciationSequenceControls(
    groupId: String,
    texts: List<String>,
    itemId: (Int) -> String,
    playLabel: String,
    modifier: Modifier = Modifier,
) {
    val state = LocalPronunciationState.current
    val controller = LocalPronunciationController.current
    val active = state.activeGroupId == groupId
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalButton(onClick = {
            if (active) controller.stop()
            else controller.speakSequence(groupId, texts.mapIndexed { index, text -> SpeechItem(itemId(index), text) })
        }, enabled = texts.any(String::isNotBlank)) {
            Icon(if (active) Icons.Outlined.StopCircle else Icons.AutoMirrored.Outlined.VolumeUp, null)
            Spacer(Modifier.width(8.dp))
            Text(if (active) "Hentikan" else playLabel)
        }
        IconButton(onClick = controller::replay, enabled = !state.speaking && state.lastGroupId == groupId) {
            Icon(Icons.Outlined.Replay, "Putar ulang rangkaian pelafalan")
        }
    }
}

@Composable
fun PronunciationAvailabilityNotice(modifier: Modifier = Modifier) {
    val state = LocalPronunciationState.current
    val controller = LocalPronunciationController.current
    if (state.ready) return
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.RecordVoiceOver, null)
                Spacer(Modifier.width(8.dp))
                Text("Suara Filipino belum siap", style = MaterialTheme.typography.titleMedium)
            }
            Text(state.message, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = controller::openVoiceDataSettings) { Text("Buka pengaturan suara") }
                TextButton(onClick = controller::refresh) { Text("Periksa ulang") }
            }
        }
    }
}

@Composable
fun PronunciationSettingsCard() {
    val state = LocalPronunciationState.current
    val controller = LocalPronunciationController.current
    Card {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.SettingsVoice, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("Pelafalan Tagalog", style = MaterialTheme.typography.titleLarge)
            }
            Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.ready) {
                Text("Suara: ${state.voiceName.orEmpty()} · offline", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Kecepatan bicara: ${"%.2f".format(state.speechRate)}×", style = MaterialTheme.typography.titleMedium)
                Slider(state.speechRate, controller::setSpeechRate, valueRange = 0.5f..1.2f, steps = 6)
                Text("Nilai awal 0,85× dibuat sedikit lebih lambat untuk pelajar.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Nada suara: ${"%.2f".format(state.pitch)}×", style = MaterialTheme.typography.titleMedium)
                Slider(state.pitch, controller::setPitch, valueRange = 0.8f..1.2f, steps = 3)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.ready) {
                    OutlinedButton(onClick = controller::stop, enabled = state.speaking) { Text("Hentikan") }
                    OutlinedButton(onClick = controller::replay, enabled = !state.speaking && state.lastGroupId != null) {
                        Icon(Icons.Outlined.Replay, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Putar ulang")
                    }
                } else {
                    Button(onClick = controller::openVoiceDataSettings) { Text("Pasang suara Filipino") }
                    OutlinedButton(onClick = controller::refresh) { Text("Periksa ulang") }
                }
            }
        }
    }
}

@Composable
fun ListeningPracticeCard(word: VocabularyEntity) {
    var showMeaning by remember(word.id) { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Latihan dengar dan tirukan", style = MaterialTheme.typography.titleLarge)
            Text("Dengarkan kata, ucapkan kembali, lalu bandingkan dalam kalimat lengkap.")
            Row(verticalAlignment = Alignment.CenterVertically) {
                PronunciationButton(word.word, "practice-word-${word.id}", word.word)
                Text(word.word, style = MaterialTheme.typography.titleLarge)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PronunciationButton(word.example, "practice-example-${word.id}", "contoh ${word.word}")
                Text(word.example, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            }
            TextButton(onClick = { showMeaning = !showMeaning }) {
                Text(if (showMeaning) "Sembunyikan perbandingan" else "Tampilkan perbandingan Indonesia")
            }
            if (showMeaning) Text("${word.translation}\n\n${word.exampleTranslation}",
                color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}
