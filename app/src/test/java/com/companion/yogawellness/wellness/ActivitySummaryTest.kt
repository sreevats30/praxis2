package com.companion.yogawellness.wellness

import com.companion.yogawellness.data.db.DailyCheckInEntity
import com.companion.yogawellness.data.db.PracticeSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivitySummaryTest {

    private val today = 20_000L

    private fun checkIn(day: Long, mood: Int = 6, stress: Int = 4) =
        DailyCheckInEntity(epochDay = day, mood = mood, stress = stress, sleepQuality = 6, updatedAtMs = 0)

    private fun session(day: Long, activeSec: Double = 600.0, score: Double? = 80.0) =
        PracticeSessionEntity(epochDay = day, startedAtMs = 0, endedAtMs = 0, activeSec = activeSec, meanScore = score, profile = "STANDARD")

    @Test
    fun emptyHistoryHasNoStreakAndSevenInactiveDays() {
        val a = ActivitySummary.empty(today)
        assertEquals(0, a.streakDays)
        assertEquals(7, a.last7.size)
        assertTrue(a.last7.none { it.active })
        assertEquals(14, a.mood14.size)
        assertNull(a.weekQuality)
    }

    @Test
    fun streakCountsBackFromTodayAcrossPracticeAndCheckIns() {
        val a = ActivitySummary.from(
            checkIns = listOf(checkIn(today), checkIn(today - 2)),
            sessions = listOf(session(today - 1), session(today - 4)),
            today = today
        )
        // today, -1, -2 active; -3 is the gap
        assertEquals(3, a.streakDays)
    }

    @Test
    fun streakIsKeptWhenTodayIsNotDoneYet() {
        val a = ActivitySummary.from(listOf(checkIn(today - 1), checkIn(today - 2)), emptyList(), today)
        assertEquals(2, a.streakDays)
        assertTrue(!a.last7.last().active)
    }

    @Test
    fun weekTotalsUseOnlyTheLastSevenDaysAndWeightQualityByTime() {
        val a = ActivitySummary.from(
            checkIns = emptyList(),
            sessions = listOf(
                session(today, activeSec = 600.0, score = 90.0),
                session(today - 6, activeSec = 1200.0, score = 60.0),
                session(today - 7, activeSec = 6000.0, score = 10.0) // outside the week
            ),
            today = today
        )
        assertEquals(2, a.weekSessions)
        assertEquals(30.0, a.weekMinutes, 1e-9)
        assertEquals((90.0 * 10 + 60.0 * 20) / 30, a.weekQuality!!, 1e-9)
    }

    @Test
    fun sparklinesAreOldestFirstWithGaps() {
        val a = ActivitySummary.from(listOf(checkIn(today, mood = 9), checkIn(today - 13, mood = 2)), emptyList(), today)
        assertEquals(2, a.mood14.first())
        assertEquals(9, a.mood14.last())
        assertNull(a.mood14[5])
    }
}
