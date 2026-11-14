package com.companion.yogawellness.ui.practice

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companion.yogawellness.pose.JointAngleExtractor
import com.companion.yogawellness.pose.JointAnchors
import com.companion.yogawellness.pose.Landmark
import com.companion.yogawellness.pose.PipelineOutput
import com.companion.yogawellness.pose.PoseLandmarkIndex
import com.companion.yogawellness.scoring.AlignmentScorer
import com.companion.yogawellness.scoring.AsanaSpecs
import com.companion.yogawellness.scoring.JointEvaluation
import com.companion.yogawellness.scoring.JointGeometry
import com.companion.yogawellness.ui.theme.Inter
import com.companion.yogawellness.ui.theme.ScoreColors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

fun scoreColor(score: Double?): Color = when {
    score == null -> ScoreColors.Neutral
    score >= AsanaSpecs.GREEN_THRESHOLD -> ScoreColors.Green
    score >= AsanaSpecs.AMBER_THRESHOLD -> ScoreColors.Amber
    else -> ScoreColors.Red
}

/**
 * Maps normalized landmarks of the upright analysis frame onto the view, matching
 * PreviewView.ScaleType.FIT_CENTER, and mirrors X for the front camera.
 */
class OverlayTransform(
    private val viewW: Float,
    private val viewH: Float,
    private val imgW: Int,
    private val imgH: Int,
    private val mirror: Boolean
) {
    private val scale = min(viewW / imgW, viewH / imgH)
    private val dx = (viewW - imgW * scale) / 2f
    private val dy = (viewH - imgH * scale) / 2f

    fun map(x: Float, y: Float): Offset =
        Offset((if (mirror) 1f - x else x) * imgW * scale + dx, y * imgH * scale + dy)

    fun map(lm: Landmark): Offset = map(lm.x, lm.y)
}

private val OVERLAY_LANDMARKS = PoseLandmarkIndex.SKELETON_EDGES.flatMap { listOf(it.first, it.second) }.toSet() +
    PoseLandmarkIndex.NOSE

@Composable
fun PoseOverlay(output: PipelineOutput?, mirror: Boolean, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier) {
        val out = output ?: return@Canvas
        val frame = out.frame
        if (!frame.hasPose) return@Canvas
        val tf = OverlayTransform(size.width, size.height, frame.imageWidth, frame.imageHeight, mirror)
        val label = out.lockedLabel
        val evals = out.alignment?.evaluations.orEmpty()
        val frontSide = label?.side?.frontSide

        // Colour each joint vertex by the score of the joint measured there (worst wins).
        val jointColor = HashMap<Int, Double>()
        if (frontSide != null) {
            for (e in evals) {
                vertexIndices(e.spec.geometry, frontSide).forEach { idx ->
                    jointColor[idx] = minOf(jointColor[idx] ?: 100.0, e.jointScore)
                }
            }
        }

        // Bones: thin white lines with a faint dark outline so they read on any background
        val coreW = 2.5.dp.toPx()
        for ((a, b) in PoseLandmarkIndex.SKELETON_EDGES) {
            val la = frame[a]
            val lb = frame[b]
            if (la.visibility < 0.4f || lb.visibility < 0.4f) continue
            drawLine(BoneShadow, tf.map(la), tf.map(lb), strokeWidth = coreW + 2.dp.toPx(), cap = StrokeCap.Round)
        }
        for ((a, b) in PoseLandmarkIndex.SKELETON_EDGES) {
            val la = frame[a]
            val lb = frame[b]
            if (la.visibility < 0.4f || lb.visibility < 0.4f) continue
            drawLine(BoneCore, tf.map(la), tf.map(lb), strokeWidth = coreW, cap = StrokeCap.Round)
        }
        // Joints: score-coloured halo, white ring, coloured core
        for (i in OVERLAY_LANDMARKS) {
            val lm = frame[i]
            if (lm.visibility < 0.4f) continue
            val p = tf.map(lm)
            val c = jointColor[i]?.let { scoreColor(it) } ?: ScoreColors.Neutral
            drawCircle(BoneCore, radius = 5.5.dp.toPx(), center = p)
            drawCircle(c, radius = 4.dp.toPx(), center = p)
        }

        if (frontSide == null) return@Canvas

        // Angle tags for every visible scored joint
        for (e in evals) {
            val anchors = JointAngleExtractor.anchors(frame, e.spec.geometry, frontSide) ?: continue
            val v = tf.map(anchors.vertex)
            drawAngleTag(textMeasurer, v, "${e.measuredDeg.roundToInt()}°", scoreColor(e.jointScore))
        }

        // Curved correction arrow on the joint behind the primary cue
        val primary = evals.firstOrNull {
            it.directionalCue != null && it.jointScore < AlignmentScorer.CUE_JOINT_SCORE_THRESHOLD
        }
        if (primary != null) {
            JointAngleExtractor.anchors(frame, primary.spec.geometry, frontSide)?.let { anchors ->
                drawCorrectionArrow(tf, anchors, primary)
            }
        }
    }
}

