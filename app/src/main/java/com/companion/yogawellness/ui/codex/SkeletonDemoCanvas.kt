package com.companion.yogawellness.ui.codex

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.companion.yogawellness.codex.AsanaCodexItem
import com.companion.yogawellness.codex.CalloutJoint
import com.companion.yogawellness.codex.CodexView
import com.companion.yogawellness.codex.ForwardKinematics
import com.companion.yogawellness.codex.PoseAngles
import com.companion.yogawellness.codex.PoseTimeline
import com.companion.yogawellness.codex.SkeletonJoint
import com.companion.yogawellness.codex.SkeletonRig
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Colours of the demo figure (§5.4). */
object DemoColors {
    val Background = Color(0xFF071A2F)    // brand navy
    val PeakBone = Color(0xFF5EEAD4)      // brand mint: at the peak / hold
    val MovingBone = Color(0xFF38BDF8)    // cyan: entering or leaving the pose
    val Joint = Color(0xFFE2E8F0)
    val Floor = Color(0xFF2B4A6B)
    val Plumb = Color(0x80E2E8F0)
    val Arc = Color(0xFFFBBF24)
    val PillBg = Color(0xE6071A2F)
    val PillText = Color.White
}

private val BONES = SkeletonRig.BONES
private const val JOINTS = 17
private val HEAD = SkeletonJoint.HEAD.ordinal
private val PELVIS = SkeletonJoint.PELVIS.ordinal

/**
 * Pre-allocated drawing state for one item. [update] runs *outside* DrawScope (in the frame loop):
 * it solves the rig and writes plain float arrays; the draw pass only reads them, so drawing a
 * frame allocates nothing.
 */
class DemoFrame(val item: AsanaCodexItem) {
    val x = FloatArray(JOINTS)
    val y = FloatArray(JOINTS)
    private val z = FloatArray(JOINTS)
    /** Bone indices back-to-front (nearer bones overdraw). */
    val boneOrder = IntArray(BONES.size) { it }
    private val boneDepth = FloatArray(BONES.size)
    /** Bones clearly behind the pelvis are drawn dimmer (depth cue in side/45° views). */
    val boneFar = BooleanArray(BONES.size)
    var atPeak = true
        private set

    private val callouts = item.callouts
    val vertex = IntArray(callouts.size) { callouts[it].joint.vertex.ordinal }
    val hasArc = BooleanArray(callouts.size)
    /** Screen-space arc start / sweep in degrees (clockwise from +x, y down). */
    val arcStart = FloatArray(callouts.size)
    val arcSweep = FloatArray(callouts.size)
    /** Unit screen direction in which the value pill sits away from the vertex. */
    val pillDx = FloatArray(callouts.size)
    val pillDy = FloatArray(callouts.size)

    /** Fixed framing for the item (union over start/peak blends and all three views). */
    val minX: Float
    val maxX: Float
    val maxY: Float

    init {
        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        var top = 0f
        for (view in CodexView.entries) for (i in 0..8) {
            val s = ForwardKinematics.solve(PoseAngles.lerp(item.start, item.peak, i / 8f), view.yawDeg)
            for (j in SkeletonJoint.entries) {
                val p = s[j]
                lo = min(lo, p.x.toFloat()); hi = max(hi, p.x.toFloat()); top = max(top, p.y.toFloat())
            }
        }
        val headR = HEAD_RADIUS.toFloat()
        minX = lo - headR; maxX = hi + headR; maxY = top + headR
        update(item.peak, item.defaultView.yawDeg, atPeak = true)
    }

    fun update(angles: PoseAngles, yawDeg: Float, atPeak: Boolean) {
        val s = ForwardKinematics.solve(angles, yawDeg)
        for (j in SkeletonJoint.entries) {
            val p = s[j]
            x[j.ordinal] = p.x.toFloat(); y[j.ordinal] = p.y.toFloat(); z[j.ordinal] = p.z.toFloat()
        }
        this.atPeak = atPeak
        val pz = z[PELVIS]
        for (b in BONES.indices) {
            boneDepth[b] = (z[BONES[b].a.ordinal] + z[BONES[b].b.ordinal]) * 0.5f
            boneFar[b] = boneDepth[b] < pz - 0.12f
            boneOrder[b] = b
        }
        // Insertion sort by depth (16 bones, no allocation).
        for (i in 1 until boneOrder.size) {
            val v = boneOrder[i]
            var k = i - 1
            while (k >= 0 && boneDepth[boneOrder[k]] > boneDepth[v]) { boneOrder[k + 1] = boneOrder[k]; k-- }
            boneOrder[k + 1] = v
        }
        for (i in callouts.indices) computeArc(i, callouts[i].joint)
    }

