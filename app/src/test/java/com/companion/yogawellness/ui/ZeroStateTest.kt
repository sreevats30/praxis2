package com.companion.yogawellness.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.core.app.ApplicationProvider
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.data.ProfileIds
import com.companion.yogawellness.ui.dashboard.WellnessDashboardScreen
import com.companion.yogawellness.ui.history.SessionHistoryScreen
import com.companion.yogawellness.ui.home.HomeScreen
import com.companion.yogawellness.ui.theme.YogaWellnessTheme
import com.companion.yogawellness.ui.wellness.CheckInScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A newly created account starts at zero: no demo data, no pre-filled values, no demo affordances. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h740dp-xxhdpi")
class ZeroStateTest {

    @get:Rule(order = 0)
    val freshViewModels = FreshViewModelFactoryRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app get() = ApplicationProvider.getApplicationContext<YogaWellnessApp>()

    @Before
    fun freshAccount() {
        // Robolectric can reuse the app database file across test classes: start from a clean slate.
        runBlocking { app.repository.deleteAllData() }
        app.preferences.consentAcceptedAtMs = 1L
    }

    @Test
    fun newAccountHasNoDataAtAll() {
        runBlocking {
            (listOf(ProfileIds.MY_ACCOUNT) + ProfileIds.DEMO_IDS).forEach { assertFalse(it, app.repository.hasData(it)) }
            assertFalse(app.repository.observeHasDemoData().first())
        }
        assertEquals(ProfileIds.MY_ACCOUNT, app.activeProfile.id.value)
    }

    @Test
    fun homeStartsEmpty() {
        compose.setContent { YogaWellnessTheme { HomeScreen(onStartPractice = {}) } }
        compose.waitForIdle()
        compose.onNodeWithText("How are you today?").assertExists()
        compose.onNodeWithText("Check in now").assertExists()
        compose.onNodeWithText("0 / 7 active").assertExists()
        compose.onNodeWithText("0 min").performScrollTo().assertExists()
        assertEquals(0, compose.onAllNodesWithText("day streak", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("Your vitals today").fetchSemanticsNodes().size)
    }

    @Test
    fun checkInIsUnratedUntilTheUserRatesAllThree() {
        compose.setContent { YogaWellnessTheme { CheckInScreen(onDone = {}) } }
        assertEquals(3, compose.onAllNodesWithText("Not rated yet").fetchSemanticsNodes().size)
        compose.onNodeWithText("Save check-in").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Rate mood, stress and sleep to save.").assertExists()

        val sliders = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        repeat(3) { i -> sliders[i].performScrollTo().performTouchInput { click(center) } }
        assertEquals(0, compose.onAllNodesWithText("Not rated yet").fetchSemanticsNodes().size)
        compose.onNodeWithText("Save check-in").performScrollTo().assertIsEnabled()
    }

    @Test
    fun insightsAndHistoryShowEmptyStatesWithoutDemoChips() {
        compose.setContent { YogaWellnessTheme { WellnessDashboardScreen() } }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Your insights start here").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, compose.onAllNodesWithText("Demo A").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("Export weekly PDF report").fetchSemanticsNodes().size)
        compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Export weekly PDF report")))
            .fetchSemanticsNodes().let { assertEquals(0, it.size) }
    }

    @Test
    fun historyStartsEmptyWithoutDemoChips() {
        compose.setContent { YogaWellnessTheme { SessionHistoryScreen(onStartPractice = {}) } }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("No sessions yet", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(0, compose.onAllNodesWithText("Demo B").fetchSemanticsNodes().size)
        compose.onNodeWithContentDescription("Show My Account").assertDoesNotExist()
    }
}
