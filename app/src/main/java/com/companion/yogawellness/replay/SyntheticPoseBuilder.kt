package com.companion.yogawellness.replay

import com.companion.yogawellness.pose.BodySide
import com.companion.yogawellness.pose.Landmark
import com.companion.yogawellness.pose.PoseFrame
import com.companion.yogawellness.pose.PoseLandmarkIndex as L
import kotlin.math.cos
import kotlin.math.sin

/**
 * Builds geometrically exact stick-figure [PoseFrame]s with known joint angles.
 *
 * Used by unit tests to verify the angle extractor / classifier / scorer end-to-end, and by the
 * replay harness to synthesize a deterministic demo clip when no camera is available.
 *
 * Pixel space is the upright, un-mirrored frame (x right, y down). For a person facing the camera
 * their LEFT side appears on the image RIGHT.
 */
class SyntheticPoseBuilder(
    val width: Int = 480,
    val height: Int = 640,
    /** Torso length (mid-hip to mid-shoulder) in pixels. */
    val torso: Double = 125.0,
    val hipX: Double = 240.0,
    val hipY: Double = 351.0
) {
    /** @param thighDeg thigh angle from the downward vertical, positive = outward (sideways) */
    data class Leg(val thighDeg: Double, val kneeDeg: Double, val kneeZ: Float = 0f)

    /** @param abductionDeg hip–shoulder–elbow angle (0 = arm down, 90 = T, 180 = overhead) */
    data class Arm(val abductionDeg: Double, val elbowDeg: Double = 180.0)

    private class P(val x: Double, val y: Double, val z: Float = 0f)

    private fun rad(d: Double) = Math.toRadians(d)

    /**
     * Front-facing figure.
     * @param trunkTiltDeg torso tilt from vertical; positive tilts the shoulders toward image right
     *                     (the person's left).
     */
    fun front(
        timestampMs: Long,
        left: Leg = Leg(3.0, 178.0),
        right: Leg = Leg(3.0, 178.0),
        leftArm: Arm = Arm(10.0),
        rightArm: Arm = Arm(10.0),
        trunkTiltDeg: Double = 0.0,
        dx: Double = 0.0,
        dy: Double = 0.0,
        visibility: Float = 0.95f,
        luminance: Double? = 120.0
    ): PoseFrame {
        val t = rad(trunkTiltDeg)
        val hx = hipX + dx
        val hy = hipY + dy
        val upX = sin(t)
        val upY = -cos(t)
        val pX = cos(t) // toward the person's left (image right when upright)
        val pY = sin(t)

        val sx = hx + upX * torso
        val sy = hy + upY * torso
        val pts = arrayOfNulls<P>(L.LANDMARK_COUNT)

        pts[L.NOSE] = P(sx + upX * 0.45 * torso, sy + upY * 0.45 * torso)
        val shHalf = 0.275 * torso
        val hipHalf = 0.2 * torso
        pts[L.LEFT_SHOULDER] = P(sx + pX * shHalf, sy + pY * shHalf)
        pts[L.RIGHT_SHOULDER] = P(sx - pX * shHalf, sy - pY * shHalf)
        pts[L.LEFT_HIP] = P(hx + hipHalf, hy)
        pts[L.RIGHT_HIP] = P(hx - hipHalf, hy)

        for (side in BodySide.entries) {
            val sign = if (side == BodySide.LEFT) 1.0 else -1.0
            val arm = if (side == BodySide.LEFT) leftArm else rightArm
            val sh = pts[if (side == BodySide.LEFT) L.LEFT_SHOULDER else L.RIGHT_SHOULDER]!!
            val a = rad(arm.abductionDeg)
            // torso-down = (-upX, -upY); outward = sign * p
            val ux = -upX * cos(a) + sign * pX * sin(a)
            val uy = -upY * cos(a) + sign * pY * sin(a)
            val elbow = P(sh.x + ux * 0.6 * torso, sh.y + uy * 0.6 * torso)
            val (fx, fy) = bendToward(ux, uy, 180.0 - arm.elbowDeg, -sign * pX, -sign * pY)
            val wrist = P(elbow.x + fx * 0.55 * torso, elbow.y + fy * 0.55 * torso)
            pts[if (side == BodySide.LEFT) L.LEFT_ELBOW else L.RIGHT_ELBOW] = elbow
            pts[if (side == BodySide.LEFT) L.LEFT_WRIST else L.RIGHT_WRIST] = wrist

            val leg = if (side == BodySide.LEFT) left else right
            val hip = pts[if (side == BodySide.LEFT) L.LEFT_HIP else L.RIGHT_HIP]!!
            val au = rad(leg.thighDeg)
            val knee = P(hip.x + sign * sin(au) * 0.95 * torso, hip.y + cos(au) * 0.95 * torso, leg.kneeZ)
            val av = rad(leg.thighDeg - (180.0 - leg.kneeDeg))
            val ankle = P(knee.x + sign * sin(av) * 0.9 * torso, knee.y + cos(av) * 0.9 * torso)
            pts[if (side == BodySide.LEFT) L.LEFT_KNEE else L.RIGHT_KNEE] = knee
            pts[if (side == BodySide.LEFT) L.LEFT_ANKLE else L.RIGHT_ANKLE] = ankle
        }
        return toFrame(timestampMs, pts, { visibility }, luminance)
    }

    /**
     * Side-on figure (for Utkatasana).
     * @param facingLeft person faces image left
     * @param trunkLeanDeg forward lean of the torso from vertical
     * @param thighDeg thigh angle from the downward vertical, positive = forward
     * @param armElevationDeg hip–shoulder–elbow angle (180 = overhead)
     */
    fun side(
        timestampMs: Long,
        trunkLeanDeg: Double,
        thighDeg: Double,
        kneeDeg: Double,
        armElevationDeg: Double,
        facingLeft: Boolean = true,
        nearSide: BodySide = BodySide.LEFT,
        farVisibility: Float = 0.7f,
        dx: Double = 0.0,
        dy: Double = 0.0,
        luminance: Double? = 120.0
    ): PoseFrame {
        val f = if (facingLeft) -1.0 else 1.0
        val lam = rad(trunkLeanDeg)
        val hx = hipX + dx
        val hy = hipY + dy
        val upX = f * sin(lam)
        val upY = -cos(lam)
        val nX = f * cos(lam) // forward, perpendicular to the torso
        val nY = sin(lam)
        val sx = hx + upX * torso
        val sy = hy + upY * torso

        val pts = arrayOfNulls<P>(L.LANDMARK_COUNT)
        pts[L.NOSE] = P(sx + upX * 0.45 * torso + f * 0.1 * torso, sy + upY * 0.45 * torso)

        val a = rad(armElevationDeg)
        val ux = -upX * cos(a) + nX * sin(a)
        val uy = -upY * cos(a) + nY * sin(a)
        val au = rad(thighDeg)
        val av = rad(thighDeg - (180.0 - kneeDeg))
        for (side in BodySide.entries) {
            val z = if (side == nearSide) -0.05f else 0.05f
            val shoulder = P(sx, sy, z)
            val elbow = P(sx + ux * 0.6 * torso, sy + uy * 0.6 * torso, z)
            val wrist = P(elbow.x + ux * 0.55 * torso, elbow.y + uy * 0.55 * torso, z)
            val hip = P(hx, hy, z)
            val knee = P(hx + f * sin(au) * 0.95 * torso, hy + cos(au) * 0.95 * torso, z)
            val ankle = P(knee.x + f * sin(av) * 0.9 * torso, knee.y + cos(av) * 0.9 * torso, z)
            val l = side == BodySide.LEFT
            pts[if (l) L.LEFT_SHOULDER else L.RIGHT_SHOULDER] = shoulder
            pts[if (l) L.LEFT_ELBOW else L.RIGHT_ELBOW] = elbow
            pts[if (l) L.LEFT_WRIST else L.RIGHT_WRIST] = wrist
            pts[if (l) L.LEFT_HIP else L.RIGHT_HIP] = hip
            pts[if (l) L.LEFT_KNEE else L.RIGHT_KNEE] = knee
            pts[if (l) L.LEFT_ANKLE else L.RIGHT_ANKLE] = ankle
        }
        val farIndices = if (nearSide == BodySide.LEFT) {
            setOf(L.RIGHT_SHOULDER, L.RIGHT_ELBOW, L.RIGHT_WRIST, L.RIGHT_HIP, L.RIGHT_KNEE, L.RIGHT_ANKLE)
        } else {
            setOf(L.LEFT_SHOULDER, L.LEFT_ELBOW, L.LEFT_WRIST, L.LEFT_HIP, L.LEFT_KNEE, L.LEFT_ANKLE)
        }
        return toFrame(timestampMs, pts, { i -> if (i in farIndices) farVisibility else 0.95f }, luminance)
    }

    /** Rotate (ux, uy) by [deg] in whichever direction moves it toward (ix, iy). */
    private fun bendToward(ux: Double, uy: Double, deg: Double, ix: Double, iy: Double): Pair<Double, Double> {
        if (deg == 0.0) return ux to uy
        val r = rad(deg)
        val c1x = ux * cos(r) - uy * sin(r)
        val c1y = ux * sin(r) + uy * cos(r)
        val c2x = ux * cos(-r) - uy * sin(-r)
        val c2y = ux * sin(-r) + uy * cos(-r)
        return if (c1x * ix + c1y * iy >= c2x * ix + c2y * iy) c1x to c1y else c2x to c2y
    }

    private fun toFrame(ts: Long, pts: Array<P?>, vis: (Int) -> Float, luminance: Double?): PoseFrame {
        val nose = pts[L.NOSE]!!
        val lms = (0 until L.LANDMARK_COUNT).map { i ->
            // Landmarks we do not model (eyes, fingers, feet...) are placed at the nose / nearest joint.
            val p = pts[i] ?: nose
            val modelled = pts[i] != null
            Landmark(
                (p.x / width).toFloat(),
                (p.y / height).toFloat(),
                p.z,
                if (modelled) vis(i) else 0.5f
            )
        }
        return PoseFrame(ts, width, height, lms, luminance)
    }

    // ------------------------------------------------------------------------------------------
    // Reference poses (all satisfy the §6.1 rules)
    // ------------------------------------------------------------------------------------------

    fun tadasana(ts: Long, dx: Double = 0.0) = front(ts, dx = dx)

    fun warriorII(ts: Long, frontSide: BodySide = BodySide.LEFT, frontKneeDeg: Double = 95.0, dx: Double = 0.0): PoseFrame {
        val frontLeg = Leg(70.0, frontKneeDeg)
        val backLeg = Leg(45.0, 178.0)
        return front(
            ts,
            left = if (frontSide == BodySide.LEFT) frontLeg else backLeg,
            right = if (frontSide == BodySide.LEFT) backLeg else frontLeg,
            leftArm = Arm(90.0), rightArm = Arm(90.0), dx = dx
        )
    }

    fun tree(ts: Long, lifted: BodySide = BodySide.LEFT, dx: Double = 0.0, dy: Double = 0.0): PoseFrame {
        val standing = Leg(2.0, 178.0)
        val liftedLeg = Leg(50.0, 40.0)
        return front(
            ts,
            left = if (lifted == BodySide.LEFT) liftedLeg else standing,
            right = if (lifted == BodySide.LEFT) standing else liftedLeg,
            leftArm = Arm(170.0, 168.0), rightArm = Arm(170.0, 168.0), dx = dx, dy = dy
        )
    }

    fun triangle(ts: Long, leading: BodySide = BodySide.LEFT, tiltDeg: Double = 68.0): PoseFrame =
        front(
            ts,
            left = Leg(35.0, 178.0), right = Leg(35.0, 178.0),
            leftArm = Arm(90.0), rightArm = Arm(90.0),
            trunkTiltDeg = if (leading == BodySide.LEFT) tiltDeg else -tiltDeg
        )

    fun chair(ts: Long, kneeDeg: Double = 112.0) =
        side(ts, trunkLeanDeg = 28.0, thighDeg = 44.0, kneeDeg = kneeDeg, armElevationDeg = 160.0)
}