    private fun computeArc(i: Int, c: CalloutJoint) {
        val v = c.vertex.ordinal
        // Directions in screen space (y flipped).
        val ax: Float; val ay: Float; val bx: Float; val by: Float
        when {
            c == CalloutJoint.TRUNK_TILT -> {
                ax = 0f; ay = -1f
                bx = x[SkeletonJoint.NECK.ordinal] - x[v]; by = -(y[SkeletonJoint.NECK.ordinal] - y[v])
            }
            c.a != null && c.c != null && c != CalloutJoint.ARM_LINE && c != CalloutJoint.BODY_LINE -> {
                ax = x[c.a.ordinal] - x[v]; ay = -(y[c.a.ordinal] - y[v])
                bx = x[c.c.ordinal] - x[v]; by = -(y[c.c.ordinal] - y[v])
            }
            else -> {
                hasArc[i] = false
                pillDx[i] = 0.7071f; pillDy[i] = -0.7071f
                return
            }
        }
        val la = sqrt(ax * ax + ay * ay); val lb = sqrt(bx * bx + by * by)
        if (la < 1e-4f || lb < 1e-4f) { hasArc[i] = false; pillDx[i] = 0.7071f; pillDy[i] = -0.7071f; return }
        val ua = Math.toDegrees(atan2(ay, ax).toDouble()).toFloat()
        val ub = Math.toDegrees(atan2(by, bx).toDouble()).toFloat()
        var sweep = ub - ua
        while (sweep > 180f) sweep -= 360f
        while (sweep < -180f) sweep += 360f
        hasArc[i] = true
        arcStart[i] = ua
        arcSweep[i] = sweep
        val nax = ax / la; val nay = ay / la
        val open = kotlin.math.abs(sweep)
        when {
            // Nearly zero (e.g. torso 0° from vertical): put the pill on the far side of the first ray.
            open < 15f -> { pillDx[i] = -nax; pillDy[i] = -nay }
            // Nearly straight (e.g. a 175° knee): perpendicular, away from the figure's centre line.
            open > 165f -> {
                val out = if (x[v] >= 0f) 1f else -1f
                var px = -nay; var py = nax
                if (px * out < 0f) { px = -px; py = -py }
                pillDx[i] = px; pillDy[i] = py
            }
            // Otherwise inside the angle, along its bisector (the usual protractor reading).
            else -> {
                val dx = nax + bx / lb; val dy = nay + by / lb
                val l = sqrt(dx * dx + dy * dy)
                pillDx[i] = dx / l; pillDy[i] = dy / l
            }
        }
    }

    /** Pill positions of the current draw (written in the draw pass; preallocated). */
    internal val pillLeft = FloatArray(callouts.size)
    internal val pillTop = FloatArray(callouts.size)
    internal val pillW = FloatArray(callouts.size)
    internal val pillH = FloatArray(callouts.size)

    companion object {
        const val HEAD_RADIUS = 0.15
    }
}

/** Objects the draw pass needs, created once per density (never inside DrawScope). */
class DemoStyle(
    val arcStroke: Stroke,
    val plumbEffect: PathEffect,
    val pillPadPx: Float,
    val arcRadiusPx: Float,
    val pillOffsetPx: Float
)

/**
 * Draws [f] scaled into the current size: floor line, plumb line from the pelvis, depth-sorted
 * bones, joints, head and (when [labels] is given and the figure is at the peak) angle arcs with
 * value pills. Allocation-free.
 */
