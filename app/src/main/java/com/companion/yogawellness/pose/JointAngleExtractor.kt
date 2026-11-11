package com.companion.yogawellness.pose

import com.companion.yogawellness.scoring.JointGeometry
import com.companion.yogawellness.scoring.JointSpec
import com.companion.yogawellness.scoring.PointRef
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

data class Vec3(val x: Double, val y: Double, val z: Double = 0.0) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Double) = Vec3(x * s, y * s, z * s)
    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    val norm: Double get() = sqrt(this dot this)
    val norm2D: Double get() = hypot(x, y)
}

/**
 * Maps landmarks into an aspect-correct pixel space: (x·W, y·H, z·W·zWeight).
 * zWeight damps MediaPipe's noisy monocular depth (§4: 0.25 front-on, 0.15 side-on).
 */
class PoseGeometry(val frame: PoseFrame, val zWeight: Double) {
    private val w = frame.imageWidth.toDouble()
    private val h = frame.imageHeight.toDouble()

    fun point(index: Int): Vec3 {
        val lm = frame[index]
        return Vec3(lm.x * w, lm.y * h, lm.z * w * zWeight)
    }

    /** Same point with undamped depth (used only for the hyperextension direction test). */
    fun rawPoint(index: Int): Vec3 {
        val lm = frame[index]
        return Vec3(lm.x * w, lm.y * h, lm.z * w)
    }

    fun visibility(index: Int): Float = frame[index].visibility

    fun resolve(ref: PointRef, frontSide: BodySide): Vec3 = when (ref) {
        is PointRef.Landmark -> point(ref.index)
        is PointRef.Midpoint -> (point(ref.a) + point(ref.b)) * 0.5
        is PointRef.Front -> point(ref.part.index(frontSide))
        is PointRef.Back -> point(ref.part.index(frontSide.opposite))
    }

    fun minVisibility(ref: PointRef, frontSide: BodySide): Float =
        ref.indices(frontSide).minOf { visibility(it) }

    val midShoulder: Vec3 get() = (point(PoseLandmarkIndex.LEFT_SHOULDER) + point(PoseLandmarkIndex.RIGHT_SHOULDER)) * 0.5
    val midHip: Vec3 get() = (point(PoseLandmarkIndex.LEFT_HIP) + point(PoseLandmarkIndex.RIGHT_HIP)) * 0.5

    /** 2D torso length ||P_mid_shldr − P_mid_hip|| in pixels (§5.1, §6.1). */
    val torsoLengthPx: Double get() = (midShoulder - midHip).norm2D
}

/** Normalized (0..1) screen anchors for drawing a joint's angle tag and correction arrow. */
data class JointAnchors(
    val a: Landmark?,
    val vertex: Landmark,
    val c: Landmark?,
    val kind: Kind
) {
    enum class Kind { THREE_POINT, VS_VERTICAL, VS_HORIZONTAL }
}

data class AngleExtraction(
    val angles: Map<String, Double>,
    val visibilityOk: Map<String, Boolean>
)

/**
 * Stage 1 (§3): turns landmarks into the named joint angles of the locked asana's spec table.
 */
object JointAngleExtractor {

    /** A joint is scored only if all of its landmarks reach this visibility (§7.1 step 3). */
    const val JOINT_VISIBILITY_MIN = 0.55f

    /** A straight-ish knee whose vertex sits this fraction of leg length behind the hip–ankle line
     *  (in raw depth) is reported as hyperextended (> 180°). */
    const val HYPEREXTENSION_DEPTH_FRACTION = 0.03
    const val HYPEREXTENSION_MIN_ANGLE = 160.0

    fun extract(frame: PoseFrame, specs: List<JointSpec>, frontSide: BodySide, zWeight: Double): AngleExtraction {
        if (!frame.hasPose) return AngleExtraction(emptyMap(), specs.associate { it.name to false })
        val g = PoseGeometry(frame, zWeight)
        val angles = HashMap<String, Double>()
        val vis = HashMap<String, Boolean>()
        for (spec in specs) {
            val (angle, visible) = measure(g, spec.geometry, frontSide)
            angles[spec.name] = angle
            vis[spec.name] = visible && !angle.isNaN()
        }
        return AngleExtraction(angles, vis)
    }

