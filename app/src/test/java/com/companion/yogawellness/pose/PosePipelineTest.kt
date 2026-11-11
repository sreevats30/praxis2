package com.companion.yogawellness.pose

import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.replay.SyntheticPoseBuilder
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.HoldPhase
import com.companion.yogawellness.scoring.PoseSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PosePipelineTest {

    private val b = SyntheticPoseBuilder()
    private val sensors = SensorSnapshot(GravityAligner.checkPhoneTilt(0f, 9.81f, 0f), 0.02)

    private class Clock { var ms = 0L; fun next(): Long { ms += 40; return ms } } // 25 FPS

    @Test
    fun fullSessionTadasanaCalibrateThenWarriorCorrection() {
        val p = PosePipeline()
        val clock = Clock()
        val events = mutableListOf<PipelineEvent>()
        var out: PipelineOutput? = null

        // 6 s of Tadasana: arms after 10 frames, locks after 14 more, calibrates after 5 s
        repeat(150) { out = p.process(b.tadasana(clock.next()), sensors); events += out!!.events }
        assertTrue(out!!.scoringArmed)
        assertEquals(Asana.TADASANA, out!!.lockedLabel?.asana)
        assertTrue(out!!.calibration.isCalibrated)
        assertEquals(125.0, out!!.calibration.calibratedTorsoPx!!, 1e-3)
        assertTrue(events.any { it is PipelineEvent.ScoringArmed })
        assertTrue(events.any { it is PipelineEvent.CalibrationComplete })

        // Wrong Warrior II (125°): Tadasana unlocks after 25 frames, Warrior locks, score ~41 (red)
        repeat(75) { out = p.process(b.warriorII(clock.next(), BodySide.LEFT, 125.0), sensors); events += out!!.events }
        assertEquals(AsanaLabel(Asana.VIRABHADRASANA_II, PoseSide.FRONT_LEFT), out!!.lockedLabel)
        assertEquals(41.4, out!!.score!!, 0.5)
        assertEquals("Bend your front knee deeper toward 90 degrees", out!!.alignment!!.primaryCorrectiveCue)
        assertEquals(HoldPhase.ALIGNING, out!!.hold.phase)

        // Corrected (95°) and held for 6 s: hold timer runs, praise fires once
        repeat(150) { out = p.process(b.warriorII(clock.next(), BodySide.LEFT, 95.0), sensors); events += out!!.events }
        assertEquals(100.0, out!!.score!!, 0.5)
        assertEquals(HoldPhase.HOLDING, out!!.hold.phase)
        assertTrue("grace hold=${out!!.hold.graceHoldSec}", out!!.hold.graceHoldSec > 5.0)
        assertEquals(1, events.count { it is PipelineEvent.Praise })
        assertNotNull(out!!.sway)
        assertEquals(100.0, out!!.sway!!.stability, 1e-3)

        // Other side, then compare left vs right
        repeat(40) { out = p.process(b.warriorII(clock.next(), BodySide.RIGHT, 95.0), sensors) }
        repeat(100) { out = p.process(b.warriorII(clock.next(), BodySide.RIGHT, 95.0), sensors) }
        assertEquals(PoseSide.FRONT_RIGHT, out!!.lockedLabel?.side)
        val cmp = p.session.sideComparisons().single()
        assertEquals(Asana.VIRABHADRASANA_II, cmp.asana)
        assertNotNull(cmp.holdSymmetryPct)
    }

    @Test
    fun scoringWaitsForGreenPreflight() {
        val p = PosePipeline()
        val tilted = SensorSnapshot(GravityAligner.checkPhoneTilt(0f, 7.0f, 6.9f), 0.02)
        var out: PipelineOutput? = null
        repeat(60) { out = p.process(b.warriorII(it * 40L), tilted) }
        assertFalse(out!!.scoringArmed)
        assertNull(out!!.lockedLabel)
        assertNull(out!!.score)
        assertFalse(out!!.preflight.allGreen)
    }

    @Test
    fun replayWithoutSensorsStillArms() {
        val p = PosePipeline()
        var out: PipelineOutput? = null
        repeat(40) { out = p.process(b.chair(it * 40L), null) }
        assertTrue(out!!.scoringArmed)
        assertEquals(Asana.UTKATASANA, out!!.lockedLabel?.asana)
        assertEquals(100.0, out!!.score!!, 0.5)
        assertEquals(100.0, out!!.intraSymmetry!!, 1e-3)
    }

    @Test
    fun safetyViolationPausesHoldTimer() {
        val p = PosePipeline()
        var out: PipelineOutput? = null
        repeat(150) {
            out = p.process(
                b.front(
                    it * 40L, left = SyntheticPoseBuilder.Leg(40.0, 64.3), right = SyntheticPoseBuilder.Leg(2.0, 178.0),
                    leftArm = SyntheticPoseBuilder.Arm(170.0, 168.0), rightArm = SyntheticPoseBuilder.Arm(170.0, 168.0)
                ),
                sensors
            )
        }
        assertEquals("tree_knee", out!!.safety?.id)
        assertTrue(out!!.score!! >= 75.0)           // alignment itself looks fine…
        assertEquals(0.0, out!!.hold.bestGraceSec, 0.0) // …but no hold time is credited
        assertEquals(HoldPhase.ALIGNING, out!!.hold.phase)
    }

    @Test
    fun personLeavingFrameUnlocks() {
        val p = PosePipeline()
        var out: PipelineOutput? = null
        repeat(40) { out = p.process(b.tree(it * 40L, BodySide.RIGHT), sensors) }
        assertEquals(PoseSide.RIGHT_LIFTED, out!!.lockedLabel?.side)
        repeat(30) { out = p.process(PoseFrame.empty(1600L + it * 40L), sensors) }
        assertNull(out!!.lockedLabel)
        assertTrue(out!!.scoringArmed) // framing loss alone does not disarm
    }
}
