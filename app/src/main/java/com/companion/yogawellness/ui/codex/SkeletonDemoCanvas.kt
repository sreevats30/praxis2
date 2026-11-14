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
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Inter
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.companion.yogawellness.codex.AsanaCodexItem
import com.companion.yogawellness.codex.CalloutJoint
import com.companion.yogawellness.codex.CodexView
import com.companion.yogawellness.codex.ForwardKinematics
import com.companion.yogawellness.codex.PoseAngles
import com.companion.yogawellness.codex.PoseTimeline
import com.companion.yogawellness.codex.SkeletonJoint
import com.companion.yogawellness.codex.SkeletonRig
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Colours of one demo rendering (§5.4): the dark 3D viewer or a light list thumbnail. */
class DemoPalette(
    val background: Color,
    val backgroundTop: Color,
    /** At the peak / hold. */
    val peakBone: Color,
    /** Entering or leaving the pose. */
    val movingBone: Color,
    val joint: Color,
    val floor: Color,
    val plumb: Color,
    val arc: Color,
    val pillBg: Color,
    val pillText: Color
)

/** The 3D viewer: a recessed viewport, light bones, jade at the hold, amber angle arcs. */
object DemoColors {
    val Background = Color(0xFF151514)
    val Viewer = DemoPalette(
        background = Background,
        backgroundTop = Color(0xFF1B1B19),
        peakBone = Color(0xFF6DBE98),
        movingBone = Color(0xFFBDBAB2),
        joint = Color(0xFFEEECE7),
        floor = Color(0xFF3B3A37),
        plumb = Color(0x59EEECE7),
        arc = Color(0xFFE2AE52),
        pillBg = Color(0xE6000000),
        pillText = Color(0xFFEEECE7)
    )
}

/** List thumbnails: recessed tile, ink-coloured figure. */
@Composable
fun thumbnailPalette(): DemoPalette {
    val c = AppTheme.colors
    return DemoPalette(
        background = c.control, backgroundTop = c.control, peakBone = c.text, movingBone = c.textSecondary,
        joint = c.text, floor = c.lineStrong, plumb = c.lineStrong, arc = c.warning, pillBg = c.raised, pillText = c.text
    )
}

/** Steepest view tilt of the interactive demo (looking down from above). */
const val MAX_TILT_DEG = 60f

private val BONES = SkeletonRig.BONES
private const val JOINTS = 17
private val HEAD = SkeletonJoint.HEAD.ordinal
private val PELVIS = SkeletonJoint.PELVIS.ordinal

/**
 * Pre-allocated drawing state for one item. [update] runs *outside* DrawScope (in the frame loop):
 * it solves the rig and writes plain float arrays; the draw pass only reads them, so drawing a
 * frame allocates nothing.
 */
