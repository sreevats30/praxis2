package com.companion.yogawellness.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.data.ProfileIds
import com.companion.yogawellness.ui.dashboard.WellnessDashboardScreen
import com.companion.yogawellness.ui.profile.demoBannerText
import com.companion.yogawellness.ui.theme.YogaWellnessTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Phase 1 acceptance: My Account → Demo B → banner → Back to My Account → original data visible. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h740dp-xxhdpi")
class ProfileSwitchUiTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun waitForText(text: String, present: Boolean = true) = compose.waitUntil(15_000) {
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() == present
    }

    @Test
    fun switchToDemoAndBackKeepsMyData() {
        val app = ApplicationProvider.getApplicationContext<YogaWellnessApp>()
        val today = LocalDate.now().toEpochDay()
        runBlocking { app.repository.saveCheckIn(today, mood = 3, stress = 8, sleep = 4, preTension = null, postTension = null) }

        compose.setContent { YogaWellnessTheme { WellnessDashboardScreen(onBack = {}) } }
        waitForText("1 / 7") // my single check-in
        waitForText(demoBannerText(ProfileIds.PERSONA_B), present = false)

        compose.onNodeWithText("Demo B").performClick()
        waitForText(demoBannerText(ProfileIds.PERSONA_B))
        assertEquals(ProfileIds.PERSONA_B, app.activeProfile.id.value)

        compose.onNodeWithText("Back to My Account").performScrollTo().performClick()
        waitForText(demoBannerText(ProfileIds.PERSONA_B), present = false)
        waitForText("1 / 7")
        assertEquals(ProfileIds.MY_ACCOUNT, app.activeProfile.id.value)
        // My data is still exactly one check-in
        runBlocking { assertEquals(1, app.database.checkIns().all(ProfileIds.MY_ACCOUNT).size) }
    }
}
