package com.companion.yogawellness.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.companion.yogawellness.data.db.WellnessDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device v1 → v2 migration check (real SQLite of the phone). The v1 database is created from
 * the committed schema export, packaged as androidTest assets (see app/build.gradle.kts), so no
 * extra test dependency (room-testing) is needed. The JVM twin is test/…/data/MigrationTest.kt.
 *   ./gradlew :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class MigrationInstrumentedTest {

    @Test
    fun migrate1To2KeepsEveryRowAsMyAccount() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val target = instrumentation.targetContext
        val schema = instrumentation.context.assets
            .open("com.companion.yogawellness.data.db.WellnessDatabase/1.json").bufferedReader().use { it.readText() }
        val file = target.getDatabasePath("migration-instrumented.db").apply { parentFile?.mkdirs(); delete() }

        val database = JSONObject(schema).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val entities = database.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i)
                val table = e.getString("tableName")
                db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = e.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = database.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.version = 1
            db.insert("daily_checkins", null, ContentValues().apply {
                put("epochDay", 20_000L); put("mood", 7); put("stress", 4); put("sleepQuality", 6); put("updatedAtMs", 1L)
            })
            val sid = db.insert("practice_sessions", null, ContentValues().apply {
                put("epochDay", 20_000L); put("startedAtMs", 10L); put("endedAtMs", 70_000L)
                put("activeSec", 42.0); put("meanScore", 81.5); put("profile", "STANDARD")
            })
            db.insert("pose_records", null, ContentValues().apply {
                put("sessionId", sid); put("asana", "VRIKSHASANA"); put("side", "LEFT_LIFTED"); put("lockedSec", 21.0)
                put("meanScore", 80.0); put("bestStrictHoldSec", 9.0); put("bestGraceHoldSec", 12.0); put("meanStability", 88.0)
            })
            db.insert("questionnaires", null, ContentValues().apply {
                put("type", "PHQ9"); put("epochDay", 20_000L); put("createdAtMs", 5L); put("answers", "1,1,1,1,1,1,1,1,1"); put("totalScore", 9)
            })
        }

        val room = Room.databaseBuilder(target, WellnessDatabase::class.java, file.absolutePath)
            .addMigrations(WellnessDatabase.MIGRATION_1_2).build()
        try {
            assertEquals(1, room.checkIns().all(ProfileIds.MY_ACCOUNT).size)
            val sessions = room.sessions().all(ProfileIds.MY_ACCOUNT)
            assertEquals(listOf("LIVE"), sessions.map { it.source })
            assertEquals(1, room.sessions().posesFor(sessions.single().id).size)
            assertEquals(listOf(9), room.questionnaires().all(ProfileIds.MY_ACCOUNT).map { it.totalScore })
        } finally {
            room.close()
        }
    }
}
