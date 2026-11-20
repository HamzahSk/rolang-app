package com.rolang.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rolang.app.LearningState
import com.rolang.app.LearningViewModel
import com.rolang.app.data.CatalogStatus
import com.rolang.app.data.Story
import kotlinx.coroutines.CancellationException

@Composable
fun LibraryStatus(state: LearningState, viewModel: LearningViewModel?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.catalogLoading) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Memperbarui pustaka…") }
        state.catalogError?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (state.stories.isEmpty() && !state.catalogLoading && state.catalogError == null) Text(when (state.catalogStatus) {
            CatalogStatus.EMPTY -> "Server terhubung, tetapi belum ada materi yang diterbitkan."
            CatalogStatus.READY -> "Materi daring diterima, tetapi tidak ada entri yang dapat ditampilkan dengan aman."
            else -> "Menghubungkan ke pustaka daring…"
        })
        viewModel?.let { TextButton(onClick = it::refreshCatalog, enabled = !state.catalogLoading) { Text("Perbarui pustaka") } }
    }
}

@Composable
fun DownloadControls(story: Story, state: LearningState, viewModel: LearningViewModel) {
    val download = state.downloads.firstOrNull { it.storyId == story.id }
    val saved = state.stories.any { it.id == story.id && it.sentences.isNotEmpty() }
    val active = download?.status in setOf("QUEUED", "DOWNLOADING", "RETRYING")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (active) {
            LinearProgressIndicator(progress = { if (download!!.total == 0) 0f else download.completed.toFloat() / download.total }, modifier = Modifier.fillMaxWidth())
            Text(when (download!!.status) {
                "QUEUED" -> "Menunggu koneksi…"
                "RETRYING" -> "Menunggu percobaan ulang · ${download.completed}/${download.total} bab tersimpan"
                else -> "Mengunduh · ${download.completed}/${download.total} bab"
            }, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { viewModel.cancelDownload(story.id) }) { Text("Batalkan unduhan") }
        } else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (download?.status == "COMPLETE") Text("Sudah diunduh", style = MaterialTheme.typography.labelMedium)
            else TextButton(onClick = { viewModel.downloadStory(story.id) }) {
                Text(if (download?.status in setOf("FAILED", "CANCELLED")) "Ulangi unduhan" else "Unduh cerita")
            }
            if (saved || download != null) TextButton(onClick = { viewModel.removeDownload(story.id) }) { Text("Hapus materi tersimpan") }
        }
        download?.error?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

/** A cached snapshot is rendered before the network refresh starts. */
@Composable
fun StoryContentRoute(id: String, viewModel: LearningViewModel, back: () -> Unit, content: @Composable (Story) -> Unit) {
    var story by remember(id) { mutableStateOf<Story?>(null) }
    var loading by remember(id) { mutableStateOf(true) }
    var error by remember(id) { mutableStateOf<String?>(null) }
    var retry by remember(id) { mutableIntStateOf(0) }
    LaunchedEffect(id, retry) {
        loading = true
        error = null
        try {
            story = viewModel.cachedStory(id)
            story = viewModel.openStory(id)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = "Materi ini belum tersimpan. Sambungkan ke server lalu coba lagi." }
        finally { loading = false }
    }
    val snapshot = story
    if (snapshot != null) content(snapshot)
    else Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = back) { Text("Kembali") }
        if (loading) { CircularProgressIndicator(); Text("Memuat materi…") }
        error?.let { Text(it); Button(onClick = { retry++ }) { Text("Coba lagi") } }
    }
}
