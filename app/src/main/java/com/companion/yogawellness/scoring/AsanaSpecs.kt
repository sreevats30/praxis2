package com.companion.yogawellness.scoring

import com.companion.yogawellness.pose.BodyPart
import com.companion.yogawellness.pose.BodySide
import com.companion.yogawellness.pose.PoseLandmarkIndex as L

/** Where the phone should see the user from (§4). */
enum class CameraAngle(val instruction: String) {
    FRONT("Face the camera"),
    SIDE_OR_45("Turn 45° or side-on to the camera")
}

/** User profile tolerance multiplier m_tau (§7.1). */
enum class ToleranceProfile(val label: String, val multiplier: Double) {
    BEGINNER("Beginner", 1.35),
    STANDARD("Standard", 1.0),
    ADVANCED("Advanced", 0.75)
}

/** The 4 core scored asanas + Tadasana calibration roster (§4). */
enum class Asana(
    val englishName: String,
    val sanskritName: String,
    val cameraAngle: CameraAngle,
    /** Depth damping applied to MediaPipe z when computing 3-point joint angles (§4). */
    val zWeight: Float,
    val hasSides: Boolean,
    val isScored: Boolean,
    /** Endurance cap in seconds, if any. */
    val maxHoldSec: Int?,
    val safetyRule: String
) {
    TADASANA(
        "Mountain Pose", "Tadasana", CameraAngle.FRONT, 0.25f,
        hasSides = false, isScored = false, maxHoldSec = null,
        safetyRule = "None (neutral standing baseline)"
    ),
    VRIKSHASANA(
        "Tree Pose", "Vrikshasana", CameraAngle.FRONT, 0.25f,
        hasSides = true, isScored = true, maxHoldSec = null,
        safetyRule = "Knee guard: never press the lifted foot against the standing knee joint"
    ),
    VIRABHADRASANA_II(
        "Warrior II", "Virabhadrasana II", CameraAngle.FRONT, 0.25f,
        hasSides = true, isScored = true, maxHoldSec = null,
        safetyRule = "Knee-over-ankle guard: front knee must not overshoot the front ankle"
    ),
    TRIKONASANA(
        "Triangle Pose", "Trikonasana", CameraAngle.FRONT, 0.25f,
        hasSides = true, isScored = true, maxHoldSec = null,
        safetyRule = "Hyperextension guard: front knee must not exceed 182°"
    ),
    UTKATASANA(
        "Chair Pose", "Utkatasana", CameraAngle.SIDE_OR_45, 0.15f,
        hasSides = false, isScored = true, maxHoldSec = 30,
        safetyRule = "Endurance & back guard: hold capped at 30 s; stop if lower back or knees ache"
    );

    val displayName: String get() = "$sanskritName ($englishName)"
}

/**
 * Which side variant of an asana is being performed (§6.1 side detection), and which anatomical
 * side plays the "front" (F_) role in the spec table:
 *  - Tree: F = standing leg, B = lifted leg
 *  - Warrior II: F = bent front leg
 *  - Triangle: F = leading leg (side of the lower wrist)
 */
enum class PoseSide(val label: String, val frontSide: BodySide, val workingSide: BodySide?) {
    LEFT_LIFTED("Left foot lifted", BodySide.RIGHT, BodySide.LEFT),
    RIGHT_LIFTED("Right foot lifted", BodySide.LEFT, BodySide.RIGHT),
    FRONT_LEFT("Left leg forward", BodySide.LEFT, BodySide.LEFT),
    FRONT_RIGHT("Right leg forward", BodySide.RIGHT, BodySide.RIGHT),
    LEADING_LEFT("Leading left", BodySide.LEFT, BodySide.LEFT),
    LEADING_RIGHT("Leading right", BodySide.RIGHT, BodySide.RIGHT),
    SYMMETRIC("Symmetric", BodySide.LEFT, null),
    NEUTRAL_CALIBRATION("Neutral", BodySide.LEFT, null);
}

/** A point on the body, possibly relative to the detected front/back side. */
sealed interface PointRef {
    data class Landmark(val index: Int) : PointRef
    data class Midpoint(val a: Int, val b: Int) : PointRef
    data class Front(val part: BodyPart) : PointRef
    data class Back(val part: BodyPart) : PointRef

    /** Landmark indices this point depends on, for the given front side. */
    fun indices(frontSide: BodySide): List<Int> = when (this) {
        is Landmark -> listOf(index)
        is Midpoint -> listOf(a, b)
        is Front -> listOf(part.index(frontSide))
        is Back -> listOf(part.index(frontSide.opposite))
    }

