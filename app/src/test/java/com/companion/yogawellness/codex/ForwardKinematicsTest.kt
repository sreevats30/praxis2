package com.companion.yogawellness.codex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class ForwardKinematicsTest {
    private val items = AsanaCatalog32.items
    private val views = CodexView.entries

    @Test fun boneLengthsStayConstantAtRandomBlends() {
        val rnd = Random(42)
        for (item in items) for (view in views) repeat(50) {
            val t = rnd.nextFloat()
            val angles = PoseTimeline.anglesAt(item.start, item.peak, rnd.nextFloat() * 16f, 1f, item.breath, false)
                .let { PoseAngles.lerp(item.start, it, t) }
            val s = ForwardKinematics.solve(angles, view.yawDeg)
            for (b in SkeletonRig.BONES) {
                val len = (s[b.b] - s[b.a]).length
                assertEquals("${item.id}/${view.name} ${b.a}-${b.b}", b.length, len, b.length * 0.001)
            }
        }
    }

    @Test fun finiteAndOnTheFloor() {
        for (item in items) for (view in views) for (p in listOf(item.start, item.peak)) {
            val s = ForwardKinematics.solve(p, view.yawDeg)
            val all = s.all
            assertTrue(item.id, all.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() })
            assertTrue(item.id, all.all { it.y >= -1e-9 })
            assertEquals(item.id, 0.0, all.minOf { it.y }, 1e-9)
            assertEquals("${item.id} centred on mean x", 0.0, all.sumOf { it.x } / all.size, 1e-9)
        }
    }

    /** Twist / root-rotation heavy poses get ±5° (§5.7); every other callout ±3°. */
    private val wide = setOf(2, 3, 4, 17, 20, 21, 23)

    @Test fun peakPoseCalloutsMatchTheirTargets() {
        val failures = mutableListOf<String>()
        for (item in items) {
            val tol = if (item.number in wide) 5.0 else 3.0
            for (view in views) {
                val s = ForwardKinematics.solve(item.peak, view.yawDeg)
                for (c in item.callouts) {
                    val v = s.measure(c.joint)
                    if (!(abs(v - c.targetDeg) <= tol)) failures += "${item.id}/${view.name} ${c.label}: ${"%.1f".format(v)} vs ${c.targetDeg}"
                }
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test fun warriorTwoFrontShinIsVerticalAndBothFeetAreGrounded() {
        val w2 = AsanaCatalog32.byId("warrior_2")!!
        for (view in views) {
            val s = ForwardKinematics.solve(w2.peak, view.yawDeg)
            val shin = s[SkeletonJoint.L_KNEE] - s[SkeletonJoint.L_ANKLE]
            assertEquals("front shin from vertical", 0.0, angleBetween(shin, V3.UP), 3.0)
            assertEquals(95.0, s.measure(CalloutJoint.L_KNEE), 2.0)
            assertEquals("front ankle", 0.0, s[SkeletonJoint.L_ANKLE].y, 0.02)
            assertEquals("back ankle", 0.0, s[SkeletonJoint.R_ANKLE].y, 0.02)
        }
    }

    @Test fun neutralStandingFacesTheViewerWithLeftOnScreenRight() {
        val front = ForwardKinematics.solve(PoseAngles.STANDING, 0f)
        assertTrue("figure's left limbs are on screen right", front[SkeletonJoint.L_WRIST].x > front[SkeletonJoint.R_WRIST].x)
        assertTrue("head is on top", front[SkeletonJoint.HEAD].y > front[SkeletonJoint.PELVIS].y)
        assertEquals(180.0, front.measure(CalloutJoint.L_KNEE), 1e-6)
        assertEquals(0.0, front.measure(CalloutJoint.TRUNK_TILT), 1e-6)
        // Toes point toward the viewer (+z) in the front view.
        assertTrue(front[SkeletonJoint.L_TOE].z > front[SkeletonJoint.L_ANKLE].z)
        // Side view: the figure faces screen right (+x after a 90° yaw).
        val side = ForwardKinematics.solve(PoseAngles(lShoulderFlex = 90f), 90f)
        assertTrue(side[SkeletonJoint.L_WRIST].x > side[SkeletonJoint.L_SHOULDER].x)
    }

    @Test fun calloutsAreViewIndependent() {
        for (item in items) for (c in item.callouts) {
            val a = ForwardKinematics.solve(item.peak, 0f).measure(c.joint)
            val b = ForwardKinematics.solve(item.peak, 90f).measure(c.joint)
            assertEquals("${item.id} ${c.label}", a, b, 1e-6)
        }
    }
}
