package com.companion.yogawellness.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.data.db.WellnessDatabase
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.PoseSegmentStats
import com.companion.yogawellness.scoring.PoseSide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionAutoRecorderTest {

    private lateinit var db: WellnessDatabase
    private lateinit var repo: WellnessRepository
    /** The recorder's own scope (stands in for YogaWellnessApp.appScope). */
    private val recorderScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var now = 1_000_000L
    private val warrior = AsanaLabel(Asana.VIRABHADRASANA_II, PoseSide.FRONT_LEFT)
    private val tree = AsanaLabel(Asana.VRIKSHASANA, PoseSide.RIGHT_LIFTED)
    private val logged = mutableListOf<String>()

    private fun seg(label: AsanaLabel, locked: Double, score: Double = 52.0) =
        PoseSegmentStats(label, locked, (locked * 25).toInt(), score, 0.0, 0.0, 80.0, peakScore = score)

    private fun recorder(store: LiveSessionStore = repo) =
        SessionAutoRecorder(store, recorderScope, clock = { now }, sampleEveryMs = 0, log = { m, _ -> logged += m })

    private fun sessions() = runBlocking { db.sessions().all(ProfileIds.MY_ACCOUNT) }

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<YogaWellnessApp>()
        db = Room.inMemoryDatabaseBuilder(app, WellnessDatabase::class.java).build()
        repo = WellnessRepository(db, app.preferences, emptyList())
    }

    @After
    fun tearDown() {
        recorderScope.cancel()
        db.close()
    }

    @Test
    fun nothingBeforeTwoSecondsThenOneRowThatIsUpdated() = runBlocking {
        val r = recorder()
        r.tick({ listOf(seg(warrior, 1.0)) }, "STANDARD")
        r.awaitPendingWrites()
        assertTrue("no empty or too-short session is written", sessions().isEmpty())

        now += 3_000
        r.tick({ listOf(seg(warrior, 2.5)) }, "STANDARD")
        r.awaitPendingWrites()
        assertEquals(1, sessions().size)
        assertEquals(2.5, sessions().single().activeSec, 1e-9)

        now += 6_000
        r.tick({ listOf(seg(warrior, 8.0), seg(tree, 2.2)) }, "STANDARD")
        r.awaitPendingWrites()
        val s = sessions().single() // still one row, updated in place
        assertEquals(10.2, s.activeSec, 1e-9)
        assertEquals(now, s.endedAtMs)
        assertEquals(2, db.sessions().posesFor(s.id).size) // pose rows replaced, not duplicated
        assertEquals(2, r.rec.value.posesSaved)
        assertTrue(r.rec.value.active)
    }

    @Test
    fun checkpointsAreThrottled() = runBlocking {
        val r = recorder()
        r.tick({ listOf(seg(warrior, 2.0)) }, "STANDARD") // first qualifying tick writes at once
        r.awaitPendingWrites()
        now += 1_000
        r.tick({ listOf(seg(warrior, 3.0)) }, "STANDARD") // < 5 s later: no write
        r.awaitPendingWrites()
        assertEquals(2.0, sessions().single().activeSec, 1e-9)
        now += 5_000
        r.tick({ listOf(seg(warrior, 8.0)) }, "STANDARD")
        r.awaitPendingWrites()
        assertEquals(8.0, sessions().single().activeSec, 1e-9)
    }

    @Test
    fun finalizeThenMoreActivityCreatesASecondSession() = runBlocking {
        val r = recorder()
        r.tick({ listOf(seg(warrior, 4.0)) }, "STANDARD")
        val receipt = r.finalizeAndRotate(listOf(seg(warrior, 6.0)), "STANDARD")
        assertNotNull(receipt)
        assertEquals(6.0, receipt!!.totalSec, 1e-9)
        assertEquals("Virabhadrasana II (Warrior II)", receipt.poses.single().name)
        assertEquals("Left leg forward", receipt.poses.single().sideLabel)
        assertEquals(SessionAutoRecorder.Rec(), r.rec.value) // badge resets
        r.awaitPendingWrites()
        assertEquals(1, sessions().size)
        assertEquals(6.0, sessions().single().activeSec, 1e-9)

        now += 60_000
        r.tick({ listOf(seg(tree, 3.0)) }, "STANDARD")
        r.awaitPendingWrites()
        assertEquals(2, sessions().size)
        assertEquals(listOf(6.0, 3.0), sessions().map { it.activeSec })
    }

    @Test
    fun finalizeWithNothingQualifyingWritesNothing() = runBlocking {
        val r = recorder()
        assertNull(r.finalizeAndRotate(listOf(seg(warrior, 1.9)), "STANDARD"))
        r.awaitPendingWrites()
        assertTrue(sessions().isEmpty())
    }

    @Test
    fun aStoreFailureIsSwallowedAndTheNextTickSucceeds() = runBlocking {
        val failures = AtomicInteger(1)
        val flaky = object : LiveSessionStore {
            override suspend fun upsertLiveSession(
                sessionId: Long?, segments: List<PoseSegmentStats>, poseStartedAtMs: Map<AsanaLabel, Long>,
                startedAtMs: Long, endedAtMs: Long, epochDay: Long, toleranceProfile: String
            ): Long? {
                if (failures.getAndDecrement() > 0) throw IllegalStateException("disk full")
                return repo.upsertLiveSession(sessionId, segments, poseStartedAtMs, startedAtMs, endedAtMs, epochDay, toleranceProfile)
            }
        }
        val r = recorder(flaky)
        r.tick({ listOf(seg(warrior, 3.0)) }, "STANDARD") // must not throw on the camera thread
        r.awaitPendingWrites()
        assertTrue(sessions().isEmpty())
        assertEquals(1, logged.size)
        now += 200 // well before the next 5 s checkpoint: the failed write is retried at once
        r.tick({ listOf(seg(warrior, 3.2)) }, "STANDARD")
        r.awaitPendingWrites()
        assertEquals(1, sessions().size)
    }

    @Test
    fun cancellingTheCallersScopeDoesNotCancelTheWrite() = runBlocking {
        val r = recorder()
        val callerScope = CoroutineScope(Job() + Dispatchers.Default) // e.g. a ViewModel scope
        callerScope.launch { r.finalizeAndRotate(listOf(seg(warrior, 5.0)), "STANDARD") }.join()
        callerScope.cancel()
        r.awaitPendingWrites()
        assertEquals(1, sessions().size)
    }
}

