package com.companion.yogawellness.ui.practice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.data.SessionAutoRecorder
import com.companion.yogawellness.data.SessionReceipt
import com.companion.yogawellness.pose.PipelineEvent
import com.companion.yogawellness.camera.AccelerometerMonitor
import com.companion.yogawellness.camera.PoseLandmarkerHelper
import com.companion.yogawellness.data.AppPreferences
import com.companion.yogawellness.feedback.CoachUtterance
import com.companion.yogawellness.feedback.OfflineTtsCoach
import com.companion.yogawellness.pose.PipelineOutput
import com.companion.yogawellness.pose.PoseFrame
import com.companion.yogawellness.pose.PosePipeline
import com.companion.yogawellness.pose.SensorSnapshot
import com.companion.yogawellness.replay.DemoClips
import com.companion.yogawellness.replay.LandmarkRecorder
import com.companion.yogawellness.replay.LandmarkReplayer
import com.companion.yogawellness.replay.PoseClip
import com.companion.yogawellness.scoring.ToleranceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PracticeUiState(
    val output: PipelineOutput? = null,
    val modelReady: Boolean = false,
    val error: String? = null,
    val fps: Double = 0.0,
    val useFrontCamera: Boolean = true,
    val profile: ToleranceProfile = ToleranceProfile.STANDARD,
    val voiceEnabled: Boolean = true,
    val caption: CoachUtterance? = null,
    val recordingSec: Double? = null,
    val replayName: String? = null,
    val message: String? = null,
    /** Live-session recording state (REC badge). */
    val rec: SessionAutoRecorder.Rec = SessionAutoRecorder.Rec(),
    /** Set after "Finish & Save" when something was recorded. */
    val receipt: SessionReceipt? = null
) {
    val isReplaying: Boolean get() = replayName != null
}

/**
 * Owns the pose pipeline. MediaPipe results arrive on its own thread and are handed to a single
 * background coroutine (Dispatchers.Default, §3) through a conflated channel, so a slow frame is
 * dropped rather than queued. Replayed clips go through the very same [process] path.
 */
class PracticeViewModel(app: Application) : AndroidViewModel(app) {

    private val container = app as YogaWellnessApp
    private val prefs: AppPreferences = container.preferences
    private val pipeline = PosePipeline(prefs.toleranceProfile)
    private val accelerometer = AccelerometerMonitor(app)
    private val coach = OfflineTtsCoach(app).apply { enabled = prefs.voiceCoachEnabled }
    val recorder = LandmarkRecorder(container.recordingsDir)
    /** Continuous saving of the live session (app-scoped; survives this ViewModel). */
    private val sessionRecorder: SessionAutoRecorder = container.sessionRecorder
    private val frames = Channel<PoseFrame>(Channel.CONFLATED)
    private var lastFrameMs = -1L
    private var replayJob: Job? = null

    private val _state = MutableStateFlow(
        PracticeUiState(
            useFrontCamera = prefs.useFrontCamera,
            profile = prefs.toleranceProfile,
            voiceEnabled = prefs.voiceCoachEnabled
        )
    )
    val state: StateFlow<PracticeUiState> = _state.asStateFlow()

    val landmarker = PoseLandmarkerHelper(
        app,
        onResult = { frames.trySend(it) },
        onError = { msg -> _state.update { it.copy(error = msg) } }
    )

    /** Single-threaded dispatcher so live frames and replay frames never touch the pipeline at once. */
    private val pipelineDispatcher = Dispatchers.Default.limitedParallelism(1)

