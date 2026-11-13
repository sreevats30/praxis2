package com.companion.yogawellness.scoring

import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.pose.BodySide
import com.companion.yogawellness.pose.JointAngleExtractor
import com.companion.yogawellness.replay.SyntheticPoseBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionTrackerTest {

    private val warrior = AsanaLabel(Asana.VIRABHADRASANA_II, PoseSide.FRONT_LEFT)
    private val specs = AsanaSpecs.VIRABHADRASANA_II
    private val b = SyntheticPoseBuilder()

    private fun alignment(kneeDeg: Double): AlignmentScoreResult {
        val ex = JointAngleExtractor.extract(b.warriorII(0, BodySide.LEFT, kneeDeg), specs, BodySide.LEFT, 0.25)
        return AlignmentScorer.evaluate(ex.angles, ex.visibilityOk, specs)
    }

    @Test
    fun qualitySecCountsOnlyFramesAtOrAboveSeventyFive() {
        val tracker = SessionTracker()
        var t = 0.0
        repeat(51) { tracker.update(t, warrior, 90.0, 80.0, HoldStatus.IDLE); t += 0.04 } // 2.0 s good
        repeat(50) { tracker.update(t, warrior, 60.0, 80.0, HoldStatus.IDLE); t += 0.04 } // 2.0 s below
        val s = tracker.segments().single()
        assertEquals(4.0, s.lockedSec, 0.05)
        assertEquals(2.0, s.qualitySec, 0.05)
        assertEquals(90.0, s.peakScore!!, 0.0)
    }

    @Test
    fun peakScoreIsTheMaximum() {
        val tracker = SessionTracker()
        listOf(41.0, 77.5, 63.0).forEachIndexed { i, sc -> tracker.update(i * 0.04, warrior, sc, null, HoldStatus.IDLE) }
        assertEquals(77.5, tracker.segments().single().peakScore!!, 0.0)
    }

    @Test
    fun mostFrequentWorstJointAndCue() {
        val tracker = SessionTracker()
        val shallow = alignment(125.0) // front knee is the problem
        var t = 0.0
        repeat(60) { tracker.update(t, warrior, shallow.finalScore, null, HoldStatus.IDLE, shallow); t += 0.04 }
        // A few frames where something else is worst do not change the argmax
        val armsOff = shallow.copy(
            evaluations = shallow.evaluations.map { if (it.spec.name == "Back Arm Level") it.copy(jointScore = 1.0) else it.copy(jointScore = 99.0) },
            primaryCorrectiveCue = "Raise your back arm to shoulder height"
        )
        repeat(10) { tracker.update(t, warrior, 50.0, null, HoldStatus.IDLE, armsOff); t += 0.04 }
        val s = tracker.segments().single()
        assertEquals("Front Knee", s.worstJoint)
        assertEquals("Bend your front knee deeper toward 90 degrees", s.topCue)
    }

    @Test
    fun perfectPoseHasNoWorstJointOrCue() {
        val tracker = SessionTracker()
        val perfect = alignment(95.0)
        repeat(30) { tracker.update(it * 0.04, warrior, perfect.finalScore, null, HoldStatus.IDLE, perfect) }
        val s = tracker.segments().single()
        assertNull(s.worstJoint)
        assertNull(s.topCue)
    }
}
