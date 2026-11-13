package com.companion.yogawellness.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.data.db.DailyCheckInEntity
import com.companion.yogawellness.data.db.QuestionnaireEntity
import com.companion.yogawellness.scoring.ToleranceProfile
import com.companion.yogawellness.ui.components.BrandMark
import com.companion.yogawellness.ui.components.GlassIconButton
import com.companion.yogawellness.ui.components.GlassPill
import com.companion.yogawellness.ui.components.HeroDecor
import com.companion.yogawellness.ui.components.PulseLine
import com.companion.yogawellness.ui.components.Reveal
import com.companion.yogawellness.ui.components.SectionHeader
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.wellness.WellnessViewModel
import com.companion.yogawellness.wellness.ActivitySummary
import com.companion.yogawellness.wellness.QuestionnaireType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HERO_DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)

/** Home: hero + today's vitals, practice launcher, weekly metrics, reflections, insights, pose library. */
@Composable
fun HomeScreen(
    onStartPractice: () -> Unit,
    onCheckIn: () -> Unit = {},
    onQuestionnaire: (QuestionnaireType) -> Unit = {},
    onDashboard: () -> Unit = {},
    onSettings: () -> Unit = {},
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
        onSettings = onSettings
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
    onSettings: () -> Unit
) {
    val scroll = rememberScrollState()
    val h = Modifier.padding(horizontal = 16.dp)
    val activity = state.activity

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            HomeHero(onSettings)
            Column(Modifier.pullUp(64.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Reveal(0, h) { TodayCard(state.today, activity, onCheckIn) }
                Reveal(1, h) { PracticeLaunchCard(state.profile, onProfile, onStartPractice) }
                Reveal(2, h) { WeekMetrics(activity) }
                Reveal(3, h) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Mind & mood", overline = "Fortnightly self-reflection")
                        ReflectionCard(QuestionnaireType.PHQ9, state.phq, state.todayEpochDay, onQuestionnaire)
                        ReflectionCard(QuestionnaireType.GAD7, state.gad, state.todayEpochDay, onQuestionnaire)
                    }
                }
                Reveal(4, h) { InsightsTeaser(activity, onDashboard) }
                Reveal(5) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionHeader("Pose library", h, overline = "Tracked & scored live")
                        PoseCarousel()
                    }
                }
                Reveal(6, h) { PrivacyFooter(onSettings) }
                Spacer(Modifier.navigationBarsPadding().height(8.dp))
            }
        }
        // Status-bar scrim fades in once the hero scrolls under it.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .graphicsLayer { alpha = (scroll.value / 160f).coerceIn(0f, 1f) }
                .background(AppTheme.colors.heroStart)
        )
    }
}

/** Shifts content up by [by] and gives the space back, so cards can overlap the hero. */
private fun Modifier.pullUp(by: Dp) = layout { measurable, constraints ->
    val px = by.roundToPx()
    val p = measurable.measure(constraints)
    layout(p.width, (p.height - px).coerceAtLeast(0)) { p.place(0, -px) }
}

private fun greeting(now: LocalTime = LocalTime.now()): String = when (now.hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeHero(onSettings: () -> Unit) {
    val c = AppTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(c.heroBrush)
    ) {
        HeroDecor(Modifier.matchParentSize())
        PulseLine(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp)
                .fillMaxWidth()
                .height(40.dp)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 128.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = 42.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Yoga Wellness Companion", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    Text("Movement · Mind · Insights", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.62f))
                }
                GlassIconButton(Icons.Rounded.Settings, "Settings & privacy", onSettings)
            }
            Spacer(Modifier.height(26.dp))
            Text(greeting(), style = MaterialTheme.typography.displaySmall, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(
                LocalDate.now().format(HERO_DATE) + " · Let's check in with your body and mind.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.76f)
            )
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill("On-device AI", icon = Icons.Rounded.Memory)
                GlassPill("No internet", icon = Icons.Rounded.WifiOff)
                GlassPill("Non-diagnostic", icon = Icons.Rounded.HealthAndSafety)
            }
        }
    }
}