    init {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                landmarker.setup()
                _state.update { it.copy(modelReady = true, error = null) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Could not load the pose model: ${e.message}") }
            }
        }
        viewModelScope.launch(pipelineDispatcher) {
            for (frame in frames) {
                if (_state.value.isReplaying) continue
                process(frame, accelerometer.snapshot(), record = true)
            }
        }
        viewModelScope.launch {
            coach.lastUtterance.collect { u -> _state.update { it.copy(caption = u) } }
        }
        viewModelScope.launch {
            sessionRecorder.rec.collect { r -> _state.update { it.copy(rec = r) } }
        }
    }

    private fun process(frame: PoseFrame, sensors: SensorSnapshot?, record: Boolean) {
        val out = pipeline.process(frame, sensors)
        coach.onOutput(out)
        if (record) {
            // Live frames only: replays are never recorded.
            if (out.events.any { it is PipelineEvent.CalibrationComplete }) sessionRecorder.markActive()
            sessionRecorder.tick({ pipeline.session.segments() }, pipeline.toleranceProfile.name)
        }
        var recordingSec: Double? = null
        if (record && recorder.isRecording) {
            val finished = recorder.append(frame, sensors)
            if (finished != null) {
                _state.update { it.copy(message = "Saved ${finished.name}") }
            } else {
                recordingSec = recorder.elapsedSec(frame.timestampMs)
            }
        }
        val dt = if (lastFrameMs < 0) 0L else frame.timestampMs - lastFrameMs
        lastFrameMs = frame.timestampMs
        _state.update { s ->
            val instFps = if (dt > 0) 1000.0 / dt else s.fps
            s.copy(
                output = out,
                fps = if (s.fps == 0.0) instFps else 0.9 * s.fps + 0.1 * instFps,
                recordingSec = recordingSec
            )
        }
    }

    /**
     * Saves the live session so far and starts a new one. The segment snapshot is taken here,
     * before any pipeline reset. Writes happen on the app scope.
     */
    private fun finalizeSession(): SessionReceipt? =
        sessionRecorder.finalizeAndRotate(pipeline.session.segments(), pipeline.toleranceProfile.name)

    /** Lifecycle ON_STOP (Home, screen lock): persist now, keep the session open. */
    fun checkpointNow() {
        if (_state.value.isReplaying) return
        sessionRecorder.checkpointNow(pipeline.session.segments(), pipeline.toleranceProfile.name)
    }

    /** "Finish & Save": close the session and show a receipt (or explain the 2 s rule). */
    fun finishAndSave() {
        viewModelScope.launch(pipelineDispatcher) {
            if (_state.value.isReplaying) return@launch
            val receipt = finalizeSession()
            resetPipeline()
            if (receipt != null) {
                container.activeProfile.returnToMyAccount() // the saved session is in My Account
                _state.update { it.copy(receipt = receipt, output = null) }
            } else {
                _state.update { it.copy(message = HOLD_TO_RECORD_MESSAGE) }
            }
        }
    }

    fun clearReceipt() = _state.update { it.copy(receipt = null) }

    private fun resetPipeline() {
        pipeline.reset()
        coach.reset()
        lastFrameMs = -1L
    }

    fun onStart() = accelerometer.start()
    fun onStop() = accelerometer.stop()

    fun toggleCamera() {
        val front = !_state.value.useFrontCamera
        prefs.useFrontCamera = front
        _state.update { it.copy(useFrontCamera = front, output = null) }
        viewModelScope.launch(pipelineDispatcher) {
            finalizeSession()
            resetPipeline()
        }
    }

    fun toggleVoice() {
        val on = !_state.value.voiceEnabled
        prefs.voiceCoachEnabled = on
        coach.enabled = on
        _state.update { it.copy(voiceEnabled = on) }
    }

    fun toggleRecording() {
        viewModelScope.launch(pipelineDispatcher) {
            if (recorder.isRecording) {
                val f = recorder.stop()
                _state.update { it.copy(recordingSec = null, message = f?.let { "Saved ${it.name}" }) }
            } else {
                recorder.start(note = "profile=${_state.value.profile.name}")
                _state.update { it.copy(recordingSec = 0.0, message = "Recording 20 s of landmarks…") }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun listClips(): List<File> = recorder.listClips()

    fun startDemoReplay() = startReplay { DemoClips.warriorCorrection() }

    fun startReplay(file: File) = startReplay { LandmarkReplayer.load(file) }

    private fun startReplay(loader: () -> PoseClip) {
        replayJob?.cancel()
        replayJob = viewModelScope.launch(pipelineDispatcher) {
            val clip = try {
                withContext(Dispatchers.IO) { loader() }
            } catch (e: Exception) {
                _state.update { it.copy(message = "Could not open clip: ${e.message}") }
                return@launch
            }
            if (recorder.isRecording) recorder.stop()
            finalizeSession() // keep the live practice done so far; replays are never saved
            resetPipeline()
            _state.update { it.copy(replayName = clip.name, output = null, recordingSec = null) }
            try {
                LandmarkReplayer.realtime(clip).collect { cf -> process(cf.frame, cf.sensors, record = false) }
                _state.update { it.copy(message = "Replay finished") }
            } finally {
                resetPipeline()
                _state.update { it.copy(replayName = null) }
            }
        }
    }

    fun stopReplay() {
        replayJob?.cancel()
    }

    override fun onCleared() {
        if (!_state.value.isReplaying) finalizeSession()
        replayJob?.cancel()
        frames.close()
        recorder.stop()
        landmarker.close()
        accelerometer.stop()
        coach.shutdown()
    }

    companion object {
        const val HOLD_TO_RECORD_MESSAGE = "Hold a pose for at least 2 seconds to record it."
    }
}
