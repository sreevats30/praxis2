package com.companion.yogawellness.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.data.db.WellnessDatabase
import com.companion.yogawellness.pose.BodySide
import com.companion.yogawellness.pose.GravityAligner
import com.companion.yogawellness.pose.PoseFrame
import com.companion.yogawellness.pose.PosePipeline
import com.companion.yogawellness.pose.SensorSnapshot
import com.companion.yogawellness.replay.SyntheticPoseBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression for the reported bug: a short, low-scoring hold vanished. On the pre-Phase-2 code a
 * 4 s shallow Warrior II (locked ~3-4 s, < 5 s session rule) saved 0 sessions; it must now be
 * recorded with its real score while the session is still running.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionRecordingE2ETest {

    @Test
    fun shallowFourSecondWarriorIsRecordedWithItsRealScore() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<YogaWellnessApp>()
        val db = Room.inMemoryDatabaseBuilder(app, WellnessDatabase::class.java).build()
        val repo = WellnessRepository(db, app.preferences, emptyList())
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val wallStart = 1_700_000_000_000L
        var now = wallStart
        val recorder = SessionAutoRecorder(repo, scope, clock = { now })
        val pipeline = PosePipeline()
        val b = SyntheticPoseBuilder()
        val sensors = SensorSnapshot(GravityAligner.checkPhoneTilt(0f, 9.81f, 0f), 0.02)
        var t = 0L

        // Exactly what PracticeViewModel.process(record = true) does per live frame
        fun live(frame: (Long) -> PoseFrame, seconds: Double) = repeat((seconds * 25).toInt()) {
            t += 40
            now = wallStart + t
            pipeline.process(frame(t), sensors)
            recorder.tick({ pipeline.session.segments() }, pipeline.toleranceProfile.name)
        }

        live({ b.tadasana(it) }, 6.0)                                   // calibrates (not scored)
        live({ b.warriorII(it, BodySide.LEFT, 125.0) }, 4.0)             // shallow front knee, red
        recorder.awaitPendingWrites()
        assertEquals("checkpointed while still practising", 1, db.sessions().all(ProfileIds.MY_ACCOUNT).size)

        live({ b.tadasana(it) }, 1.6)                                    // release
        recorder.finalizeAndRotate(pipeline.session.segments(), pipeline.toleranceProfile.name) // Finish & Save
        recorder.awaitPendingWrites()

        val sessions = db.sessions().all(ProfileIds.MY_ACCOUNT)
        assertEquals(1, sessions.size)
        val poses = db.sessions().posesFor(sessions.single().id)
        assertEquals(1, poses.size)
        val p = poses.single()
        assertEquals("VIRABHADRASANA_II", p.asana)
        assertTrue("lockedSec=${p.lockedSec}", p.lockedSec >= 2.0)
        assertTrue("meanScore=${p.meanScore}", p.meanScore!! < 75.0)
        assertTrue("qualitySec=${p.qualitySec}", p.qualitySec < 0.5)
        assertEquals("Front Knee", p.worstJoint) // spec name in AsanaSpecs
        assertNotNull(p.topCue)
        assertEquals("Bend your front knee deeper toward 90 degrees", p.topCue)
        assertTrue(p.startedAtMs > wallStart)
        assertEquals(p.lockedSec, sessions.single().activeSec, 1e-9)
        scope.cancel()
        db.close()
    }
}
