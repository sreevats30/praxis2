package com.companion.yogawellness.wellness

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.data.ProfileIds
import com.companion.yogawellness.data.WellnessRepository
import com.companion.yogawellness.data.db.PracticeSessionEntity
import com.companion.yogawellness.data.db.WellnessDatabase
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.PoseSegmentStats
import com.companion.yogawellness.scoring.PoseSide
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Manual entries count as practice minutes but never touch alignment analytics. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ManualSessionTest {

    private lateinit var db: WellnessDatabase
    private lateinit var repo: WellnessRepository

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<YogaWellnessApp>()
        db = Room.inMemoryDatabaseBuilder(app, WellnessDatabase::class.java).build()
        repo = WellnessRepository(db, app.preferences, emptyList())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun manualMinutesCountButQualityIsUnchanged() = runBlocking {
        val today = LocalDate.now().toEpochDay()
        val id = repo.logManualSession(30)
        val manual = db.sessions().all(ProfileIds.MY_ACCOUNT).single()
        assertEquals(PracticeSessionEntity.SOURCE_MANUAL, manual.source)
        assertEquals(1800.0, manual.activeSec, 0.0)
        assertNull(manual.meanScore)
        assertEquals(today, manual.epochDay)
        assertTrue(db.sessions().posesFor(id).isEmpty()) // no invented pose rows

        val manualOnly = DailyAggregator.combine(emptyList(), listOf(manual), today, today).single()
        assertEquals(30.0, manualOnly.practiceMinutes, 1e-9)
        assertNull(manualOnly.practiceQuality)

        // Add a real 60 s live session at 80% on the same day: quality stays 80, minutes add up.
        repo.upsertLiveSession(
            null, listOf(PoseSegmentStats(AsanaLabel(Asana.UTKATASANA, PoseSide.SYMMETRIC), 60.0, 1500, 80.0, 20.0, 25.0, 90.0)),
            emptyMap(), 1L, 2L, today, "STANDARD"
        )
        val both = DailyAggregator.combine(emptyList(), db.sessions().all(ProfileIds.MY_ACCOUNT), today, today).single()
        assertEquals(31.0, both.practiceMinutes, 1e-9)
        assertEquals(80.0, both.practiceQuality!!, 1e-9)

        val summary = HistoryAggregator.summary(
            HistoryAggregator.group(db.sessions().all(ProfileIds.MY_ACCOUNT), emptyList())
        )
        assertEquals(31.0, summary.totalMinutes, 1e-9)
        assertEquals(80.0, summary.meanAlignment!!, 1e-9) // the manual entry has no score to dilute it
    }

    @Test(expected = IllegalArgumentException::class)
    fun outOfRangeMinutesAreRejected() {
        runBlocking { repo.logManualSession(0) }
    }

    @Test
    fun durationFormatting() {
        assertEquals("42s", HistoryAggregator.formatDuration(42.0))
        assertEquals("3m 20s", HistoryAggregator.formatDuration(200.0))
        assertEquals("30m", HistoryAggregator.formatDuration(1800.0))
    }
}
