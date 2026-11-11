package com.companion.yogawellness.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.companion.yogawellness.ui.home.HomeScreen
import com.companion.yogawellness.ui.practice.PracticeScreen

object Routes {
    const val HOME = "home"
    const val PRACTICE = "practice"
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onStartPractice = { nav.navigate(Routes.PRACTICE) })
        }
        composable(Routes.PRACTICE) {
            PracticeScreen(onBack = { nav.popBackStack() })
        }
    }
}
