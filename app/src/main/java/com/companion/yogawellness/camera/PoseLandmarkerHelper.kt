package com.companion.yogawellness.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.companion.yogawellness.pose.Landmark
import com.companion.yogawellness.pose.LuminanceMeter
import com.companion.yogawellness.pose.PoseFrame
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * MediaPipe PoseLandmarker in LIVE_STREAM mode (§2, §10 Prompt 2).
 *
 * Each CameraX frame is rotated upright using `imageProxy.imageInfo.rotationDegrees` but is NOT
 * mirrored, so MediaPipe's LEFT/RIGHT landmarks stay anatomically correct for the classifier;
 * only the on-screen overlay mirrors X for the front camera.
 */
class PoseLandmarkerHelper(
    private val context: Context,
    private val onResult: (PoseFrame) -> Unit,
    private val onError: (String) -> Unit
) {
    private val lock = Any()
    private var landmarker: PoseLandmarker? = null
    private var lastTimestampMs = 0L

    @Volatile
    private var lastLuminance: Double? = null

    /** Loads the model. Call off the main thread; throws if the model asset is missing. */
    fun setup() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET)
            .setDelegate(Delegate.CPU)
            .build()
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setResultListener { result: PoseLandmarkerResult, image: MPImage -> handleResult(result, image) }
            .setErrorListener { e: RuntimeException -> onError(e.message ?: "Pose detection error") }
            .build()
        val created = PoseLandmarker.createFromOptions(context, options)
        synchronized(lock) {
            landmarker?.close()
            landmarker = created
        }
    }

    val isReady: Boolean get() = synchronized(lock) { landmarker != null }

    /** Called on the CameraX analyzer thread. Always closes [imageProxy]. */
    fun detectLiveStream(imageProxy: ImageProxy) {
        val rotation = imageProxy.imageInfo.rotationDegrees
        // toBitmap() copies the RGBA pixels, so the proxy can be released right away.
        val source: Bitmap = try {
            imageProxy.toBitmap()
        } catch (e: Exception) {
            Log.w(TAG, "Could not convert frame", e)
            null
        } finally {
            imageProxy.close()
        } ?: return

        val upright = if (rotation != 0) {
            val m = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, m, false)
        } else {
            source
        }
        lastLuminance = measureLuminance(upright)

        synchronized(lock) {
            val lm = landmarker ?: return
            var ts = SystemClock.uptimeMillis()
            if (ts <= lastTimestampMs) ts = lastTimestampMs + 1 // detectAsync needs increasing timestamps
            lastTimestampMs = ts
            try {
                lm.detectAsync(BitmapImageBuilder(upright).build(), ts)
            } catch (e: Exception) {
                onError(e.message ?: "Pose detection failed")
            }
        }
    }

    private fun handleResult(result: PoseLandmarkerResult, image: MPImage) {
        val pose = result.landmarks().firstOrNull()
        val landmarks = pose?.map { nl ->
            Landmark(nl.x(), nl.y(), nl.z(), nl.visibility().orElse(0f))
        } ?: emptyList()
        onResult(PoseFrame(result.timestampMs(), image.width, image.height, landmarks, lastLuminance))
    }

    fun close() {
        synchronized(lock) {
            landmarker?.close()
            landmarker = null
        }
    }

    private fun measureLuminance(bitmap: Bitmap): Double {
        // Downsample first so the luminance check costs well under a millisecond.
        val small = Bitmap.createScaledBitmap(bitmap, 64, 64 * bitmap.height / bitmap.width.coerceAtLeast(1), false)
        val px = IntArray(small.width * small.height)
        small.getPixels(px, 0, small.width, 0, 0, small.width, small.height)
        return LuminanceMeter.meanLuminance(px, small.width, small.height, stride = 1)
    }

    companion object {
        const val MODEL_ASSET = "pose_landmarker_full.task"
        private const val TAG = "PoseLandmarkerHelper"
    }
}
