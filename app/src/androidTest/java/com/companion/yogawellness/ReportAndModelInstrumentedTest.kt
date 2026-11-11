package com.companion.yogawellness

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.companion.yogawellness.report.PdfReportExporter
import com.companion.yogawellness.wellness.DashboardBuilder
import com.companion.yogawellness.wellness.MockPersonaSeeder
import com.companion.yogawellness.wellness.Persona
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/**
 * On-device checks for the two things the JVM cannot run: the real PdfDocument backend and the
 * MediaPipe native pose model. Run with a phone connected:
 *   ./gradlew :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class ReportAndModelInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun weeklyReportIsAOnePagePdf() {
        val d = MockPersonaSeeder.generate(Persona.A_CONSISTENT, 20_000L)
        val data = DashboardBuilder.build(d.checkIns, d.sessions.map { it.session }, d.sessions.flatMap { it.poses }, d.questionnaires, 20_000L)
        val out = ByteArrayOutputStream()
        PdfReportExporter.writePdf(data, out, Persona.A_CONSISTENT.label)
        val bytes = out.toByteArray()
        assertEquals("%PDF", String(bytes, 0, 4))
        assertTrue(bytes.size > 2_000)
        assertEquals(1, Regex("/Type\\s*/Page[^s]").findAll(String(bytes, Charsets.ISO_8859_1)).count())
    }

    @Test
    fun bundledPoseModelLoadsAndRuns() {
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("pose_landmarker_full.task").build())
            .setRunningMode(RunningMode.IMAGE)
            .setNumPoses(1)
            .build()
        PoseLandmarker.createFromOptions(context, options).use { landmarker ->
            val blank = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }
            val result = landmarker.detect(BitmapImageBuilder(blank).build())
            assertTrue("no person expected in a blank frame", result.landmarks().isEmpty())
        }
    }
}
