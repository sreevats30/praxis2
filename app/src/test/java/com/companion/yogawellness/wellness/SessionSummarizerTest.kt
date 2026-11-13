package com.companion.yogawellness.wellness

import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.PoseSegmentStats
import com.companion.yogawellness.scoring.PoseSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SessionSummarizerTest {

    private val warrior = AsanaLabel(Asana.VIRABHADRASANA_II, PoseSide.FRONT_LEFT)
    private val tree = AsanaLabel(Asana.VRIKSHASANA, PoseSide.LEFT_LIFTED)

    private fun seg(label: AsanaLabel, locked: Double, score: Double?) = PoseSegmentStats(
        label, locked, (locked * 25).toInt(), score, 1.0, 1.5, 88.0,
        qualitySec = 0.4, peakScore = score?.plus(5), worstJoint = "Front Knee", topCue = "Bend your front knee deeper toward 90 degrees"
    )

    @Test
    fun twoSecondsAtFiftyTwoPercentIsSaved() {
        val (session, poses) = SessionSummarizer.toEntities(listOf(seg(warrior, 2.0, 52.0)), 0, 10_000, 1, "STANDARD")!!
        assertEquals(2.0, session.activeSec, 0.0)
        assertEquals(52.0, session.meanScore!!, 0.0)
        assertEquals(1, poses.size)
    }

    @Test
    fun underTwoSecondsIsNotSaved() {
        assertNull(SessionSummarizer.toEntities(listOf(seg(warrior, 1.9, 90.0)), 0, 10_000, 1, "STANDARD"))
    }

    @Test
    fun onlyQualifyingSegmentsAreKept() {
        val (session, poses) = SessionSummarizer.toEntities(
            listOf(seg(tree, 1.0, 95.0), seg(warrior, 3.0, 60.0)), 0, 10_000, 1, "STANDARD"
        )!!
        assertEquals(listOf("VIRABHADRASANA_II"), poses.map { it.asana })
        assertEquals(3.0, session.activeSec, 0.0)
        assertEquals(60.0, session.meanScore!!, 0.0) // the 1 s tree at 95% does not count
    }

    @Test
    fun allUnderTwoSecondsIsNull() {
        assertNull(SessionSummarizer.toEntities(listOf(seg(tree, 1.5, 80.0), seg(warrior, 0.5, 40.0)), 0, 1, 1, "STANDARD"))
    }

    @Test
    fun newPoseFieldsArePopulated() {
        val started = mapOf(warrior to 4_242L)
        val (_, poses) = SessionSummarizer.toEntities(listOf(seg(warrior, 4.0, 41.0)), 1_000, 9_000, 1, "STANDARD", started)!!
        val p = poses.single()
        assertEquals(0.4, p.qualitySec, 0.0)
        assertEquals(46.0, p.peakScore!!, 0.0)
        assertEquals("Front Knee", p.worstJoint)
        assertEquals("Bend your front knee deeper toward 90 degrees", p.topCue)
        assertEquals(4_242L, p.startedAtMs)
        // A pose with no recorded start falls back to the session start
        val (_, fallback) = SessionSummarizer.toEntities(listOf(seg(warrior, 4.0, 41.0)), 1_000, 9_000, 1, "STANDARD")!!
        assertEquals(1_000L, fallback.single().startedAtMs)
    }

    @Test
    fun unscoredTadasanaNeverCounts() {
        val tadasana = AsanaLabel(Asana.TADASANA, PoseSide.NEUTRAL_CALIBRATION)
        assertNull(SessionSummarizer.toEntities(listOf(seg(tadasana, 30.0, null)), 0, 1, 1, "STANDARD"))
        assertNotNull(SessionSummarizer.toEntities(listOf(seg(tadasana, 30.0, null), seg(tree, 2.0, null)), 0, 1, 1, "STANDARD"))
    }
}