/**
 * Deterministic regression for an ordering race found in testing: two writes launched in order can
 * acquire the mutex in reverse order on a multi-threaded dispatcher. Here a dispatcher runs queued
 * tasks newest-first, forcing that inversion; the stale (older) snapshot must not overwrite the
 * newer one. Plain JVM, fake store.
 */
class SessionAutoRecorderOrderingTest {

    private class LifoExecutor : java.util.concurrent.Executor {
        val queue = ArrayDeque<Runnable>()
        override fun execute(command: Runnable) { queue.addLast(command) }
        fun runAllNewestFirst() { while (queue.isNotEmpty()) queue.removeLast().run() }
    }

    private class FakeStore : LiveSessionStore {
        val rows = linkedMapOf<Long, Double>()
        override suspend fun upsertLiveSession(
            sessionId: Long?, segments: List<PoseSegmentStats>, poseStartedAtMs: Map<AsanaLabel, Long>,
            startedAtMs: Long, endedAtMs: Long, epochDay: Long, toleranceProfile: String
        ): Long? {
            val active = com.companion.yogawellness.wellness.SessionSummarizer.qualifying(segments).sumOf { it.lockedSec }
            val id = sessionId ?: (rows.size + 1L)
            rows[id] = active
            return id
        }
    }

    @Test
    fun olderSnapshotNeverOverwritesNewer() {
        val exec = LifoExecutor()
        val store = FakeStore()
        val scope = CoroutineScope(SupervisorJob() + exec.asCoroutineDispatcher())
        val warrior = AsanaLabel(Asana.VIRABHADRASANA_II, PoseSide.FRONT_LEFT)
        fun seg(locked: Double) = PoseSegmentStats(warrior, locked, 1, 50.0, 0.0, 0.0, null)
        val r = SessionAutoRecorder(store, scope, clock = { 0L }, sampleEveryMs = 0, log = { _, _ -> })

        r.tick({ listOf(seg(4.0)) }, "STANDARD")                    // write #1 (older)
        r.finalizeAndRotate(listOf(seg(6.0)), "STANDARD")          // write #2 (newer)
        exec.runAllNewestFirst()                                   // #2 runs first, then #1

        assertEquals(mapOf(1L to 6.0), store.rows)
        scope.cancel()
    }
}
