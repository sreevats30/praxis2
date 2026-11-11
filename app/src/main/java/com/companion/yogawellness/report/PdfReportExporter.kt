package com.companion.yogawellness.report

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.companion.yogawellness.ui.consent.NON_DIAGNOSTIC_STATEMENT
import com.companion.yogawellness.ui.dashboard.ChartColors
import com.companion.yogawellness.ui.dashboard.TrendChartRenderer
import com.companion.yogawellness.wellness.CrisisLines
import com.companion.yogawellness.wellness.DashboardData
import com.companion.yogawellness.wellness.QuestionnaireType
import java.io.File
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 1-page Weekly Progress Report using the built-in android.graphics.pdf.PdfDocument (§2, §10 Prompt 5),
 * shared via FileProvider. A4 portrait in PostScript points (595 × 842).
 */
object PdfReportExporter {
    const val PAGE_W = 595
    const val PAGE_H = 842

    fun writePdf(data: DashboardData, out: OutputStream, personaLabel: String? = null) {
        val doc = PdfDocument()
        try {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
            ReportPageRenderer.draw(page.canvas, PAGE_W.toFloat(), PAGE_H.toFloat(), data, personaLabel)
            doc.finishPage(page)
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }

    fun export(dir: File, data: DashboardData, personaLabel: String?): File {
        dir.mkdirs()
        val file = File(dir, "weekly_report_${LocalDate.ofEpochDay(data.toEpochDay)}.pdf")
        file.outputStream().use { writePdf(data, it, personaLabel) }
        return file
    }

    fun shareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Weekly yoga & wellness report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Share weekly report")
    }
}

/** Lays out the report page on any Canvas (PDF page, or a Bitmap for previews/tests). */
object ReportPageRenderer {
    private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val SHORT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val C = ChartColors.Light
    private const val MARGIN = 40f

    private fun f0(v: Double) = String.format(Locale.US, "%.0f", v)
    private fun f1(v: Double) = String.format(Locale.US, "%.1f", v)

    fun draw(canvas: Canvas, w: Float, h: Float, data: DashboardData, personaLabel: String?) {
        val contentW = w - 2 * MARGIN
        canvas.drawColor(0xFFFFFFFF.toInt())
        var y = MARGIN

        val title = text(20f, C.inkPrimary, bold = true)
        canvas.drawText("Weekly Progress Report", MARGIN, y + 18f, title)
        y += 26f
        val weekFrom = LocalDate.ofEpochDay(data.toEpochDay - 6)
        val to = LocalDate.ofEpochDay(data.toEpochDay)
        canvas.drawText(
            "Week ${weekFrom.format(SHORT)} – ${to.format(DATE)} · trend covers ${data.weeks.size} weeks · created on this device",
            MARGIN, y + 10f, text(9.5f, C.inkSecondary)
        )
        y += 16f
        if (personaLabel != null) {
            canvas.drawText("Demo data: $personaLabel", MARGIN, y + 10f, text(9f, C.muted))
            y += 14f
        }
        y += 4f
        y += paragraph(canvas, NON_DIAGNOSTIC_STATEMENT, MARGIN, y, contentW, 9f, C.inkSecondary, italic = true) + 10f

        // ---- Stat tiles -------------------------------------------------------------------------
        val hl = data.headline
        val quality = hl.meanQuality?.let { q ->
            "${f0(q)}%" + (hl.previousWeekQuality?.let { p -> " (${if (q >= p) "+" else ""}${f0(q - p)})" } ?: "")
        } ?: "–"
        val tiles = listOf(
            "Practice this week" to "${f0(hl.practiceMinutes)} min",
            "Sessions" to hl.sessions.toString(),
            "Avg alignment" to quality,
            "Check-ins" to "${hl.checkInDays} / 7"
        )
        val tileW = (contentW - 3 * 8f) / 4
        val tileBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF4F1E8.toInt() }
        tiles.forEachIndexed { i, (label, value) ->
            val x = MARGIN + i * (tileW + 8f)
            canvas.drawRoundRect(RectF(x, y, x + tileW, y + 46f), 6f, 6f, tileBg)
            canvas.drawText(label, x + 8f, y + 15f, text(8.5f, C.inkSecondary))
            canvas.drawText(value, x + 8f, y + 36f, text(16f, C.inkPrimary, bold = true))
        }
        y += 58f

        // ---- Trend chart ------------------------------------------------------------------------
        canvas.drawText("Weekly trend", MARGIN, y + 11f, text(12f, C.inkPrimary, bold = true))
        y += 16f
        val chartH = 220f
        canvas.save()
        canvas.translate(MARGIN, y)
        TrendChartRenderer(C, dp = 0.8f, sp = 0.8f).draw(
            canvas, contentW, chartH, data.weeks,
            data.weeks.map { LocalDate.ofEpochDay(it.weekStartEpochDay).format(SHORT) }
        )
        canvas.restore()
        y += chartH + 8f

