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
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
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

    @get:Rule(order = 0)
    val freshViewModels = FreshViewModelFactoryRule()

    @get:Rule(order = 1)
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
    fun bottomPanelFinishAndSaveAndRecBadge() {
        val out = warriorOutput(95.0)
        val rec = com.companion.yogawellness.data.SessionAutoRecorder.Rec(active = true, elapsedSec = 42, posesSaved = 2)
        setScreen {
            Box(Modifier.fillMaxSize().background(Color(0xFF30363A))) {
                com.companion.yogawellness.ui.practice.RecBadge(rec, Modifier.align(Alignment.TopEnd).padding(16.dp))
                BottomPanel(
                    PracticeUiState(output = out, modelReady = true, rec = rec),
                    onExplain = {},
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(10.dp),
                    onFinish = {}
                )
            }
        }
        compose.onNodeWithText("Finish & Save").assertExists()
        compose.onNodeWithContentDescription("Recording session, 42 seconds, 2 poses saved").assertExists()
        save("practice_finish_and_rec")
    }

    @Test
    fun bottomPanelWithoutFinishBeforeASessionStarts() {
        setScreen {
            BottomPanel(PracticeUiState(output = null, modelReady = true), onExplain = {}, modifier = Modifier.fillMaxWidth(), onFinish = {})
        }
        compose.onNodeWithText("∑ Explain Score").assertExists()
        compose.onNodeWithText("Finish & Save").assertDoesNotExist()
    }

    @Test
    fun sessionReceiptDialog() {
        val receipt = com.companion.yogawellness.data.SessionReceipt(
            poses = listOf(
                com.companion.yogawellness.data.ReceiptPose("Virabhadrasana II (Warrior II)", "Left leg forward", 12.4, 9.1, 86.0),
                com.companion.yogawellness.data.ReceiptPose("Utkatasana (Chair Pose)", null, 3.0, 0.0, 52.0)
            ),
            totalSec = 15.4, meanScore = 79.4
        )
        setScreen { com.companion.yogawellness.ui.practice.SessionReceiptDialog(receipt, onViewHistory = {}, onKeepPractising = {}) }
        compose.onNodeWithText("Virabhadrasana II · Left leg forward — 12.4 s held, 9.1 s at ≥75%, 86% alignment").assertExists()
        compose.onNodeWithText("Keep practising").assertExists()
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
        compose.onNodeWithText("Your own data is not changed.", substring = true).assertExists()
        compose.onNodeWithText("Remove demo data").assertExists()
        save("settings")
    }

    @Test
    fun profileSelectorAndDemoBanner() {
        setScreen {
            Surface {
                androidx.compose.foundation.layout.Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    com.companion.yogawellness.ui.profile.ProfileSelector(
                        activeId = com.companion.yogawellness.data.ProfileIds.PERSONA_A, busyId = null, onSelect = {}
                    )
                    com.companion.yogawellness.ui.profile.DemoBanner(com.companion.yogawellness.data.ProfileIds.PERSONA_A, onBackToMyAccount = {})
                    com.companion.yogawellness.ui.profile.DemoBanner(com.companion.yogawellness.data.ProfileIds.MY_ACCOUNT, onBackToMyAccount = {})
                }
            }
        }
        compose.onNodeWithText("Viewing demo data (Demo A · Consistent). Your own data is safe.").assertExists()
        compose.onNodeWithText("👤 My Account").assertExists()
        org.junit.Assert.assertEquals(1, compose.onAllNodesWithText("Back to My Account").fetchSemanticsNodes().size)
        save("profile_selector_demo_banner")
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

    private fun historyState(profileId: String = com.companion.yogawellness.data.ProfileIds.MY_ACCOUNT): com.companion.yogawellness.ui.history.HistoryUiState {
        val now = 1_731_400_000_000L
        val live = com.companion.yogawellness.data.db.PracticeSessionEntity(
            id = 2, epochDay = 20_040, startedAtMs = now, endedAtMs = now + 200_000, activeSec = 200.0, meanScore = 72.0,
            profile = "STANDARD", profileId = profileId
        )
        val manual = com.companion.yogawellness.data.db.PracticeSessionEntity(
            id = 1, epochDay = 20_039, startedAtMs = now - 86_400_000, endedAtMs = now - 86_400_000, activeSec = 1800.0,
            meanScore = null, profile = "STANDARD", profileId = profileId, source = "MANUAL"
        )
        val poses = listOf(
            com.companion.yogawellness.data.db.PoseRecordEntity(
                id = 1, sessionId = 2, asana = "VIRABHADRASANA_II", side = "FRONT_LEFT", lockedSec = 12.4, meanScore = 86.0,
                bestStrictHoldSec = 8.0, bestGraceHoldSec = 9.5, meanStability = 92.0, startedAtMs = now, qualitySec = 9.1,
                peakScore = 95.0
            ),
            com.companion.yogawellness.data.db.PoseRecordEntity(
                id = 2, sessionId = 2, asana = "VRIKSHASANA", side = "LEFT_LIFTED", lockedSec = 4.0, meanScore = 41.0,
                bestStrictHoldSec = 0.0, bestGraceHoldSec = 0.0, meanStability = 80.0, startedAtMs = now + 20_000, qualitySec = 0.0,
                peakScore = 55.0, worstJoint = "Pelvic Level", topCue = "Level your hips horizontally"
            )
        )
        val items = com.companion.yogawellness.wellness.HistoryAggregator.group(listOf(live, manual), poses)
        return com.companion.yogawellness.ui.history.HistoryUiState(
            profileId, items, com.companion.yogawellness.wellness.HistoryAggregator.summary(items), loading = false
        )
    }

    @Test
    fun historyEmpty() {
        setScreen {
            com.companion.yogawellness.ui.history.SessionHistoryContent(
                com.companion.yogawellness.ui.history.HistoryUiState(loading = false), onStartPractice = {}, onDelete = {}, onLogManual = {}
            )
        }
        compose.onNodeWithText("No sessions yet. Start a practice — every pose you hold for 2 seconds is saved here.").assertExists()
        compose.onNodeWithText("Start practice").assertExists()
        save("history_empty")
    }

    @Test
    fun historyPopulatedAndExpanded() {
        setScreen { com.companion.yogawellness.ui.history.SessionHistoryContent(historyState(), onStartPractice = {}, onDelete = {}, onLogManual = {}) }
        compose.onNodeWithText("72% align").assertExists()
        compose.onNodeWithText("Manual").assertExists()
        compose.onAllNodesWithText("Collapsed", substring = true, useUnmergedTree = true)
        compose.onAllNodesWithContentDescription("Delete session from", substring = true)[0].assertExists()
        compose.onAllNodesWithText("3m 20s")[0].performClick() // expand the live session
        compose.onNodeWithText("Virabhadrasana II · Left leg forward").assertExists()
        compose.onNodeWithText("Held 12.4 s (≥75%: 9.1 s)").assertExists()
        compose.onNodeWithText("Worst joint: Pelvic Level — \"Level your hips horizontally\"").assertExists()
        save("history_populated")
    }

    @Test
    fun historyDemoProfileIsReadOnly() {
        setScreen {
            com.companion.yogawellness.ui.history.SessionHistoryContent(
                historyState(com.companion.yogawellness.data.ProfileIds.PERSONA_B), onStartPractice = {}, onDelete = {}, onLogManual = {},
                header = {
                    com.companion.yogawellness.ui.profile.DemoBanner(com.companion.yogawellness.data.ProfileIds.PERSONA_B, onBackToMyAccount = {})
                }
            )
        }
        compose.onNodeWithText("Viewing demo data (Demo B · Exam Gap). Your own data is safe.").assertExists()
        compose.onNodeWithText("Log manual practice").assertDoesNotExist()
        org.junit.Assert.assertTrue(compose.onAllNodesWithContentDescription("Delete session from", substring = true).fetchSemanticsNodes().isEmpty())
        save("history_demo")
    }

    @Test
    fun historyAtDoubleFontScale() {
        setScreen {
            val d = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(d.density, fontScale = 2f)
            ) {
                com.companion.yogawellness.ui.history.SessionHistoryContent(historyState(), onStartPractice = {}, onDelete = {}, onLogManual = {})
            }
        }
        compose.onAllNodesWithText("3m 20s")[0].performClick()
        save("history_fontscale_2")
    }

    @Test
    fun homeWithBottomNavigation() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<com.companion.yogawellness.YogaWellnessApp>()
        // Robolectric can reuse the app database file across test classes: start from a clean slate.
        kotlinx.coroutines.runBlocking { app.repository.deleteAllData() }
        app.preferences.consentAcceptedAtMs = 1L
        setScreen { AppNavHost() }
        listOf("Home", "Learn", "History", "Insights").forEach { compose.onNodeWithText(it).assertExists() }
        save("home_with_bottom_nav")
        compose.onNodeWithText("History").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("No sessions yet", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Insights").assertExists() // bar stays on top-level routes
    }

    @Test
    fun bottomBarOnlyOnTopLevelRoutes() {
        listOf(Routes.HOME, Routes.CODEX, Routes.HISTORY, Routes.DASHBOARD).forEach { org.junit.Assert.assertTrue(it, showsBottomBar(it)) }
        listOf(Routes.CONSENT, Routes.PRACTICE, Routes.CHECK_IN, Routes.QUESTIONNAIRE, Routes.SETTINGS, Routes.CODEX_DETAIL, null).forEach {
            org.junit.Assert.assertFalse("$it", showsBottomBar(it))
        }
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
