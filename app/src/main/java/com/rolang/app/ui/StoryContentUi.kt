package com.rolang.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
        if (state.catalogLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Memperbarui pustaka…")
        }
        state.catalogError?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (state.stories.isEmpty() && !state.catalogLoading && state.catalogError == null)
            Text(
                when (state.catalogStatus) {
                    CatalogStatus.EMPTY -> "Manifest GitHub valid, tetapi belum memuat pelajaran."
                    CatalogStatus.READY ->
                        "Katalog diterima, tetapi tidak ada entri yang dapat ditampilkan dengan aman."
                    else -> "Menghubungkan ke pustaka GitHub…"
                }
            )
        viewModel?.let {
            TextButton(onClick = it::refreshCatalog, enabled = !state.catalogLoading) {
                Text("Perbarui pustaka")
            }
        }
    }
}

@Composable
fun DownloadControls(
    story: Story,
    state: LearningState,
    viewModel: LearningViewModel,
    compact: Boolean = false,
) {
    if (story.contentPath.isBlank()) return
    val download = state.downloads.firstOrNull { it.storyId == story.id }
    val active = download?.status == "DOWNLOADING"
    val current = download?.status == "COMPLETE" && download.version == story.contentHash
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (active) {
            LinearProgressIndicator(
                progress = {
                    if (download!!.total == 0) 0f else download.completed.toFloat() / download.total
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Menyimpan gambar · ${download!!.completed}/${download.total}",
                style = MaterialTheme.typography.bodySmall,
            )
        } else
            Row(
                modifier =
                    if (compact) Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    else Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (current) Text("Tersimpan luring", style = MaterialTheme.typography.labelMedium)
                else
                    TextButton(onClick = { viewModel.downloadLesson(story.id) }) {
                        Text(
                            when {
                                download?.status == "COMPLETE" -> "Perbarui unduhan"
                                download?.status == "FAILED" -> "Coba simpan lagi"
                                else -> "Simpan untuk luring"
                            }
                        )
                    }
                if (download != null)
                    TextButton(onClick = { viewModel.removeLessonDownload(story.id) }) {
                        Text("Hapus unduhan")
                    }
            }
        download?.error?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

/** A cached snapshot is rendered before the network refresh starts. */
@Composable
fun StoryContentRoute(
    id: String,
    viewModel: LearningViewModel,
    back: () -> Unit,
    content: @Composable (Story) -> Unit,
) {
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
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error =
                "Materi ini belum tersimpan. Sambungkan ke internet untuk mengakses GitHub lalu coba lagi."
        } finally {
            loading = false
        }
    }
    val snapshot = story
    if (snapshot != null) content(snapshot)
    else
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TextButton(onClick = back) { Text("Kembali") }
            if (loading) {
                CircularProgressIndicator()
                Text("Memuat materi…")
            }
            error?.let {
                Text(it)
                Button(onClick = { retry++ }) { Text("Coba lagi") }
            }
        }
}
