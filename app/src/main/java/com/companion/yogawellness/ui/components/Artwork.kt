package com.companion.yogawellness.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.ui.theme.AppTheme

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

/** Draws [asana] as a single-weight line figure: bones, a ring for the head, small solid joints. */
@Composable
fun PoseGlyph(
    asana: Asana,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.text
) {
    val fig = figureFor(asana)
    Canvas(modifier) {
        val s = minOf(size.width, size.height) / 100f
        val dx = (size.width - 100f * s) / 2f
        val dy = (size.height - 100f * s) / 2f
        fun m(p: Offset) = Offset(dx + p.x * s, dy + p.y * s)
        drawFigure(fig, ::m, s, color)
    }
}

private fun DrawScope.drawFigure(fig: Figure, m: (Offset) -> Offset, s: Float, color: Color) {
    val bone = 2.6f * s
    for ((a, b) in fig.bones) drawLine(color, m(a), m(b), strokeWidth = bone, cap = StrokeCap.Round)
    drawCircle(color, fig.headR * s, m(fig.head), style = androidx.compose.ui.graphics.drawscope.Stroke(bone))
    for (j in fig.joints) drawCircle(color, 2.1f * s, m(j))
}
