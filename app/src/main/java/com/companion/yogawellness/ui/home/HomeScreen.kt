package com.companion.yogawellness.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.data.db.DailyCheckInEntity
import com.companion.yogawellness.data.db.QuestionnaireEntity
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.ToleranceProfile
import com.companion.yogawellness.ui.components.AppScreen
import com.companion.yogawellness.ui.components.IconAction
import com.companion.yogawellness.ui.profile.DemoBannerBar
import com.companion.yogawellness.ui.theme.Space
import com.companion.yogawellness.ui.wellness.WellnessViewModel
import com.companion.yogawellness.wellness.ActivitySummary
import com.companion.yogawellness.wellness.QuestionnaireType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_LINE = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)

/** Time-of-day greeting for the Home title. */
fun greeting(time: LocalTime = LocalTime.now()): String = when (time.hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

/** Home: today's check-in, practice, this week, reflections and trends. */
@Composable
fun HomeScreen(
    onStartPractice: () -> Unit,
    onCheckIn: () -> Unit = {},
    onQuestionnaire: (QuestionnaireType) -> Unit = {},
    onDashboard: () -> Unit = {},
    onSettings: () -> Unit = {},
    onHistory: () -> Unit = {},
    /** Opens the Learn-tab page for a pose. */
    onPose: (Asana) -> Unit = {},
    vm: WellnessViewModel = viewModel()
) {
    val today by vm.todayCheckIn.collectAsStateWithLifecycle()
    val phq by vm.latestPhq.collectAsStateWithLifecycle()
    val gad by vm.latestGad.collectAsStateWithLifecycle()
    val activity by vm.activity.collectAsStateWithLifecycle()
    var profile by remember { mutableStateOf(vm.prefs.toleranceProfile) }
    HomeContent(
        HomeState(today, phq, gad, activity, vm.todayEpochDay, profile),
        onProfile = { profile = it; vm.setProfile(it) },
        onStartPractice = onStartPractice,
        onCheckIn = onCheckIn,
        onQuestionnaire = onQuestionnaire,
        onDashboard = onDashboard,
        onSettings = onSettings,
        onHistory = onHistory,
        onPose = onPose,
        banner = { DemoBannerBar() }
    )
}

/** Everything the home screen shows, so it can be rendered without a database. */
data class HomeState(
    val today: DailyCheckInEntity?,
    val phq: QuestionnaireEntity?,
    val gad: QuestionnaireEntity?,
    val activity: ActivitySummary,
    val todayEpochDay: Long,
    val profile: ToleranceProfile
)

@Composable
fun HomeContent(
    state: HomeState,
    onProfile: (ToleranceProfile) -> Unit,
    onStartPractice: () -> Unit,
    onCheckIn: () -> Unit,
    onQuestionnaire: (QuestionnaireType) -> Unit,
    onDashboard: () -> Unit,
    onSettings: () -> Unit,
    onHistory: () -> Unit = {},
    onPose: (Asana) -> Unit = {},
    /** Shown under the title, e.g. the "viewing demo data" banner. */
    banner: @Composable () -> Unit = {}
) {
    AppScreen(
        title = "Today",
        headline = greeting(),
        eyebrow = LocalDate.ofEpochDay(state.todayEpochDay).format(DATE_LINE),
        actions = { IconAction(Icons.Outlined.Settings, "Settings & privacy", onSettings) }
    ) {
        Box(Modifier.padding(horizontal = Space.Gutter)) { banner() }
        CheckInSection(state.today, state.activity, onCheckIn)
        PracticeSection(state.profile, onProfile, onStartPractice, onPose)
        WeekSection(state.activity, onHistory)
        ReflectionsSection(state.phq, state.gad, state.todayEpochDay, onQuestionnaire)
        TrendsSection(state.activity, onDashboard)
        Footer()
    }
}
