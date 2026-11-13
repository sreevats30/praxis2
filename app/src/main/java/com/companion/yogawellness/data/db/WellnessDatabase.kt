package com.companion.yogawellness.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Local on-device SQLite storage (§2). android:allowBackup="false" and the data-extraction rules
 * keep it out of cloud backups. Optional hardening: swap in SQLCipher's SupportOpenHelperFactory.
 *
 * Schema history (exported to app/schemas/):
 *  1 — initial release
 *  2 — profileId on check-ins/sessions/questionnaires (demo personas live beside the user's data),
 *      session source, richer per-pose stats. Upgraded by [MIGRATION_1_2]; there is deliberately
 *      NO destructive fallback, because wiping a user's history on upgrade is never acceptable.
 */
@Database(
    entities = [DailyCheckInEntity::class, PracticeSessionEntity::class, PoseRecordEntity::class, QuestionnaireEntity::class],
    version = 2,
    exportSchema = true
)
abstract class WellnessDatabase : RoomDatabase() {
    abstract fun checkIns(): CheckInDao
    abstract fun sessions(): SessionDao
    abstract fun questionnaires(): QuestionnaireDao

    companion object {
        const val NAME = "wellness.db"

        /** v1 → v2: additive only. Every existing row becomes MY_ACCOUNT / LIVE. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_checkins ADD COLUMN profileId TEXT NOT NULL DEFAULT 'MY_ACCOUNT'")
                db.execSQL("ALTER TABLE practice_sessions ADD COLUMN profileId TEXT NOT NULL DEFAULT 'MY_ACCOUNT'")
                db.execSQL("ALTER TABLE practice_sessions ADD COLUMN source TEXT NOT NULL DEFAULT 'LIVE'")
                db.execSQL("ALTER TABLE questionnaires ADD COLUMN profileId TEXT NOT NULL DEFAULT 'MY_ACCOUNT'")
                db.execSQL("ALTER TABLE pose_records ADD COLUMN startedAtMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pose_records ADD COLUMN qualitySec REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pose_records ADD COLUMN peakScore REAL")
                db.execSQL("ALTER TABLE pose_records ADD COLUMN worstJoint TEXT")
                db.execSQL("ALTER TABLE pose_records ADD COLUMN topCue TEXT")
                db.execSQL("DROP INDEX IF EXISTS index_daily_checkins_epochDay")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_daily_checkins_profileId_epochDay ON daily_checkins(profileId, epochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_practice_sessions_profileId_epochDay ON practice_sessions(profileId, epochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_questionnaires_profileId ON questionnaires(profileId)")
            }
        }

        fun create(context: Context): WellnessDatabase =
            Room.databaseBuilder(context.applicationContext, WellnessDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