class DemoFrame(
    val item: AsanaCodexItem,
    /** Frame the figure for a full 360° turn (interactive demo): centred on the turn axis, same size at every angle. */
    val fullTurn: Boolean = false
) {
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

    /** View tilt (camera elevation) of the last [update], for the floor ellipse and framing. */
    var tiltSin = 0f
        private set
    var tiltDeg = 0f
        private set
    /** Head centre in display space; the head rests on the floor (world space) before tilting. */
    var headX = 0f
        private set
    var headY = 0f
        private set
    /** Display height of the floor point under the pelvis (foot of the plumb line). */
    var pelvisFloorY = 0f
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

    /** Highest display point per tilt step (full-turn frames); see [maxYFor]. */
    private val maxYAtTilt: FloatArray

    /** Framing height at [tiltDeg]: covers every yaw and the whole start→peak motion. */
    fun maxYFor(tiltDeg: Float): Float {
        val pos = (tiltDeg / TILT_STEP_DEG).coerceIn(0f, TILT_STEPS.toFloat())
        val i = pos.toInt().coerceAtMost(TILT_STEPS - 1)
        val f = pos - i
        return maxYAtTilt[i] * (1 - f) + maxYAtTilt[i + 1] * f
    }

    /** Fixed framing for the item (union over start/peak blends and all three views, or a full turn). */
    val minX: Float
    val maxX: Float
    val maxY: Float

    init {
        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        var top = 0f
        var radius = 0f // distance from the vertical turn axis: the same at every yaw
        val topAtTilt = FloatArray(TILT_STEPS + 1) // highest display point per tilt step, over every yaw
        val yaws = if (fullTurn) FloatArray(12) { it * 30f } else FloatArray(CodexView.entries.size) { CodexView.entries[it].yawDeg }
        for (yawDeg in yaws) for (i in 0..8) {
            val s = ForwardKinematics.solve(PoseAngles.lerp(item.start, item.peak, i / 8f), yawDeg)
            for (j in SkeletonJoint.entries) {
                val p = s[j]
                lo = min(lo, p.x.toFloat()); hi = max(hi, p.x.toFloat()); top = max(top, p.y.toFloat())
                val rho = sqrt((p.x * p.x + p.z * p.z).toFloat())
                radius = max(radius, rho)
                if (fullTurn) for (k in 0..TILT_STEPS) {
                    // y·cos t − z·sin t is largest when the joint is turned furthest away (z = −rho).
                    val t = Math.toRadians(k * TILT_STEP_DEG.toDouble())
                    topAtTilt[k] = max(topAtTilt[k], (p.y * cos(t) + rho * sin(t)).toFloat())
                }
            }
        }
        val headR = HEAD_RADIUS.toFloat()
        if (fullTurn) {
            // Symmetric about the turn axis, so spinning never shifts or rescales the figure.
            val r = max(radius, max(abs(lo), abs(hi))) + headR
            minX = -r; maxX = r
        } else {
            minX = lo - headR; maxX = hi + headR
        }
        maxY = top + headR
        maxYAtTilt = FloatArray(TILT_STEPS + 1) { if (fullTurn) topAtTilt[it] + headR else maxY }
        update(item.peak, item.defaultView.yawDeg, atPeak = true)
    }

    /**
     * Solves the rig at [yawDeg] (any angle: a full turn about the vertical axis) and applies a
     * view [tiltDeg] (0 = eye level, positive = looking down from above). +z points at the viewer.
     */
    fun update(angles: PoseAngles, yawDeg: Float, atPeak: Boolean, tiltDeg: Float = 0f) {
        val s = ForwardKinematics.solve(angles, yawDeg)
        val t = Math.toRadians(tiltDeg.toDouble())
        val st = sin(t).toFloat()
        val ct = cos(t).toFloat()
        for (j in SkeletonJoint.entries) {
            val p = s[j]
            val wy = p.y.toFloat()
            val wz = p.z.toFloat()
            x[j.ordinal] = p.x.toFloat()
            y[j.ordinal] = wy * ct - wz * st
            z[j.ordinal] = wz * ct + wy * st
        }
        tiltSin = st
        this.tiltDeg = tiltDeg
        val head = s[SkeletonJoint.HEAD]
        headX = head.x.toFloat()
        headY = max(head.y.toFloat(), HEAD_RADIUS.toFloat()) * ct - head.z.toFloat() * st
        pelvisFloorY = -s[SkeletonJoint.PELVIS].z.toFloat() * st
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
        private const val TILT_STEP_DEG = 5f
        private val TILT_STEPS = (MAX_TILT_DEG / TILT_STEP_DEG).toInt()
    }
}

