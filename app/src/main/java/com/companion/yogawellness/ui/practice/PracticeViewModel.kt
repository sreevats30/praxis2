package com.companion.yogawellness.ui.practice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.companion.yogawellness.YogaWellnessApp
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
import java.time.LocalDate

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
    val message: String? = null
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
    @Volatile
    private var sessionStartMs = System.currentTimeMillis()
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
    }

    private fun process(frame: PoseFrame, sensors: SensorSnapshot?, record: Boolean) {
        val out = pipeline.process(frame, sensors)
        coach.onOutput(out)
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

    /** Persists the live session so far (if long enough) to the on-device database. */
    private fun saveSession() {
        val segments = pipeline.session.segments()
        val start = sessionStartMs
        val end = System.currentTimeMillis()
        val profile = pipeline.toleranceProfile.name
        container.appScope.launch {
            container.repository.saveSession(segments, start, end, LocalDate.now().toEpochDay(), profile)
        }
    }

    private fun resetPipeline() {
        sessionStartMs = System.currentTimeMillis()
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
            saveSession()
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
            saveSession() // keep the live practice done so far; replays are never saved
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
        if (!_state.value.isReplaying) saveSession()
        replayJob?.cancel()
        frames.close()
        recorder.stop()
        landmarker.close()
        accelerometer.stop()
        coach.shutdown()
    }
}
