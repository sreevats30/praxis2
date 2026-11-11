package com.companion.yogawellness.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(checkIn: DailyCheckInEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(checkIns: List<DailyCheckInEntity>)

    @Query("SELECT * FROM daily_checkins WHERE epochDay = :epochDay LIMIT 1")
    suspend fun forDay(epochDay: Long): DailyCheckInEntity?

    @Query("SELECT * FROM daily_checkins WHERE epochDay = :epochDay LIMIT 1")
    fun observeDay(epochDay: Long): Flow<DailyCheckInEntity?>

    @Query("SELECT * FROM daily_checkins WHERE epochDay >= :fromEpochDay ORDER BY epochDay")
    fun observeSince(fromEpochDay: Long): Flow<List<DailyCheckInEntity>>

    @Query("SELECT * FROM daily_checkins ORDER BY epochDay")
    suspend fun all(): List<DailyCheckInEntity>
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

    @Query("SELECT * FROM practice_sessions WHERE epochDay >= :fromEpochDay ORDER BY startedAtMs")
    fun observeSince(fromEpochDay: Long): Flow<List<PracticeSessionEntity>>

    @Query("SELECT * FROM practice_sessions ORDER BY startedAtMs")
    suspend fun all(): List<PracticeSessionEntity>

    @Query(
        "SELECT p.* FROM pose_records p JOIN practice_sessions s ON p.sessionId = s.id " +
            "WHERE s.epochDay >= :fromEpochDay ORDER BY s.startedAtMs"
    )
    fun observePosesSince(fromEpochDay: Long): Flow<List<PoseRecordEntity>>

    @Query("SELECT * FROM pose_records WHERE sessionId = :sessionId")
    suspend fun posesFor(sessionId: Long): List<PoseRecordEntity>
}

@Dao
interface QuestionnaireDao {
    @Insert
    suspend fun insert(q: QuestionnaireEntity): Long

    @Insert
    suspend fun insertAll(q: List<QuestionnaireEntity>)

    @Query("SELECT * FROM questionnaires WHERE type = :type ORDER BY epochDay DESC, createdAtMs DESC LIMIT 1")
    fun observeLatest(type: String): Flow<QuestionnaireEntity?>

    @Query("SELECT * FROM questionnaires WHERE type = :type ORDER BY epochDay")
    fun observeAll(type: String): Flow<List<QuestionnaireEntity>>

    @Query("SELECT * FROM questionnaires ORDER BY epochDay")
    suspend fun all(): List<QuestionnaireEntity>
}
