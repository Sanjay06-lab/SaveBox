package com.savebox.app.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.savebox.app.MainActivity
import com.savebox.app.utils.MoneyFormatter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end test that drives the real app like a user:
 * onboarding → create goal → add savings → denomination box → history → settings → delete goal.
 *
 * It uses a unique goal name and the current currency, so it works whether or not the
 * emulator already has SaveBox data. Run with: gradlew connectedDebugAndroidTest
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MainFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val goalName = "UiTest ${System.currentTimeMillis() % 100_000}"

    /** Waits (up to 5 s) until some node shows [text]; database work is asynchronous. */
    private fun waitForText(text: String, substring: Boolean = false) {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun exists(text: String): Boolean =
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    /** Clicks the first clickable node with this text (titles with the same text are skipped). */
    private fun clickButton(text: String) {
        val node = compose.onAllNodes(hasText(text) and hasClickAction()).onFirst()
        scrollToIfPossible(node)
        node.performClick()
    }

    /** Types into the text field whose label is [label]. */
    private fun typeInto(label: String, value: String) {
        val node = compose.onNode(hasText(label) and hasSetTextAction())
        scrollToIfPossible(node)
        node.performTextInput(value)
    }

    /** Scrolls a node into view when it's inside a scrollable list; does nothing otherwise (e.g. in a dialog). */
    private fun scrollToIfPossible(node: androidx.compose.ui.test.SemanticsNodeInteraction) {
        try {
            node.performScrollTo()
        } catch (error: AssertionError) {
            // Not inside a scrollable container - it is already on screen.
        }
    }

    @Test
    fun mainUserJourney() {
        // 1. Onboarding (only on a fresh install).
        compose.waitForIdle()
        if (exists("Skip")) compose.onNodeWithText("Skip").performClick()

        // 2. Create a goal.
        waitForText("My savings")
        if (exists("New goal")) clickButton("New goal") else clickButton("Create a goal")
        waitForText("Goal name")
        typeInto("Goal name", goalName)
        typeInto("Target amount", "1000")
        clickButton("Create goal")

        // 3. Goal details: 0% saved.
        waitForText("0%")
        compose.onAllNodesWithText(goalName).onFirst().assertExists()

        // 4. Add a custom saving of 250 -> 25%.
        clickButton("Add savings")
        waitForText("Amount saved")
        typeInto("Amount saved", "250")
        clickButton("Add savings")
        waitForText("25%")
        waitForText(MoneyFormatter.format(25_000), substring = true)

        // 5. Denomination grid: add a row of 5 boxes worth 100 and tick one -> 35%.
        compose.onNodeWithText("Boxes", substring = true).performClick()
        waitForText("Add a row of boxes")
        clickButton("Add a row of boxes")
        typeInto("Value of one box", "100")
        typeInto("Number of boxes", "5")
        clickButton("Add")
        val firstBox = "${MoneyFormatter.format(10_000)} box 1"
        compose.waitUntil(5_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasContentDescription(firstBox)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(firstBox).performClick()
        waitForText("1 of 5 ticked", substring = true)
        compose.onNodeWithContentDescription("Back").performClick()
        waitForText("35%")

        // 6. Delete the goal -> back on the dashboard, goal gone.
        compose.onNodeWithContentDescription("Delete goal").performClick()
        clickButton("Delete")
        waitForText("My savings")
        compose.waitUntil(5_000) { !exists(goalName) }

        // 7. The other tabs open.
        compose.onNodeWithText("History").performClick()
        waitForText("Every rupee you've recorded")
        compose.onNodeWithText("Settings").performClick()
        waitForText("Appearance")
        compose.onNodeWithText("Home").performClick()
        waitForText("My savings")
    }
}
