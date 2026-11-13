package com.companion.yogawellness.codex

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Small immutable 3-vector for the solver (world: +x screen right, +y up, +z toward viewer). */
data class V3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(o: V3) = V3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: V3) = V3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Double) = V3(x * s, y * s, z * s)
    operator fun unaryMinus() = V3(-x, -y, -z)
    infix fun dot(o: V3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: V3) = V3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    val length: Double get() = sqrt(this dot this)

    companion object {
        val ZERO = V3(0.0, 0.0, 0.0)
        val UP = V3(0.0, 1.0, 0.0)
        val DOWN = V3(0.0, -1.0, 0.0)
        val FWD = V3(0.0, 0.0, 1.0)
        val BACK = V3(0.0, 0.0, -1.0)
    }
}

/** Row-major 3x3 rotation matrix. */
class M3(private val m: DoubleArray) {
    operator fun times(o: M3): M3 {
        val r = DoubleArray(9)
        for (i in 0..2) for (j in 0..2) {
            r[i * 3 + j] = m[i * 3] * o.m[j] + m[i * 3 + 1] * o.m[3 + j] + m[i * 3 + 2] * o.m[6 + j]
        }
        return M3(r)
    }

    operator fun times(v: V3) = V3(
        m[0] * v.x + m[1] * v.y + m[2] * v.z,
        m[3] * v.x + m[4] * v.y + m[5] * v.z,
        m[6] * v.x + m[7] * v.y + m[8] * v.z
    )

    companion object {
        private fun rad(d: Double) = Math.toRadians(d)
        fun rx(d: Double): M3 { val c = cos(rad(d)); val s = sin(rad(d)); return M3(doubleArrayOf(1.0, 0.0, 0.0, 0.0, c, -s, 0.0, s, c)) }
        fun ry(d: Double): M3 { val c = cos(rad(d)); val s = sin(rad(d)); return M3(doubleArrayOf(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c)) }
        fun rz(d: Double): M3 { val c = cos(rad(d)); val s = sin(rad(d)); return M3(doubleArrayOf(c, -s, 0.0, s, c, 0.0, 0.0, 0.0, 1.0)) }
    }
}

/** Rotates [v] about the unit-direction of [axis] by [deg] (Rodrigues). */
fun rotateAbout(v: V3, axis: V3, deg: Double): V3 {
    val a = axis * (1.0 / axis.length)
    val t = Math.toRadians(deg)
    return v * cos(t) + (a cross v) * sin(t) + a * ((a dot v) * (1 - cos(t)))
}

/**
 * Solved figure: joint positions after root rotation, view yaw and ground contact
 * (lowest joint on y = 0, centred on the mean x of all joints), plus the trunk- and
 * pelvis-forward directions needed by a few callouts.
 */
class Skeleton(private val joints: Array<V3>, val trunkForward: V3, val pelvisForward: V3) {
    operator fun get(j: SkeletonJoint): V3 = joints[j.ordinal]
    val all: List<V3> get() = joints.toList()

    /** Named angle measured from the final 3D points (what the on-screen labels show). */
    fun measure(c: CalloutJoint): Double = c.measure(this)
}

/** Interior angle (degrees) between two vectors. */
fun angleBetween(u: V3, v: V3): Double {
    val d = u.length * v.length
    if (d < 1e-12) return Double.NaN
    return Math.toDegrees(acos(((u dot v) / d).coerceIn(-1.0, 1.0)))
}

/**
 * Forward kinematics for the 17-joint rig (§5.3). Builds the skeleton from the pelvis outward in
 * the body frame, applies root pitch/roll, then the view yaw (Front 0°, 45°, Side 90°), then
 * ground contact.
 */
object ForwardKinematics {
    private val SIDES = listOf(1.0, -1.0) // +1 = figure's left (+x), −1 = right