private fun vertexIndices(g: JointGeometry, frontSide: com.companion.yogawellness.pose.BodySide): List<Int> = when (g) {
    is JointGeometry.ThreePoint -> g.vertex.indices(frontSide)
    is JointGeometry.BilateralAverage -> listOf(g.vertex.leftIndex, g.vertex.rightIndex)
    is JointGeometry.SegmentVsVertical -> emptyList()
    is JointGeometry.SegmentVsHorizontal -> g.left.indices(frontSide) + g.right.indices(frontSide)
}

private val BoneShadow = Color(0x59000000)
private val BoneCore = Color(0xF2EEECE7)

private fun DrawScope.drawAngleTag(tm: TextMeasurer, at: Offset, text: String, color: Color) {
    val layout = tm.measure(
        text,
        TextStyle(color = Color(0xFFEEECE7), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = Inter, fontFeatureSettings = "tnum")
    )
    val padX = 8.dp.toPx()
    val padY = 3.dp.toPx()
    val topLeft = Offset(at.x + 10.dp.toPx(), at.y - layout.size.height - 10.dp.toPx())
    val size = androidx.compose.ui.geometry.Size(layout.size.width + 2 * padX, layout.size.height + 2 * padY)
    // Leader line from the joint to the tag
    drawLine(Color.White.copy(alpha = 0.7f), at, topLeft + Offset(0f, size.height), strokeWidth = 1.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(color = Color(0xD90E0E0D), topLeft = topLeft, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()))
    drawRect(color = color, topLeft = topLeft, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
    drawText(layout, topLeft = topLeft + Offset(padX, padY))
}

private fun wrapDeg(d: Double): Double {
    var x = d % 360.0
    if (x > 180.0) x -= 360.0
    if (x <= -180.0) x += 360.0
    return x
}

private fun DrawScope.drawCorrectionArrow(tf: OverlayTransform, anchors: JointAnchors, e: JointEvaluation) {
    val v = tf.map(anchors.vertex)
    val delta = (e.spec.targetDeg - e.measuredDeg).coerceIn(-60.0, 60.0)
    val startDeg: Double
    val sweepDeg: Double
    val radius: Float

    when (anchors.kind) {
        JointAnchors.Kind.THREE_POINT -> {
            val a = tf.map(anchors.a ?: return)
            val c = tf.map(anchors.c ?: return)
            val thA = Math.toDegrees(atan2((a.y - v.y).toDouble(), (a.x - v.x).toDouble()))
            val thC = Math.toDegrees(atan2((c.y - v.y).toDouble(), (c.x - v.x).toDouble()))
            val turn = sign(wrapDeg(thC - thA)).let { if (it == 0.0) 1.0 else it }
            startDeg = thC
            // Rotating the c-ray away from a opens the joint, toward a closes it.
            sweepDeg = turn * delta
            radius = (0.55f * hypot(c.x - v.x, c.y - v.y)).coerceIn(36.dp.toPx(), 80.dp.toPx())
        }

        JointAnchors.Kind.VS_VERTICAL -> {
            val upper = tf.map(anchors.a ?: return)
            val cur = Math.toDegrees(atan2((upper.y - v.y).toDouble(), (upper.x - v.x).toDouble()))
            val lean = wrapDeg(cur + 90.0) // 0 = straight up on screen
            val side = if (lean >= 0) 1.0 else -1.0
            val target = -90.0 + side * e.spec.targetDeg
            startDeg = cur
            sweepDeg = wrapDeg(target - cur)
            radius = (0.6f * hypot(upper.x - v.x, upper.y - v.y)).coerceIn(40.dp.toPx(), 90.dp.toPx())
        }

        JointAnchors.Kind.VS_HORIZONTAL -> {
            val r = tf.map(anchors.c ?: return)
            val cur = Math.toDegrees(atan2((r.y - v.y).toDouble(), (r.x - v.x).toDouble()))
            val target = if (abs(wrapDeg(cur)) <= 90.0) 0.0 else 180.0
            startDeg = cur
            sweepDeg = wrapDeg(target - cur)
            radius = (hypot(r.x - v.x, r.y - v.y) * 1.2f).coerceIn(30.dp.toPx(), 70.dp.toPx())
        }
    }
    if (abs(sweepDeg) < 2.0) return

    val color = ScoreColors.Amber
    val path = Path().apply {
        arcTo(Rect(v, radius), startDeg.toFloat(), sweepDeg.toFloat(), forceMoveTo = true)
    }
    drawPath(path, Color(0x59000000), style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
    drawPath(path, color, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))

    // Arrowhead at the end of the arc, pointing along the direction of travel
    val endRad = Math.toRadians(startDeg + sweepDeg)
    val end = Offset(v.x + radius * cos(endRad).toFloat(), v.y + radius * sin(endRad).toFloat())
    val dir = if (sweepDeg > 0) Offset(-sin(endRad).toFloat(), cos(endRad).toFloat())
    else Offset(sin(endRad).toFloat(), -cos(endRad).toFloat())
    val head = 12.dp.toPx()
    for (spread in listOf(25.0, -25.0)) {
        val s = Math.toRadians(spread)
        val bx = -dir.x * cos(s).toFloat() + dir.y * sin(s).toFloat()
        val by = -dir.x * sin(s).toFloat() - dir.y * cos(s).toFloat()
        drawLine(color, end, Offset(end.x + bx * head, end.y + by * head), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
    }
}
