package com.companion.yogawellness.ui.practice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.companion.yogawellness.camera.AccelerometerMonitor
import com.companion.yogawellness.camera.PoseLandmarkerHelper
import com.companion.yogawellness.data.AppPreferences
import com.companion.yogawellness.pose.PipelineOutput
import com.companion.yogawellness.pose.PoseFrame
import com.companion.yogawellness.pose.PosePipeline
import com.companion.yogawellness.scoring.ToleranceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

data class PracticeUiState(
    val output: PipelineOutput? = null,
    val modelReady: Boolean = false,
    val error: String? = null,
    val fps: Double = 0.0,
    val useFrontCamera: Boolean = true,
    val profile: ToleranceProfile = ToleranceProfile.STANDARD
)

/**
 * Owns the pose pipeline. MediaPipe results arrive on its own thread and are handed to a single
 * background coroutine (Dispatchers.Default, §3) through a conflated channel, so a slow frame is
 * dropped rather than queued.
 */
class PracticeViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = AppPreferences(app)
    private val pipeline = PosePipeline(prefs.toleranceProfile)
    private val accelerometer = AccelerometerMonitor(app)
    private val frames = Channel<PoseFrame>(Channel.CONFLATED)
    private val resetRequested = AtomicBoolean(false)
    private var lastFrameMs = -1L

    private val _state = MutableStateFlow(
        PracticeUiState(useFrontCamera = prefs.useFrontCamera, profile = prefs.toleranceProfile)
    )
    val state: StateFlow<PracticeUiState> = _state.asStateFlow()

    val landmarker = PoseLandmarkerHelper(
        app,
        onResult = { frames.trySend(it) },
        onError = { msg -> _state.update { it.copy(error = msg) } }
    )

    init {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                landmarker.setup()
                _state.update { it.copy(modelReady = true, error = null) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Could not load the pose model: ${e.message}") }
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            for (frame in frames) {
                if (resetRequested.getAndSet(false)) {
                    pipeline.reset()
                    lastFrameMs = -1L
                }
                val out = pipeline.process(frame, accelerometer.snapshot())
                val dt = if (lastFrameMs < 0) 0L else frame.timestampMs - lastFrameMs
                lastFrameMs = frame.timestampMs
                _state.update { s ->
                    val instFps = if (dt > 0) 1000.0 / dt else s.fps
                    s.copy(output = out, fps = if (s.fps == 0.0) instFps else 0.9 * s.fps + 0.1 * instFps)
                }
            }
        }
    }

    fun onStart() = accelerometer.start()
    fun onStop() = accelerometer.stop()

    fun toggleCamera() {
        val front = !_state.value.useFrontCamera
        prefs.useFrontCamera = front
        resetRequested.set(true)
        _state.update { it.copy(useFrontCamera = front, output = null) }
    }

    fun restartSession() {
        resetRequested.set(true)
    }

    override fun onCleared() {
        frames.close()
        landmarker.close()
        accelerometer.stop()
    }
}
