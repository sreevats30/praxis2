package com.companion.yogawellness.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/*
 * Every read that feeds a screen is scoped by profileId. The per-profile clears protect
 * MY_ACCOUNT in SQL (`AND profileId != 'MY_ACCOUNT'`), not only in Kotlin.
 */

@Dao
interface CheckInDao {
    /** REPLACE keys on the unique (profileId, epochDay) index. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(checkIn: DailyCheckInEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(checkIns: List<DailyCheckInEntity>)

    @Query("SELECT * FROM daily_checkins WHERE epochDay = :epochDay AND profileId = :profileId LIMIT 1")
    suspend fun forDay(epochDay: Long, profileId: String): DailyCheckInEntity?

    @Query("SELECT * FROM daily_checkins WHERE epochDay = :epochDay AND profileId = :profileId LIMIT 1")
    fun observeDay(epochDay: Long, profileId: String): Flow<DailyCheckInEntity?>

    @Query("SELECT * FROM daily_checkins WHERE epochDay >= :fromEpochDay AND profileId = :profileId ORDER BY epochDay")
    fun observeSince(fromEpochDay: Long, profileId: String): Flow<List<DailyCheckInEntity>>

    @Query("SELECT * FROM daily_checkins WHERE profileId = :profileId ORDER BY epochDay")
    suspend fun all(profileId: String): List<DailyCheckInEntity>

    @Query("SELECT COUNT(*) FROM daily_checkins WHERE profileId = :profileId")
    suspend fun count(profileId: String): Int

    @Query("DELETE FROM daily_checkins WHERE profileId = :demoId AND profileId != 'MY_ACCOUNT'")
    suspend fun clearDemoCheckIns(demoId: String)
}

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: PracticeSessionEntity): Long

    @Insert
    suspend fun insertPoses(poses: List<PoseRecordEntity>)

    @Transaction
    suspend fun insertWithPoses(session: PracticeSessionEntity, poses: List<PoseRecordEntity>): Long {
        val id = insertSession(session)
        insertPoses(poses.map { it.copy(sessionId = id) })
        return id
    }

    @Query("SELECT * FROM practice_sessions WHERE epochDay >= :fromEpochDay AND profileId = :profileId ORDER BY startedAtMs")
    fun observeSince(fromEpochDay: Long, profileId: String): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM practice_sessions WHERE profileId = :profileId ORDER BY startedAtMs")
    suspend fun all(profileId: String): List<PracticeSessionEntity>

    @Query(
        "SELECT p.* FROM pose_records p JOIN practice_sessions s ON p.sessionId = s.id " +
            "WHERE s.epochDay >= :fromEpochDay AND s.profileId = :profileId ORDER BY s.startedAtMs"
    )
    fun observePosesSince(fromEpochDay: Long, profileId: String): Flow<List<PoseRecordEntity>>

    @Query("SELECT * FROM pose_records WHERE sessionId = :sessionId")
    suspend fun posesFor(sessionId: Long): List<PoseRecordEntity>

    @Query("SELECT * FROM practice_sessions WHERE profileId = :profileId ORDER BY startedAtMs DESC")
    fun observeAllSessions(profileId: String): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM pose_records WHERE sessionId IN (SELECT id FROM practice_sessions WHERE profileId = :profileId) ORDER BY startedAtMs")
    fun observeAllPoses(profileId: String): Flow<List<PoseRecordEntity>>

    @Query("SELECT COUNT(*) FROM practice_sessions WHERE profileId = :profileId")
    suspend fun count(profileId: String): Int

    /** True once any demo persona has been loaded (check-ins or sessions outside My Account). */
    @Query(
        "SELECT EXISTS(SELECT 1 FROM daily_checkins WHERE profileId != 'MY_ACCOUNT') " +
            "OR EXISTS(SELECT 1 FROM practice_sessions WHERE profileId != 'MY_ACCOUNT')"
    )
    fun observeHasDemoData(): Flow<Boolean>

    /** Live-session checkpoint: only My Account sessions are ever updated. Returns rows changed. */
    @Query(
        "UPDATE practice_sessions SET endedAtMs = :endedAtMs, activeSec = :activeSec, meanScore = :meanScore " +
            "WHERE id = :sessionId AND profileId = 'MY_ACCOUNT'"
    )
    suspend fun updateLiveSession(sessionId: Long, endedAtMs: Long, activeSec: Double, meanScore: Double?): Int

    @Query("DELETE FROM pose_records WHERE sessionId = :sessionId")
    suspend fun deletePosesFor(sessionId: Long)

    /** Pose rows go with it (ON DELETE CASCADE). */
    @Query("DELETE FROM practice_sessions WHERE id = :sessionId AND profileId = :profileId")
    suspend fun deleteSession(sessionId: Long, profileId: String)

    @Query("DELETE FROM practice_sessions WHERE profileId = :demoId AND profileId != 'MY_ACCOUNT'")
    suspend fun clearDemoSessions(demoId: String)
}

@Dao
interface QuestionnaireDao {
    @Insert
    suspend fun insert(q: QuestionnaireEntity): Long

    @Insert
    suspend fun insertAll(q: List<QuestionnaireEntity>)

    @Query("SELECT * FROM questionnaires WHERE type = :type AND profileId = :profileId ORDER BY epochDay DESC, createdAtMs DESC LIMIT 1")
    fun observeLatest(type: String, profileId: String): Flow<QuestionnaireEntity?>

    @Query("SELECT * FROM questionnaires WHERE type = :type AND profileId = :profileId ORDER BY epochDay")
    fun observeAll(type: String, profileId: String): Flow<List<QuestionnaireEntity>>

    @Query("SELECT * FROM questionnaires WHERE profileId = :profileId ORDER BY epochDay")
    suspend fun all(profileId: String): List<QuestionnaireEntity>

    @Query("DELETE FROM questionnaires WHERE profileId = :demoId AND profileId != 'MY_ACCOUNT'")
    suspend fun clearDemoQuestionnaires(demoId: String)
}
