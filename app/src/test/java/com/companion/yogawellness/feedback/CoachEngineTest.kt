package com.companion.yogawellness.feedback

import com.companion.yogawellness.pose.BodySide
import com.companion.yogawellness.pose.GravityAligner
import com.companion.yogawellness.pose.PipelineOutput
import com.companion.yogawellness.pose.PosePipeline
import com.companion.yogawellness.pose.PoseFrame
import com.companion.yogawellness.pose.SensorSnapshot
import com.companion.yogawellness.replay.SyntheticPoseBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachEngineTest {

    private val b = SyntheticPoseBuilder()
    private val sensors = SensorSnapshot(GravityAligner.checkPhoneTilt(0f, 9.81f, 0f), 0.02)

    private class Run {
        val pipeline = PosePipeline()
        val coach = CoachEngine()
        var t = 0L
        val said = mutableListOf<Pair<Double, CoachUtterance>>()
        fun feed(seconds: Double, make: (Long) -> PoseFrame, sensors: SensorSnapshot?): PipelineOutput? {
            var out: PipelineOutput? = null
            repeat((seconds * 25).toInt()) {
                t += 40
                out = pipeline.process(make(t), sensors)
                coach.update(out!!)?.let { said += t / 1000.0 to it }
            }
            return out
        }
    }

    @Test
    fun wrongWarriorCueIsSpokenAtMostTwiceWithCooldown() {
        val r = Run()
        r.feed(6.0, { b.tadasana(it) }, sensors)
        r.feed(30.0, { b.warriorII(it, BodySide.LEFT, 125.0) }, sensors)

        val cue = "Bend your front knee deeper toward 90 degrees"
        val cues = r.said.filter { it.second.kind == CoachUtterance.Kind.CORRECTION }
        // Spoken exactly twice in 30 s: first merged with the pose announcement, then once more
        assertEquals(2, cues.size)
        assertEquals("${CoachEngine.POSE_ANNOUNCE_PREFIX}Warrior two pose, left leg forward. $cue.", cues[0].second.text)
        assertEquals(cue, cues[1].second.text)
        // Every utterance is >= 4.5 s after the previous one (none of these are safety)
        r.said.zipWithNext().forEach { (a, c) -> assertTrue("${a.first} -> ${c.first}", c.first - a.first >= 4.5 - 1e-9) }
    }

    @Test
    fun correctionNeedsToPersistFor1point2Seconds() {
        val r = Run()
        r.feed(6.0, { b.tadasana(it) }, sensors)
        r.feed(6.0, { b.warriorII(it, BodySide.LEFT, 95.0) }, sensors)
        val before = r.said.size
        // A 1 s slip is not long enough to trigger a cue...
        r.feed(1.0, { b.warriorII(it, BodySide.LEFT, 125.0) }, sensors)
        r.feed(3.0, { b.warriorII(it, BodySide.LEFT, 95.0) }, sensors)
        assertTrue(r.said.drop(before).none { it.second.kind == CoachUtterance.Kind.CORRECTION })
    }

    @Test
    fun praiseWithChimeAfterGoodHold() {
        val r = Run()
        r.feed(6.0, { b.tadasana(it) }, sensors)
        r.feed(15.0, { b.warriorII(it, BodySide.LEFT, 95.0) }, sensors)
        val praise = r.said.filter { it.second.kind == CoachUtterance.Kind.PRAISE }
        assertEquals(1, praise.size)
        assertTrue(praise.single().second.chime)
    }

    @Test
    fun safetyComesFirstAndInterrupts() {
        val r = Run()
        r.feed(6.0, { b.tadasana(it) }, sensors)
        val unsafe = { t: Long ->
            b.front(
                t, left = SyntheticPoseBuilder.Leg(40.0, 64.3), right = SyntheticPoseBuilder.Leg(2.0, 178.0),
                leftArm = SyntheticPoseBuilder.Arm(170.0, 168.0), rightArm = SyntheticPoseBuilder.Arm(170.0, 168.0)
            )
        }
        r.feed(14.0, unsafe, sensors)
        val safety = r.said.filter { it.second.kind == CoachUtterance.Kind.SAFETY }
        assertTrue("said=${r.said.map { it.second.text }}", safety.size >= 2)
        assertTrue(safety.all { it.second.text == SafetyGuards.TREE_KNEE_MESSAGE && it.second.interrupt })
        // Safety repeats every ~6 s
        assertTrue(safety[1].first - safety[0].first >= 6.0 - 1e-9)
    }

    @Test
    fun painReminderIsPeriodic() {
        val r = Run()
        r.feed(6.0, { b.tadasana(it) }, sensors)
        r.feed(250.0, { b.warriorII(it, BodySide.LEFT, 95.0) }, sensors)
        val reminders = r.said.filter { it.second.kind == CoachUtterance.Kind.REMINDER }
        assertEquals(2, reminders.size)
    }

    @Test
    fun utkatasanaEnduranceCapIsAnnounced() {
        val r = Run()
        r.feed(35.0, { b.chair(it) }, null)
        assertTrue(r.said.any { it.second.text.startsWith("That's 30 seconds") })
    }
}
