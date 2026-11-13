package com.companion.yogawellness.wellness

import com.companion.yogawellness.data.db.PoseRecordEntity
import com.companion.yogawellness.data.db.PracticeSessionEntity

data class HistorySummary(
    val sessions: Int,
    val totalMinutes: Double,
    /** All-time best (grace-adjusted) hold of any pose, if any live pose exists. */
    val bestHoldSec: Double?,
    /** Time-weighted mean alignment over scored (live) sessions. */
    val meanAlignment: Double?
)

/** Pure helpers behind the History screen. */
object HistoryAggregator {

    /** Sessions newest first, each with its pose rows in the order they were first held. */
    fun group(sessions: List<PracticeSessionEntity>, poses: List<PoseRecordEntity>): List<SessionWithPoses> {
        val bySession = poses.groupBy { it.sessionId }
        return sessions.sortedByDescending { it.startedAtMs }
            .map { s -> SessionWithPoses(s, bySession[s.id].orEmpty().sortedBy { it.startedAtMs }) }
    }

    fun summary(items: List<SessionWithPoses>): HistorySummary {
        val scored = items.map { it.session }.filter { it.meanScore != null && it.activeSec > 0 }
        return HistorySummary(
            sessions = items.size,
            totalMinutes = items.sumOf { it.session.activeSec } / 60.0,
            bestHoldSec = items.flatMap { it.poses }.maxOfOrNull { it.bestGraceHoldSec },
            meanAlignment = if (scored.isEmpty()) null
            else scored.sumOf { it.meanScore!! * it.activeSec } / scored.sumOf { it.activeSec }
        )
    }

    /** Time-weighted (by locked seconds) mean stability of a session's poses, or null. */
    fun stability(poses: List<PoseRecordEntity>): Double? {
        val withStab = poses.filter { it.meanStability != null && it.lockedSec > 0 }
        if (withStab.isEmpty()) return null
        return withStab.sumOf { it.meanStability!! * it.lockedSec } / withStab.sumOf { it.lockedSec }
    }

    /** "42s", "3m 20s", "30m". */
    fun formatDuration(seconds: Double): String {
        val total = Math.round(seconds).toInt().coerceAtLeast(0)
        val m = total / 60
        val s = total % 60
        return when {
            m == 0 -> "${s}s"
            s == 0 -> "${m}m"
            else -> "${m}m ${s}s"
        }
    }
}
