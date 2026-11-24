package com.rolang.app

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import com.rolang.app.network.AppServices
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.rules.ExternalResource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises navigation, actual word links, saving, translation, and vocabulary quiz grading. */
@RunWith(AndroidJUnit4::class)
class LearningFlowTest {
    @get:Rule(order = 0) val fixture = object : ExternalResource() {
        override fun before() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            runBlocking { TestCorpus.seed(AppServices.get(context).database.dao(), context) }
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun readSaveAndReviewAWordThroughTheInterface() {
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Lanjutkan kemajuan").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("nav-stories").performClick()
        scrollClick("stories-list", "Pasar pagi")
        compose.onNodeWithTag("reader-content").performScrollToNode(hasText("Kailangan ng nanay niya ng kamatis, sibuyas, saging, at sariwang isda."))
        val text = compose.onNodeWithText("Kailangan ng nanay niya ng kamatis, sibuyas, saging, at sariwang isda.")
        val layouts = mutableListOf<TextLayoutResult>()
        text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        text.performTouchInput { click(layouts.single().getBoundingBox(1).center) }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("word-details").fetchSemanticsNodes().isNotEmpty() }
        scrollClick("word-details", "Simpan kata")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Tersimpan di kosakata").fetchSemanticsNodes().isNotEmpty() }
        scrollClick("word-details", "Hampir dikuasai")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Status belajar: Hampir dikuasai").fetchSemanticsNodes().isNotEmpty() }
        scrollClick("word-details", "Dikuasai")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Status belajar: Dikuasai").fetchSemanticsNodes().isNotEmpty() }
        scrollClick("word-details", "Dipelajari")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Status belajar: Dipelajari").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("word-details").performScrollToIndex(0)
        compose.onNodeWithContentDescription("Tutup rincian kata").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("word-details").fetchSemanticsNodes().isEmpty() }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("Next passage").assertDoesNotExist()
        scrollClick("reader-content", "Tampilkan terjemahan")
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("Sembunyikan terjemahan").assertExists()
        compose.onNodeWithText("Ibunya memerlukan tomat, bawang, pisang, dan ikan segar.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("reader-content").performScrollToNode(hasTestTag("story-paragraph-2"))
        compose.onNodeWithTag("story-paragraph-2").assertIsDisplayed()
        compose.onNodeWithContentDescription("Kembali ke daftar cerita").performClick()
        compose.onNodeWithText("Kosakata", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("vocabulary-list").performScrollToNode(hasText("kailangan"))
        compose.onNodeWithText("kailangan").assertIsDisplayed()
        compose.onNodeWithText("Tinjauan", useUnmergedTree = true).performClick()
        scrollClick("review-content", "Latih kata berikutnya")
        compose.onNodeWithTag("review-content").performScrollToIndex(0)
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("quiz-question").fetchSemanticsNodes().isNotEmpty() }
        scrollClick("review-content", "memerlukan")
        scrollClick("review-content", "Periksa jawaban")
        compose.onNodeWithText("Jawaban diingat").performScrollTo().assertIsDisplayed()
        scrollClick("review-content", "Baik · 1 hari")
        compose.onNodeWithTag("review-content").performScrollToIndex(0)
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("quiz-question").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("review-content").performScrollToNode(hasText("Semua tinjauan selesai"))
        compose.onNodeWithText("Semua tinjauan selesai").assertIsDisplayed()
        compose.onNodeWithText("Profil", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("profile-content").performScrollToNode(hasText("Pelajaran selesai: 0 · Cerita selesai: 0"))
        compose.onNodeWithText("Pelajaran selesai: 0 · Cerita selesai: 0").assertIsDisplayed()
        compose.onNodeWithText("Tinjauan tercatat: 1").assertIsDisplayed()
    }
    private fun scrollClick(container: String, text: String) {
        compose.onNodeWithTag(container).performScrollToNode(hasText(text))
        // Exercise accessibility clicks after scrolling; word links above use real touch input.
        compose.onNode(hasText(text) and hasAnyAncestor(hasTestTag(container)))
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
    }
}
