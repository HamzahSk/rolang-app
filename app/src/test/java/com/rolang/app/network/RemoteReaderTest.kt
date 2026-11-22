package com.rolang.app.network

import com.rolang.app.TestCorpus

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rolang.app.LearningState
import com.rolang.app.LearningViewModel
import com.rolang.app.data.*
import com.rolang.app.domain.Language
import com.rolang.app.ui.ReaderScreen
import com.rolang.app.ui.RoLangTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class RemoteReaderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun staleSyncedPositionAndShortenedRemoteStoryRemainReadable() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val initial = Corpus.story(TestCorpus.load(application).first.first())
        var story by mutableStateOf(initial)
        val viewModel = LearningViewModel(application)
        val state = LearningState(readings = listOf(ReadingEntity(initial.id, "fil", 299)), ready = true)
        compose.setContent { RoLangTheme(false) { ReaderScreen(story, state, viewModel) {} } }
        compose.onNodeWithText("Paragraf ${initial.sentences.size} dari ${initial.sentences.size}").assertExists()
        compose.runOnIdle { story = initial.copy(sentences = initial.sentences.take(1)) }
        compose.onNodeWithText("Paragraf 1 dari 1").assertExists()
        compose.onNodeWithText(initial.sentences.first().getValue("fil")).assertExists()
    }

    @Test fun fixedIndonesianReferenceKeepsEveryWordLinkInteractive() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        runBlocking { TestCorpus.seed(AppServices.get(application).database.dao(), application) }
        val story = Corpus.story(TestCorpus.load(application).first.first())
        val viewModel = LearningViewModel(application)
        var state by mutableStateOf(LearningState(ready = true))
        compose.setContent { RoLangTheme(false) { ReaderScreen(story, state, viewModel) {} } }
        fun tapFirstWord() {
            compose.onNodeWithTag("reader-content").performScrollToNode(hasTestTag("story-paragraph-0"))
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag("story-paragraph-0").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            compose.onNodeWithTag("story-paragraph-0").performTouchInput { click(layouts.single().getBoundingBox(1).center) }
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("word-details").fetchSemanticsNodes().isNotEmpty() }
        }
        tapFirstWord()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Tagalog → Indonesia", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Tutup rincian kata").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("word-details").fetchSemanticsNodes().isEmpty() }
        tapFirstWord()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Tagalog → Indonesia", substring = true).fetchSemanticsNodes().isNotEmpty() }
    }
}
