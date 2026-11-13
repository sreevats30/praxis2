package com.companion.yogawellness.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.pose.BodySide
import com.companion.yogawellness.pose.GravityAligner
import com.companion.yogawellness.pose.PipelineOutput
import com.companion.yogawellness.pose.PosePipeline
import com.companion.yogawellness.pose.SensorSnapshot
import com.companion.yogawellness.replay.SyntheticPoseBuilder
import com.companion.yogawellness.scoring.ToleranceProfile
import com.companion.yogawellness.ui.home.HomeContent
import com.companion.yogawellness.ui.home.HomeScreen
import com.companion.yogawellness.ui.home.HomeState
import com.companion.yogawellness.wellness.ActivitySummary
import com.companion.yogawellness.wellness.QuestionnaireType
import com.companion.yogawellness.ui.practice.BottomPanel
import com.companion.yogawellness.ui.practice.ExplainScoreContent
import com.companion.yogawellness.ui.practice.PoseOverlay
import com.companion.yogawellness.ui.practice.PracticeUiState
import com.companion.yogawellness.ui.theme.YogaWellnessTheme
import com.companion.yogawellness.wellness.MockPersonaSeeder
import com.companion.yogawellness.wellness.Persona
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders real Compose screens on the JVM (Robolectric native graphics). Each test fails if the
 * screen throws while composing/drawing, and saves a PNG to app/build/screenshots/ for review.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h740dp-xxhdpi")
class ScreenRenderTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Software-draws the activity's root view (captureToImage() hangs under Robolectric). */
    private fun save(name: String) {
        compose.waitForIdle()
        val root = compose.activity.window.decorView.rootView
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bmp))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun setScreen(content: @Composable () -> Unit) = compose.setContent {
        YogaWellnessTheme { content() }
    }

    companion object {
        /** Runs the real pipeline: Tadasana calibration, then Warrior II with the given front knee. */
        fun warriorOutput(kneeDeg: Double, frontSide: BodySide = BodySide.LEFT): PipelineOutput {
            val b = SyntheticPoseBuilder()
            val p = PosePipeline()
            val sensors = SensorSnapshot(GravityAligner.checkPhoneTilt(0f, 9.81f, 0f), 0.02)
            var t = 0L
            var out: PipelineOutput? = null
            repeat(150) { t += 40; out = p.process(b.tadasana(t), sensors) }
            repeat(120) { t += 40; out = p.process(b.warriorII(t, frontSide, kneeDeg), sensors) }
            return out!!
        }
    }

    @Test
    fun practiceOverlayWrongWarriorII() {
        val out = warriorOutput(125.0)
        setScreen {
            Box(Modifier.fillMaxSize().background(Color(0xFF30363A))) {
                PoseOverlay(out, mirror = true, modifier = Modifier.fillMaxSize())
                BottomPanel(
                    PracticeUiState(output = out, modelReady = true, fps = 27.0),
                    onExplain = {},
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp)
                )
            }
        }
        compose.onNodeWithText("Bend your front knee deeper toward 90 degrees").assertExists()
        save("practice_warrior_125")
    }

    @Test
    fun practiceOverlayCorrectedWarriorII() {
        val out = warriorOutput(95.0, BodySide.RIGHT)
        setScreen {
            Box(Modifier.fillMaxSize().background(Color(0xFF30363A))) {
                PoseOverlay(out, mirror = true, modifier = Modifier.fillMaxSize())
                BottomPanel(
                    PracticeUiState(output = out, modelReady = true, fps = 27.0),
                    onExplain = {},
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp)
                )
            }
        }
        compose.onNodeWithText("Beautiful — hold it there").assertExists()
        save("practice_warrior_95")
    }

    @Test
    fun practiceTreeKneeSafetyAlert() {
        val b = SyntheticPoseBuilder()
        val p = PosePipeline()
        var t = 0L
        var out: PipelineOutput? = null
        repeat(40) {
            t += 40
            out = p.process(
                b.front(
                    t, left = SyntheticPoseBuilder.Leg(40.0, 64.3), right = SyntheticPoseBuilder.Leg(2.0, 178.0),
                    leftArm = SyntheticPoseBuilder.Arm(170.0, 168.0), rightArm = SyntheticPoseBuilder.Arm(170.0, 168.0)
                ),
                null
            )
        }
        val caption = com.companion.yogawellness.feedback.CoachUtterance(
            com.companion.yogawellness.feedback.SafetyGuards.TREE_KNEE_MESSAGE,
            com.companion.yogawellness.feedback.CoachUtterance.Kind.SAFETY
        )
        setScreen {
            Box(Modifier.fillMaxSize().background(Color(0xFF30363A))) {
                PoseOverlay(out, mirror = true, modifier = Modifier.fillMaxSize())
                BottomPanel(
                    PracticeUiState(output = out, modelReady = true, caption = caption),
                    onExplain = {},
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp)
                )
            }
        }
        compose.onNodeWithText(com.companion.yogawellness.feedback.SafetyGuards.TREE_KNEE_MESSAGE).assertExists()
        save("practice_tree_knee_guard")
    }

    @Test
    fun explainScoreSheetContent() {
        val out = warriorOutput(125.0)
        setScreen { Surface { ExplainScoreContent(out, ToleranceProfile.STANDARD) } }
        compose.onNodeWithText("Final score S = 41.4%  (capped by a critical joint)").assertExists()
        save("explain_score_warrior_125")
    }

    @Test
    fun consentScreenRequiresAllThreeBoxes() {
        var accepted = false
        setScreen { com.companion.yogawellness.ui.consent.ConsentAndSafetyScreen(onAccepted = { accepted = true }) }
        compose.onNodeWithText(com.companion.yogawellness.ui.consent.NON_DIAGNOSTIC_STATEMENT).assertExists()
        compose.onNodeWithText("Agree and continue").assertIsNotEnabled()
        save("consent")
        listOf(
            "I understand this app does not diagnose or treat any condition.",
            "I agree to on-device camera processing and to storing my data only on this device.",
            "I will practise safely and stop if anything hurts."
        ).forEach { compose.onNodeWithText(it).performScrollTo().performClick() }
        compose.onNodeWithText("Agree and continue").performScrollTo().assertIsEnabled().performClick()
        org.junit.Assert.assertTrue(accepted)
    }

    @Test
    fun checkInScreen() {
        setScreen { com.companion.yogawellness.ui.wellness.CheckInScreen(onDone = {}) }
        compose.onNodeWithText("Stress").assertExists()
        save("checkin")
    }

    @Test
    fun phq9ItemNineOpensCrisisSupportImmediately() {
        setScreen { com.companion.yogawellness.ui.wellness.QuestionnaireScreen(com.companion.yogawellness.wellness.QuestionnaireType.PHQ9, onDone = {}) }
        save("phq9")
        // "Several days" on item 9 (the 9th "Several days" option on the screen)
        compose.onAllNodesWithText("Several days")[8].performScrollTo().performClick()
        compose.onNodeWithText("You don't have to go through this alone").assertExists()
        compose.onNodeWithText("Call Tele-MANAS 14416", substring = true).assertExists()
        save("phq9_crisis_sheet")
    }

    @Test
    fun crisisSupportContent() {
        setScreen { Surface { com.companion.yogawellness.ui.wellness.CrisisSupportContent(onDismiss = {}) } }
        compose.onNodeWithText("Emergency services 112", substring = true).assertExists()
        save("crisis_support")
    }

    @Test
    fun settingsScreen() {
        setScreen { com.companion.yogawellness.ui.settings.SettingsScreen(onBack = {}, onDataDeleted = {}) }
        compose.onAllNodesWithText("Delete All My Data")[0].assertExists()
        save("settings")
    }

    @Test
    fun dashboardWithPersonaA() {
        val data = com.companion.yogawellness.wellness.DashboardBuilderTest().personaDashboard(com.companion.yogawellness.wellness.Persona.A_CONSISTENT)
        setScreen {
            Surface {
                androidx.compose.foundation.layout.Column(
                    Modifier.padding(16.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)
                ) { com.companion.yogawellness.ui.dashboard.DashboardContent(data, onExport = {}) }
            }
        }
        compose.onNodeWithText("Weekly trend").assertExists()
        compose.onNodeWithText("Practice & stress").assertExists()
        save("dashboard_persona_a")
    }

    @Test
    fun homeScreen() {
        setScreen { HomeScreen(onStartPractice = {}) }
        compose.onNodeWithText("Yoga Wellness Companion").assertExists()
        compose.onNodeWithText("Start practice").assertExists()
        save("home")
    }

    /** Home state built from a demo persona (42 days), without going through the database. */
    private fun personaHome(persona: Persona = Persona.A_CONSISTENT): HomeState {
        val today = LocalDate.now().toEpochDay()
        val d = MockPersonaSeeder.generate(persona, today)
        fun latest(t: QuestionnaireType) = d.questionnaires.filter { it.type == t.name }.maxByOrNull { it.epochDay }
        return HomeState(
            today = d.checkIns.firstOrNull { it.epochDay == today },
            phq = latest(QuestionnaireType.PHQ9),
            gad = latest(QuestionnaireType.GAD7),
            activity = ActivitySummary.from(d.checkIns, d.sessions.map { it.session }, today),
            todayEpochDay = today,
            profile = ToleranceProfile.STANDARD
        )
    }

    private fun renderHome(state: HomeState) = setScreen {
        HomeContent(state, onProfile = {}, onStartPractice = {}, onCheckIn = {}, onQuestionnaire = {}, onDashboard = {}, onSettings = {})
    }

    /** Full-length home with 42 days of demo history (tall viewport, for design review). */
    @Test
    @Config(qualifiers = "w360dp-h2600dp-xxhdpi")
    fun homeScreenWithHistoryFullLength() {
        renderHome(personaHome())
        compose.onNodeWithText("Your vitals today").assertExists()
        compose.onNodeWithText("Pose library").assertExists()
        save("home_full_persona_a")
    }

    /** Same screen in dark theme. */
    @Test
    @Config(qualifiers = "w360dp-h2600dp-night-xxhdpi")
    fun homeScreenDarkFullLength() {
        renderHome(personaHome())
        save("home_full_dark")
    }

    /** First run: nothing logged yet. */
    @Test
    fun homeScreenFirstRun() {
        val today = LocalDate.now().toEpochDay()
        renderHome(HomeState(null, null, null, ActivitySummary.empty(today), today, ToleranceProfile.STANDARD))
        compose.onNodeWithText("Check in now").assertExists()
        save("home_first_run")
    }

    @Test
    @Config(qualifiers = "w360dp-h2400dp-xxhdpi")
    fun consentScreenFullLength() {
        setScreen { com.companion.yogawellness.ui.consent.ConsentAndSafetyScreen(onAccepted = {}) }
        compose.onNodeWithText("I will practise safely and stop if anything hurts.").performClick()
        save("consent_full")
    }

    @Test
    @Config(qualifiers = "w360dp-h1700dp-xxhdpi")
    fun checkInScreenFullLength() {
        setScreen { com.companion.yogawellness.ui.wellness.CheckInScreen(onDone = {}) }
        save("checkin_full")
    }

    @Test
    @Config(qualifiers = "w360dp-h3000dp-xxhdpi")
    fun dashboardScreenFullLength() {
        val data = com.companion.yogawellness.wellness.DashboardBuilderTest().personaDashboard(Persona.A_CONSISTENT)
        setScreen {
            com.companion.yogawellness.ui.dashboard.DashboardScreenContent(
                data, Persona.A_CONSISTENT.label, error = null, onBack = {}, onExport = {}
            )
        }
        compose.onNodeWithText("Weekly trend").assertExists()
        save("dashboard_full")
    }

    @Test
    @Config(qualifiers = "w360dp-h1900dp-xxhdpi")
    fun settingsScreenFullLength() {
        setScreen { com.companion.yogawellness.ui.settings.SettingsScreen(onBack = {}, onDataDeleted = {}) }
        save("settings_full")
    }

    @Test
    fun practiceLoadingState() {
        setScreen {
            Box(Modifier.fillMaxSize().background(Color(0xFF30363A))) {
                BottomPanel(
                    PracticeUiState(output = null, modelReady = true),
                    onExplain = {},
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp)
                )
            }
        }
        compose.onNodeWithText("Set up your space").assertExists()
        save("practice_setup")
    }
}
