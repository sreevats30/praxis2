package com.companion.yogawellness.pose

/**
 * One MediaPipe image landmark: x, y normalized to [0, 1] of the *upright, un-mirrored* frame,
 * z in roughly the same scale as x (smaller = closer to the camera), visibility in [0, 1].
 */
data class Landmark(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val visibility: Float = 1f
)

/**
 * One analysed camera frame. This is the only thing the scoring pipeline sees, so live camera
 * frames and replayed `.jsonl` clips (§10 Prompt 3) go through exactly the same code path.
 *
 * @param imageWidth width of the upright frame in pixels (after applying rotationDegrees)
 * @param imageHeight height of the upright frame in pixels
 * @param landmarks the 33 pose landmarks, or empty when no person was detected
 * @param meanLuminance mean luma (0–255) of the centre 50% crop, if measured
 */
data class PoseFrame(
    val timestampMs: Long,
    val imageWidth: Int,
    val imageHeight: Int,
    val landmarks: List<Landmark>,
    val meanLuminance: Double? = null
) {
    val hasPose: Boolean get() = landmarks.size >= PoseLandmarkIndex.LANDMARK_COUNT
    val timestampSec: Double get() = timestampMs / 1000.0

    operator fun get(index: Int): Landmark = landmarks[index]

    companion object {
        fun empty(timestampMs: Long, width: Int = 480, height: Int = 640, luminance: Double? = null) =
            PoseFrame(timestampMs, width, height, emptyList(), luminance)
    }
}
