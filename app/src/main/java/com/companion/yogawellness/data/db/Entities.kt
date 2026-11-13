package com.companion.yogawellness.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.companion.yogawellness.data.ProfileIds

/** One < 30 s daily check-in (§9.2). One row per calendar day *per profile* (upserted). */
@Entity(tableName = "daily_checkins", indices = [Index(value = ["profileId", "epochDay"], unique = true)])
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
    val updatedAtMs: Long,
    /** [ProfileIds.MY_ACCOUNT] for the user's own data, a persona id for demo data. */
    @ColumnInfo(defaultValue = ProfileIds.MY_ACCOUNT) val profileId: String = ProfileIds.MY_ACCOUNT
)

@Entity(tableName = "practice_sessions", indices = [Index("epochDay"), Index("profileId", "epochDay")])
data class PracticeSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val startedAtMs: Long,
    val endedAtMs: Long,
    /** Seconds spent locked in scored asanas. */
    val activeSec: Double,
    /** Time-weighted mean alignment score over the session. */
    val meanScore: Double?,
    /** Alignment *tolerance* profile (STANDARD/BEGINNER/ADVANCED) — not the user/demo profile. */
    val profile: String,
    @ColumnInfo(defaultValue = ProfileIds.MY_ACCOUNT) val profileId: String = ProfileIds.MY_ACCOUNT,
    /** "LIVE" (camera-scored) or "MANUAL" (user-logged minutes, no score). */
    @ColumnInfo(defaultValue = "LIVE") val source: String = SOURCE_LIVE
) {
    companion object {
        const val SOURCE_LIVE = "LIVE"
        const val SOURCE_MANUAL = "MANUAL"
    }
}

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
    val meanStability: Double?,
    /** Wall-clock time this pose first locked in the session. */
    @ColumnInfo(defaultValue = "0") val startedAtMs: Long = 0L,
    /** Seconds with alignment >= AsanaSpecs.HOLD_THRESHOLD (75). */
    @ColumnInfo(defaultValue = "0") val qualitySec: Double = 0.0,
    val peakScore: Double? = null,
    /** Joint name with the lowest joint score most often, e.g. "Front Knee". */
    val worstJoint: String? = null,
    /** Most frequent primary corrective cue. */
    val topCue: String? = null
)

/** A fortnightly PHQ-9 or GAD-7 self-reflection check-in (§9.3). */
@Entity(tableName = "questionnaires", indices = [Index("type"), Index("epochDay"), Index("profileId")])
data class QuestionnaireEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [com.companion.yogawellness.wellness.QuestionnaireType] name */
    val type: String,
    val epochDay: Long,
    val createdAtMs: Long,
    /** Comma-separated item answers 0–3. */
    val answers: String,
    val totalScore: Int,
    @ColumnInfo(defaultValue = ProfileIds.MY_ACCOUNT) val profileId: String = ProfileIds.MY_ACCOUNT
)
