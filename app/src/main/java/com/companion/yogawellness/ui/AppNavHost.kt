package com.companion.yogawellness.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.ui.consent.ConsentAndSafetyScreen
import com.companion.yogawellness.ui.dashboard.WellnessDashboardScreen
import com.companion.yogawellness.ui.home.HomeScreen
import com.companion.yogawellness.ui.practice.PracticeScreen
import com.companion.yogawellness.ui.settings.SettingsScreen
import com.companion.yogawellness.ui.wellness.CheckInScreen
import com.companion.yogawellness.ui.wellness.QuestionnaireScreen
import com.companion.yogawellness.ui.wellness.WellnessViewModel
import com.companion.yogawellness.wellness.QuestionnaireType

object Routes {
    const val CONSENT = "consent"
    const val HOME = "home"
    const val PRACTICE = "practice"
    const val CHECK_IN = "checkin"
    const val QUESTIONNAIRE = "questionnaire/{type}"
    const val SETTINGS = "settings"
    const val DASHBOARD = "dashboard"

    fun questionnaire(type: QuestionnaireType) = "questionnaire/${type.name}"
}

@Composable
fun AppNavHost(nav: NavHostController = rememberNavController()) {
    val app = LocalContext.current.applicationContext as YogaWellnessApp
    val start = if (app.preferences.hasConsent) Routes.HOME else Routes.CONSENT

    NavHost(
        navController = nav,
        startDestination = start,
        enterTransition = { fadeIn(tween(320)) + slideInHorizontally(tween(360)) { it / 6 } },
        exitTransition = { fadeOut(tween(220)) + slideOutHorizontally(tween(360)) { -it / 10 } },
        popEnterTransition = { fadeIn(tween(320)) + slideInHorizontally(tween(360)) { -it / 10 } },
        popExitTransition = { fadeOut(tween(220)) + slideOutHorizontally(tween(360)) { it / 6 } }
    ) {
        composable(Routes.CONSENT) {
            val vm: WellnessViewModel = viewModel()
            ConsentAndSafetyScreen(onAccepted = {
                vm.acceptConsent()
                nav.navigate(Routes.HOME) { popUpTo(Routes.CONSENT) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onStartPractice = { nav.navigate(Routes.PRACTICE) },
                onCheckIn = { nav.navigate(Routes.CHECK_IN) },
                onQuestionnaire = { nav.navigate(Routes.questionnaire(it)) },
                onDashboard = { nav.navigate(Routes.DASHBOARD) },
                onSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.PRACTICE) { PracticeScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.CHECK_IN) { CheckInScreen(onDone = { nav.popBackStack() }) }
        composable(Routes.QUESTIONNAIRE) { entry ->
            val type = QuestionnaireType.valueOf(entry.arguments?.getString("type") ?: QuestionnaireType.PHQ9.name)
            QuestionnaireScreen(type, onDone = { nav.popBackStack() })
        }
        composable(Routes.DASHBOARD) { WellnessDashboardScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onDataDeleted = {
                    nav.navigate(Routes.CONSENT) { popUpTo(0) { inclusive = true } }
                }
            )
        }
    }
}
