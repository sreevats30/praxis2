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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * "Press Home mid-session, the process is killed, relaunch": the ON_STOP checkpoint
 * (PracticeScreen → vm.checkpointNow()) must leave the session on disk with no finalize call.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionSurvivesRestartTest {

    @Test
    fun checkpointedSessionIsOnDiskAfterProcessDeath() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<YogaWellnessApp>()
        val file = app.getDatabasePath("restart-test.db").apply { parentFile?.mkdirs(); delete() }
        val tree = AsanaLabel(Asana.VRIKSHASANA, PoseSide.LEFT_LIFTED)
        val segs = listOf(PoseSegmentStats(tree, 7.5, 180, 83.0, 5.0, 6.0, 91.0, qualitySec = 6.0))

        // Process 1: practising, then Home → ON_STOP → checkpointNow; then the process dies.
        val db1 = Room.databaseBuilder(app, WellnessDatabase::class.java, file.absolutePath)
            .addMigrations(WellnessDatabase.MIGRATION_1_2).build()
        val scope1 = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val recorder1 = SessionAutoRecorder(WellnessRepository(db1, app.preferences, emptyList()), scope1)
        recorder1.checkpointNow(segs, "STANDARD")
        recorder1.awaitPendingWrites()
        scope1.cancel()
        db1.close() // no finalizeAndRotate: simulates the process being killed in the background

        // Process 2: relaunch
        val db2 = Room.databaseBuilder(app, WellnessDatabase::class.java, file.absolutePath)
            .addMigrations(WellnessDatabase.MIGRATION_1_2).build()
        val sessions = db2.sessions().all(ProfileIds.MY_ACCOUNT)
        assertEquals(1, sessions.size)
        assertEquals(7.5, sessions.single().activeSec, 1e-9)
        assertEquals(6.0, db2.sessions().posesFor(sessions.single().id).single().qualitySec, 1e-9)
        db2.close()
    }
}