    companion object {
        val NOSE = Landmark(L.NOSE)
        val MID_SHOULDER = Midpoint(L.LEFT_SHOULDER, L.RIGHT_SHOULDER)
        val MID_HIP = Midpoint(L.LEFT_HIP, L.RIGHT_HIP)
    }
}

/** How a "joint angle" in the spec table is measured. */
sealed interface JointGeometry {
    /**
     * Angle at [vertex] between rays to [a] and [c] (0..180°). When [hyperextensionAware] is true
     * (knees), a nearly straight joint whose vertex sits *behind* the a–c line in depth is reported
     * as 360° − angle, so hyperextension reads as > 180° (§4 Triangle guard, §7.2 "do not lock").
     */
    data class ThreePoint(
        val a: PointRef,
        val vertex: PointRef,
        val c: PointRef,
        val hyperextensionAware: Boolean = false
    ) : JointGeometry

    /** Mean of the left and right 3-point angles a–vertex–c (uses whichever side is visible). */
    data class BilateralAverage(val a: BodyPart, val vertex: BodyPart, val c: BodyPart) : JointGeometry

    /** Unsigned angle (0..180°) between the segment lower→upper and the image "up" direction. */
    data class SegmentVsVertical(val lower: PointRef, val upper: PointRef) : JointGeometry

    /** Signed angle (−90..90°) of the segment left→right against the image horizontal. */
    data class SegmentVsHorizontal(val left: PointRef, val right: PointRef) : JointGeometry
}

/**
 * One row of the §7.2 master reference table. The scorer (§8.2) only needs the numeric fields;
 * [geometry] tells the angle extractor how to measure [name] from landmarks.
 */
data class JointSpec(
    val name: String,
    val geometry: JointGeometry,
    val targetDeg: Double,
    val toleranceDeg: Double,
    val maxDeviationDeg: Double = 30.0,
    val weight: Double,
    val isCritical: Boolean = false,
    val cueWhenTooLarge: String,
    val cueWhenTooSmall: String
)

/** Complete master reference table (§7.2). */
object AsanaSpecs {

    /** Hold timer runs only while S_align >= this (§7.1). */
    const val HOLD_THRESHOLD = 75.0

    /** Praise + chime when S_align stays >= this for [PRAISE_HOLD_SEC] (§3 Stage 4). */
    const val PRAISE_THRESHOLD = 88.0
    const val PRAISE_HOLD_SEC = 4.0

    /** Score colour bands used by the UI. */
    const val GREEN_THRESHOLD = HOLD_THRESHOLD
    const val AMBER_THRESHOLD = 50.0

    private val TORSO_VERTICAL = JointGeometry.SegmentVsVertical(PointRef.MID_HIP, PointRef.MID_SHOULDER)

    private fun front(p: BodyPart) = PointRef.Front(p)
    private fun back(p: BodyPart) = PointRef.Back(p)
    private fun knee(a: PointRef, b: PointRef, c: PointRef) =
        JointGeometry.ThreePoint(a, b, c, hyperextensionAware = true)

    val VRIKSHASANA: List<JointSpec> = listOf(
        JointSpec(
            name = "Standing Knee",
            geometry = knee(front(BodyPart.HIP), front(BodyPart.KNEE), front(BodyPart.ANKLE)),
            targetDeg = 178.0, toleranceDeg = 7.0, maxDeviationDeg = 30.0, weight = 0.30, isCritical = true,
            cueWhenTooLarge = "Keep a micro-softness; do not lock your standing knee",
            cueWhenTooSmall = "Firm and straighten your standing leg"
        ),
        JointSpec(
            name = "Lifted Knee Flexion",
            geometry = JointGeometry.ThreePoint(back(BodyPart.HIP), back(BodyPart.KNEE), back(BodyPart.ANKLE)),
            targetDeg = 75.0, toleranceDeg = 20.0, maxDeviationDeg = 45.0, weight = 0.20, isCritical = false,
            cueWhenTooLarge = "Draw your lifted foot slightly higher",
            cueWhenTooSmall = "Relax your lifted foot angle"
        ),
        JointSpec(
            name = "Pelvic Level",
            geometry = JointGeometry.SegmentVsHorizontal(PointRef.Landmark(L.LEFT_HIP), PointRef.Landmark(L.RIGHT_HIP)),
            targetDeg = 0.0, toleranceDeg = 6.0, maxDeviationDeg = 22.0, weight = 0.20, isCritical = true,
            cueWhenTooLarge = "Level your hips horizontally",
            cueWhenTooSmall = "Level your hips so neither side drops"
        ),
        JointSpec(
            name = "Torso Verticality",
            geometry = TORSO_VERTICAL,
            targetDeg = 0.0, toleranceDeg = 5.0, maxDeviationDeg = 22.0, weight = 0.20, isCritical = true,
            cueWhenTooLarge = "Stand tall and stack your shoulders over your hips",
            cueWhenTooSmall = "Bring your spine upright"
        ),
        JointSpec(
            name = "Overhead Elbows",
            geometry = JointGeometry.BilateralAverage(BodyPart.SHOULDER, BodyPart.ELBOW, BodyPart.WRIST),
            targetDeg = 168.0, toleranceDeg = 12.0, maxDeviationDeg = 40.0, weight = 0.10, isCritical = false,
            cueWhenTooLarge = "Relax your shoulders away from your ears",
            cueWhenTooSmall = "Reach your arms straighter overhead"
        )
    )

