package com.companion.yogawellness.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companion.yogawellness.codex.AsanaCatalog32
import com.companion.yogawellness.codex.CodexText
import com.companion.yogawellness.codex.CodexView
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.ui.codex.AsanaCodexScreen
import com.companion.yogawellness.ui.codex.AsanaDetailContent
import com.companion.yogawellness.ui.codex.AsanaDetailScreen
import com.companion.yogawellness.ui.codex.DemoColors
import com.companion.yogawellness.ui.codex.SkeletonThumbnail
import com.companion.yogawellness.ui.theme.YogaWellnessTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Learn tab screens rendered on the JVM (same approach as [ScreenRenderTest]); PNGs in app/build/screenshots/. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h740dp-xxhdpi")
class CodexRenderTest {
    @get:Rule(order = 0)
    val freshViewModels = FreshViewModelFactoryRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun save(name: String) {
        compose.waitForIdle()
        val root = compose.activity.window.decorView.rootView
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bmp))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun setScreen(content: @Composable () -> Unit) = compose.setContent { YogaWellnessTheme { content() } }

    @Test
    fun codexListAndLiveFilter() {
        var opened: String? = null
        setScreen { AsanaCodexScreen(onOpen = { opened = it }) }
        compose.onNodeWithText("Learn").assertExists()
        compose.onNodeWithText("1. Cat-Cow Flow").assertExists()
        save("codex_list")
        compose.onNodeWithText("Scored live").performClick()
        listOf("14. Warrior II", "15. Chair Pose", "19. Tree Pose", "24. Triangle Pose").forEach {
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(it))
            compose.onNodeWithText(it).assertExists()
        }
        compose.onNodeWithText("1. Cat-Cow Flow").assertDoesNotExist()
        save("codex_list_live_only")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("14. Warrior II"))
        compose.onNodeWithText("14. Warrior II").performClick()
        assertEquals("warrior_2", opened)
    }

    @Test
    fun liveDetailOffersPracticeThisLive() {
        var practised: Asana? = null
        setScreen { AsanaDetailContent(AsanaCatalog32.byId("warrior_2")!!, initialPlaying = false, onBack = {}, onPracticeLive = { practised = it }) }
        compose.onNodeWithContentDescription("Warrior II demo", substring = true).assertExists()
        compose.onNodeWithText("Play").assertExists() // paused at the peak: a visible Play button
        save("codex_detail_warrior_2")
        compose.onNodeWithText("Practice this live").performClick() // pinned to the bottom of the screen
        assertEquals(Asana.VIRABHADRASANA_II, practised)
        compose.onNodeWithText(CodexText.GLOBAL_DISCLAIMER).assertExists()
        save("codex_detail_warrior_2_bottom")
    }

    @Test
    fun nonLiveDetailIsFollowAlongOnly() {
        setScreen { AsanaDetailContent(AsanaCatalog32.byId("downward_dog")!!, initialPlaying = false, onBack = {}, onPracticeLive = {}) }
        compose.onNodeWithText(CodexText.FOLLOW_ALONG_NOTE).performScrollTo().assertExists()
        compose.onNodeWithText("Practice this live").assertDoesNotExist()
        compose.onNodeWithText("Side").performScrollTo().assertExists()
        save("codex_detail_downward_dog")
    }

    @Test
    fun detailControlsWorkWhilePlaying() {
        setScreen { AsanaDetailContent(AsanaCatalog32.byId("triangle_pose")!!, initialPlaying = true, onBack = {}, onPracticeLive = {}) }
        compose.onNodeWithText("Pause at peak").performClick()
        compose.onNodeWithText("Play").assertExists()
        compose.onNodeWithText("Side").performClick()
        compose.onNodeWithText("0.5×").performClick()
        compose.onNodeWithText("Show angles").performClick()
        compose.onNodeWithText("Play").performClick()
        compose.onNodeWithText("Pause at peak").assertExists()
        save("codex_detail_triangle_controls")
    }

    @Test
    fun unknownIdShowsPoseNotFound() {
        setScreen { AsanaDetailScreen(id = "no_such_pose", onBack = {}, onPracticeLive = {}) }
        compose.onNodeWithText("Pose not found").assertExists()
    }

    @Test
    fun detailAtDoubleFontScale() {
        setScreen {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = 2f)) {
                AsanaDetailContent(AsanaCatalog32.byId("tree_pose")!!, initialPlaying = false, onBack = {}, onPracticeLive = {})
            }
        }
        compose.onNodeWithText("Show angles").performScrollTo().assertExists()
        save("codex_detail_tree_fontscale_2")
    }

    @Test
    fun learnTabNavigatesToDetailWithoutBottomBar() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<com.companion.yogawellness.YogaWellnessApp>()
        kotlinx.coroutines.runBlocking { app.repository.deleteAllData() }
        app.preferences.consentAcceptedAtMs = 1L
        setScreen { AppNavHost() }
        compose.onNodeWithText("Learn").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("1. Cat-Cow Flow").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("1. Cat-Cow Flow").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Marjaryasana-Bitilasana", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Insights").assertDoesNotExist() // no bottom bar on the detail route
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Insights").fetchSemanticsNodes().isNotEmpty() }
    }
}

/**
 * Contact sheets of all 32 peak poses (front and side view): the human review artifact of §5.7.
 * Open app/build/screenshots/codex_contact_sheet_{front,side}.png and look for limbs through the
 * floor, mirrored sides or floating feet.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w960dp-h1440dp-xhdpi")
class CodexContactSheetTest {
    @get:Rule(order = 0)
    val freshViewModels = FreshViewModelFactoryRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun sheet(view: CodexView) {
        compose.setContent {
            YogaWellnessTheme {
                Column(Modifier.fillMaxSize().background(Color(0xFF1E293B)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Peak poses · ${view.label} view", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    AsanaCatalog32.items.chunked(6).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { item ->
                                Column(Modifier.weight(1f)) {
                                    SkeletonThumbnail(item, view, Modifier.fillMaxWidth())
                                    Text(
                                        "${item.number}. ${item.englishName}", color = Color.White, fontSize = 10.sp, maxLines = 1,
                                        modifier = Modifier.background(DemoColors.Background).fillMaxWidth()
                                    )
                                }
                            }
                            repeat(6 - row.size) { Spacer(Modifier.weight(1f)) } // keep every tile the same size
                        }
                    }
                }
            }
        }
        val root = compose.activity.window.decorView.rootView
        compose.waitForIdle()
        val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(bmp))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "codex_contact_sheet_${view.name.lowercase()}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun contactSheetFront() = sheet(CodexView.FRONT)

    @Test fun contactSheetSide() = sheet(CodexView.SIDE)

    @Test fun contactSheet45() = sheet(CodexView.DEG45)
}
