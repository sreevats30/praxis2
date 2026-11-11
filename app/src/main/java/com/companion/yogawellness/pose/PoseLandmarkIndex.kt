package com.companion.yogawellness.pose

/**
 * MediaPipe Pose (BlazePose, 33 landmarks) indices used by this app.
 * LEFT/RIGHT are the person's anatomical sides on an un-mirrored camera frame.
 */
object PoseLandmarkIndex {
    const val NOSE = 0
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28

    const val LANDMARK_COUNT = 33

    /** Bones drawn by the skeleton overlay. */
    val SKELETON_EDGES: List<Pair<Int, Int>> = listOf(
        LEFT_SHOULDER to RIGHT_SHOULDER,
        LEFT_SHOULDER to LEFT_ELBOW, LEFT_ELBOW to LEFT_WRIST,
        RIGHT_SHOULDER to RIGHT_ELBOW, RIGHT_ELBOW to RIGHT_WRIST,
        LEFT_SHOULDER to LEFT_HIP, RIGHT_SHOULDER to RIGHT_HIP,
        LEFT_HIP to RIGHT_HIP,
        LEFT_HIP to LEFT_KNEE, LEFT_KNEE to LEFT_ANKLE,
        RIGHT_HIP to RIGHT_KNEE, RIGHT_KNEE to RIGHT_ANKLE
    )
}

enum class BodySide {
    LEFT, RIGHT;

    val opposite: BodySide get() = if (this == LEFT) RIGHT else LEFT
}

/** Limb joints that exist on both sides of the body. */
enum class BodyPart(val leftIndex: Int, val rightIndex: Int) {
    SHOULDER(PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.RIGHT_SHOULDER),
    ELBOW(PoseLandmarkIndex.LEFT_ELBOW, PoseLandmarkIndex.RIGHT_ELBOW),
    WRIST(PoseLandmarkIndex.LEFT_WRIST, PoseLandmarkIndex.RIGHT_WRIST),
    HIP(PoseLandmarkIndex.LEFT_HIP, PoseLandmarkIndex.RIGHT_HIP),
    KNEE(PoseLandmarkIndex.LEFT_KNEE, PoseLandmarkIndex.RIGHT_KNEE),
    ANKLE(PoseLandmarkIndex.LEFT_ANKLE, PoseLandmarkIndex.RIGHT_ANKLE);

    fun index(side: BodySide): Int = if (side == BodySide.LEFT) leftIndex else rightIndex
}