        // ---- Left vs right ----------------------------------------------------------------------
        canvas.drawText("Left vs right (mean best hold)", MARGIN, y + 11f, text(12f, C.inkPrimary, bold = true))
        y += 18f
        if (data.symmetry.isEmpty()) {
            canvas.drawText("Practise both sides of Tree, Warrior II or Triangle to compare.", MARGIN, y + 9f, text(9.5f, C.inkSecondary))
            y += 14f
        }
        data.symmetry.forEach { s ->
            val line = "${s.asana.sanskritName} (${s.asana.englishName}): left ${f1(s.leftHoldSec)} s · right ${f1(s.rightHoldSec)} s" +
                (s.holdSymmetryPct?.let { " · ${f0(it)}% symmetric" } ?: "")
            canvas.drawText(line, MARGIN, y + 9f, text(9.5f, C.inkPrimary))
            y += 13f
        }
        y += 6f

        // ---- Insights ---------------------------------------------------------------------------
        canvas.drawText("Patterns in your logs", MARGIN, y + 11f, text(12f, C.inkPrimary, bold = true))
        y += 16f
        data.insights.forEach { card ->
            y += paragraph(canvas, "${card.title}: ${card.text}", MARGIN, y, contentW, 8.5f, C.inkPrimary) + 6f
        }

        // ---- Reflection scores & tension --------------------------------------------------------
        canvas.drawText("Self-reflection check-ins", MARGIN, y + 11f, text(12f, C.inkPrimary, bold = true))
        y += 17f
        fun scores(type: QuestionnaireType, list: List<com.companion.yogawellness.data.db.QuestionnaireEntity>) =
            if (list.isEmpty()) "${type.shortName}: not completed yet"
            else "${type.shortName}: " + list.takeLast(4).joinToString(" → ") {
                "${it.totalScore}/${type.maxScore} (${LocalDate.ofEpochDay(it.epochDay).format(SHORT)})"
            }
        canvas.drawText(scores(QuestionnaireType.PHQ9, data.phq), MARGIN, y + 9f, text(9.5f, C.inkPrimary)); y += 13f
        canvas.drawText(scores(QuestionnaireType.GAD7, data.gad), MARGIN, y + 9f, text(9.5f, C.inkPrimary)); y += 13f
        data.tension?.let {
            canvas.drawText(
                "Self-rated body tension: ${f1(it.meanBefore)} before → ${f1(it.meanAfter)} after practice (average of ${it.days} days)",
                MARGIN, y + 9f, text(9.5f, C.inkPrimary)
            )
            y += 13f
        }
        canvas.drawText("Scores are for noticing change over time, not a diagnosis.", MARGIN, y + 9f, text(8.5f, C.inkSecondary))

        // ---- Footer -----------------------------------------------------------------------------
        val footer = "Private by design: this report was generated on your phone; nothing leaves it unless you share this file. " +
            "Need to talk to someone? Tele-MANAS ${CrisisLines.TELE_MANAS_SHORT} / ${CrisisLines.TELE_MANAS_TOLL_FREE} · Emergency ${CrisisLines.EMERGENCY}."
        val fh = measure(footer, contentW, 8f)
        canvas.drawLine(MARGIN, h - MARGIN - fh - 6f, w - MARGIN, h - MARGIN - fh - 6f, Paint().apply { color = C.grid })
        paragraph(canvas, footer, MARGIN, h - MARGIN - fh, contentW, 8f, C.muted)
    }

    private fun text(size: Float, color: Int, bold: Boolean = false, italic: Boolean = false) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(
                Typeface.SANS_SERIF,
                when {
                    bold && italic -> Typeface.BOLD_ITALIC
                    bold -> Typeface.BOLD
                    italic -> Typeface.ITALIC
                    else -> Typeface.NORMAL
                }
            )
        }

    private fun layout(s: String, width: Float, size: Float, color: Int = C.inkPrimary, italic: Boolean = false): StaticLayout =
        StaticLayout.Builder.obtain(s, 0, s.length, text(size, color, italic = italic), width.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1.5f, 1f)
            .build()

    private fun measure(s: String, width: Float, size: Float): Float = layout(s, width, size).height.toFloat()

    /** Draws wrapped text and returns its height. */
    private fun paragraph(canvas: Canvas, s: String, x: Float, y: Float, width: Float, size: Float, color: Int, italic: Boolean = false): Float {
        val l = layout(s, width, size, color, italic)
        canvas.save()
        canvas.translate(x, y)
        l.draw(canvas)
        canvas.restore()
        return l.height.toFloat()
    }
}
