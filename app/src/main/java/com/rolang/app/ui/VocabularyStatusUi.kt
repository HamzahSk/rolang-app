package com.rolang.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.rolang.app.domain.VocabularyStatus

/** Shared, high-contrast colors for reading, vocabulary, and practice in both themes. */
fun vocabularyStyle(status: VocabularyStatus, textColor: Color): SpanStyle = when (status) {
    VocabularyStatus.NEW -> SpanStyle(color = Color(0xFF174A8B), background = Color(0xFFDCEBFF))
    VocabularyStatus.LEARNING -> SpanStyle(color = Color(0xFF14532D), background = Color(0xFFDCF5E2))
    VocabularyStatus.ALMOST_MASTERED -> SpanStyle(color = Color(0xFF654900), background = Color(0xFFFFF0AD))
    VocabularyStatus.KNOWN -> SpanStyle(color = textColor, background = Color.Transparent)
}

@Composable
fun VocabularyStatusLabel(status: VocabularyStatus) {
    val style = vocabularyStyle(status, MaterialTheme.colorScheme.onSurface)
    Surface(color = style.background, contentColor = style.color, shape = MaterialTheme.shapes.small) {
        Text(status.label, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun VocabularyLegend(modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        VocabularyStatus.entries.forEach { VocabularyStatusLabel(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VocabularyStatusSelector(status: VocabularyStatus, scrollable: Boolean = false, onChange: (VocabularyStatus) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Status belajar: ${status.label}", style = MaterialTheme.typography.titleMedium)
        val chips: @Composable () -> Unit = {
            VocabularyStatus.entries.forEach { value ->
                val style = vocabularyStyle(value, MaterialTheme.colorScheme.onSurface)
                FilterChip(selected = status == value, onClick = { onChange(value) }, label = { Text(value.label) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = style.background, labelColor = style.color,
                        selectedContainerColor = style.background, selectedLabelColor = style.color))
            }
        }
        if (scrollable) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
        else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
        Text(if (status == VocabularyStatus.KNOWN) "Kata yang dikuasai tidak disorot dan dijeda dari tinjauan."
            else "Tersimpan di perangkat ini. Latihan akan menyesuaikan status dan jadwal tinjauan.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