/** Objects the draw pass needs, created once per density (never inside DrawScope). */
class DemoStyle(
    val arcStroke: Stroke,
    /** Outline of the floor ellipse when the view is tilted. */
    val floorStroke: Stroke = Stroke(width = 2f),
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
fun DrawScope.drawDemoFrame(
    f: DemoFrame,
    style: DemoStyle?,
    labels: List<TextLayoutResult>?,
    showPlumb: Boolean,
    palette: DemoPalette = DemoColors.Viewer
) {
    val w = size.width
    val h = size.height
    val worldW = f.maxX - f.minX
    // Full-turn frames are symmetric about the turn axis, so spinning never moves or rescales the
    // figure; tilting zooms out smoothly to fit the raised far side and the floor disc.
    val floorDepth = if (f.fullTurn) worldW / 2f * f.tiltSin else 0f
    val top = if (f.fullTurn) f.maxYFor(f.tiltDeg) else f.maxY
    val scale = min(w * 0.86f / worldW, h * 0.80f / (top + floorDepth))
    val floorY = h * 0.90f - floorDepth * scale
    val cx = w / 2f - (f.minX + f.maxX) / 2f * scale
    val bone = if (f.atPeak) palette.peakBone else palette.movingBone
    val boneW = max(2f, scale * 0.075f)

    if (f.tiltSin > 0.02f && style != null) {
        // Tilted view: the floor is a disc around the turn axis.
        val rx = worldW / 2f * scale
        val ry = rx * f.tiltSin
        drawOval(palette.floor.copy(alpha = 0.25f), Offset(cx - rx, floorY - ry), Size(2 * rx, 2 * ry))
        drawOval(palette.floor, Offset(cx - rx, floorY - ry), Size(2 * rx, 2 * ry), style = style.floorStroke)
    } else {
        drawLine(palette.floor, Offset(w * 0.04f, floorY), Offset(w * 0.96f, floorY), strokeWidth = max(1.5f, scale * 0.02f))
    }
    if (showPlumb && style != null) {
        val px = cx + f.x[PELVIS] * scale
        drawLine(
            palette.plumb, Offset(px, floorY - f.y[PELVIS] * scale), Offset(px, floorY - f.pelvisFloorY * scale),
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
        drawCircle(palette.joint, jointR, Offset(cx + f.x[j] * scale, floorY - f.y[j] * scale))
    }
    // Head: a ring drawn as two discs (no Stroke object needed). The rig's ground contact is
    // joint-based, so a head resting on the floor (puppy, child's pose, lying poses) would be half
    // buried; rest the circle on the floor instead.
    val head = Offset(cx + f.headX * scale, floorY - f.headY * scale)
    val headR = (DemoFrame.HEAD_RADIUS * scale).toFloat()
    drawCircle(bone, headR, head)
    drawCircle(palette.background, headR - boneW * 0.7f, head)

    if (labels == null || style == null || !f.atPeak) return
    for (i in labels.indices) {
        val v = f.vertex[i]
        val vx = cx + f.x[v] * scale
        val vy = floorY - f.y[v] * scale
        val r = style.arcRadiusPx
        if (f.hasArc[i]) {
            drawArc(
                palette.arc, f.arcStart[i], f.arcSweep[i], useCenter = false,
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
            palette.pillBg, Offset(px, py), Size(pw, ph),
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
    breathTime: MutableFloatState? = null,
    /** View tilt: 0 = eye level, up to [MAX_TILT_DEG] = looking down from above. */
    tiltDeg: Float = 0f,
    /** Keep the figure centred and the same size through a full 360° turn. */
    fullTurn: Boolean = false
) {
    val density = LocalDensity.current
    val frame = remember(item, fullTurn) { DemoFrame(item, fullTurn) }
    var version by remember { mutableIntStateOf(0) }
    val clock = remember(item) { FloatArray(2) } // [0] timeline seconds (speed applied), [1] breath seconds
    var resumeAtPeak by remember(item) { mutableStateOf(!playing) }
    val currentYaw by rememberUpdatedState(yawDeg)
    val currentTilt by rememberUpdatedState(tiltDeg)
    val currentSpeed by rememberUpdatedState(speed)
    var resumed by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        resumed = true
        onPauseOrDispose { resumed = false }
    }

    fun refresh(play: Boolean) {
        if (!play) {
            frame.update(item.peak, currentYaw, atPeak = true, tiltDeg = currentTilt)
        } else {
            val state = PoseTimeline.timelineAt(clock[0])
            val breath = PoseTimeline.breathOffset(clock[1], item.breath, state.phase)
            frame.update(PoseTimeline.anglesFor(item.start, item.peak, state, breath), currentYaw, atPeak = state.blend >= 0.98f, tiltDeg = currentTilt)
        }
        version++
    }

    // Rotation / tilt / play-pause changes while no frame loop is running.
    LaunchedEffect(item, yawDeg, tiltDeg, playing) {
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
        val ts = TextStyle(color = DemoColors.Viewer.pillText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = Inter)
        calloutValueLabels(item).map { measurer.measure(it, ts) }
    }
    val style = remember(density) {
        with(density) {
            DemoStyle(
                arcStroke = Stroke(width = 2.5.dp.toPx()),
                plumbEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                pillPadPx = 6.dp.toPx(),
                arcRadiusPx = 18.dp.toPx(),
                pillOffsetPx = 40.dp.toPx(),
                floorStroke = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
    val description = remember(item) { item.demoDescription() }
    Canvas(
        modifier
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(listOf(DemoColors.Viewer.backgroundTop, DemoColors.Viewer.background)))
            .semantics { contentDescription = description }
    ) {
        if (version < 0) return@Canvas // draw-phase read: redraw on every solved frame
        drawDemoFrame(frame, style, if (showAngles) labels else null, showPlumb = true)
    }
}

/** Static peak-pose figure for list rows and the contact sheet (light, theme-following tile). */
@Composable
fun SkeletonThumbnail(item: AsanaCodexItem, view: CodexView, modifier: Modifier = Modifier) {
    val frame = remember(item, view) { DemoFrame(item).also { it.update(item.peak, view.yawDeg, atPeak = true) } }
    val palette = thumbnailPalette()
    Canvas(
        modifier
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.background)
    ) { drawDemoFrame(frame, null, null, showPlumb = false, palette = palette) }
}
