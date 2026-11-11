package com.companion.yogawellness.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One < 30 s daily check-in (§9.2). One row per calendar day (upserted). */
@Entity(tableName = "daily_checkins", indices = [Index(value = ["epochDay"], unique = true)])
data class DailyCheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    /** 1–10 */
    val mood: Int,
    /** 1–10 */
    val stress: Int,
    /** 1–10 */
    val sleepQuality: Int,
    /** Optional 1-tap tension rating before / after today's practice (1–10). */
    val preTension: Int? = null,
    val postTension: Int? = null,
    val updatedAtMs: Long
)

@Entity(tableName = "practice_sessions", indices = [Index("epochDay")])
data class PracticeSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val startedAtMs: Long,
    val endedAtMs: Long,
    /** Seconds spent locked in scored asanas. */
    val activeSec: Double,
    /** Time-weighted mean alignment score over the session. */
    val meanScore: Double?,
    val profile: String
)

@Entity(
    tableName = "pose_records",
    foreignKeys = [
        ForeignKey(
            entity = PracticeSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class PoseRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    /** [com.companion.yogawellness.scoring.Asana] name */
    val asana: String,
    /** [com.companion.yogawellness.scoring.PoseSide] name */
    val side: String,
    val lockedSec: Double,
    val meanScore: Double?,
    val bestStrictHoldSec: Double,
    val bestGraceHoldSec: Double,
    val meanStability: Double?
)

/** A fortnightly PHQ-9 or GAD-7 self-reflection check-in (§9.3). */
@Entity(tableName = "questionnaires", indices = [Index("type"), Index("epochDay")])
data class QuestionnaireEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [com.companion.yogawellness.wellness.QuestionnaireType] name */
    val type: String,
    val epochDay: Long,
    val createdAtMs: Long,
    /** Comma-separated item answers 0–3. */
    val answers: String,
    val totalScore: Int
)
