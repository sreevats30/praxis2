package com.companion.yogawellness.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class SwayAndHoldTest {

    @Test
    fun steadyHipsScoreFullStability() {
        val s = SwayStabilityTracker()
        var r: SwayResult? = null
        for (i in 0 until 100) r = s.update(i / 25.0, 240.0, 350.0, 125.0)
        assertEquals(100.0, r!!.stability, 1e-6)
    }

    @Test
    fun linearDriftIsDetrendedAway() {
        val s = SwayStabilityTracker()
        var r: SwayResult? = null
        for (i in 0 until 100) r = s.update(i / 25.0, 240.0 + i * 0.5, 350.0, 125.0)
        assertTrue("stability=${r!!.stability}", r.stability > 95.0)
    }

    @Test
    fun wobbleLowersStability() {
        fun run(amplitudePx: Double): Double {
            val s = SwayStabilityTracker()
            var r: SwayResult? = null
            for (i in 0 until 100) {
                val t = i / 25.0
                r = s.update(t, 240.0 + amplitudePx * sin(2 * PI * 0.5 * t), 350.0, 125.0)
            }
            return r!!.stability
        }
        val small = run(1.0)
        val large = run(4.0)
        val huge = run(10.0)
        assertTrue("small=$small large=$large", small > large)
        assertTrue("huge=$huge", huge < 10.0)
    }

    @Test
    fun needsEnoughSamples() {
        val s = SwayStabilityTracker()
        assertNull(s.update(0.0, 240.0, 350.0, 125.0))
    }

    @Test
    fun strictResetsButGraceSurvivesShortDip() {
        val h = HoldStateMachine()
        var st = HoldStatus.IDLE
        var t = 0.0
        repeat(76) { st = h.update(t, 90.0); t += 0.04 }        // ~3.0 s good
        assertEquals(3.0, st.strictHoldSec, 0.05)
        repeat(10) { st = h.update(t, 60.0); t += 0.04 }        // 0.4 s dip
        assertEquals(HoldPhase.GRACE, st.phase)
        assertEquals(0.0, st.strictHoldSec, 0.0)
        repeat(26) { st = h.update(t, 90.0); t += 0.04 }        // 1.0 s good again
        assertEquals(1.0, st.strictHoldSec, 0.05)
        assertEquals(4.0, st.graceHoldSec, 0.1)
        assertEquals(3.0, st.bestStrictSec, 0.05)
    }

    @Test
    fun longDipResetsGrace() {
        val h = HoldStateMachine(graceSec = 1.5)
        var st = HoldStatus.IDLE
        var t = 0.0
        repeat(50) { st = h.update(t, 90.0); t += 0.04 }
        repeat(50) { st = h.update(t, 50.0); t += 0.04 }        // 2 s dip
        assertEquals(HoldPhase.ALIGNING, st.phase)
        assertEquals(0.0, st.graceHoldSec, 0.0)
        assertTrue(st.bestGraceSec > 1.9)
    }

    @Test
    fun praiseFiresOnceAfterFourSecondsAboveEightyEight() {
        val h = HoldStateMachine()
        var t = 0.0
        var praises = 0
        repeat(200) {
            if (h.update(t, 92.0).praiseTriggered) praises++
            t += 0.04
        }
        assertEquals(1, praises)
    }

    @Test
    fun praiseNotAt80Percent() {
        val h = HoldStateMachine()
        var t = 0.0
        var praised = false
        repeat(200) { praised = praised || h.update(t, 80.0).praiseTriggered; t += 0.04 }
        assertFalse(praised)
    }

    @Test
    fun enduranceCapCompletesUtkatasana() {
        val h = HoldStateMachine(maxHoldSec = Asana.UTKATASANA.maxHoldSec)
        var t = 0.0
        var caps = 0
        var st = HoldStatus.IDLE
        repeat(25 * 35) {
            st = h.update(t, 90.0)
            if (st.capReached) caps++
            t += 0.04
        }
        assertEquals(1, caps)
        assertEquals(HoldPhase.COMPLETE, st.phase)
        assertEquals(30.0, st.bestGraceSec, 0.1)
    }

    @Test
    fun intraPoseSymmetry() {
        val angles = mapOf("Left Knee Flexion" to 110.0, "Right Knee Flexion" to 125.0)
        val vis = mapOf("Left Knee Flexion" to true, "Right Knee Flexion" to true)
        assertEquals(50.0, SymmetryCalculator.intraPose(Asana.UTKATASANA, angles, vis)!!, 1e-9)
        assertNull(SymmetryCalculator.intraPose(Asana.VRIKSHASANA, angles, vis))
        assertEquals(75.0, SymmetryCalculator.holdSymmetry(30.0, 40.0)!!, 1e-9)
    }
}
