package com.companion.yogawellness.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.companion.yogawellness.ui.components.Hairline
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.codex.AsanaCatalog32
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.ui.codex.AsanaCodexScreen
import com.companion.yogawellness.ui.codex.AsanaDetailScreen
import com.companion.yogawellness.ui.consent.ConsentAndSafetyScreen
import com.companion.yogawellness.ui.dashboard.WellnessDashboardScreen
import com.companion.yogawellness.ui.history.SessionHistoryScreen
import com.companion.yogawellness.ui.home.HomeScreen
import com.companion.yogawellness.ui.practice.PracticeScreen
import com.companion.yogawellness.ui.settings.SettingsScreen
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.wellness.CheckInScreen
import com.companion.yogawellness.ui.wellness.QuestionnaireScreen
import com.companion.yogawellness.ui.wellness.WellnessViewModel
import com.companion.yogawellness.wellness.QuestionnaireType

object Routes {
    const val CONSENT = "consent"
    const val HOME = "home"
    /** Practice, with an optional `asana` hint from the Learn tab (display only). */
    const val PRACTICE = "practice?asana={asana}"
    const val CHECK_IN = "checkin"
    const val QUESTIONNAIRE = "questionnaire/{type}"
    const val SETTINGS = "settings"
    const val DASHBOARD = "dashboard"
    const val HISTORY = "history"
    const val CODEX = "codex"
    const val CODEX_DETAIL = "codex/{id}"

    fun questionnaire(type: QuestionnaireType) = "questionnaire/${type.name}"
    fun practice(target: Asana? = null) = if (target == null) "practice" else "practice?asana=${target.name}"
    fun codexDetail(id: String) = "codex/$id"
}

/** The four destinations of the bottom navigation bar (outlined icon, filled when selected). */
enum class TopLevelDestination(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME(Routes.HOME, "Home", Icons.Outlined.Home, Icons.Filled.Home),
    LEARN(Routes.CODEX, "Learn", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    HISTORY(Routes.HISTORY, "History", Icons.Outlined.History, Icons.Filled.History),
    INSIGHTS(Routes.DASHBOARD, "Insights", Icons.Outlined.Insights, Icons.Filled.Insights);

    companion object {
        val routes: Set<String> = entries.map { it.route }.toSet()
    }
}

/** Bar is shown only on the four top-level routes (never on consent, practice, check-in, …). */
fun showsBottomBar(route: String?): Boolean = route != null && route in TopLevelDestination.routes

fun NavHostController.navigateTopLevel(route: String) = navigate(route) {
    popUpTo(Routes.HOME) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** Learn-tab page for a pose shown on the home screen (Tadasana maps to the Mountain entry). */
private fun codexIdFor(asana: Asana): String? =
    AsanaCatalog32.items.firstOrNull { it.liveAsana == asana }?.id
        ?: if (asana == Asana.TADASANA) AsanaCatalog32.byId("mountain")?.id else null

/**
 * Bottom navigation: icon over label on the page colour, separated by a hairline. The active tab
 * is marked three ways — filled icon, full-strength label and a short jade bar on the top edge —
 * so it never relies on colour alone. No pill behind the icon.
 */
@Composable
fun AppBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    val c = AppTheme.colors
    Column(Modifier.fillMaxWidth().background(c.canvas)) {
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(60.dp)
                .selectableGroup()
        ) {
            TopLevelDestination.entries.forEach { d ->
                val selected = currentRoute == d.route
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(selected = selected, role = Role.Tab, onClick = { if (!selected) onNavigate(d.route) }),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            Modifier
                                .align(Alignment.TopCenter)
                                .size(width = 20.dp, height = 2.dp)
                                .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(c.accent)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            if (selected) d.selectedIcon else d.icon,
                            contentDescription = null,
                            tint = if (selected) c.text else c.textTertiary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            d.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) c.text else c.textTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavHost(nav: NavHostController = rememberNavController()) {
    val app = LocalContext.current.applicationContext as YogaWellnessApp
    val start = if (app.preferences.hasConsent) Routes.HOME else Routes.CONSENT
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    Scaffold(
        containerColor = AppTheme.colors.canvas,
        // Screens handle their own top insets (their app bars); only the bottom bar is inset here.
        contentWindowInsets = WindowInsets(0),
        bottomBar = { if (showsBottomBar(route)) AppBottomBar(route, nav::navigateTopLevel) }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = start,
            // Inner screens keep their own Scaffolds; consuming here stops them adding the
            // bottom/system insets a second time.
            modifier = Modifier
                .padding(inner)
                .consumeWindowInsets(inner),
            // Quiet transitions: a short cross-fade with a slight drift in the direction of travel.
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 14 } },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(260)) { it / 14 } }
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
                    onStartPractice = { nav.navigate(Routes.practice()) },
                    onCheckIn = { nav.navigate(Routes.CHECK_IN) },
                    onQuestionnaire = { nav.navigate(Routes.questionnaire(it)) },
                    onDashboard = { nav.navigateTopLevel(Routes.DASHBOARD) },
                    onHistory = { nav.navigateTopLevel(Routes.HISTORY) },
                    onPose = { asana -> codexIdFor(asana)?.let { nav.navigate(Routes.codexDetail(it)) } },
                    onSettings = { nav.navigate(Routes.SETTINGS) }
                )
            }
            composable(
                Routes.PRACTICE,
                arguments = listOf(navArgument("asana") { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { entry ->
                val target = entry.arguments?.getString("asana")?.let { n -> Asana.entries.firstOrNull { it.name == n && it.isScored } }
                PracticeScreen(
                    targetAsana = target,
                    onBack = { nav.popBackStack() },
                    onFinished = {
                        // Pop Practice for good (no saveState) so its camera/model resources are released.
                        nav.navigate(Routes.HISTORY) {
                            popUpTo(Routes.PRACTICE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.CHECK_IN) { CheckInScreen(onDone = { nav.popBackStack() }) }
            composable(Routes.QUESTIONNAIRE) { entry ->
                val type = QuestionnaireType.valueOf(entry.arguments?.getString("type") ?: QuestionnaireType.PHQ9.name)
                QuestionnaireScreen(type, onDone = { nav.popBackStack() })
            }
            // Top-level tab: no back arrow (system back returns to Home).
            composable(Routes.DASHBOARD) { WellnessDashboardScreen(onBack = null) }
            composable(Routes.HISTORY) { SessionHistoryScreen(onStartPractice = { nav.navigate(Routes.practice()) }) }
            composable(Routes.CODEX) { AsanaCodexScreen(onOpen = { nav.navigate(Routes.codexDetail(it)) }) }
            composable(Routes.CODEX_DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                AsanaDetailScreen(
                    id = entry.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onPracticeLive = { nav.navigate(Routes.practice(it)) }
                )
            }
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
}