fun DrawScope.drawDemoFrame(f: DemoFrame, style: DemoStyle?, labels: List<TextLayoutResult>?, showPlumb: Boolean) {
    val w = size.width
    val h = size.height
    val worldW = f.maxX - f.minX
    val scale = min(w * 0.86f / worldW, h * 0.80f / f.maxY)
    val floorY = h * 0.90f
    val cx = w / 2f - (f.minX + f.maxX) / 2f * scale
    val bone = if (f.atPeak) DemoColors.PeakBone else DemoColors.MovingBone
    val boneW = max(2f, scale * 0.075f)

    drawLine(DemoColors.Floor, Offset(w * 0.04f, floorY), Offset(w * 0.96f, floorY), strokeWidth = max(1.5f, scale * 0.02f))
    if (showPlumb && style != null) {
        val px = cx + f.x[PELVIS] * scale
        drawLine(
            DemoColors.Plumb, Offset(px, floorY - f.y[PELVIS] * scale), Offset(px, floorY),
            strokeWidth = max(1f, scale * 0.015f), pathEffect = style.plumbEffect
        )
    }
    for (k in f.boneOrder.indices) {
        val b = BONES[f.boneOrder[k]]
        val ia = b.a.ordinal; val ib = b.b.ordinal
        drawLine(
            if (f.boneFar[f.boneOrder[k]]) bone.copy(alpha = 0.55f) else bone,
            Offset(cx + f.x[ia] * scale, floorY - f.y[ia] * scale),
            Offset(cx + f.x[ib] * scale, floorY - f.y[ib] * scale),
            strokeWidth = boneW, cap = StrokeCap.Round
        )
    }
    val jointR = boneW * 0.62f
    for (j in 0 until JOINTS) {
        if (j == HEAD) continue
        drawCircle(DemoColors.Joint, jointR, Offset(cx + f.x[j] * scale, floorY - f.y[j] * scale))
    }
    // Head: a ring drawn as two discs (no Stroke object needed). The rig's ground contact is
    // joint-based, so a head resting on the floor (puppy, child's pose, lying poses) would be half
    // buried; rest the circle on the floor instead.
    val head = Offset(cx + f.x[HEAD] * scale, floorY - max(f.y[HEAD], DemoFrame.HEAD_RADIUS.toFloat()) * scale)
    val headR = (DemoFrame.HEAD_RADIUS * scale).toFloat()
    drawCircle(bone, headR, head)
    drawCircle(DemoColors.Background, headR - boneW * 0.7f, head)

    if (labels == null || style == null || !f.atPeak) return
    for (i in labels.indices) {
        val v = f.vertex[i]
        val vx = cx + f.x[v] * scale
        val vy = floorY - f.y[v] * scale
        val r = style.arcRadiusPx
        if (f.hasArc[i]) {
            drawArc(
                DemoColors.Arc, f.arcStart[i], f.arcSweep[i], useCenter = false,
                topLeft = Offset(vx - r, vy - r), size = Size(2 * r, 2 * r), style = style.arcStroke
            )
        }
        val t = labels[i]
        val pw = t.size.width + 2 * style.pillPadPx
        val ph = t.size.height + style.pillPadPx
        var px = (vx + f.pillDx[i] * style.pillOffsetPx - pw / 2).coerceIn(0f, w - pw)
        var py = (vy + f.pillDy[i] * style.pillOffsetPx - ph / 2).coerceIn(0f, h - ph)
        // Nudge down past any earlier pill it would overlap (n ≤ 5, no allocation).
        for (j in 0 until i) {
            val overlaps = px < f.pillLeft[j] + f.pillW[j] && px + pw > f.pillLeft[j] &&
                py < f.pillTop[j] + f.pillH[j] && py + ph > f.pillTop[j]
            if (overlaps) py = (f.pillTop[j] + f.pillH[j] + style.pillPadPx / 2).coerceAtMost(h - ph)
        }
        px = px.coerceIn(0f, w - pw)
        f.pillLeft[i] = px; f.pillTop[i] = py; f.pillW[i] = pw; f.pillH[i] = ph
        drawRoundRect(
            DemoColors.PillBg, Offset(px, py), Size(pw, ph),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(ph / 2, ph / 2)
        )
        drawText(t, topLeft = Offset(px + style.pillPadPx, py + style.pillPadPx / 2))
    }
}

