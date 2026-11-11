package com.companion.yogawellness.replay

import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.feedback.CoachEngine
import com.companion.yogawellness.feedback.CoachUtterance
import com.companion.yogawellness.pose.PipelineEvent
import com.companion.yogawellness.pose.PipelineOutput
import com.companion.yogawellness.pose.PosePipeline
import com.companion.yogawellness.scoring.ToleranceProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.Reader
import java.util.Locale

data class TimedUtterance(val tSec: Double, val utterance: CoachUtterance)

/** One contiguous locked-pose segment of a replay. */
data class LockSegment(
    val label: AsanaLabel,
    val startSec: Double,
    val endSec: Double,
    val minScore: Double?,
    val meanScore: Double?,
    val maxScore: Double?,
    val bestStrictHoldSec: Double,
    val bestGraceHoldSec: Double,
    val meanStability: Double?
)

data class ReplayReport(
    val clipName: String,
    val frameCount: Int,
    val durationSec: Double,
    val outputs: List<PipelineOutput>,
    val utterances: List<TimedUtterance>,
    val segments: List<LockSegment>
) {
    val praiseCount: Int get() = outputs.sumOf { o -> o.events.count { it is PipelineEvent.Praise } }
    val safetyFrames: Int get() = outputs.count { it.safety != null }

    fun summary(): String = buildString {
        fun f(v: Double?) = v?.let { String.format(Locale.US, "%.1f", it) } ?: "–"
        appendLine("Replay of $clipName: $frameCount frames, ${f(durationSec)} s")
        if (segments.isEmpty()) appendLine("  (no pose locked)")
        segments.forEach {
            appendLine(
                "  ${f(it.startSec)}–${f(it.endSec)} s  ${it.label}: score min/mean/max ${f(it.minScore)}/${f(it.meanScore)}/${f(it.maxScore)}" +
                    ", best hold ${f(it.bestGraceHoldSec)} s (strict ${f(it.bestStrictHoldSec)} s), stability ${f(it.meanStability)}"
            )
        }
        appendLine("  praise events: $praiseCount, frames with a safety alert: $safetyFrames")
        utterances.forEach { appendLine("  [${f(it.tSec)} s] ${it.utterance.kind}: ${it.utterance.text}") }
    }
}

/**
 * Replays recorded `.jsonl` clips through the exact live pipeline (§10 Prompt 3).
 */
object LandmarkReplayer {

    fun load(file: File): PoseClip = file.reader().use { parse(file.nameWithoutExtension, it) }

    fun parse(name: String, reader: Reader): PoseClip {
        var createdAt: Long? = null
        val frames = ArrayList<ClipFrame>()
        reader.buffered().lineSequence().forEachIndexed { i, line ->
            if (i == 0) createdAt = PoseClipCodec.headerCreatedAt(line)
            PoseClipCodec.decode(line)?.let { frames += it }
        }
        return PoseClip(name, createdAt, frames)
    }

    /** Processes every frame as fast as possible (for tests and threshold tuning). */
    fun replayAll(
        clip: PoseClip,
        pipeline: PosePipeline = PosePipeline(ToleranceProfile.STANDARD),
        coach: CoachEngine? = CoachEngine()
    ): ReplayReport {
        pipeline.reset()
        coach?.reset()
        val outputs = ArrayList<PipelineOutput>(clip.frames.size)
        val spoken = ArrayList<TimedUtterance>()
        val t0 = clip.frames.firstOrNull()?.frame?.timestampSec ?: 0.0
        for (cf in clip.frames) {
            val out = pipeline.process(cf.frame, cf.sensors)
            outputs += out
            coach?.update(out)?.let { spoken += TimedUtterance(out.frame.timestampSec - t0, it) }
        }
        return ReplayReport(clip.name, clip.frames.size, clip.durationSec, outputs, spoken, segments(outputs, t0))
    }

    /** Emits frames with their original timing (for on-screen replay in the app). */
    fun realtime(clip: PoseClip): Flow<ClipFrame> = flow {
        var prev = -1L
        for (cf in clip.frames) {
            if (prev >= 0) delay((cf.frame.timestampMs - prev).coerceIn(0L, 500L))
            prev = cf.frame.timestampMs
            emit(cf)
        }
    }

    private fun segments(outputs: List<PipelineOutput>, t0: Double): List<LockSegment> {
        val result = ArrayList<LockSegment>()
        var i = 0
        while (i < outputs.size) {
            val label = outputs[i].lockedLabel
            if (label == null) { i++; continue }
            var j = i
            while (j + 1 < outputs.size && outputs[j + 1].lockedLabel == label) j++
            val seg = outputs.subList(i, j + 1)
            val scores = seg.mapNotNull { it.score }
            val stab = seg.mapNotNull { it.sway?.stability }
            result += LockSegment(
                label,
                seg.first().frame.timestampSec - t0,
                seg.last().frame.timestampSec - t0,
                scores.minOrNull(), scores.takeIf { it.isNotEmpty() }?.average(), scores.maxOrNull(),
                seg.maxOf { it.hold.bestStrictSec }, seg.maxOf { it.hold.bestGraceSec },
                stab.takeIf { it.isNotEmpty() }?.average()
            )
            i = j + 1
        }
        return result
    }
}