    fun solve(p: PoseAngles, yawDeg: Float): Skeleton {
        val r = SkeletonRig
        val pos = arrayOfNulls<V3>(SkeletonJoint.entries.size)
        val root = M3.rx(p.rootPitch.toDouble()) * M3.rz(-p.rootRoll.toDouble())
        val trunk = root * M3.rx(p.trunkPitch.toDouble()) * M3.rz(-p.trunkRoll.toDouble()) * M3.ry(p.trunkTwist.toDouble())

        val neck = trunk * V3.UP * r.TORSO
        pos[SkeletonJoint.PELVIS.ordinal] = V3.ZERO
        pos[SkeletonJoint.NECK.ordinal] = neck
        pos[SkeletonJoint.HEAD.ordinal] = neck + (trunk * M3.rx(p.neckPitch.toDouble())) * V3.UP * r.NECK_TO_HEAD

        for (sg in SIDES) {
            val left = sg > 0
            val flex = (if (left) p.lShoulderFlex else p.rShoulderFlex).toDouble()
            val abd = (if (left) p.lShoulderAbd else p.rShoulderAbd).toDouble()
            val elbowFlex = (if (left) p.lElbowFlex else p.rElbowFlex).toDouble()
            val sh = neck + trunk * V3(sg * r.HALF_SHOULDER_WIDTH, 0.0, 0.0)
            val armR = trunk * M3.rz(sg * abd)
            val el = sh + (armR * M3.rx(-flex)) * V3.DOWN * r.UPPER_ARM
            val wr = el + (armR * M3.rx(-(flex + elbowFlex))) * V3.DOWN * r.FOREARM

            val hipFlex = (if (left) p.lHipFlex else p.rHipFlex).toDouble()
            val hipAbd = (if (left) p.lHipAbd else p.rHipAbd).toDouble()
            val hipRot = (if (left) p.lHipRot else p.rHipRot).toDouble()
            val k = Math.toRadians((if (left) p.lKneeFlex else p.rKneeFlex).toDouble())
            val af = Math.toRadians((if (left) p.lAnkleFlex else p.rAnkleFlex).toDouble())
            val hip = root * V3(sg * r.HALF_HIP_WIDTH, 0.0, 0.0)
            val thighR = root * M3.rz(sg * hipAbd) * M3.rx(-hipFlex)
            val t = thighR * V3.DOWN
            // The knee-bend direction, rotated about the thigh axis by hipRot (mirrored per side).
            val d = rotateAbout(thighR * V3.BACK, t, -sg * hipRot)
            val knee = hip + t * r.THIGH
            val s = t * cos(k) + d * sin(k)
            val ankle = knee + s * r.SHIN
            val anterior = -d * cos(k) + t * sin(k)
            val toe = ankle + (anterior * cos(af) + s * sin(af)) * r.FOOT

            fun put(j: SkeletonJoint, rj: SkeletonJoint, v: V3) { pos[(if (left) j else rj).ordinal] = v }
            put(SkeletonJoint.L_SHOULDER, SkeletonJoint.R_SHOULDER, sh)
            put(SkeletonJoint.L_ELBOW, SkeletonJoint.R_ELBOW, el)
            put(SkeletonJoint.L_WRIST, SkeletonJoint.R_WRIST, wr)
            put(SkeletonJoint.L_HIP, SkeletonJoint.R_HIP, hip)
            put(SkeletonJoint.L_KNEE, SkeletonJoint.R_KNEE, knee)
            put(SkeletonJoint.L_ANKLE, SkeletonJoint.R_ANKLE, ankle)
            put(SkeletonJoint.L_TOE, SkeletonJoint.R_TOE, toe)
        }

        val yaw = M3.ry(yawDeg.toDouble())
        val rotated = Array(pos.size) { yaw * pos[it]!! }
        val minY = rotated.minOf { it.y }
        val meanX = rotated.sumOf { it.x } / rotated.size
        val shift = V3(meanX, minY, 0.0)
        return Skeleton(
            Array(rotated.size) { rotated[it] - shift },
            trunkForward = yaw * (trunk * V3.FWD),
            pelvisForward = yaw * (root * V3.FWD)
        )
    }
}

