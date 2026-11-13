package com.companion.yogawellness.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.ui.dashboard.DashboardViewModel
import com.companion.yogawellness.ui.dashboard.WellnessDashboardScreen
import com.companion.yogawellness.ui.theme.YogaWellnessTheme
import org.junit.Assert.assertSame
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Guards [FreshViewModelFactoryRule]: the second test must get a ViewModel wired to *its own*
 * Application, not the one the first test cached in AndroidViewModelFactory (fails without the rule).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h740dp-xxhdpi")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ViewModelAppIsolationTest {
    @get:Rule(order = 0)
    val freshViewModels = FreshViewModelFactoryRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun dashboardVmUsesThisTestsApp() {
        compose.setContent { YogaWellnessTheme { WellnessDashboardScreen(onBack = {}) } }
        compose.waitForIdle()
        val vm = ViewModelProvider(compose.activity)[DashboardViewModel::class.java]
        assertSame(ApplicationProvider.getApplicationContext<YogaWellnessApp>(), vm.getApplication<YogaWellnessApp>())
    }

    @Test fun a_firstTestCachesTheFactory() = dashboardVmUsesThisTestsApp()

    @Test fun b_secondTestStillGetsItsOwnApplication() = dashboardVmUsesThisTestsApp()
}
