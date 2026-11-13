package com.companion.yogawellness.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.companion.yogawellness.codex.AsanaCatalog32
import com.companion.yogawellness.ui.codex.AsanaDetailContent
import com.companion.yogawellness.ui.theme.YogaWellnessTheme
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Learn detail: the demo figure can be turned through 360° and tilted with the fingers. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h740dp-xxhdpi")
class CodexRotationTest {

    @get:Rule(order = 0)
    val freshViewModels = FreshViewModelFactoryRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val warrior = AsanaCatalog32.byId("warrior_2")!! // default view: Front

    @Before
    fun show() {
        compose.setContent {
            YogaWellnessTheme { AsanaDetailContent(warrior, initialPlaying = false, onBack = {}, onPracticeLive = {}) }
        }
    }

    private fun demo() = compose.onNodeWithContentDescription("Warrior II demo", substring = true)

    private fun rotationReadout(): String =
        compose.onNode(hasContentDescription("Rotation", substring = true)).fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription].joinToString()

    private fun save(name: String) {
        compose.waitForIdle()
        val root = compose.activity.window.decorView.rootView
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bmp))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun startsFacingItsDefaultView() {
        compose.onNodeWithText("Front").assertIsSelected()
        assertTrue(rotationReadout(), rotationReadout().startsWith("Rotation 0°"))
        compose.onNodeWithText("Drag to rotate 360°").assertExists()
        save("codex_rotate_default")
    }

    @Test
    fun sidewaysDragSpinsTheFigure() {
        demo().performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(-12f, 0f), delayMillis = 40) }
            up()
        }
        compose.waitForIdle()
        compose.onNodeWithText("Front").assertIsNotSelected()
        assertNotEquals("Rotation 0°", rotationReadout())
        compose.onNodeWithText("Drag to rotate 360°").assertDoesNotExist() // hint goes after first use
        save("codex_rotate_spun")
    }

    @Test
    fun verticalSwipeScrollsInsteadOfRotating() {
        demo().performTouchInput { swipeUp() }
        compose.waitForIdle()
        compose.onNodeWithText("Front").assertIsSelected()
    }

    @Test
    fun backShortcutThenDoubleTapResets() {
        compose.onNodeWithText("Back").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Back").assertIsSelected()
        assertTrue(rotationReadout(), rotationReadout().startsWith("Rotation 180°"))
        demo().performScrollTo().performTouchInput { doubleClick() }
        compose.waitForIdle()
        compose.onNodeWithText("Front").assertIsSelected()
    }

    @Test
    fun twoFingerDragTiltsTheView() {
        demo().performTouchInput {
            down(0, center + Offset(-80f, 0f))
            down(1, center + Offset(80f, 0f))
            repeat(8) {
                updatePointerBy(0, Offset(0f, 30f))
                updatePointerBy(1, Offset(0f, 30f))
                move()
            }
            up(0)
            up(1)
        }
        compose.waitForIdle()
        assertTrue(rotationReadout(), rotationReadout().contains("tilt"))
        // Turn it too, for the review screenshot: 45° view from above.
        compose.onNodeWithText("45°").performScrollTo().performClick()
        compose.waitForIdle()
        save("codex_rotate_tilted")
    }
}
