package com.companion.yogawellness.replay

import com.companion.yogawellness.pose.PoseFrame
import com.companion.yogawellness.pose.SensorSnapshot
import java.io.BufferedWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Records live pose landmarks (never images) to `.jsonl` clips of up to [maxDurationSec] seconds
 * (§10 Prompt 3) so thresholds can be tuned later by replaying them through the same pipeline.
 */
class LandmarkRecorder(private val dir: File, private val maxDurationSec: Double = 20.0) {

    private var writer: BufferedWriter? = null
    private var file: File? = null
    private var firstFrameMs = -1L
    var framesWritten = 0
        private set

    val isRecording: Boolean @Synchronized get() = writer != null

    @Synchronized
    fun start(nowMs: Long = System.currentTimeMillis(), note: String? = null): File {
        stop()
        dir.mkdirs()
        val f = File(dir, defaultName(nowMs))
        writer = f.bufferedWriter().also {
            it.write(PoseClipCodec.header(nowMs, note))
            it.newLine()
        }
        file = f
        firstFrameMs = -1L
        framesWritten = 0
        return f
    }

    /** Appends one frame. Returns the finished file when the clip reached its maximum length. */
    @Synchronized
    fun append(frame: PoseFrame, sensors: SensorSnapshot?): File? {
        val w = writer ?: return null
        if (firstFrameMs < 0) firstFrameMs = frame.timestampMs
        w.write(PoseClipCodec.encode(frame, sensors))
        w.newLine()
        framesWritten++
        return if ((frame.timestampMs - firstFrameMs) / 1000.0 >= maxDurationSec) stop() else null
    }

    /** Seconds recorded so far in the current clip. */
    @Synchronized
    fun elapsedSec(latestFrameMs: Long): Double =
        if (writer == null || firstFrameMs < 0) 0.0 else (latestFrameMs - firstFrameMs) / 1000.0

    @Synchronized
    fun stop(): File? {
        val w = writer ?: return null
        w.flush()
        w.close()
        writer = null
        return file
    }

    fun listClips(): List<File> =
        dir.listFiles { f -> f.isFile && f.name.endsWith(".jsonl") }?.sortedByDescending { it.name } ?: emptyList()

    companion object {
        fun defaultName(nowMs: Long): String =
            "clip_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(nowMs)) + ".jsonl"
    }
}
