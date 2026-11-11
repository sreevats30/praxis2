package com.companion.yogawellness.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local on-device SQLite storage (§2). android:allowBackup="false" and the data-extraction rules
 * keep it out of cloud backups. Optional hardening: swap in SQLCipher's SupportOpenHelperFactory.
 */
@Database(
    entities = [DailyCheckInEntity::class, PracticeSessionEntity::class, PoseRecordEntity::class, QuestionnaireEntity::class],
    version = 1,
    exportSchema = true
)
abstract class WellnessDatabase : RoomDatabase() {
    abstract fun checkIns(): CheckInDao
    abstract fun sessions(): SessionDao
    abstract fun questionnaires(): QuestionnaireDao

    companion object {
        const val NAME = "wellness.db"

        fun create(context: Context): WellnessDatabase =
            Room.databaseBuilder(context.applicationContext, WellnessDatabase::class.java, NAME).build()
    }
}
