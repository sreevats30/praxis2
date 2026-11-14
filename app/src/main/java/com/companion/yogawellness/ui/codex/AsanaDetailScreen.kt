package com.companion.yogawellness.ui.codex

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SwipeLeft
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companion.yogawellness.codex.AsanaCatalog32
import com.companion.yogawellness.codex.AsanaCodexItem
import com.companion.yogawellness.codex.CodexText
import com.companion.yogawellness.codex.CodexView
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.ui.components.AppScreen
import com.companion.yogawellness.ui.components.AppSwitch
import com.companion.yogawellness.ui.components.BulletList
import com.companion.yogawellness.ui.components.Chip
import com.companion.yogawellness.ui.components.EmptyState
import com.companion.yogawellness.ui.components.Hairline
import com.companion.yogawellness.ui.components.Notice
import com.companion.yogawellness.ui.components.PrimaryButton
import com.companion.yogawellness.ui.components.Section
import com.companion.yogawellness.ui.components.SecondaryButton
import com.companion.yogawellness.ui.components.Segmented
import com.companion.yogawellness.ui.components.Tone
import com.companion.yogawellness.ui.components.ValueRow
import com.companion.yogawellness.ui.practice.animationsDisabled
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.NumericStyle
import com.companion.yogawellness.ui.theme.Radius
import com.companion.yogawellness.ui.theme.SanskritStyle
import com.companion.yogawellness.ui.theme.Space
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Route entry: looks the pose up by id; an unknown id shows "Pose not found". */
@Composable
fun AsanaDetailScreen(id: String, onBack: () -> Unit, onPracticeLive: (Asana) -> Unit) {
    val item = remember(id) { AsanaCatalog32.byId(id) }
    if (item == null) {
        PoseNotFound(onBack)
    } else {
        // Reduced motion: no auto-play; the static peak pose is shown with a visible Play button.
        AsanaDetailContent(item, initialPlaying = !animationsDisabled(), onBack = onBack, onPracticeLive = onPracticeLive)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AsanaDetailContent(
    item: AsanaCodexItem,
    initialPlaying: Boolean,
    onBack: () -> Unit,
    onPracticeLive: (Asana) -> Unit
) {
    val reducedMotion = animationsDisabled()
    var playing by rememberSaveable(item.id) { mutableStateOf(initialPlaying) }
    var halfSpeed by rememberSaveable(item.id) { mutableStateOf(false) }
    // 360° view: yaw turns the figure about its vertical axis (any angle), tilt looks down from above.
    val yaw = remember(item.id) { Animatable(item.defaultView.yawDeg) }
    val tilt = remember(item.id) { Animatable(0f) }
    var autoRotate by rememberSaveable(item.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val presetIndex by remember(item.id) { derivedStateOf { presetIndexFor(yaw.value) } }
    val goToYaw: (Float) -> Unit = { target ->
        autoRotate = false
        scope.launch {
            val end = yaw.value + shortestTurn(yaw.value, target)
            if (reducedMotion) yaw.snapTo(end) else yaw.animateTo(end, tween(380))
            yaw.snapTo(normalizeDeg(yaw.value))
        }
    }
    LaunchedEffect(autoRotate, item.id) {
        if (!autoRotate || reducedMotion) return@LaunchedEffect
        var last = -1L
        while (true) {
            val now = withInfiniteAnimationFrameNanos { it }
            if (last >= 0) yaw.snapTo(normalizeDeg(yaw.value + (now - last) / 1e9f * AUTO_ROTATE_DEG_PER_SEC))
            last = now
        }
    }
    var showAngles by rememberSaveable(item.id) { mutableStateOf(true) }
    val breathTime = remember(item.id) { mutableFloatStateOf(0f) }
    val c = AppTheme.colors
    val live = item.liveAsana

    AppScreen(
        title = item.englishName,
        onBack = onBack,
        largeTitle = true,
        bottomBar = if (live != null) ({ PracticeLiveBar { onPracticeLive(live) } }) else null
    ) {
        // Identity: Sanskrit name, then the facts that decide whether to try it.
        Column(Modifier.padding(horizontal = Space.Gutter), verticalArrangement = Arrangement.spacedBy(Space.S)) {
            Text("${item.number}. ${item.sanskritName}", style = SanskritStyle.copy(fontSize = 17.sp), color = c.textSecondary)
            Text(
                buildAnnotatedString {
                    append("${item.category.shortTitle} · ${item.difficulty.label} · hold about ${item.recommendedHoldSec} s")
                    if (item.isLive) {
                        append(" · ")
                        withStyle(SpanStyle(color = c.accent)) { append("Scored live") }
                    }
                },
                style = MaterialTheme.typography.labelMedium,
                color = c.textTertiary
            )
        }

        // 3D viewer, then its controls on the page (not stacked inside the frame).
        Column(Modifier.padding(start = Space.Gutter, end = Space.Gutter, top = Space.L, bottom = Space.XL), verticalArrangement = Arrangement.spacedBy(Space.M)) {
            Box(Modifier.clip(RoundedCornerShape(Radius.L))) {
                RotatableDemo(
                    item = item,
                    playing = playing,
                    halfSpeed = halfSpeed,
                    showAngles = showAngles,
                    yaw = yaw,
                    tilt = tilt,
                    reducedMotion = reducedMotion,
                    breathTime = breathTime,
                    onUserRotate = { autoRotate = false },
                    onRotateBy = { goToYaw(yaw.value + it) },
                    onReset = {
                        goToYaw(item.defaultView.yawDeg)
                        scope.launch { if (reducedMotion) tilt.snapTo(0f) else tilt.animateTo(0f, tween(380)) }
                    }
                )
            }
            // View first (the 3D control), then playback, then the overlay toggle.
            Segmented(VIEW_PRESETS.map { it.first }, presetIndex, { goToYaw(VIEW_PRESETS[it].second) }, Modifier.fillMaxWidth(), fill = true)
            // FlowRow so the playback controls wrap instead of clipping at large font sizes.
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.S),
                verticalArrangement = Arrangement.spacedBy(Space.S)
            ) {
                SecondaryButton(
                    if (playing) "Pause at peak" else "Play",
                    { playing = !playing },
                    icon = if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    compact = true
                )
                Segmented(listOf("0.5×", "1×"), if (halfSpeed) 0 else 1, { halfSpeed = it == 0 })
                Chip(
                    "Auto-rotate", selected = autoRotate, onClick = { autoRotate = !autoRotate },
                    modifier = Modifier.heightIn(min = 44.dp),
                    leading = { Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (autoRotate) c.onInverse else c.textSecondary) }
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(value = showAngles, role = Role.Switch, onValueChange = { showAngles = it }),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Show angles", style = MaterialTheme.typography.bodyMedium, color = c.text)
                    Text("Target angles and arcs at the hold", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                }
                AppSwitch(showAngles)
            }
        }

        Section(title = "Why it helps") {
            Text(item.everydayBenefit, style = MaterialTheme.typography.bodyLarge, color = c.text)
        }
        Section(title = "Breathing") { BreathPacer(item.breath, breathTime) }
        Section(title = "Key angles", spacing = 0.dp) {
            item.callouts.forEachIndexed { i, co ->
                if (i > 0) Hairline()
                ValueRow(co.label, "${co.targetDeg}°", supporting = co.joint.description)
            }
        }
        Section(title = "Steps") {
            item.steps.forEachIndexed { i, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.M)) {
                    Text(
                        "${i + 1}",
                        style = NumericStyle.copy(fontSize = 14.sp, lineHeight = 20.sp),
                        color = c.textTertiary,
                        modifier = Modifier.width(18.dp)
                    )
                    Text(step, style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
                }
            }
        }
        Section(title = "Common mistakes") { BulletList(item.commonMistakes) }
        if (item.modifications.isNotEmpty()) {
            Section(title = "Make it easier") { BulletList(item.modifications) }
        }
        Section {
            Notice(item.safety, tone = Tone.WARNING, title = "Safety") {
                Text(CodexText.GLOBAL_DISCLAIMER, style = MaterialTheme.typography.bodySmall, color = c.textTertiary, modifier = Modifier.padding(top = Space.XS))
            }
            if (live == null) {
                // Deliberately not a button: these poses are never scored or recorded.
                Text(CodexText.FOLLOW_ALONG_NOTE, style = MaterialTheme.typography.bodySmall, color = c.textTertiary, modifier = Modifier.padding(top = Space.S))
            }
        }
    }
}

