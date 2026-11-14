package com.companion.yogawellness.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.data.db.DailyCheckInEntity
import com.companion.yogawellness.data.db.QuestionnaireEntity
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.ToleranceProfile
import com.companion.yogawellness.ui.components.AppScreen
import com.companion.yogawellness.ui.components.ScreenColumn
import com.companion.yogawellness.ui.components.SectionLabel
import com.companion.yogawellness.ui.profile.DemoBannerBar
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.wellness.WellnessViewModel
import com.companion.yogawellness.wellness.ActivitySummary
import com.companion.yogawellness.wellness.QuestionnaireType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_LINE = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)

/** Home: today's check-in, practice, this week, reflections, trends, poses. */
@Composable
fun HomeScreen(
    onStartPractice: () -> Unit,
    onCheckIn: () -> Unit = {},
    onQuestionnaire: (QuestionnaireType) -> Unit = {},
    onDashboard: () -> Unit = {},
    onSettings: () -> Unit = {},
    onHistory: () -> Unit = {},
    /** Opens the Learn-tab page for a pose card. */
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
    /** Shown above the content, e.g. the "viewing demo data" banner. */
    banner: @Composable () -> Unit = {}
) {
    AppScreen(
        title = "Today",
        large = true,
        actions = { IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Settings & privacy") } }
    ) { padding ->
        ScreenColumn(padding) {
            Text(
                LocalDate.ofEpochDay(state.todayEpochDay).format(DATE_LINE),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
            banner()
            SectionLabel("Check-in", trailing = if (state.today != null) ({
                TextButton(onClick = onCheckIn, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(32.dp)) {
                    Text("Edit", style = MaterialTheme.typography.labelLarge, color = AppTheme.colors.accent)
                }
            }) else null)
            CheckInPanel(state.today, state.activity, onCheckIn)
            SectionLabel("Practice")
            PracticePanel(state.profile, onProfile, onStartPractice)
            SectionLabel("This week")
            WeekPanel(state.activity, onHistory)
            SectionLabel("Reflections")
            ReflectionsPanel(state.phq, state.gad, state.todayEpochDay, onQuestionnaire)
            SectionLabel("Trends")
            TrendsPanel(state.activity, onDashboard)
            SectionLabel("Poses")
            PoseRow(onPose)
            Footer()
        }
    }
}
