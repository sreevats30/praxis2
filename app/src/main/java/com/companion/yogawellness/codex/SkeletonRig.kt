package com.companion.yogawellness.codex

/** The 17-joint stick-figure rig used by the Learn tab demos (not MediaPipe's 33 landmarks). */
enum class SkeletonJoint {
    HEAD, NECK, L_SHOULDER, R_SHOULDER, L_ELBOW, R_ELBOW, L_WRIST, R_WRIST,
    PELVIS, L_HIP, R_HIP, L_KNEE, R_KNEE, L_ANKLE, R_ANKLE, L_TOE, R_TOE
}

/**
 * Fixed bone lengths in units of torso length T (pelvis → neck = 1.0). Bone lengths are never
 * animated: the demo interpolates *angles* and re-solves, so limbs can never stretch.
 */
object SkeletonRig {
    const val TORSO = 1.0
    const val UPPER_ARM = 0.55
    const val FOREARM = 0.50
    const val THIGH = 0.85
    const val SHIN = 0.80
    const val FOOT = 0.30
    const val NECK_TO_HEAD = 0.35
    const val HALF_SHOULDER_WIDTH = 0.28
    const val HALF_HIP_WIDTH = 0.16

    data class Bone(val a: SkeletonJoint, val b: SkeletonJoint, val length: Double)

    val BONES: List<Bone> = listOf(
        Bone(SkeletonJoint.PELVIS, SkeletonJoint.NECK, TORSO),
        Bone(SkeletonJoint.NECK, SkeletonJoint.HEAD, NECK_TO_HEAD),
        Bone(SkeletonJoint.NECK, SkeletonJoint.L_SHOULDER, HALF_SHOULDER_WIDTH),
        Bone(SkeletonJoint.NECK, SkeletonJoint.R_SHOULDER, HALF_SHOULDER_WIDTH),
        Bone(SkeletonJoint.L_SHOULDER, SkeletonJoint.L_ELBOW, UPPER_ARM),
        Bone(SkeletonJoint.R_SHOULDER, SkeletonJoint.R_ELBOW, UPPER_ARM),
        Bone(SkeletonJoint.L_ELBOW, SkeletonJoint.L_WRIST, FOREARM),
        Bone(SkeletonJoint.R_ELBOW, SkeletonJoint.R_WRIST, FOREARM),
        Bone(SkeletonJoint.PELVIS, SkeletonJoint.L_HIP, HALF_HIP_WIDTH),
        Bone(SkeletonJoint.PELVIS, SkeletonJoint.R_HIP, HALF_HIP_WIDTH),
        Bone(SkeletonJoint.L_HIP, SkeletonJoint.L_KNEE, THIGH),
        Bone(SkeletonJoint.R_HIP, SkeletonJoint.R_KNEE, THIGH),
        Bone(SkeletonJoint.L_KNEE, SkeletonJoint.L_ANKLE, SHIN),
        Bone(SkeletonJoint.R_KNEE, SkeletonJoint.R_ANKLE, SHIN),
        Bone(SkeletonJoint.L_ANKLE, SkeletonJoint.L_TOE, FOOT),
        Bone(SkeletonJoint.R_ANKLE, SkeletonJoint.R_TOE, FOOT)
    )
}
