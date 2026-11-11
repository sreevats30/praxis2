package com.companion.yogawellness.wellness

import com.companion.yogawellness.data.db.DailyCheckInEntity
import com.companion.yogawellness.data.db.PoseRecordEntity
import com.companion.yogawellness.data.db.PracticeSessionEntity
import com.companion.yogawellness.scoring.PoseSegmentStats
import kotlin.math.floor

data class DailyRow(
    val epochDay: Long,
    val mood: Int?,
    val stress: Int?,
    val sleep: Int?,
    val practiceMinutes: Double,
    /** Time-weighted mean alignment score of that day's practice, if any. */
    val practiceQuality: Double?
)

data class WeeklyRow(
    val weekStartEpochDay: Long,
    val practiceMinutes: Double,
    val practiceQuality: Double?,
    val meanStress: Double?,
    val meanMood: Double?
)

/** Joins check-ins and practice sessions into one row per calendar day. */
object DailyAggregator {

    fun combine(
        checkIns: List<DailyCheckInEntity>,
        sessions: List<PracticeSessionEntity>,
        fromEpochDay: Long,
        toEpochDay: Long
    ): List<DailyRow> {
        val byDay = checkIns.associateBy { it.epochDay }
        val sessionsByDay = sessions.groupBy { it.epochDay }
        return (fromEpochDay..toEpochDay).map { day ->
            val c = byDay[day]
            val s = sessionsByDay[day].orEmpty()
            val activeSec = s.sumOf { it.activeSec }
            val scored = s.filter { it.meanScore != null && it.activeSec > 0 }
            val quality = if (scored.isEmpty()) null
            else scored.sumOf { it.meanScore!! * it.activeSec } / scored.sumOf { it.activeSec }
            DailyRow(day, c?.mood, c?.stress, c?.sleepQuality, activeSec / 60.0, quality)
        }
    }

    /** Paired series (practice minutes, check-in value) over days that have a check-in. */
    fun minutesVs(rows: List<DailyRow>, value: (DailyRow) -> Int?): Pair<DoubleArray, DoubleArray> {
        val paired = rows.mapNotNull { r -> value(r)?.let { r.practiceMinutes to it.toDouble() } }
        return paired.map { it.first }.toDoubleArray() to paired.map { it.second }.toDoubleArray()
    }

    /** Groups rows into 7-day buckets ending on the last row's day. */
    fun weekly(rows: List<DailyRow>): List<WeeklyRow> {
        if (rows.isEmpty()) return emptyList()
        val last = rows.last().epochDay
        return rows.groupBy { floor((last - it.epochDay) / 7.0).toLong() }
            .toSortedMap(compareByDescending { it })
            .map { (_, week) ->
                val practiced = week.filter { it.practiceQuality != null && it.practiceMinutes > 0 }
                WeeklyRow(
                    weekStartEpochDay = week.minOf { it.epochDay },
                    practiceMinutes = week.sumOf { it.practiceMinutes },
                    practiceQuality = if (practiced.isEmpty()) null
                    else practiced.sumOf { it.practiceQuality!! * it.practiceMinutes } / practiced.sumOf { it.practiceMinutes },
                    meanStress = week.mapNotNull { it.stress }.takeIf { it.isNotEmpty() }?.average(),
                    meanMood = week.mapNotNull { it.mood }.takeIf { it.isNotEmpty() }?.average()
                )
            }
    }
}

/** Turns an in-memory practice session into database rows. */
object SessionSummarizer {
    /** Sessions with less scored time than this are not saved. */
    const val MIN_ACTIVE_SEC = 5.0

    fun toEntities(
        segments: List<PoseSegmentStats>,
        startedAtMs: Long,
        endedAtMs: Long,
        epochDay: Long,
        profile: String
    ): Pair<PracticeSessionEntity, List<PoseRecordEntity>>? {
        val scored = segments.filter { it.label.asana.isScored && it.lockedSec > 0 }
        val active = scored.sumOf { it.lockedSec }
        if (active < MIN_ACTIVE_SEC) return null
        val withScore = scored.filter { it.meanScore != null }
        val mean = if (withScore.isEmpty()) null
        else withScore.sumOf { it.meanScore!! * it.lockedSec } / withScore.sumOf { it.lockedSec }
        val session = PracticeSessionEntity(
            epochDay = epochDay, startedAtMs = startedAtMs, endedAtMs = endedAtMs,
            activeSec = active, meanScore = mean, profile = profile
        )
        val poses = scored.map {
            PoseRecordEntity(
                sessionId = 0, asana = it.label.asana.name, side = it.label.side.name,
                lockedSec = it.lockedSec, meanScore = it.meanScore,
                bestStrictHoldSec = it.bestStrictHoldSec, bestGraceHoldSec = it.bestGraceHoldSec,
                meanStability = it.meanStability
            )
        }
        return session to poses
    }
}