    val VIRABHADRASANA_II: List<JointSpec> = listOf(
        JointSpec(
            name = "Front Knee",
            geometry = knee(front(BodyPart.HIP), front(BodyPart.KNEE), front(BodyPart.ANKLE)),
            targetDeg = 95.0, toleranceDeg = 8.0, maxDeviationDeg = 30.0, weight = 0.30, isCritical = true,
            cueWhenTooLarge = "Bend your front knee deeper toward 90 degrees",
            cueWhenTooSmall = "Ease up slightly so your knee stays above your ankle"
        ),
        JointSpec(
            name = "Back Knee",
            geometry = knee(back(BodyPart.HIP), back(BodyPart.KNEE), back(BodyPart.ANKLE)),
            targetDeg = 175.0, toleranceDeg = 7.0, maxDeviationDeg = 30.0, weight = 0.20, isCritical = true,
            cueWhenTooLarge = "Avoid hyperextending your back knee",
            cueWhenTooSmall = "Straighten and anchor your back leg"
        ),
        JointSpec(
            name = "Front Arm Level",
            geometry = JointGeometry.ThreePoint(front(BodyPart.HIP), front(BodyPart.SHOULDER), front(BodyPart.ELBOW)),
            targetDeg = 90.0, toleranceDeg = 7.0, maxDeviationDeg = 35.0, weight = 0.15, isCritical = false,
            cueWhenTooLarge = "Lower your front arm to shoulder height",
            cueWhenTooSmall = "Lift your front arm up to shoulder level"
        ),
        JointSpec(
            name = "Back Arm Level",
            geometry = JointGeometry.ThreePoint(back(BodyPart.HIP), back(BodyPart.SHOULDER), back(BodyPart.ELBOW)),
            targetDeg = 90.0, toleranceDeg = 7.0, maxDeviationDeg = 35.0, weight = 0.15, isCritical = false,
            cueWhenTooLarge = "Lower your back arm to shoulder height",
            cueWhenTooSmall = "Raise your back arm to shoulder height"
        ),
        JointSpec(
            name = "Torso Verticality",
            geometry = TORSO_VERTICAL,
            targetDeg = 0.0, toleranceDeg = 6.0, maxDeviationDeg = 25.0, weight = 0.20, isCritical = true,
            cueWhenTooLarge = "Bring your torso upright; avoid leaning over your front leg",
            cueWhenTooSmall = "Center your chest directly above your hips"
        )
    )

