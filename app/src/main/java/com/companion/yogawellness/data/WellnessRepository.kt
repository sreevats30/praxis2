package com.companion.yogawellness.data

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
import androidx.room.withTransaction
import java.io.File

/** Single access point for everything the app stores — all of it on this device only. */
class WellnessRepository(
    private val db: WellnessDatabase,
    private val prefs: AppPreferences,
    /** Directories wiped by "Delete All My Data" (pose clips, exported reports). */
    private val privateDirs: List<File>,
    private val clock: () -> Long = System::currentTimeMillis
) {
    // ---- Daily check-in ---------------------------------------------------------------------
    fun observeCheckIn(epochDay: Long): Flow<DailyCheckInEntity?> = db.checkIns().observeDay(epochDay)
    fun observeCheckInsSince(fromEpochDay: Long): Flow<List<DailyCheckInEntity>> = db.checkIns().observeSince(fromEpochDay)

    suspend fun saveCheckIn(
        epochDay: Long, mood: Int, stress: Int, sleep: Int, preTension: Int?, postTension: Int?
    ) {
        db.checkIns().upsert(
            DailyCheckInEntity(
                epochDay = epochDay, mood = mood, stress = stress, sleepQuality = sleep,
                preTension = preTension, postTension = postTension, updatedAtMs = clock()
            )
        )
    }

    // ---- Practice sessions --------------------------------------------------------------------
    fun observeSessionsSince(fromEpochDay: Long): Flow<List<PracticeSessionEntity>> = db.sessions().observeSince(fromEpochDay)
    fun observePosesSince(fromEpochDay: Long): Flow<List<PoseRecordEntity>> = db.sessions().observePosesSince(fromEpochDay)

    /** Saves a finished practice session; returns its id, or null if it was too short to keep. */
    suspend fun saveSession(
        segments: List<PoseSegmentStats>, startedAtMs: Long, endedAtMs: Long, epochDay: Long, profile: String
    ): Long? {
        val (session, poses) = SessionSummarizer.toEntities(segments, startedAtMs, endedAtMs, epochDay, profile) ?: return null
        return db.sessions().insertWithPoses(session, poses)
    }

    // ---- Questionnaires -----------------------------------------------------------------------
    fun observeLatest(type: QuestionnaireType): Flow<QuestionnaireEntity?> = db.questionnaires().observeLatest(type.name)
    fun observeHistory(type: QuestionnaireType): Flow<List<QuestionnaireEntity>> = db.questionnaires().observeAll(type.name)

    suspend fun saveQuestionnaire(type: QuestionnaireType, epochDay: Long, answers: List<Int?>): QuestionnaireEntity {
        val entity = QuestionnaireEntity(
            type = type.name, epochDay = epochDay, createdAtMs = clock(),
            answers = Questionnaires.encodeAnswers(answers), totalScore = Questionnaires.score(answers)
        )
        val id = db.questionnaires().insert(entity)
        return entity.copy(id = id)
    }

    // ---- Demo personas & deletion ---------------------------------------------------------------
    /** Replaces ALL stored data with a seeded demo persona (§10 Prompt 4). */
    suspend fun replaceWithPersona(data: PersonaData) = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.clearAllTables()
            db.checkIns().upsertAll(data.checkIns)
            data.sessions.forEach { db.sessions().insertWithPoses(it.session, it.poses) }
            db.questionnaires().insertAll(data.questionnaires)
        }
        prefs.demoPersona = data.persona.name
    }

    /** "Delete All My Data": database rows, pose clips, exported reports and settings (incl. consent). */
    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        privateDirs.forEach { it.deleteRecursively() }
        prefs.clearAll()
    }
}
