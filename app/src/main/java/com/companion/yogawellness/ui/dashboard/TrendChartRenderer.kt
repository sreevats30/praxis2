package com.companion.yogawellness.ui.dashboard

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.companion.yogawellness.wellness.WeeklyRow
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

/**
 * Chart colours (validated categorical slots 1–3 against this app's card surfaces in both modes).
 * Text always uses ink tokens; series colours are only used for marks and legend keys.
 */
data class ChartColors(
    val surface: Int,
    val inkPrimary: Int,
    val inkSecondary: Int,
    val muted: Int,
    val grid: Int,
    val axis: Int,
    val quality: Int,
    val stress: Int,
    val mood: Int
) {
    companion object {
        val Light = ChartColors(
            surface = 0xFFFFFCF6.toInt(), inkPrimary = 0xFF0B0B0B.toInt(), inkSecondary = 0xFF52514E.toInt(),
            muted = 0xFF898781.toInt(), grid = 0xFFE1E0D9.toInt(), axis = 0xFFC3C2B7.toInt(),
            quality = 0xFF2A78D6.toInt(), stress = 0xFFEB6834.toInt(), mood = 0xFF1BAF7A.toInt()
        )
        val Dark = ChartColors(
            surface = 0xFF1A1C1B.toInt(), inkPrimary = 0xFFF0EFEC.toInt(), inkSecondary = 0xFFC3C2B7.toInt(),
            muted = 0xFF898781.toInt(), grid = 0xFF2C2C2A.toInt(), axis = 0xFF383835.toInt(),
            quality = 0xFF3987E5.toInt(), stress = 0xFFD95926.toInt(), mood = 0xFF199E70.toInt()
        )
    }
}

/**
 * Weekly trend as two small-multiple panels on a shared week axis:
 *   top    — practice quality (0–100)
 *   bottom — self-rated stress and mood (1–10)
 * The blueprint asks for a "dual-axis" chart; two y-scales on one plot let the reader infer a
 * relationship from arbitrary axis ranges, so the same comparison is drawn as stacked panels.
 *
 * Drawn on a plain [Canvas] so the app (Compose nativeCanvas) and the PDF report share one renderer.
 * @param dp pixels per dp, @param sp pixels per sp (use ~0.8 for PDF points)
 */
class TrendChartRenderer(private val c: ChartColors, private val dp: Float, private val sp: Float) {

