package com.companion.yogawellness.ui.codex

import com.companion.yogawellness.codex.AsanaCatalog32
import com.companion.yogawellness.codex.PoseAngles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 360° view of the Learn demos: angle helpers and full-turn framing. */
class RotationMathTest {

    @Test
    fun normalizeAndShortestTurn() {
        assertEquals(350f, normalizeDeg(-10f), 1e-4f)
        assertEquals(10f, normalizeDeg(730f), 1e-4f)
        assertEquals(20f, shortestTurn(350f, 10f), 1e-4f)   // forward across 0°
        assertEquals(-20f, shortestTurn(10f, 350f), 1e-4f)  // backward across 0°
        assertEquals(180f, kotlin.math.abs(shortestTurn(0f, 180f)), 1e-4f)
    }

    @Test
    fun presetsAreRecognisedOnlyWhenFacingThem() {
        assertEquals(0, presetIndexFor(0f))        // Front
        assertEquals(0, presetIndexFor(359f))
        assertEquals(1, presetIndexFor(45.5f))     // 45°
        assertEquals(2, presetIndexFor(450f))      // Side (90°, after a full turn)
        assertEquals(3, presetIndexFor(-180f))     // Back
        assertEquals(-1, presetIndexFor(120f))     // between shortcuts
    }

    @Test
    fun everyPoseStaysInsideTheFrameAtEveryAngleAndTilt() {
        for (item in AsanaCatalog32.items) {
            val frame = DemoFrame(item, fullTurn = true)
            assertEquals("${item.id} frame is centred on the turn axis", -frame.minX, frame.maxX, 1e-4f)
            for (blend in listOf(0f, 0.5f, 1f)) {
                val angles = PoseAngles.lerp(item.start, item.peak, blend)
                for (yaw in 0 until 360 step 15) for (tilt in listOf(0f, 30f, MAX_TILT_DEG)) {
                    frame.update(angles, yaw.toFloat(), atPeak = true, tiltDeg = tilt)
                    for (j in frame.x.indices) {
                        assertTrue("${item.id} joint $j x=${frame.x[j]} yaw=$yaw", frame.x[j] in frame.minX..frame.maxX)
                        assertTrue("${item.id} joint $j y=${frame.y[j]} yaw=$yaw tilt=$tilt", frame.y[j] <= frame.maxYFor(tilt) + 1e-3f)
                    }
                }
            }
        }
    }

    @Test
    fun noTiltMatchesTheOriginalSideOnRendering() {
        val item = AsanaCatalog32.byId("warrior_2")!!
        val frame = DemoFrame(item)
        frame.update(item.peak, 90f, atPeak = true)
        val plain = frame.y.copyOf()
        frame.update(item.peak, 90f, atPeak = true, tiltDeg = 0f)
        assertTrue(plain.contentEquals(frame.y))
        assertEquals(0f, frame.tiltSin, 0f)
        assertEquals(0f, frame.pelvisFloorY, 0f)
    }
}
