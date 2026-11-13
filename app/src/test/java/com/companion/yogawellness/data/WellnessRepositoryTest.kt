package com.companion.yogawellness.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.data.db.WellnessDatabase
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.PoseSegmentStats
import com.companion.yogawellness.scoring.PoseSide
import com.companion.yogawellness.wellness.MockPersonaSeeder
import com.companion.yogawellness.wellness.Persona
import com.companion.yogawellness.wellness.QuestionnaireType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Runs the real Room DAOs (SQLite) on the JVM via Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WellnessRepositoryTest {

    private lateinit var app: YogaWellnessApp
    private lateinit var db: WellnessDatabase
    private lateinit var repo: WellnessRepository
    private lateinit var clipsDir: File

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, WellnessDatabase::class.java).allowMainThreadQueries().build()
        clipsDir = File(app.filesDir, "recordings-test").apply { mkdirs(); File(this, "clip.jsonl").writeText("{}") }
        repo = WellnessRepository(db, app.preferences, listOf(clipsDir))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun checkInIsUpsertedPerDay() = runTest {
        repo.saveCheckIn(100, mood = 5, stress = 7, sleep = 6, preTension = 7, postTension = 4)
        repo.saveCheckIn(100, mood = 8, stress = 3, sleep = 7, preTension = null, postTension = null)
        val all = db.checkIns().all(ProfileIds.MY_ACCOUNT)
        assertEquals(1, all.size)
        assertEquals(8, all.single().mood)
        assertEquals(8, repo.observeCheckIn(100, ProfileIds.MY_ACCOUNT).first()!!.mood)
    }

    @Test
    fun sessionWithPosesIsSavedAndCascadeDeleted() = runTest {
        val segs = listOf(
            PoseSegmentStats(AsanaLabel(Asana.VRIKSHASANA, PoseSide.LEFT_LIFTED), 20.0, 400, 85.0, 12.0, 15.0, 90.0),
            PoseSegmentStats(AsanaLabel(Asana.VRIKSHASANA, PoseSide.RIGHT_LIFTED), 20.0, 400, 80.0, 9.0, 11.0, 88.0)
        )
        val id = repo.saveSession(segs, 0, 60_000, 100, "STANDARD")
        assertNotNull(id)
        assertEquals(2, db.sessions().posesFor(id!!).size)
        assertEquals(2, repo.observePosesSince(0, ProfileIds.MY_ACCOUNT).first().size)
        assertNull(repo.saveSession(emptyList(), 0, 1, 100, "STANDARD"))
    }

    @Test
    fun questionnaireLatestAndHistory() = runTest {
        repo.saveQuestionnaire(QuestionnaireType.PHQ9, 100, List(9) { 1 })
        repo.saveQuestionnaire(QuestionnaireType.PHQ9, 114, List(9) { 0 })
        repo.saveQuestionnaire(QuestionnaireType.GAD7, 114, List(7) { 2 })
        assertEquals(0, repo.observeLatest(QuestionnaireType.PHQ9, ProfileIds.MY_ACCOUNT).first()!!.totalScore)
        assertEquals(listOf(9, 0), repo.observeHistory(QuestionnaireType.PHQ9, ProfileIds.MY_ACCOUNT).first().map { it.totalScore })
        assertEquals(14, repo.observeLatest(QuestionnaireType.GAD7, ProfileIds.MY_ACCOUNT).first()!!.totalScore)
    }

    @Test
    fun personaIsAddedBesideMyDataAndDeleteAllWipesEverything() = runTest {
        // v1.1: seeding a persona no longer replaces the user's data (it used clearAllTables()).
        repo.saveCheckIn(5, 1, 1, 1, null, null)
        val data = MockPersonaSeeder.generate(Persona.A_CONSISTENT, 20_000L)
        repo.seedPersona(data)
        val a = Persona.A_CONSISTENT.profileId
        assertEquals(data.checkIns.size, db.checkIns().all(a).size)
        assertEquals(data.sessions.size, db.sessions().all(a).size)
        assertEquals(6, db.questionnaires().all(a).size)
        assertEquals(1, db.checkIns().all(ProfileIds.MY_ACCOUNT).size)

        app.preferences.consentAcceptedAtMs = 123L
        repo.deleteAllData()
        for (pid in listOf(ProfileIds.MY_ACCOUNT) + ProfileIds.DEMO_IDS) {
            assertTrue(db.checkIns().all(pid).isEmpty())
            assertTrue(db.sessions().all(pid).isEmpty())
            assertTrue(db.questionnaires().all(pid).isEmpty())
        }
        assertFalse(clipsDir.exists())
        assertFalse(app.preferences.hasConsent)
    }
}
