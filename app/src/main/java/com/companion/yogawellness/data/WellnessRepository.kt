package com.companion.yogawellness.data

import androidx.room.withTransaction
import com.companion.yogawellness.classifier.AsanaLabel
import com.companion.yogawellness.data.db.DailyCheckInEntity
import com.companion.yogawellness.data.db.PoseRecordEntity
import com.companion.yogawellness.data.db.PracticeSessionEntity
import com.companion.yogawellness.data.db.QuestionnaireEntity
import com.companion.yogawellness.data.db.WellnessDatabase
import com.companion.yogawellness.scoring.PoseSegmentStats
import com.companion.yogawellness.wellness.PersonaData
import com.companion.yogawellness.wellness.QuestionnaireType
import com.companion.yogawellness.wellness.Questionnaires
import com.companion.yogawellness.wellness.SessionSummarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId

/**
 * Single access point for everything the app stores — all of it on this device only.
 *
 * Profiles: reads take a `profileId` (My Account or a demo persona). Writes of real user data
 * take **no** profile id and always land in [ProfileIds.MY_ACCOUNT], so no screen can write into a
 * demo dataset by accident. Demo data is only ever written by [seedPersona].
 */
class WellnessRepository(
    private val db: WellnessDatabase,
    private val prefs: AppPreferences,
    /** Directories wiped by "Delete All My Data" (pose clips, exported reports). */
    private val privateDirs: List<File>,
    private val clock: () -> Long = System::currentTimeMillis
) : LiveSessionStore {
    // ---- Daily check-in ---------------------------------------------------------------------
    fun observeCheckIn(epochDay: Long, profileId: String): Flow<DailyCheckInEntity?> =
        db.checkIns().observeDay(epochDay, profileId)

    fun observeCheckInsSince(fromEpochDay: Long, profileId: String): Flow<List<DailyCheckInEntity>> =
        db.checkIns().observeSince(fromEpochDay, profileId)

    suspend fun saveCheckIn(
        epochDay: Long, mood: Int, stress: Int, sleep: Int, preTension: Int?, postTension: Int?
    ) {
        db.checkIns().upsert(
            DailyCheckInEntity(
                epochDay = epochDay, mood = mood, stress = stress, sleepQuality = sleep,
                preTension = preTension, postTension = postTension, updatedAtMs = clock(),
                profileId = ProfileIds.MY_ACCOUNT
            )
        )
    }

    // ---- Practice sessions --------------------------------------------------------------------
    fun observeSessionsSince(fromEpochDay: Long, profileId: String): Flow<List<PracticeSessionEntity>> =
        db.sessions().observeSince(fromEpochDay, profileId)

    fun observePosesSince(fromEpochDay: Long, profileId: String): Flow<List<PoseRecordEntity>> =
        db.sessions().observePosesSince(fromEpochDay, profileId)

    /** Every session of a profile, newest first. */
    fun observeAllSessions(profileId: String): Flow<List<PracticeSessionEntity>> = db.sessions().observeAllSessions(profileId)

    /** Whether any demo persona is stored; the profile switcher stays hidden until one is. */
    fun observeHasDemoData(): Flow<Boolean> = db.sessions().observeHasDemoData()

    /** Every pose record of a profile's sessions. */
    fun observeAllPoses(profileId: String): Flow<List<PoseRecordEntity>> = db.sessions().observeAllPoses(profileId)

    /** Saves a finished practice session to My Account; returns its id, or null if nothing qualified. */
    suspend fun saveSession(
        segments: List<PoseSegmentStats>, startedAtMs: Long, endedAtMs: Long, epochDay: Long, profile: String
    ): Long? {
        val (session, poses) = SessionSummarizer.toEntities(segments, startedAtMs, endedAtMs, epochDay, profile) ?: return null
        return db.sessions().insertWithPoses(session.copy(profileId = ProfileIds.MY_ACCOUNT), poses)
    }

    /**
     * Idempotent checkpoint of the live session (Phase 2): inserts it on the first call
     * ([sessionId] == null) and afterwards updates the same row and replaces its pose rows, all in
     * one transaction. Always My Account. Returns the session id, or [sessionId] unchanged if no
     * pose qualifies yet (no empty sessions are ever written).
     */
    override suspend fun upsertLiveSession(
        sessionId: Long?,
        segments: List<PoseSegmentStats>,
        poseStartedAtMs: Map<AsanaLabel, Long>,
        startedAtMs: Long,
        endedAtMs: Long,
        epochDay: Long,
        toleranceProfile: String
    ): Long? {
        val (session, poses) = SessionSummarizer.toEntities(
            segments, startedAtMs, endedAtMs, epochDay, toleranceProfile, poseStartedAtMs
        ) ?: return sessionId
        val mine = session.copy(profileId = ProfileIds.MY_ACCOUNT)
        return db.withTransaction {
            val updated = sessionId != null &&
                db.sessions().updateLiveSession(sessionId, mine.endedAtMs, mine.activeSec, mine.meanScore) > 0
            if (updated) {
                db.sessions().deletePosesFor(sessionId!!)
                db.sessions().insertPoses(poses.map { it.copy(sessionId = sessionId) })
                sessionId
            } else {
                // First checkpoint (or the row was deleted from History meanwhile): insert afresh.
                db.sessions().insertWithPoses(mine, poses)
            }
        }
    }

    /** Deletes one of the user's own sessions (pose rows cascade). A demo session id is a no-op. */
    suspend fun deleteSession(sessionId: Long) = db.sessions().deleteSession(sessionId, ProfileIds.MY_ACCOUNT)

    // ---- Questionnaires -----------------------------------------------------------------------
    fun observeLatest(type: QuestionnaireType, profileId: String): Flow<QuestionnaireEntity?> =
        db.questionnaires().observeLatest(type.name, profileId)

    fun observeHistory(type: QuestionnaireType, profileId: String): Flow<List<QuestionnaireEntity>> =
        db.questionnaires().observeAll(type.name, profileId)

    suspend fun saveQuestionnaire(type: QuestionnaireType, epochDay: Long, answers: List<Int?>): QuestionnaireEntity {
        val entity = QuestionnaireEntity(
            type = type.name, epochDay = epochDay, createdAtMs = clock(),
            answers = Questionnaires.encodeAnswers(answers), totalScore = Questionnaires.score(answers),
            profileId = ProfileIds.MY_ACCOUNT
        )
        val id = db.questionnaires().insert(entity)
        return entity.copy(id = id)
    }

    // ---- Demo personas ------------------------------------------------------------------------
    /** True if the profile has any check-ins or sessions stored. */
    suspend fun hasData(profileId: String): Boolean =
        db.sessions().count(profileId) > 0 || db.checkIns().count(profileId) > 0

    /**
     * (Re)seeds one demo persona beside the user's data. Only that persona's rows are cleared
     * first (MY_ACCOUNT is protected in the DELETE statements), so seeding is idempotent and
     * never touches the user's own data.
     */
    suspend fun seedPersona(data: PersonaData) = withContext(Dispatchers.IO) {
        val pid = data.persona.profileId
        require(ProfileIds.isDemo(pid)) { "Demo data must not be seeded into My Account" }
        db.withTransaction {
            db.sessions().clearDemoSessions(pid)
            db.checkIns().clearDemoCheckIns(pid)
            db.questionnaires().clearDemoQuestionnaires(pid)
            db.checkIns().upsertAll(data.checkIns.map { it.copy(profileId = pid) })
            data.sessions.forEach { db.sessions().insertWithPoses(it.session.copy(profileId = pid), it.poses) }
            db.questionnaires().insertAll(data.questionnaires.map { it.copy(profileId = pid) })
        }
    }

    /** Removes all three demo personas; My Account is untouched. */
    suspend fun removeAllDemoData() = withContext(Dispatchers.IO) {
        db.withTransaction {
            ProfileIds.DEMO_IDS.forEach { pid ->
                db.sessions().clearDemoSessions(pid)
                db.checkIns().clearDemoCheckIns(pid)
                db.questionnaires().clearDemoQuestionnaires(pid)
            }
        }
    }

    /**
     * "Delete All My Data": database rows of every profile (real and demo), pose clips, exported
     * reports and settings (incl. consent). This button is meant to erase everything.
     */
    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        privateDirs.forEach { it.deleteRecursively() }
        prefs.clearAll()
    }
}