/** Pinned bottom action for the four poses the camera can score. */
@Composable
private fun PracticeLiveBar(onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(AppTheme.colors.canvas)) {
        Hairline()
        PrimaryButton(
            "Practice this live", onClick,
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Space.Gutter, vertical = Space.M),
            icon = Icons.Outlined.Videocam
        )
    }
}

@Composable
private fun PoseNotFound(onBack: () -> Unit) {
    AppScreen(title = "Learn", onBack = onBack, largeTitle = false) {
        EmptyState(
            "Pose not found",
            "This pose is no longer in the catalogue.",
            Modifier.padding(horizontal = Space.Gutter, vertical = Space.L)
        ) { PrimaryButton("Back to Learn", onBack, compact = true) }
    }
}

/** View shortcuts: Front, 45°, Side (the catalogue's views) plus Back. */
private val VIEW_PRESETS: List<Pair<String, Float>> =
    CodexView.entries.map { it.label to it.yawDeg } + ("Back" to 180f)

private const val AUTO_ROTATE_DEG_PER_SEC = 36f

/** Angle in [0, 360). */
internal fun normalizeDeg(deg: Float): Float = ((deg % 360f) + 360f) % 360f

/** Signed shortest rotation from [from] to [to], in (-180, 180]. */
internal fun shortestTurn(from: Float, to: Float): Float {
    val d = normalizeDeg(to - from)
    return if (d > 180f) d - 360f else d
}

/** Index of the view shortcut the figure is facing (within 2°), or -1 between shortcuts. */
internal fun presetIndexFor(yawDeg: Float): Int =
    VIEW_PRESETS.indexOfFirst { kotlin.math.abs(shortestTurn(yawDeg, it.second)) < 2f }

