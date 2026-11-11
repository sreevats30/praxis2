package com.companion.yogawellness.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.pose.BodySide
import com.companion.yogawellness.pose.GravityAligner
import com.companion.yogawellness.pose.PipelineOutput
import com.companion.yogawellness.pose.PosePipeline
import com.companion.yogawellness.pose.SensorSnapshot
import com.companion.yogawellness.replay.SyntheticPoseBuilder
import com.companion.yogawellness.scoring.ToleranceProfile
import com.companion.yogawellness.ui.home.HomeScreen
import com.companion.yogawellness.ui.practice.BottomPanel
import com.companion.yogawellness.ui.practice.ExplainScoreContent
import com.companion.yogawellness.ui.practice.PoseOverlay
import com.companion.yogawellness.ui.practice.PracticeUiState
import com.companion.yogawellness.ui.theme.YogaWellnessTheme
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
    fun homeScreen() {
        setScreen { HomeScreen(onStartPractice = {}) }
        compose.onNodeWithText("Yoga Wellness Companion").assertExists()
        save("home")
    }
}