/** Joint used for the pill label of a callout, and (when [a]/[c] are set) the arc to draw. */
enum class CalloutJoint(
    val vertex: SkeletonJoint,
    val a: SkeletonJoint? = null,
    val c: SkeletonJoint? = null,
    val description: String
) {
    L_KNEE(SkeletonJoint.L_KNEE, SkeletonJoint.L_HIP, SkeletonJoint.L_ANKLE, description = "left knee (hip–knee–ankle)"),
    R_KNEE(SkeletonJoint.R_KNEE, SkeletonJoint.R_HIP, SkeletonJoint.R_ANKLE, description = "right knee (hip–knee–ankle)"),
    L_ELBOW(SkeletonJoint.L_ELBOW, SkeletonJoint.L_SHOULDER, SkeletonJoint.L_WRIST, description = "left elbow"),
    R_ELBOW(SkeletonJoint.R_ELBOW, SkeletonJoint.R_SHOULDER, SkeletonJoint.R_WRIST, description = "right elbow"),
    /** Upper arm relative to the trunk axis (0 = by the side, 90 = level, 180 = overhead). */
    L_SHOULDER(SkeletonJoint.L_SHOULDER, SkeletonJoint.L_HIP, SkeletonJoint.L_ELBOW, description = "left arm to torso"),
    R_SHOULDER(SkeletonJoint.R_SHOULDER, SkeletonJoint.R_HIP, SkeletonJoint.R_ELBOW, description = "right arm to torso"),
    /** Thigh relative to the trunk axis (180 = standing, 90 = seated). */
    L_HIP(SkeletonJoint.L_HIP, SkeletonJoint.NECK, SkeletonJoint.L_KNEE, description = "left hip (torso to thigh)"),
    R_HIP(SkeletonJoint.R_HIP, SkeletonJoint.NECK, SkeletonJoint.R_KNEE, description = "right hip (torso to thigh)"),
    /** Pelvis→neck from vertical (0 = upright, 90 = horizontal). */
    TRUNK_TILT(SkeletonJoint.PELVIS, description = "torso from vertical"),
    /** Head–neck–pelvis; > 180 when the head tips back (extension). */
    NECK(SkeletonJoint.NECK, SkeletonJoint.HEAD, SkeletonJoint.PELVIS, description = "head to spine"),
    PELVIS_LEVEL(SkeletonJoint.PELVIS, description = "hip line from horizontal"),
    ARM_LINE(SkeletonJoint.NECK, SkeletonJoint.L_WRIST, SkeletonJoint.R_WRIST, description = "wrist–neck–wrist line"),
    BODY_LINE(SkeletonJoint.PELVIS, SkeletonJoint.NECK, SkeletonJoint.L_ANKLE, description = "head-to-heel line"),
    L_LEG_LINE(SkeletonJoint.PELVIS, SkeletonJoint.NECK, SkeletonJoint.L_ANKLE, description = "torso to left leg line"),
    R_LEG_LINE(SkeletonJoint.PELVIS, SkeletonJoint.NECK, SkeletonJoint.R_ANKLE, description = "torso to right leg line"),
    L_LEG_FROM_VERTICAL(SkeletonJoint.L_HIP, description = "left leg from vertical"),
    R_LEG_FROM_VERTICAL(SkeletonJoint.R_HIP, description = "right leg from vertical"),
    TRUNK_TWIST(SkeletonJoint.NECK, description = "shoulder line vs hip line"),
    LEG_SPREAD(SkeletonJoint.PELVIS, SkeletonJoint.L_KNEE, SkeletonJoint.R_KNEE, description = "angle between thighs"),
    L_THIGH_OPENING(SkeletonJoint.L_HIP, description = "left thigh out from forward"),
    R_THIGH_OPENING(SkeletonJoint.R_HIP, description = "right thigh out from forward"),
    /** Back leg parallel to the same-side arm (180 = one long line). */
    L_SIDE_LINE(SkeletonJoint.L_SHOULDER, description = "left leg to left arm line"),
    R_SIDE_LINE(SkeletonJoint.R_SHOULDER, description = "right leg to right arm line");

    fun measure(s: Skeleton): Double {
        fun p(x: SkeletonJoint) = s[x]
        fun side(lj: SkeletonJoint, rj: SkeletonJoint) = if (name.startsWith("L_")) lj else rj
        return when (this) {
            L_KNEE, R_KNEE -> {
                val knee = p(side(SkeletonJoint.L_KNEE, SkeletonJoint.R_KNEE))
                angleBetween(p(side(SkeletonJoint.L_HIP, SkeletonJoint.R_HIP)) - knee, p(side(SkeletonJoint.L_ANKLE, SkeletonJoint.R_ANKLE)) - knee)
            }
            L_ELBOW, R_ELBOW -> {
                val el = p(side(SkeletonJoint.L_ELBOW, SkeletonJoint.R_ELBOW))
                angleBetween(p(side(SkeletonJoint.L_SHOULDER, SkeletonJoint.R_SHOULDER)) - el, p(side(SkeletonJoint.L_WRIST, SkeletonJoint.R_WRIST)) - el)
            }
            L_SHOULDER, R_SHOULDER -> angleBetween(
                p(SkeletonJoint.PELVIS) - p(SkeletonJoint.NECK),
                p(side(SkeletonJoint.L_ELBOW, SkeletonJoint.R_ELBOW)) - p(side(SkeletonJoint.L_SHOULDER, SkeletonJoint.R_SHOULDER))
            )
            L_HIP, R_HIP -> angleBetween(
                p(SkeletonJoint.NECK) - p(SkeletonJoint.PELVIS),
                p(side(SkeletonJoint.L_KNEE, SkeletonJoint.R_KNEE)) - p(side(SkeletonJoint.L_HIP, SkeletonJoint.R_HIP))
            )
            TRUNK_TILT -> angleBetween(p(SkeletonJoint.NECK) - p(SkeletonJoint.PELVIS), V3.UP)
            NECK -> {
                val head = p(SkeletonJoint.HEAD) - p(SkeletonJoint.NECK)
                val a = angleBetween(head, p(SkeletonJoint.PELVIS) - p(SkeletonJoint.NECK))
                if ((head dot s.trunkForward) < -1e-9) 360.0 - a else a
            }
            PELVIS_LEVEL -> {
                val v = p(SkeletonJoint.L_HIP) - p(SkeletonJoint.R_HIP)
                Math.toDegrees(asin((abs(v.y) / v.length).coerceIn(0.0, 1.0)))
            }
            ARM_LINE -> angleBetween(p(SkeletonJoint.L_WRIST) - p(SkeletonJoint.NECK), p(SkeletonJoint.R_WRIST) - p(SkeletonJoint.NECK))
            BODY_LINE -> angleBetween(
                p(SkeletonJoint.NECK) - p(SkeletonJoint.PELVIS),
                (p(SkeletonJoint.L_ANKLE) + p(SkeletonJoint.R_ANKLE)) * 0.5 - (p(SkeletonJoint.L_HIP) + p(SkeletonJoint.R_HIP)) * 0.5
            )
            L_LEG_LINE, R_LEG_LINE -> angleBetween(
                p(SkeletonJoint.NECK) - p(SkeletonJoint.PELVIS),
                p(side(SkeletonJoint.L_ANKLE, SkeletonJoint.R_ANKLE)) - p(side(SkeletonJoint.L_HIP, SkeletonJoint.R_HIP))
            )
            L_LEG_FROM_VERTICAL, R_LEG_FROM_VERTICAL ->
                angleBetween(p(side(SkeletonJoint.L_ANKLE, SkeletonJoint.R_ANKLE)) - p(side(SkeletonJoint.L_HIP, SkeletonJoint.R_HIP)), V3.DOWN)
            TRUNK_TWIST -> {
                val a = p(SkeletonJoint.L_SHOULDER) - p(SkeletonJoint.R_SHOULDER)
                val b = p(SkeletonJoint.L_HIP) - p(SkeletonJoint.R_HIP)
                angleBetween(V3(a.x, 0.0, a.z), V3(b.x, 0.0, b.z))
            }
            LEG_SPREAD -> angleBetween(p(SkeletonJoint.L_KNEE) - p(SkeletonJoint.L_HIP), p(SkeletonJoint.R_KNEE) - p(SkeletonJoint.R_HIP))
            L_THIGH_OPENING, R_THIGH_OPENING ->
                angleBetween(p(side(SkeletonJoint.L_KNEE, SkeletonJoint.R_KNEE)) - p(side(SkeletonJoint.L_HIP, SkeletonJoint.R_HIP)), s.pelvisForward)
            L_SIDE_LINE, R_SIDE_LINE -> 180.0 - angleBetween(
                p(side(SkeletonJoint.L_HIP, SkeletonJoint.R_HIP)) - p(side(SkeletonJoint.L_ANKLE, SkeletonJoint.R_ANKLE)),
                p(side(SkeletonJoint.L_WRIST, SkeletonJoint.R_WRIST)) - p(side(SkeletonJoint.L_SHOULDER, SkeletonJoint.R_SHOULDER))
            )
        }
    }
}