/**
 * The animated figure with 360° finger control:
 *  - drag sideways to spin it (flick for momentum); vertical swipes still scroll the page,
 *  - drag with two fingers to tilt the view (look down from above),
 *  - double-tap to reset; screen readers get rotate / tilt / reset actions.
 */
@Composable
private fun RotatableDemo(
    item: AsanaCodexItem,
    playing: Boolean,
    halfSpeed: Boolean,
    showAngles: Boolean,
    yaw: Animatable<Float, AnimationVector1D>,
    tilt: Animatable<Float, AnimationVector1D>,
    reducedMotion: Boolean,
    breathTime: MutableFloatState,
    onUserRotate: () -> Unit,
    onRotateBy: (Float) -> Unit,
    onReset: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var touched by rememberSaveable(item.id) { mutableStateOf(false) }
    val decay = remember { exponentialDecay<Float>(frictionMultiplier = 1.6f) }
    Box(
        Modifier
            .fillMaxWidth()
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Rotate left 45 degrees") { onRotateBy(-45f); true },
                    CustomAccessibilityAction("Rotate right 45 degrees") { onRotateBy(45f); true },
                    CustomAccessibilityAction("Tilt view from above") {
                        scope.launch { tilt.snapTo((tilt.value + 20f).coerceAtMost(MAX_TILT_DEG)) }; true
                    },
                    CustomAccessibilityAction("Reset view") { onReset(); true }
                )
            }
            .pointerInput(item.id) {
                detectTapGestures(onDoubleTap = { touched = true; onReset() })
            }
            .pointerInput(item.id) {
                val degPerPx = 360f / (size.width * 1.1f)
                val tiltPerPx = 80f / size.height
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    var spinning = false
                    var tilting = false
                    var dx = 0f
                    var dy = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        if (pressed.size >= 2) {
                            // Two fingers: tilt (drag down to look from above).
                            if (!tilting) { tilting = true; touched = true; onUserRotate(); scope.launch { yaw.stop() } }
                            val moveY = pressed.fold(0f) { acc, c -> acc + c.positionChange().y } / pressed.size
                            scope.launch { tilt.snapTo((tilt.value + moveY * tiltPerPx).coerceIn(0f, MAX_TILT_DEG)) }
                            pressed.forEach { it.consume() }
                            continue
                        }
                        if (tilting) { pressed.forEach { it.consume() }; continue }
                        val c = pressed.first()
                        if (c.isConsumed) break // the page scroll took this gesture
                        val d = c.positionChange()
                        if (!spinning) {
                            dx += d.x; dy += d.y
                            if (kotlin.math.abs(dx) > viewConfiguration.touchSlop && kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                                spinning = true; touched = true; onUserRotate(); scope.launch { yaw.stop() }
                            } else if (kotlin.math.abs(dy) > viewConfiguration.touchSlop) {
                                break // vertical swipe: leave it to the page scroll
                            }
                        }
                        if (spinning) {
                            tracker.addPosition(c.uptimeMillis, c.position)
                            scope.launch { yaw.snapTo(yaw.value + d.x * degPerPx) }
                            c.consume()
                        }
                    }
                    if (spinning) {
                        val velocity = tracker.calculateVelocity().x * degPerPx
                        scope.launch {
                            if (!reducedMotion) yaw.animateDecay(velocity, decay)
                            yaw.snapTo(normalizeDeg(yaw.value))
                        }
                    }
                }
            }
    ) {
        SkeletonDemoCanvas(
            item = item,
            playing = playing,
            speed = if (halfSpeed) 0.5f else 1f,
            yawDeg = yaw.value,
            showAngles = showAngles,
            modifier = Modifier.fillMaxWidth(),
            breathTime = breathTime,
            tiltDeg = tilt.value,
            fullTurn = true
        )
        // Live orientation readout
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(Radius.S))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
                .clearAndSetSemantics {
                    contentDescription = "Rotation ${normalizeDeg(yaw.value).roundToInt() % 360}°" +
                        if (tilt.value >= 1f) ", tilt ${tilt.value.roundToInt()}°" else ""
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text("${normalizeDeg(yaw.value).roundToInt() % 360}°", style = NumericStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize), color = DemoColors.Viewer.pillText)
            if (tilt.value >= 1f) {
                Text("· tilt ${tilt.value.roundToInt()}°", style = MaterialTheme.typography.labelMedium, color = DemoColors.Viewer.pillText.copy(alpha = 0.7f))
            }
        }
        // First-use hint (top-left, clear of the figure), gone after the first rotation
        if (!touched) {
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(Radius.S))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.SwipeLeft, contentDescription = null, tint = DemoColors.Viewer.pillText, modifier = Modifier.size(16.dp))
                Column {
                    Text("Drag to rotate 360°", style = MaterialTheme.typography.labelMedium, color = DemoColors.Viewer.pillText)
                    Text("Two fingers to tilt", style = MaterialTheme.typography.labelSmall, color = DemoColors.Viewer.pillText.copy(alpha = 0.65f))
                }
            }
        }
    }
}

