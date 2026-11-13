package com.companion.yogawellness.wellness

import com.companion.yogawellness.data.db.DailyCheckInEntity
import com.companion.yogawellness.data.db.PracticeSessionEntity

/** One calendar day of engagement, for the home screen's week strip. */
data class DayActivity(val epochDay: Long, val practised: Boolean, val checkedIn: Boolean) {
    val active: Boolean get() = practised || checkedIn
}

/**
 * Lightweight home-screen summary (no statistics): this week's practice, an engagement streak
 * and the last 14 days of self-rated mood and stress for sparklines. Pure Kotlin.
 */
data class ActivitySummary(
    /** Consecutive active days ending today — or yesterday, so the streak isn't "lost" before today's practice. */
    val streakDays: Int,
    val weekMinutes: Double,
    val weekSessions: Int,
    /** Practice-time-weighted mean alignment over the last 7 days (same formula as the dashboard). */
    val weekQuality: Double?,
    /** Last 7 days, oldest first. */
    val last7: List<DayActivity>,
    /** Last 14 days of self-rated mood / stress (1–10), oldest first; null = no check-in that day. */
    val mood14: List<Int?>,
    val stress14: List<Int?>
) {
    companion object {
        /** Days loaded for the streak; a longer streak is shown as "60+". */
        const val LOOKBACK_DAYS = 60

        fun empty(today: Long) = from(emptyList(), emptyList(), today)

        fun from(checkIns: List<DailyCheckInEntity>, sessions: List<PracticeSessionEntity>, today: Long): ActivitySummary {
            val from = today - (LOOKBACK_DAYS - 1)
            val daily = DailyAggregator.combine(checkIns, sessions.filter { it.epochDay in from..today }, from, today)
            val activeDays = daily.map { it.practiceMinutes > 0 || it.mood != null }

            var i = activeDays.lastIndex
            if (i >= 0 && !activeDays[i]) i-- // today not done yet: count up to yesterday
            var streak = 0
            while (i >= 0 && activeDays[i]) { streak++; i-- }

            val last7 = daily.takeLast(7)
            val practised = last7.filter { it.practiceQuality != null && it.practiceMinutes > 0 }
            val quality = if (practised.isEmpty()) null
            else practised.sumOf { it.practiceQuality!! * it.practiceMinutes } / practised.sumOf { it.practiceMinutes }

            val last14 = daily.takeLast(14)
            return ActivitySummary(
                streakDays = streak,
                weekMinutes = last7.sumOf { it.practiceMinutes },
                weekSessions = sessions.count { it.epochDay in (today - 6)..today },
                weekQuality = quality,
                last7 = last7.map { DayActivity(it.epochDay, it.practiceMinutes > 0, it.mood != null) },
                mood14 = last14.map { it.mood },
                stress14 = last14.map { it.stress }
            )
        }
    }
}
