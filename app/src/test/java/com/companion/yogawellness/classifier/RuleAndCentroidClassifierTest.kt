package com.companion.yogawellness.classifier

import com.companion.yogawellness.pose.BodySide
import com.companion.yogawellness.pose.PoseFrame
import com.companion.yogawellness.replay.SyntheticPoseBuilder
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.PoseSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleAndCentroidClassifierTest {

    private val b = SyntheticPoseBuilder()

    private fun classify(frame: PoseFrame): ClassificationResult = RuleAndCentroidClassifier().classify(frame)

    @Test
    fun tadasana() {
        val r = classify(b.tadasana(0))
        assertEquals(AsanaLabel(Asana.TADASANA, PoseSide.NEUTRAL_CALIBRATION), r.label)
        assertEquals(ClassificationMethod.RULE, r.method)
    }

    @Test
    fun treeBothSides() {
        assertEquals(AsanaLabel(Asana.VRIKSHASANA, PoseSide.LEFT_LIFTED), classify(b.tree(0, BodySide.LEFT)).label)
        assertEquals(AsanaLabel(Asana.VRIKSHASANA, PoseSide.RIGHT_LIFTED), classify(b.tree(0, BodySide.RIGHT)).label)
    }

    @Test
    fun warriorIIBothSidesAcrossKneeAngles() {
        for (knee in listOf(95.0, 115.0, 125.0, 140.0)) {
            assertEquals(
                "knee=$knee", AsanaLabel(Asana.VIRABHADRASANA_II, PoseSide.FRONT_LEFT),
                classify(b.warriorII(0, BodySide.LEFT, knee)).label
            )
            assertEquals(
                "knee=$knee", AsanaLabel(Asana.VIRABHADRASANA_II, PoseSide.FRONT_RIGHT),
                classify(b.warriorII(0, BodySide.RIGHT, knee)).label
            )
        }
    }

    @Test
    fun triangleBothSides() {
        assertEquals(AsanaLabel(Asana.TRIKONASANA, PoseSide.LEADING_LEFT), classify(b.triangle(0, BodySide.LEFT)).label)
        assertEquals(AsanaLabel(Asana.TRIKONASANA, PoseSide.LEADING_RIGHT), classify(b.triangle(0, BodySide.RIGHT)).label)
    }

    @Test
    fun chairSideOn() {
        assertEquals(AsanaLabel(Asana.UTKATASANA, PoseSide.SYMMETRIC), classify(b.chair(0)).label)
        // Facing the other way too
        val mirrored = b.side(0, 28.0, 44.0, 112.0, 160.0, facingLeft = false, nearSide = BodySide.RIGHT)
        assertEquals(Asana.UTKATASANA, classify(mirrored).label?.asana)
    }

    @Test
    fun ruleTraceExplainsDecision() {
        val r = classify(b.warriorII(0))
        val warriorCheck = r.ruleTrace.first { it.asana == Asana.VIRABHADRASANA_II }
        assertTrue(warriorCheck.passed)
        assertTrue(warriorCheck.detail.contains("front knee=95°"))
        // Tree and Chair were evaluated first and rejected
        assertTrue(r.ruleTrace.first { it.asana == Asana.VRIKSHASANA }.passed.not())
    }

    @Test
    fun prototypeDistanceIsSmallestForTheTruePose() {
        val c = RuleAndCentroidClassifier()
        for ((frame, truth) in listOf(
            b.tadasana(0) to Asana.TADASANA,
            b.warriorII(0) to Asana.VIRABHADRASANA_II,
            b.triangle(0) to Asana.TRIKONASANA,
            b.chair(0) to Asana.UTKATASANA,
            b.tree(0) to Asana.VRIKSHASANA
        )) {
            c.reset()
            val d = c.classify(frame).prototypeDistances
            assertEquals("distances=$d", truth, d.minBy { it.value }.key)
        }
    }

    @Test
    fun ambiguousPoseIsRejected() {
        // Arms out (T) with straight legs together: not any of the roster
        val r = classify(b.front(0, leftArm = SyntheticPoseBuilder.Arm(90.0), rightArm = SyntheticPoseBuilder.Arm(90.0)))
        assertNull(r.label)
        assertEquals(ClassificationMethod.NONE, r.method)
    }

    @Test
    fun noPoseGivesNoLabel() {
        val r = classify(PoseFrame.empty(0))
        assertNull(r.label)
        assertNull(r.features)
    }

    @Test
    fun scaleInvariance() {
        val small = SyntheticPoseBuilder(torso = 100.0, hipY = 330.0).warriorII(0, BodySide.LEFT, 100.0)
        assertNotNull(classify(small).label)
        assertEquals(Asana.VIRABHADRASANA_II, classify(small).label!!.asana)
    }
}
