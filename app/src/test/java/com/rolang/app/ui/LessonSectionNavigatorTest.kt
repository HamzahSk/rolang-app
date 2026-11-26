package com.rolang.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.rolang.app.data.LessonSection
import com.rolang.app.data.LessonSectionType
import com.rolang.app.data.LessonText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LessonSectionNavigatorTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun shortLessonShowsItsOnlySection() {
        compose.setContent { MaterialTheme { LessonSectionNavigator(listOf(section(1)), 0) {} } }

        compose.onNodeWithText("1 / 1").assertIsDisplayed()
        compose.onNodeWithText("Bagian 1").assertIsDisplayed()
    }

    @Test
    fun longLessonCanJumpToItsLastSection() {
        val sections = (1..56).map(::section)
        compose.setContent {
            var selected by mutableIntStateOf(0)
            MaterialTheme { LessonSectionNavigator(sections, selected) { selected = it } }
        }

        compose.onNodeWithText("Materi").performClick()
        compose.onNodeWithText("56. Bagian 56").performScrollTo().performClick()
        compose.onNodeWithText("56 / 56").assertIsDisplayed()
        compose.onNodeWithText("Bagian 56").assertIsDisplayed()
    }

    private fun section(number: Int) =
        LessonSection(
            id = "section-$number",
            type = LessonSectionType.LEARNING_MATERIAL,
            title = LessonText("mixed", "Bagian $number", "Bagian $number"),
            blocks = emptyList(),
        )
}
