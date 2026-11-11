package com.companion.yogawellness.pose

import com.companion.yogawellness.replay.SyntheticPoseBuilder
import com.companion.yogawellness.scoring.AlignmentScorer
import com.companion.yogawellness.scoring.AsanaSpecs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JointAngleExtractorTest {

    private val b = SyntheticPoseBuilder()

    @Test
    fun basicGeometry() {
        assertEquals(90.0, JointAngleExtractor.angle3(Vec3(1.0, 0.0), Vec3(0.0, 0.0), Vec3(0.0, 1.0)), 1e-9)
        assertEquals(180.0, JointAngleExtractor.angle3(Vec3(-1.0, 0.0), Vec3(0.0, 0.0), Vec3(1.0, 0.0)), 1e-9)
        // y grows downward: upper above lower => 0° from vertical
        assertEquals(0.0, JointAngleExtractor.segmentVsVertical(Vec3(0.0, 10.0), Vec3(0.0, 0.0)), 1e-9)
        assertEquals(45.0, JointAngleExtractor.segmentVsVertical(Vec3(0.0, 10.0), Vec3(10.0, 0.0)), 1e-9)
        // right end higher => positive
        assertEquals(45.0, JointAngleExtractor.segmentVsHorizontal(Vec3(0.0, 10.0), Vec3(10.0, 0.0)), 1e-9)
        assertEquals(-45.0, JointAngleExtractor.segmentVsHorizontal(Vec3(0.0, 0.0), Vec3(10.0, 10.0)), 1e-9)
    }

    @Test
    fun warriorIIExtractedAnglesMatchConstruction() {
        for (knee in listOf(95.0, 108.0, 125.0)) {
            val frame = b.warriorII(0, BodySide.LEFT, knee)
            val ex = JointAngleExtractor.extract(frame, AsanaSpecs.VIRABHADRASANA_II, BodySide.LEFT, 0.25)
            assertEquals(knee, ex.angles.getValue("Front Knee"), 1e-3)
            assertEquals(178.0, ex.angles.getValue("Back Knee"), 1e-3)
            assertEquals(0.0, ex.angles.getValue("Torso Verticality"), 1e-3)
            assertEquals(90.0, ex.angles.getValue("Front Arm Level"), 7.0)
            assertTrue(ex.visibilityOk.values.all { it })
        }
    }

    @Test
    fun endToEndWarriorScoreMatchesProofTable() {
        val frame = b.warriorII(0, BodySide.RIGHT, 125.0)
        val ex = JointAngleExtractor.extract(frame, AsanaSpecs.VIRABHADRASANA_II, BodySide.RIGHT, 0.25)
        val result = AlignmentScorer.evaluate(ex.angles, ex.visibilityOk, AsanaSpecs.VIRABHADRASANA_II)
        // Geometry is not perfectly "all other joints at target" (arms ~94°, back knee 178°) but those
        // are inside their deadbands, so the knee cap dominates exactly as in §7.1.
        assertEquals(41.4, result.finalScore!!, 0.1)
        assertEquals("Bend your front knee deeper toward 90 degrees", result.primaryCorrectiveCue)
    }

    @Test
    fun triangleTiltAndArmLine() {
        val frame = b.triangle(0, BodySide.LEFT, 68.0)
        val ex = JointAngleExtractor.extract(frame, AsanaSpecs.TRIKONASANA, BodySide.LEFT, 0.25)
        assertEquals(68.0, ex.angles.getValue("Trunk Lateral Tilt"), 1e-3)
        assertEquals(180.0, ex.angles.getValue("Vertical Arm Line"), 1e-3)
        assertEquals(180.0, ex.angles.getValue("Neck Alignment"), 1e-3)
    }

    @Test
    fun chairSideOnAnglesMatchConstruction() {
        val frame = b.chair(0, kneeDeg = 112.0)
        val ex = JointAngleExtractor.extract(frame, AsanaSpecs.UTKATASANA, BodySide.LEFT, 0.15)
        assertEquals(112.0, ex.angles.getValue("Left Knee Flexion"), 1e-3)
        assertEquals(112.0, ex.angles.getValue("Right Knee Flexion"), 1e-3)
        assertEquals(108.0, ex.angles.getValue("Hip Flexion"), 1e-3)
        assertEquals(28.0, ex.angles.getValue("Trunk Forward Lean"), 1e-3)
        assertEquals(160.0, ex.angles.getValue("Arm Elevation"), 1e-3)
    }

    @Test
    fun treePelvisLevelAndTorsoVertical() {
        val frame = b.tree(0, BodySide.LEFT)
        // LEFT lifted -> standing (front) side = RIGHT
        val ex = JointAngleExtractor.extract(frame, AsanaSpecs.VRIKSHASANA, BodySide.RIGHT, 0.25)
        assertEquals(178.0, ex.angles.getValue("Standing Knee"), 1e-3)
        assertEquals(40.0, ex.angles.getValue("Lifted Knee Flexion"), 1e-3)
        assertEquals(0.0, ex.angles.getValue("Pelvic Level"), 1e-3)
        assertEquals(168.0, ex.angles.getValue("Overhead Elbows"), 1e-3)
    }

    @Test
    fun hyperextendedKneeReadsAbove180() {
        // Straight leg with the knee pushed back (away from camera) in depth
        val straight = b.front(0, left = SyntheticPoseBuilder.Leg(35.0, 178.0, kneeZ = 0.08f))
        val g = PoseGeometry(straight, 0.25)
        val spec = AsanaSpecs.TRIKONASANA.first { it.name == "Front Knee" }
        val (angle, _) = JointAngleExtractor.measure(g, spec.geometry, BodySide.LEFT)
        assertTrue("expected > 182°, got $angle", angle > 182.0)

        // Knee pushed toward the camera is ordinary flexion, not hyperextension
        val forward = b.front(0, left = SyntheticPoseBuilder.Leg(35.0, 178.0, kneeZ = -0.08f))
        val (angle2, _) = JointAngleExtractor.measure(PoseGeometry(forward, 0.25), spec.geometry, BodySide.LEFT)
        assertTrue("expected < 180°, got $angle2", angle2 < 180.0)
    }

    @Test
    fun lowVisibilityJointsAreFlagged() {
        val frame = b.chair(0).let { f ->
            f.copy(landmarks = f.landmarks.mapIndexed { i, lm ->
                if (i == PoseLandmarkIndex.RIGHT_KNEE) lm.copy(visibility = 0.2f) else lm
            })
        }
        val ex = JointAngleExtractor.extract(frame, AsanaSpecs.UTKATASANA, BodySide.LEFT, 0.15)
        assertFalse(ex.visibilityOk.getValue("Right Knee Flexion"))
        assertTrue(ex.visibilityOk.getValue("Left Knee Flexion"))
    }

    @Test
    fun noPoseGivesNoAngles() {
        val ex = JointAngleExtractor.extract(PoseFrame.empty(0), AsanaSpecs.UTKATASANA, BodySide.LEFT, 0.15)
        assertTrue(ex.angles.isEmpty())
        assertTrue(ex.visibilityOk.values.none { it })
    }
}
