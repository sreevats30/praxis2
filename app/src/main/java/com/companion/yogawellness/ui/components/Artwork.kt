package com.companion.yogawellness.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.R
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.ui.theme.Brand

/** Soft glows, concentric rings and a dotted grid layered on the hero gradient. */
@Composable
fun HeroDecor(modifier: Modifier = Modifier, intensity: Float = 1f) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val glowR = w * 0.75f
        drawCircle(
            Brush.radialGradient(listOf(Brand.Teal400.copy(alpha = 0.30f * intensity), Color.Transparent), Offset(w * 0.95f, 0f), glowR),
            radius = glowR, center = Offset(w * 0.95f, 0f)
        )
        val glow2 = w * 0.55f
        drawCircle(
            Brush.radialGradient(listOf(Brand.Cyan.copy(alpha = 0.12f * intensity), Color.Transparent), Offset(0f, h), glow2),
            radius = glow2, center = Offset(0f, h)
        )
        val ringCenter = Offset(w * 0.92f, h * 0.08f)
        for (i in 1..4) {
            drawCircle(
                Color.White.copy(alpha = 0.07f * intensity / i),
                radius = 52.dp.toPx() * i, center = ringCenter, style = Stroke(1.dp.toPx())
            )
        }
        // Dot grid, top-left — a quiet "instrument panel" texture
        val step = 14.dp.toPx()
        val dot = 1.1.dp.toPx()
        for (r in 0 until 5) for (c in 0 until 7) {
            drawCircle(Color.White.copy(alpha = 0.09f * intensity), dot, Offset(18.dp.toPx() + c * step, h * 0.55f + r * step))
        }
    }
}

/**
 * Electrocardiogram-style pulse line. A bright highlight travels along the trace when [animate]
 * is on (infinite animations are suspended in UI tests, leaving the static trace).
 */