    private val leftPad = 30 * dp
    private val rightPad = 40 * dp
    private val titleH = 22 * dp
    private val gap = 16 * dp
    private val xLabelH = 20 * dp
    private val legendH = 20 * dp

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.grid; strokeWidth = max(1f, 0.75f * dp) }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.axis; strokeWidth = max(1f, 0.75f * dp) }
    private val tickText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.muted; textSize = 11 * sp; textAlign = Paint.Align.RIGHT }
    private val xText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.muted; textSize = 11 * sp; textAlign = Paint.Align.CENTER }
    private val titleText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.inkSecondary; textSize = 12 * sp }
    private val endText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.inkPrimary; textSize = 12 * sp; isFakeBoldText = true }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2 * dp; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val selPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c.inkSecondary; strokeWidth = max(1f, dp); alpha = 140 }

    private class Series(val name: String, val color: Int, val values: List<Double?>)

    /** Index of the week column under [x], for tap-to-inspect. */
    fun indexAt(x: Float, width: Float, n: Int): Int? {
        if (n == 0) return null
        val plotW = width - leftPad - rightPad
        val i = floor((x - leftPad) / (plotW / n)).toInt()
        return i.takeIf { it in 0 until n }
    }

    fun draw(canvas: Canvas, width: Float, height: Float, weeks: List<WeeklyRow>, labels: List<String>, selected: Int? = null) {
        val n = weeks.size
        if (n == 0) return
        val plotW = width - leftPad - rightPad
        val plotH = (height - 2 * titleH - gap - xLabelH - legendH) / 2f
        val step = plotW / n
        fun x(i: Int) = leftPad + step * (i + 0.5f)

        // ---- Panel A: practice quality --------------------------------------------------------
        val aTop = titleH
        canvas.drawText("Practice quality (alignment %, 0–100)", leftPad, aTop - 7 * dp, titleText)
        drawPanel(
            canvas, aTop, plotH, plotW, 0.0, 100.0, listOf(0.0, 50.0, 100.0), ::x,
            listOf(Series("Quality", c.quality, weeks.map { it.practiceQuality })), selected, decimals = 0
        )

        // ---- Panel B: stress & mood -----------------------------------------------------------
        val bTop = aTop + plotH + gap + titleH
        canvas.drawText("Stress & mood (self-rated, 1–10)", leftPad, bTop - 7 * dp, titleText)
        drawPanel(
            canvas, bTop, plotH, plotW, 0.0, 10.0, listOf(0.0, 5.0, 10.0), ::x,
            listOf(
                Series("Stress", c.stress, weeks.map { it.meanStress }),
                Series("Mood", c.mood, weeks.map { it.meanMood })
            ),
            selected, decimals = 1
        )

        // ---- Shared week axis -----------------------------------------------------------------
        val xBase = bTop + plotH + 15 * dp
        // Thin the labels (every 2nd, 3rd…) when they would crowd the column width.
        val widest = labels.maxOfOrNull { xText.measureText(it) } ?: 0f
        val every = max(1, kotlin.math.ceil((widest + 8 * dp) / step).toInt())
        labels.forEachIndexed { i, l -> if ((n - 1 - i) % every == 0) canvas.drawText(l, x(i), xBase, xText) }

        // Legend on its own row so it never collides with a panel title at narrow widths.
        drawLegend(canvas, leftPad, xBase + legendH - 2 * dp)

        selected?.let { i ->
            canvas.drawLine(x(i), aTop, x(i), aTop + plotH, selPaint)
            canvas.drawLine(x(i), bTop, x(i), bTop + plotH, selPaint)
        }
    }

    private fun drawLegend(canvas: Canvas, left: Float, baseline: Float) {
        val key = 14 * dp
        var cursor = left
        for ((name, color) in listOf("Quality" to c.quality, "Stress" to c.stress, "Mood" to c.mood)) {
            linePaint.color = color
            val y = baseline - 4 * dp
            canvas.drawLine(cursor, y, cursor + key, y, linePaint)
            cursor += key + 4 * dp
            canvas.drawText(name, cursor, baseline, titleText)
            cursor += titleText.measureText(name) + 14 * dp
        }
    }

    private fun drawPanel(
        canvas: Canvas,
        top: Float,
        h: Float,
        plotW: Float,
        min: Double,
        maxV: Double,
        ticks: List<Double>,
        x: (Int) -> Float,
        series: List<Series>,
        selected: Int?,
        decimals: Int
    ) {
        fun y(v: Double) = (top + h - (v - min) / (maxV - min) * h).toFloat()
        // Recessive gridlines + baseline
        ticks.forEach { t ->
            val yy = y(t)
            canvas.drawLine(leftPad, yy, leftPad + plotW, yy, if (t == min) axisPaint else gridPaint)
            canvas.drawText(t.toInt().toString(), leftPad - 6 * dp, yy + 4 * dp, tickText)
        }
        // Lines (broken at weeks with no data)
        for (s in series) {
            linePaint.color = s.color
            val path = Path()
            var penDown = false
            s.values.forEachIndexed { i, v ->
                if (v == null) { penDown = false; return@forEachIndexed }
                if (!penDown) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v))
                penDown = true
            }
            canvas.drawPath(path, linePaint)
        }
        // Markers with a 2dp surface ring
        for (s in series) {
            s.values.forEachIndexed { i, v ->
                if (v == null) return@forEachIndexed
                val r = if (i == selected) 5.5f * dp else 4f * dp
                fill.color = c.surface
                canvas.drawCircle(x(i), y(v), r + 2 * dp, fill)
                fill.color = s.color
                canvas.drawCircle(x(i), y(v), r, fill)
            }
        }
        // Direct end labels (skipped when they would collide; the legend + table carry identity then)
        val ends = series.mapNotNull { s ->
            val i = s.values.indexOfLast { it != null }
            if (i < 0) null else Triple(s, i, s.values[i]!!)
        }
        val collide = ends.size == 2 && abs(y(ends[0].third) - y(ends[1].third)) < 14 * dp
        if (!collide) {
            ends.forEach { (_, i, v) ->
                val txt = if (decimals == 0) v.toInt().toString() else String.format(java.util.Locale.US, "%.1f", v)
                canvas.drawText(txt, x(i) + 10 * dp, y(v) + 4 * dp, endText)
            }
        }
    }
}