    val TRIKONASANA: List<JointSpec> = listOf(
        JointSpec(
            name = "Front Knee",
            geometry = knee(front(BodyPart.HIP), front(BodyPart.KNEE), front(BodyPart.ANKLE)),
            targetDeg = 175.0, toleranceDeg = 7.0, maxDeviationDeg = 30.0, weight = 0.25, isCritical = true,
            cueWhenTooLarge = "Keep a soft micro-bend in your front knee",
            cueWhenTooSmall = "Straighten your front leg as you hinge sideways"
        ),
        JointSpec(
            name = "Back Knee",
            geometry = knee(back(BodyPart.HIP), back(BodyPart.KNEE), back(BodyPart.ANKLE)),
            targetDeg = 176.0, toleranceDeg = 7.0, maxDeviationDeg = 30.0, weight = 0.20, isCritical = false,
            cueWhenTooLarge = "Keep your back leg steady",
            cueWhenTooSmall = "Firm and straighten your back leg"
        ),
        JointSpec(
            name = "Trunk Lateral Tilt",
            geometry = TORSO_VERTICAL,
            targetDeg = 68.0, toleranceDeg = 10.0, maxDeviationDeg = 32.0, weight = 0.30, isCritical = true,
            cueWhenTooLarge = "Come up slightly to keep your lower spine long",
            cueWhenTooSmall = "Hinge deeper sideways from your front hip"
        ),
        JointSpec(
            name = "Vertical Arm Line",
            geometry = JointGeometry.ThreePoint(front(BodyPart.WRIST), PointRef.MID_SHOULDER, back(BodyPart.WRIST)),
            targetDeg = 174.0, toleranceDeg = 8.0, maxDeviationDeg = 35.0, weight = 0.15, isCritical = false,
            cueWhenTooLarge = "Stack your top arm directly over your shoulder",
            cueWhenTooSmall = "Open your chest and reach your top arm straight up"
        ),
        JointSpec(
            name = "Neck Alignment",
            geometry = JointGeometry.ThreePoint(PointRef.NOSE, PointRef.MID_SHOULDER, PointRef.MID_HIP),
            targetDeg = 174.0, toleranceDeg = 8.0, maxDeviationDeg = 30.0, weight = 0.10, isCritical = false,
            cueWhenTooLarge = "Keep your neck long in line with your spine",
            cueWhenTooSmall = "Align your head with your spine"
        )
    )

    val UTKATASANA: List<JointSpec> = listOf(
        JointSpec(
            name = "Left Knee Flexion",
            geometry = knee(PointRef.Landmark(L.LEFT_HIP), PointRef.Landmark(L.LEFT_KNEE), PointRef.Landmark(L.LEFT_ANKLE)),
            targetDeg = 112.0, toleranceDeg = 10.0, maxDeviationDeg = 32.0, weight = 0.25, isCritical = true,
            cueWhenTooLarge = "Sit your hips back and bend your knees deeper",
            cueWhenTooSmall = "Raise your hips slightly to protect your knees"
        ),
        JointSpec(
            name = "Right Knee Flexion",
            geometry = knee(PointRef.Landmark(L.RIGHT_HIP), PointRef.Landmark(L.RIGHT_KNEE), PointRef.Landmark(L.RIGHT_ANKLE)),
            targetDeg = 112.0, toleranceDeg = 10.0, maxDeviationDeg = 32.0, weight = 0.25, isCritical = true,
            cueWhenTooLarge = "Bend both knees evenly into the chair position",
            cueWhenTooSmall = "Raise your hips slightly"
        ),
        JointSpec(
            name = "Hip Flexion",
            geometry = JointGeometry.BilateralAverage(BodyPart.SHOULDER, BodyPart.HIP, BodyPart.KNEE),
            targetDeg = 108.0, toleranceDeg = 12.0, maxDeviationDeg = 35.0, weight = 0.20, isCritical = true,
            cueWhenTooLarge = "Shift your weight toward your heels and crease your hips",
            cueWhenTooSmall = "Lift your chest so your torso doesn't rest on your thighs"
        ),
        JointSpec(
            name = "Trunk Forward Lean",
            geometry = TORSO_VERTICAL,
            targetDeg = 28.0, toleranceDeg = 10.0, maxDeviationDeg = 30.0, weight = 0.15, isCritical = false,
            cueWhenTooLarge = "Lift your chest higher; avoid leaning too far forward",
            cueWhenTooSmall = "Allow a natural forward hinge from the hips"
        ),
        JointSpec(
            name = "Arm Elevation",
            geometry = JointGeometry.BilateralAverage(BodyPart.HIP, BodyPart.SHOULDER, BodyPart.ELBOW),
            targetDeg = 160.0, toleranceDeg = 12.0, maxDeviationDeg = 40.0, weight = 0.15, isCritical = false,
            cueWhenTooLarge = "Relax your neck and shoulders",
            cueWhenTooSmall = "Reach your arms higher alongside your ears"
        )
    )

    fun specsFor(asana: Asana): List<JointSpec> = when (asana) {
        Asana.TADASANA -> emptyList()
        Asana.VRIKSHASANA -> VRIKSHASANA
        Asana.VIRABHADRASANA_II -> VIRABHADRASANA_II
        Asana.TRIKONASANA -> TRIKONASANA
        Asana.UTKATASANA -> UTKATASANA
    }

    val scoredAsanas: List<Asana> = Asana.entries.filter { it.isScored }
}