@Composable
fun PulseLine(modifier: Modifier = Modifier, color: Color = Brand.Mint, animate: Boolean = true, periodDp: Dp = 150.dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    // Read only inside the draw lambda, so the trace redraws each frame without recomposing.
    val phase = transition.animateFloat(
        0.35f, 1.35f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "phase"
    )
    Canvas(modifier) {
        val path = ecgPath(size.width, size.height, periodDp.toPx())
        drawPath(path, color.copy(alpha = 0.22f), style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val x = size.width * (if (animate) phase.value % 1f else 0.35f)
        val band = size.width * 0.22f
        drawPath(
            path,
            Brush.horizontalGradient(
                0f to Color.Transparent,
                ((x - band) / size.width).coerceIn(0f, 1f) to Color.Transparent,
                (x / size.width).coerceIn(0f, 1f) to color,
                ((x + 2) / size.width).coerceIn(0f, 1f) to Color.Transparent,
                1f to Color.Transparent
            ),
            style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun ecgPath(w: Float, h: Float, period: Float): Path {
    val mid = h * 0.62f
    val amp = h * 0.9f
    val p = Path()
    p.moveTo(0f, mid)
    var x0 = 0f
    while (x0 < w) {
        fun at(f: Float) = x0 + period * f
        p.lineTo(at(0.30f), mid)
        p.quadraticTo(at(0.35f), mid - amp * 0.12f, at(0.40f), mid)          // P wave
        p.lineTo(at(0.46f), mid)
        p.lineTo(at(0.49f), mid + amp * 0.08f)                                 // Q
        p.lineTo(at(0.525f), mid - amp * 0.62f)                                // R
        p.lineTo(at(0.56f), mid + amp * 0.22f)                                 // S
        p.lineTo(at(0.59f), mid)
        p.lineTo(at(0.68f), mid)
        p.quadraticTo(at(0.75f), mid - amp * 0.20f, at(0.82f), mid)           // T wave
        p.lineTo(at(1f), mid)
        x0 += period
    }
    return p
}

/** The launcher artwork on its navy disc — used as the in-app brand mark. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 44.dp, ring: Boolean = true) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF0D2E35), Color(0xFF0A3B44), Color(0xFF061E23))))
            .then(if (ring) Modifier.border(1.dp, Brand.Mint.copy(alpha = 0.45f), CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // The foreground art occupies the middle ~45% of its 108-unit canvas; scale it up to fill the disc.
        Image(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(size * 1.75f)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Pose pictograms — keypoint-style stick figures, echoing the live skeleton overlay.
// Coordinates are in a 100 × 100 box.
// ---------------------------------------------------------------------------------------------

private class Figure(val head: Offset, val headR: Float, val bones: List<Pair<Offset, Offset>>) {
    val joints: Set<Offset> get() = bones.flatMap { listOf(it.first, it.second) }.toSet()
}

private fun o(x: Number, y: Number) = Offset(x.toFloat(), y.toFloat())

/** A limb: joint → joint → joint. */
private fun chain(a: Offset, b: Offset, c: Offset): List<Pair<Offset, Offset>> = listOf(a to b, b to c)

private fun figureFor(asana: Asana): Figure = when (asana) {
    Asana.TADASANA -> {
        val ls = o(41, 28); val rs = o(59, 28); val lh = o(45, 56); val rh = o(55, 56)
        Figure(
            o(50, 15), 7f,
            listOf(ls to rs, lh to rh, ls to lh, rs to rh) +
                chain(ls, o(38, 42), o(37, 55)) + chain(rs, o(62, 42), o(63, 55)) +
                chain(lh, o(45, 75), o(45, 94)) + chain(rh, o(55, 75), o(55, 94))
        )
    }
    Asana.VRIKSHASANA -> {
        val ls = o(42, 38); val rs = o(58, 38); val lh = o(45, 62); val rh = o(55, 62)
        Figure(
            o(50, 27), 6.5f,
            listOf(ls to rs, lh to rh, ls to lh, rs to rh) +
                chain(ls, o(38, 23), o(48, 8)) + chain(rs, o(62, 23), o(52, 8)) +
                chain(lh, o(45, 79), o(45, 96)) + chain(rh, o(68, 73), o(49, 72))
        )
    }
    Asana.VIRABHADRASANA_II -> {
        val ls = o(46, 36); val rs = o(62, 36); val lh = o(49, 60); val rh = o(60, 60)
        Figure(
            o(54, 24), 6.5f,
            listOf(ls to rs, lh to rh, ls to lh, rs to rh) +
                chain(ls, o(29, 36), o(11, 36)) + chain(rs, o(78, 36), o(95, 36)) +
                chain(lh, o(28, 63), o(27, 94)) + chain(rh, o(74, 77), o(89, 94))
        )
    }
    Asana.TRIKONASANA -> {
        val ls = o(31, 48); val rs = o(41, 36); val lh = o(50, 62); val rh = o(58, 54)
        Figure(
            o(27, 34), 6.5f,
            listOf(ls to rs, lh to rh, ls to lh, rs to rh) +
                chain(ls, o(29, 62), o(27, 76)) + chain(rs, o(42, 22), o(43, 7)) +
                chain(lh, o(36, 78), o(21, 94)) + chain(rh, o(70, 74), o(83, 94))
        )
    }
    Asana.UTKATASANA -> {
        val s = o(48, 35); val hip = o(61, 62)
        Figure(
            o(37, 28), 6.5f,
            listOf(s to hip) + chain(s, o(44, 20), o(40, 5)) + chain(hip, o(37, 69), o(45, 94)) + listOf(o(45, 94) to o(34, 95))
        )
    }
}

/** Draws [asana] as a glowing keypoint figure. */
@Composable
fun PoseGlyph(
    asana: Asana,
    modifier: Modifier = Modifier,
    color: Color = Brand.Mint,
    jointColor: Color = Color.White,
    glow: Boolean = true
) {
    val fig = figureFor(asana)
    Canvas(modifier) {
        val s = minOf(size.width, size.height) / 100f
        val dx = (size.width - 100f * s) / 2f
        val dy = (size.height - 100f * s) / 2f
        fun m(p: Offset) = Offset(dx + p.x * s, dy + p.y * s)
        drawFigure(fig, ::m, s, color, jointColor, glow)
    }
}

private fun DrawScope.drawFigure(
    fig: Figure,
    m: (Offset) -> Offset,
    s: Float,
    color: Color,
    jointColor: Color,
    glow: Boolean
) {
    val bone = 3.6f * s
    if (glow) {
        for ((a, b) in fig.bones) drawLine(color.copy(alpha = 0.22f), m(a), m(b), strokeWidth = bone * 3f, cap = StrokeCap.Round)
        drawCircle(color.copy(alpha = 0.18f), fig.headR * s * 1.5f, m(fig.head))
    }
    for ((a, b) in fig.bones) drawLine(color, m(a), m(b), strokeWidth = bone, cap = StrokeCap.Round)
    drawCircle(color, fig.headR * s, m(fig.head))
    for (j in fig.joints) {
        drawCircle(jointColor, 2.7f * s, m(j))
        drawCircle(color, 1.5f * s, m(j))
    }
}
