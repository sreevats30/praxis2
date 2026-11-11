package com.companion.yogawellness.feedback

import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.classifier.RuleAndCentroidClassifier
import com.companion.yogawellness.pose.BodySide
import com.companion.yogawellness.replay.SyntheticPoseBuilder
import com.companion.yogawellness.replay.SyntheticPoseBuilder.Arm
import com.companion.yogawellness.replay.SyntheticPoseBuilder.Leg
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.PoseSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyGuardsTest {

    private val b = SyntheticPoseBuilder()
    private val treeLeft = AsanaLabel(Asana.VRIKSHASANA, PoseSide.LEFT_LIFTED)

    @Test
    fun treeFootAboveKneeIsSafe() {
        val frame = b.tree(0, BodySide.LEFT)
        val lambda = SafetyGuards.treeLambda(frame, BodySide.LEFT)!!
        assertTrue("lambda=$lambda", lambda < SafetyGuards.TREE_LAMBDA_MIN)
        assertNull(SafetyGuards.evaluate(treeLeft, frame, emptyMap()))
    }

    @Test
    fun treeFootOnKneeTriggersGuard() {
        // Lifted ankle placed at the height of the standing knee, right next to it
        val frame = b.front(
            0, left = Leg(40.0, 64.3), right = Leg(2.0, 178.0),
            leftArm = Arm(170.0, 168.0), rightArm = Arm(170.0, 168.0)
        )
        // It is still classified as Tree…
        assertEquals(treeLeft, RuleAndCentroidClassifier().classify(frame).label)
        val lambda = SafetyGuards.treeLambda(frame, BodySide.LEFT)!!
        assertEquals(1.0, lambda, 0.05)
        // …but the knee guard fires
        assertEquals("tree_knee", SafetyGuards.evaluate(treeLeft, frame, emptyMap())!!.id)
    }

    @Test
    fun warriorKneeStackedOverAnkleIsSafe() {
        val frame = b.warriorII(0, BodySide.LEFT, 95.0)
        assertEquals(0.0, SafetyGuards.warriorKneeOvershoot(frame, BodySide.LEFT)!!, 0.01)
        assertNull(SafetyGuards.evaluate(AsanaLabel(Asana.VIRABHADRASANA_II, PoseSide.FRONT_LEFT), frame, emptyMap()))
    }

    @Test
    fun warriorKneePastAnkleTriggersGuard() {
        // Front shin tilted 15° inward -> knee 26% of shin length past the ankle
        val frame = b.front(0, left = Leg(70.0, 95.0), right = Leg(45.0, 178.0), leftArm = Arm(90.0), rightArm = Arm(90.0))
        val overshoot = SafetyGuards.warriorKneeOvershoot(frame, BodySide.LEFT)!!
        assertEquals(0.259, overshoot, 0.01)
        val alert = SafetyGuards.evaluate(AsanaLabel(Asana.VIRABHADRASANA_II, PoseSide.FRONT_LEFT), frame, emptyMap())
        assertEquals("warrior_knee", alert!!.id)
    }

    @Test
    fun warriorRightSideMirrorsCorrectly() {
        val frame = b.front(0, left = Leg(45.0, 178.0), right = Leg(70.0, 95.0), leftArm = Arm(90.0), rightArm = Arm(90.0))
        assertEquals(0.259, SafetyGuards.warriorKneeOvershoot(frame, BodySide.RIGHT)!!, 0.01)
    }

    @Test
    fun triangleHyperextension() {
        val label = AsanaLabel(Asana.TRIKONASANA, PoseSide.LEADING_LEFT)
        val frame = b.triangle(0)
        assertNull(SafetyGuards.evaluate(label, frame, mapOf("Front Knee" to 178.0)))
        assertNotNull(SafetyGuards.evaluate(label, frame, mapOf("Front Knee" to 184.0)))
    }

    @Test
    fun noLabelNoAlert() {
        assertNull(SafetyGuards.evaluate(null, b.tree(0), emptyMap()))
    }
}
