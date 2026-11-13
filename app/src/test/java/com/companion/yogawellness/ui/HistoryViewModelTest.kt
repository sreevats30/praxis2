package com.companion.yogawellness.ui

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.data.ActiveProfile
import com.companion.yogawellness.data.ProfileIds
import com.companion.yogawellness.data.WellnessRepository
import com.companion.yogawellness.data.db.WellnessDatabase
import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.scoring.PoseSegmentStats
import com.companion.yogawellness.scoring.PoseSide
import com.companion.yogawellness.ui.history.HistoryUiState
import com.companion.yogawellness.ui.history.HistoryViewModel
import com.companion.yogawellness.wellness.MockPersonaSeeder
import com.companion.yogawellness.wellness.Persona
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HistoryViewModelTest {

    private lateinit var db: WellnessDatabase
    private lateinit var repo: WellnessRepository
    private lateinit var active: ActiveProfile
    private lateinit var vm: HistoryViewModel
    private val tree = AsanaLabel(Asana.VRIKSHASANA, PoseSide.LEFT_LIFTED)
    private val warrior = AsanaLabel(Asana.VIRABHADRASANA_II, PoseSide.FRONT_LEFT)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = ApplicationProvider.getApplicationContext<YogaWellnessApp>()
        db = Room.inMemoryDatabaseBuilder(app, WellnessDatabase::class.java).build()
        repo = WellnessRepository(db, app.preferences, emptyList())
        active = ActiveProfile()
        vm = HistoryViewModel(repo, active)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun awaitState(predicate: (HistoryUiState) -> Boolean): HistoryUiState =
        runBlocking { withTimeout(10_000) { vm.state.first { !it.loading && predicate(it) } } }

    private fun saveMySession(): Long = runBlocking {
        repo.upsertLiveSession(
            null,
            listOf(
                PoseSegmentStats(tree, 12.0, 300, 86.0, 8.0, 9.1, 92.0, qualitySec = 9.1, worstJoint = "Pelvic Level"),
                PoseSegmentStats(warrior, 4.0, 100, 41.0, 0.0, 0.0, 88.0, topCue = "Bend your front knee deeper toward 90 degrees")
            ),
            emptyMap(), 1_000L, 61_000L, 20_000L, "STANDARD"
        )!!
    }

    @Test
    fun switchingTheActiveProfileSwapsTheList() {
        saveMySession()
        runBlocking { repo.seedPersona(MockPersonaSeeder.generate(Persona.A_CONSISTENT, 20_000L)) }

        val mine = awaitState { it.profileId == ProfileIds.MY_ACCOUNT && it.sessions.isNotEmpty() }
        assertEquals(1, mine.sessions.size)
        assertEquals(2, mine.sessions.single().poses.size)
        assertTrue(mine.canEdit)
        assertEquals(1, mine.summary.sessions)
        assertEquals(9.1, mine.summary.bestHoldSec!!, 1e-9)

        active.select(ProfileIds.PERSONA_A)
        val demo = awaitState { it.profileId == ProfileIds.PERSONA_A }
        assertTrue(demo.sessions.size > 20)
        assertFalse(demo.canEdit)
        assertTrue(demo.sessions.all { it.session.profileId == ProfileIds.PERSONA_A })
        // newest first
        assertEquals(demo.sessions.map { it.session.startedAtMs }.sortedDescending(), demo.sessions.map { it.session.startedAtMs })

        active.returnToMyAccount()
        assertEquals(1, awaitState { it.profileId == ProfileIds.MY_ACCOUNT }.sessions.size)
    }

    @Test
    fun deleteRemovesTheSessionAndItsPoses() {
        val id = saveMySession()
        awaitState { it.sessions.size == 1 }
        vm.deleteSession(id)
        awaitState { it.sessions.isEmpty() }
        runBlocking { assertTrue(db.sessions().posesFor(id).isEmpty()) }
    }

    @Test
    fun deleteOnADemoProfileIsANoOp() {
        runBlocking { repo.seedPersona(MockPersonaSeeder.generate(Persona.B_EXAM_GAP, 20_000L)) }
        active.select(ProfileIds.PERSONA_B)
        val before = awaitState { it.profileId == ProfileIds.PERSONA_B && it.sessions.isNotEmpty() }
        val demoId = before.sessions.first().session.id
        vm.deleteSession(demoId)
        runBlocking {
            repo.deleteSession(demoId) // even straight through the repository: My-Account-only SQL
            assertEquals(before.sessions.size, db.sessions().count(ProfileIds.PERSONA_B))
            assertEquals(before.sessions.first().poses.size, db.sessions().posesFor(demoId).size)
        }
    }
}