/** "95°": the value the solver measures on the peak pose (view-independent). */
fun calloutValueLabels(item: AsanaCodexItem): List<String> {
    val s = ForwardKinematics.solve(item.peak, 0f)
    return item.callouts.map { "${s.measure(it.joint).roundToInt()}°" }
}

/**
 * Animated demo of [item] (§5.4). The frame loop runs only while [playing] and the screen is
 * RESUMED; when paused, the figure freezes at the peak. Bone lengths never change: angles are
 * interpolated and the rig is re-solved every frame. [breathTime] (optional) receives the
 * breath clock so the Breath Pacer stays in step with the micro-motion.
 */
@Composable
fun SkeletonDemoCanvas(
    item: AsanaCodexItem,
    playing: Boolean,
    speed: Float,
    yawDeg: Float,
    showAngles: Boolean,
    modifier: Modifier = Modifier,
    breathTime: MutableFloatState? = null
) {
    val density = LocalDensity.current
    val frame = remember(item) { DemoFrame(item) }
    var version by remember { mutableIntStateOf(0) }
    val clock = remember(item) { FloatArray(2) } // [0] timeline seconds (speed applied), [1] breath seconds
    var resumeAtPeak by remember(item) { mutableStateOf(!playing) }
    val currentYaw by rememberUpdatedState(yawDeg)
    val currentSpeed by rememberUpdatedState(speed)
    var resumed by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        resumed = true
        onPauseOrDispose { resumed = false }
    }

    fun refresh(play: Boolean) {
        if (!play) {
            frame.update(item.peak, currentYaw, atPeak = true)
        } else {
            val state = PoseTimeline.timelineAt(clock[0])
            val breath = PoseTimeline.breathOffset(clock[1], item.breath, state.phase)
            frame.update(PoseTimeline.anglesFor(item.start, item.peak, state, breath), currentYaw, atPeak = state.blend >= 0.98f)
        }
        version++
    }

    // Yaw tween / play-pause changes while no frame loop is running.
    LaunchedEffect(item, yawDeg, playing) {
        if (!playing) resumeAtPeak = true
        refresh(playing)
    }
    LaunchedEffect(item, playing, resumed) {
        if (!playing || !resumed) return@LaunchedEffect
        if (resumeAtPeak) { clock[0] = 2f; resumeAtPeak = false } // continue from the start of the hold
        var last = -1L
        while (true) {
            // withFrameNanos that test/idle policies may suspend: the loop is infinite.
            withInfiniteAnimationFrameNanos { now ->
                if (last >= 0) {
                    val dt = ((now - last) / 1e9f).coerceIn(0f, 0.1f)
                    clock[0] = (clock[0] + dt * currentSpeed) % PoseTimeline.LOOP_SEC
                    clock[1] += dt
                    breathTime?.floatValue = clock[1]
                }
                last = now
                refresh(true)
            }
        }
    }

    val measurer = rememberTextMeasurer()
    val labels = remember(item, density) {
        val ts = TextStyle(color = DemoColors.PillText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        calloutValueLabels(item).map { measurer.measure(it, ts) }
    }
    val style = remember(density) {
        with(density) {
            DemoStyle(
                arcStroke = Stroke(width = 2.5.dp.toPx()),
                plumbEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                pillPadPx = 6.dp.toPx(),
                arcRadiusPx = 18.dp.toPx(),
                pillOffsetPx = 40.dp.toPx()
            )
        }
    }
    val description = remember(item) { item.demoDescription() }
    Canvas(
        modifier
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF0B2A4A), DemoColors.Background)))
            .semantics { contentDescription = description }
    ) {
        if (version < 0) return@Canvas // draw-phase read: redraw on every solved frame
        drawDemoFrame(frame, style, if (showAngles) labels else null, showPlumb = true)
    }
}

/** Static peak-pose figure for list rows and the contact sheet. */
@Composable
fun SkeletonThumbnail(item: AsanaCodexItem, view: CodexView, modifier: Modifier = Modifier) {
    val frame = remember(item, view) { DemoFrame(item).also { it.update(item.peak, view.yawDeg, atPeak = true) } }
    Canvas(
        modifier
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF0B2A4A), DemoColors.Background)))
    ) { drawDemoFrame(frame, null, null, showPlumb = false) }
}
