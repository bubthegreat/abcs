package us.jmresearch.abcflashcards

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class AppSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeShowsSubjectsAndDeckOpens() {
        rule.onNodeWithText("Let's Learn!").assertIsDisplayed()
        rule.onNodeWithText("Letters").assertIsDisplayed()
        rule.onNodeWithText("Letters 1").performClick()
        rule.onNodeWithText("✓ Got it").assertIsDisplayed()
    }
}
