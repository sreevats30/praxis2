package com.companion.yogawellness.pose

import com.companion.yogawellness.replay.SyntheticPoseBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreflightGuardTest {

    private val okSensors = SensorSnapshot(GravityAligner.checkPhoneTilt(0f, 9.81f, 0f), 0.02)

    @Test
    fun wellFramedTadasanaIsAllGreen() {
        val r = PreflightGuard.evaluate(SyntheticPoseBuilder().tadasana(0), okSensors)
        assertTrue(r.checks.toString(), r.allGreen)
        assertNull(r.primaryMessage)
    }

    @Test
    fun noPersonFailsBodyAndDistance() {
        val r = PreflightGuard.evaluate(PoseFrame.empty(0), okSensors)
        assertFalse(r.check(PreflightCheckId.FULL_BODY).ok)
        assertFalse(r.check(PreflightCheckId.DISTANCE).ok)
    }

    @Test
    fun tooCloseAndTooFar() {
        val close = SyntheticPoseBuilder(torso = 260.0, hipY = 380.0).tadasana(0)
        val rc = PreflightGuard.evaluate(close, okSensors)
        assertFalse(rc.check(PreflightCheckId.DISTANCE).ok)

        val far = SyntheticPoseBuilder(torso = 90.0, hipY = 330.0).tadasana(0)
        val rf = PreflightGuard.evaluate(far, okSensors)
        assertEquals("Come a little closer to the phone", rf.check(PreflightCheckId.DISTANCE).message)
    }

    @Test
    fun feetOutOfFrame() {
        val frame = SyntheticPoseBuilder(hipY = 470.0).tadasana(0)
        val r = PreflightGuard.evaluate(frame, okSensors)
        assertFalse(r.check(PreflightCheckId.FULL_BODY).ok)
        assertEquals("Make sure your feet are in the frame", r.check(PreflightCheckId.FULL_BODY).message)
    }

    @Test
    fun tiltedOrShakyPhoneFails() {
        val frame = SyntheticPoseBuilder().tadasana(0)
        val tilted = SensorSnapshot(GravityAligner.checkPhoneTilt(0f, 8.0f, 5.6f), 0.02)
        assertFalse(PreflightGuard.evaluate(frame, tilted).check(PreflightCheckId.PHONE).ok)
        val shaky = SensorSnapshot(GravityAligner.checkPhoneTilt(0f, 9.81f, 0f), 0.6)
        assertFalse(PreflightGuard.evaluate(frame, shaky).check(PreflightCheckId.PHONE).ok)
        // Replay (no sensors) never blocks
        assertTrue(PreflightGuard.evaluate(frame, null).check(PreflightCheckId.PHONE).ok)
    }

    @Test
    fun darkRoomFails() {
        val frame = SyntheticPoseBuilder().front(0, luminance = 30.0)
        assertFalse(PreflightGuard.evaluate(frame, okSensors).check(PreflightCheckId.LIGHTING).ok)
    }

    @Test
    fun luminanceMeterReadsCentreCrop() {
        val w = 40
        val h = 40
        val px = IntArray(w * h) { 0xFF000000.toInt() } // black border
        for (y in 10 until 30) for (x in 10 until 30) px[y * w + x] = 0xFFFFFFFF.toInt() // white centre
        assertEquals(255.0, LuminanceMeter.meanLuminance(px, w, h, stride = 1), 1e-6)
    }

    @Test
    fun motionTrackerSd() {
        val t = MotionSteadinessTracker()
        for (i in 0 until 50) t.add(i * 20_000_000L, 0f, 9.81f, 0f)
        assertEquals(0.0, t.sd()!!, 1e-6)
        val shaky = MotionSteadinessTracker()
        for (i in 0 until 50) shaky.add(i * 20_000_000L, 0f, if (i % 2 == 0) 9.0f else 10.6f, 0f)
        assertTrue(shaky.sd()!! > 0.3)
    }
}
