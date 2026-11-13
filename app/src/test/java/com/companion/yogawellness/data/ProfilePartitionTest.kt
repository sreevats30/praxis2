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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Demo personas live beside the user's data and can never change it (Phase 1). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfilePartitionTest {

    private lateinit var app: YogaWellnessApp
    private lateinit var db: WellnessDatabase
    private lateinit var repo: WellnessRepository
    private val today = 20_000L
    private val me = ProfileIds.MY_ACCOUNT

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, WellnessDatabase::class.java).allowMainThreadQueries().build()
        repo = WellnessRepository(db, app.preferences, emptyList())
    }

    @After
    fun tearDown() = db.close()

    private suspend fun saveMyData() {
        repo.saveCheckIn(today, mood = 6, stress = 5, sleep = 7, preTension = 6, postTension = 3)
        repo.saveSession(
            listOf(PoseSegmentStats(AsanaLabel(Asana.VIRABHADRASANA_II, PoseSide.FRONT_LEFT), 30.0, 700, 72.0, 8.0, 11.0, 90.0)),
            1_000L, 61_000L, today, "STANDARD"
        )
        repo.saveQuestionnaire(QuestionnaireType.PHQ9, today, List(9) { if (it == 0) 2 else 0 })
    }

    /** Everything stored for a profile, as comparable values (ids included). */
    private suspend fun snapshot(pid: String): List<Any> {
        val sessions = db.sessions().all(pid)
        return listOf(
            db.checkIns().all(pid),
            sessions,
            sessions.flatMap { db.sessions().posesFor(it.id) },
            db.questionnaires().all(pid)
        )
    }

    @Test
    fun seedingDemosNeverChangesMyAccount() = runTest {
        saveMyData()
        val before = snapshot(me)
        repo.seedPersona(MockPersonaSeeder.generate(Persona.A_CONSISTENT, today))
        repo.seedPersona(MockPersonaSeeder.generate(Persona.B_EXAM_GAP, today))
        assertEquals(before, snapshot(me))
        assertEquals(1, db.checkIns().all(me).size)
        assertEquals(1, db.sessions().all(me).size)
        assertTrue(db.sessions().all(ProfileIds.PERSONA_A).size > 20)
    }

    @Test
    fun seedingTwiceIsIdempotent() = runTest {
        val data = MockPersonaSeeder.generate(Persona.A_CONSISTENT, today)
        repo.seedPersona(data)
        val once = listOf(
            db.checkIns().count(ProfileIds.PERSONA_A), db.sessions().count(ProfileIds.PERSONA_A),
            db.questionnaires().all(ProfileIds.PERSONA_A).size
        )
        repo.seedPersona(data)
        assertEquals(once, listOf(
            db.checkIns().count(ProfileIds.PERSONA_A), db.sessions().count(ProfileIds.PERSONA_A),
            db.questionnaires().all(ProfileIds.PERSONA_A).size
        ))
        assertEquals(listOf(data.checkIns.size, data.sessions.size, data.questionnaires.size), once)
    }

    @Test
    fun sameDayCheckInsCoexistAcrossProfiles() = runTest {
        repo.saveCheckIn(today, 6, 5, 7, null, null)
        repo.seedPersona(MockPersonaSeeder.generate(Persona.A_CONSISTENT, today)) // also has a row for `today`
        assertEquals(6, db.checkIns().forDay(today, me)!!.mood)
        val demo = db.checkIns().forDay(today, ProfileIds.PERSONA_A)
        if (demo != null) assertEquals(ProfileIds.PERSONA_A, demo.profileId)
        // Updating my check-in replaces only mine
        repo.saveCheckIn(today, 9, 2, 8, null, null)
        assertEquals(9, db.checkIns().forDay(today, me)!!.mood)
        assertEquals(demo, db.checkIns().forDay(today, ProfileIds.PERSONA_A))
    }

    @Test
    fun everyObserveQueryReturnsOnlyItsProfile() = runTest {
        saveMyData()
        repo.seedPersona(MockPersonaSeeder.generate(Persona.C_NULL, today))
        val c = ProfileIds.PERSONA_C
        val from = today - 41

        assertTrue(repo.observeCheckInsSince(from, me).first().all { it.profileId == me })
        assertTrue(repo.observeCheckInsSince(from, c).first().all { it.profileId == c })
        assertEquals(1, repo.observeCheckInsSince(from, me).first().size)

        assertEquals(1, repo.observeSessionsSince(from, me).first().size)
        assertTrue(repo.observeSessionsSince(from, c).first().all { it.profileId == c })
        assertEquals(1, repo.observeAllSessions(me).first().size)

        val mySessionIds = db.sessions().all(me).map { it.id }.toSet()
        assertTrue(repo.observePosesSince(from, me).first().all { it.sessionId in mySessionIds })
        assertTrue(repo.observeAllPoses(me).first().all { it.sessionId in mySessionIds })
        assertTrue(repo.observeAllPoses(c).first().none { it.sessionId in mySessionIds })

        assertEquals(listOf(2), repo.observeHistory(QuestionnaireType.PHQ9, me).first().map { it.totalScore })
        assertEquals(2, repo.observeLatest(QuestionnaireType.PHQ9, me).first()!!.totalScore)
        assertEquals(3, repo.observeHistory(QuestionnaireType.PHQ9, c).first().size)
        assertEquals(6, repo.observeCheckIn(today, me).first()!!.mood)
    }

    @Test
    fun removeAllDemoDataKeepsMineButDeleteAllEmptiesEverything() = runTest {
        saveMyData()
        Persona.entries.forEach { repo.seedPersona(MockPersonaSeeder.generate(it, today)) }
        val mine = snapshot(me)
        repo.removeAllDemoData()
        assertEquals(mine, snapshot(me))
        ProfileIds.DEMO_IDS.forEach { pid ->
            assertEquals(0, db.sessions().count(pid))
            assertEquals(0, db.checkIns().count(pid))
            assertTrue(db.questionnaires().all(pid).isEmpty())
        }
        repo.seedPersona(MockPersonaSeeder.generate(Persona.B_EXAM_GAP, today))
        repo.deleteAllData()
        (listOf(me) + ProfileIds.DEMO_IDS).forEach { pid ->
            assertEquals(0, db.sessions().count(pid))
            assertEquals(0, db.checkIns().count(pid))
        }
    }

    @Test
    fun clearDemoSessionsCannotDeleteMyAccount() = runTest {
        saveMyData()
        db.sessions().clearDemoSessions(me)
        db.checkIns().clearDemoCheckIns(me)
        db.questionnaires().clearDemoQuestionnaires(me)
        assertEquals(1, db.sessions().count(me))
        assertEquals(1, db.checkIns().count(me))
        assertEquals(1, db.questionnaires().all(me).size)
    }

    @Test
    fun switcherSeedsOnFirstSelectAndActiveProfileStartsOnMyAccount() = runTest {
        val active = ActiveProfile()
        assertEquals(me, active.id.value)
        val switcher = ProfileSwitcher(repo, active) { today }
        switcher.select(ProfileIds.PERSONA_B)
        assertEquals(ProfileIds.PERSONA_B, active.id.value)
        assertTrue(repo.hasData(ProfileIds.PERSONA_B))
        active.returnToMyAccount()
        assertEquals(me, active.id.value)
        switcher.removeAllDemoData()
        assertTrue(!repo.hasData(ProfileIds.PERSONA_B))
    }
}