    fun measure(g: PoseGeometry, geometry: JointGeometry, frontSide: BodySide): Pair<Double, Boolean> =
        when (geometry) {
            is JointGeometry.ThreePoint -> {
                val visible = listOf(geometry.a, geometry.vertex, geometry.c)
                    .all { g.minVisibility(it, frontSide) >= JOINT_VISIBILITY_MIN }
                val a = g.resolve(geometry.a, frontSide)
                val b = g.resolve(geometry.vertex, frontSide)
                val c = g.resolve(geometry.c, frontSide)
                var theta = angle3(a, b, c)
                if (geometry.hyperextensionAware && theta > HYPEREXTENSION_MIN_ANGLE) {
                    val idxA = geometry.a.indices(frontSide)
                    val idxB = geometry.vertex.indices(frontSide)
                    val idxC = geometry.c.indices(frontSide)
                    if (idxA.size == 1 && idxB.size == 1 && idxC.size == 1 &&
                        isPosterior(g.rawPoint(idxA[0]), g.rawPoint(idxB[0]), g.rawPoint(idxC[0]))
                    ) {
                        theta = 360.0 - theta
                    }
                }
                theta to visible
            }

            is JointGeometry.BilateralAverage -> {
                val values = BodySide.entries.mapNotNull { side ->
                    val ia = geometry.a.index(side)
                    val ib = geometry.vertex.index(side)
                    val ic = geometry.c.index(side)
                    val ok = listOf(ia, ib, ic).all { g.visibility(it) >= JOINT_VISIBILITY_MIN }
                    if (ok) angle3(g.point(ia), g.point(ib), g.point(ic)) else null
                }
                if (values.isEmpty()) Double.NaN to false else values.average() to true
            }

            is JointGeometry.SegmentVsVertical -> {
                val visible = g.minVisibility(geometry.lower, frontSide) >= JOINT_VISIBILITY_MIN &&
                    g.minVisibility(geometry.upper, frontSide) >= JOINT_VISIBILITY_MIN
                segmentVsVertical(g.resolve(geometry.lower, frontSide), g.resolve(geometry.upper, frontSide)) to visible
            }

            is JointGeometry.SegmentVsHorizontal -> {
                val visible = g.minVisibility(geometry.left, frontSide) >= JOINT_VISIBILITY_MIN &&
                    g.minVisibility(geometry.right, frontSide) >= JOINT_VISIBILITY_MIN
                segmentVsHorizontal(g.resolve(geometry.left, frontSide), g.resolve(geometry.right, frontSide)) to visible
            }
        }

    /** Screen anchors (normalized, un-mirrored) for the overlay. Null if no pose. */
    fun anchors(frame: PoseFrame, geometry: JointGeometry, frontSide: BodySide): JointAnchors? {
        if (!frame.hasPose) return null
        fun lm(ref: PointRef): Landmark {
            val idx = ref.indices(frontSide)
            val xs = idx.map { frame[it].x }.average().toFloat()
            val ys = idx.map { frame[it].y }.average().toFloat()
            val v = idx.minOf { frame[it].visibility }
            return Landmark(xs, ys, 0f, v)
        }
        return when (geometry) {
            is JointGeometry.ThreePoint ->
                JointAnchors(lm(geometry.a), lm(geometry.vertex), lm(geometry.c), JointAnchors.Kind.THREE_POINT)

            is JointGeometry.BilateralAverage -> {
                val side = BodySide.entries.maxBy { s ->
                    minOf(
                        frame[geometry.a.index(s)].visibility,
                        frame[geometry.vertex.index(s)].visibility,
                        frame[geometry.c.index(s)].visibility
                    )
                }
                JointAnchors(
                    frame[geometry.a.index(side)], frame[geometry.vertex.index(side)],
                    frame[geometry.c.index(side)], JointAnchors.Kind.THREE_POINT
                )
            }

            is JointGeometry.SegmentVsVertical ->
                JointAnchors(lm(geometry.upper), lm(geometry.lower), null, JointAnchors.Kind.VS_VERTICAL)

            is JointGeometry.SegmentVsHorizontal ->
                JointAnchors(lm(geometry.left), lm(geometry.left).midTo(lm(geometry.right)), lm(geometry.right), JointAnchors.Kind.VS_HORIZONTAL)
        }
    }

    private fun Landmark.midTo(o: Landmark) = Landmark((x + o.x) / 2f, (y + o.y) / 2f, 0f, minOf(visibility, o.visibility))

    /** Interior angle at [b] between rays b→a and b→c, in degrees [0, 180]. NaN if degenerate. */
    fun angle3(a: Vec3, b: Vec3, c: Vec3): Double {
        val ba = a - b
        val bc = c - b
        val denom = ba.norm * bc.norm
        if (denom < 1e-9) return Double.NaN
        val cos = ((ba dot bc) / denom).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cos))
    }

    /** Unsigned angle in [0, 180] between segment lower→upper and the image "up" direction (2D). */
    fun segmentVsVertical(lower: Vec3, upper: Vec3): Double {
        val dx = upper.x - lower.x
        val dy = upper.y - lower.y
        if (abs(dx) < 1e-9 && abs(dy) < 1e-9) return Double.NaN
        return Math.toDegrees(atan2(abs(dx), -dy))
    }

    /** Signed angle in [−90, 90] of segment left→right vs horizontal; positive = right end higher. */
    fun segmentVsHorizontal(left: Vec3, right: Vec3): Double {
        val dx = right.x - left.x
        val dy = right.y - left.y
        if (abs(dx) < 1e-9 && abs(dy) < 1e-9) return Double.NaN
        return Math.toDegrees(atan2(-dy, abs(dx)))
    }

    /** True if [knee] sits behind (further from camera than) the hip–ankle line by a margin. */
    private fun isPosterior(hip: Vec3, knee: Vec3, ankle: Vec3): Boolean {
        val line = ankle - hip
        val len2 = line dot line
        if (len2 < 1e-9) return false
        val t = ((knee - hip) dot line) / len2
        val closest = hip + line * t
        val legLength = (knee - hip).norm + (ankle - knee).norm
        return knee.z - closest.z > HYPEREXTENSION_DEPTH_FRACTION * legLength
    }
}
