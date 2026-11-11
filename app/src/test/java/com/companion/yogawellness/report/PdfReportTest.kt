package com.companion.yogawellness.report

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.wellness.DashboardBuilderTest
import com.companion.yogawellness.wellness.Persona
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PdfReportTest {

    private val data = DashboardBuilderTest().personaDashboard(Persona.A_CONSISTENT)

    @Test
    fun reportPageRendersToBitmapForReview() {
        // Same layout code as the PDF, drawn at 2x onto a bitmap so it can be inspected.
        val scale = 2f
        val bmp = Bitmap.createBitmap((PdfReportExporter.PAGE_W * scale).toInt(), (PdfReportExporter.PAGE_H * scale).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.scale(scale, scale)
        ReportPageRenderer.draw(canvas, PdfReportExporter.PAGE_W.toFloat(), PdfReportExporter.PAGE_H.toFloat(), data, Persona.A_CONSISTENT.label)
        File("build/screenshots").mkdirs()
        File("build/screenshots/weekly_report_page.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Robolectric has no native PdfDocument backend (the native handle is 0). */
    private fun pdfBackendAvailable(): Boolean = runCatching {
        val doc = android.graphics.pdf.PdfDocument()
        try {
            doc.finishPage(doc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(10, 10, 1).create()))
        } finally {
            doc.close()
        }
    }.isSuccess

    /** Skipped on the JVM; the same check runs for real in androidTest/ReportAndModelInstrumentedTest. */
    @Test
    fun writesAOnePagePdf() {
        org.junit.Assume.assumeTrue("PdfDocument has no JVM backend — verified by the on-device test", pdfBackendAvailable())
        val out = ByteArrayOutputStream()
        PdfReportExporter.writePdf(data, out, Persona.A_CONSISTENT.label)
        val bytes = out.toByteArray()
        assertTrue("PDF too small: ${bytes.size}", bytes.size > 1000)
        assertEquals("%PDF", String(bytes, 0, 4))
        File("build/screenshots").mkdirs()
        File("build/screenshots/weekly_report.pdf").writeBytes(bytes)
    }

    @Test
    fun exportAndShareThroughFileProvider() {
        val app: YogaWellnessApp = ApplicationProvider.getApplicationContext()
        // Stand-in file: this test checks the FileProvider wiring, not PDF generation.
        val file = File(app.reportsDir.apply { mkdirs() }, "weekly_report_test.pdf").apply { writeText("%PDF-stub") }
        val chooser = PdfReportExporter.shareIntent(app, file)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals("application/pdf", send.type)
        @Suppress("DEPRECATION")
        val uri = send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("content", uri.scheme)
        assertEquals("${app.packageName}.fileprovider", uri.authority)
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
