package com.companion.yogawellness.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.companion.yogawellness.data.db.WellnessDatabase
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * v1 → v2 upgrade on a real SQLite file (Robolectric), without adding room-testing:
 * the v1 database is built from the committed schema export `app/schemas/.../1.json` (the exact
 * SQL Room 1 executed), filled with one row per table, then opened by the v2 Room database with
 * MIGRATION_1_2. Room runs the migration and then its own schema validation (it throws if the
 * migrated tables, columns, defaults or indices differ from the v2 entities).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    private val schemaDir = File("schemas/com.companion.yogawellness.data.db.WellnessDatabase")

    @Test
    fun migrate1To2KeepsEveryRowAsMyAccount() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbFile = context.getDatabasePath("migration-test.db").apply { parentFile?.mkdirs(); delete() }
        createV1Database(dbFile, JSONObject(File(schemaDir, "1.json").readText()))

        val db = Room.databaseBuilder(context, WellnessDatabase::class.java, dbFile.absolutePath)
            .addMigrations(WellnessDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val checkIns = db.checkIns().all(ProfileIds.MY_ACCOUNT)
            assertEquals(1, checkIns.size)
            assertEquals(7, checkIns.single().mood)

            val sessions = db.sessions().all(ProfileIds.MY_ACCOUNT)
            assertEquals(1, sessions.size)
            assertEquals("LIVE", sessions.single().source)
            assertEquals("STANDARD", sessions.single().profile) // tolerance profile column untouched

            val poses = db.sessions().posesFor(sessions.single().id)
            assertEquals(1, poses.size)
            assertEquals(0L, poses.single().startedAtMs)
            assertEquals(0.0, poses.single().qualitySec, 0.0)
            assertNull(poses.single().worstJoint)

            val q = db.questionnaires().all(ProfileIds.MY_ACCOUNT)
            assertEquals(listOf(9), q.map { it.totalScore })

            // The new per-profile unique index lets a demo row share the same day.
            db.checkIns().upsert(checkIns.single().copy(id = 0, profileId = ProfileIds.PERSONA_A))
            assertEquals(1, db.checkIns().all(ProfileIds.MY_ACCOUNT).size)
            assertEquals(1, db.checkIns().all(ProfileIds.PERSONA_A).size)
        } finally {
            db.close()
        }
    }

    @Test
    fun v2SchemaExportIsCommittedAndMatchesVersion() {
        val v2 = JSONObject(File(schemaDir, "2.json").readText()).getJSONObject("database")
        assertEquals(2, v2.getInt("version"))
    }

    companion object {
        /** Recreates a version-1 database exactly as Room 2.6.1 created it (tables, indices, identity hash). */
        fun createV1Database(file: File, schema: JSONObject) {
            val database = schema.getJSONObject("database")
            SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
                val entities = database.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val e = entities.getJSONObject(i)
                    val table = e.getString("tableName")
                    db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", table))
                    val indices = e.optJSONArray("indices") ?: continue
                    for (j in 0 until indices.length()) {
                        db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                    }
                }
                val setup = database.getJSONArray("setupQueries")
                for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
                db.version = database.getInt("version")

                db.insert("daily_checkins", null, ContentValues().apply {
                    put("epochDay", 20_000L); put("mood", 7); put("stress", 4); put("sleepQuality", 6); put("updatedAtMs", 1L)
                })
                val sessionId = db.insert("practice_sessions", null, ContentValues().apply {
                    put("epochDay", 20_000L); put("startedAtMs", 10L); put("endedAtMs", 70_000L)
                    put("activeSec", 42.0); put("meanScore", 81.5); put("profile", "STANDARD")
                })
                db.insert("pose_records", null, ContentValues().apply {
                    put("sessionId", sessionId); put("asana", "VRIKSHASANA"); put("side", "LEFT_LIFTED")
                    put("lockedSec", 21.0); put("meanScore", 80.0); put("bestStrictHoldSec", 9.0)
                    put("bestGraceHoldSec", 12.0); put("meanStability", 88.0)
                })
                db.insert("questionnaires", null, ContentValues().apply {
                    put("type", "PHQ9"); put("epochDay", 20_000L); put("createdAtMs", 5L)
                    put("answers", "1,1,1,1,1,1,1,1,1"); put("totalScore", 9)
                })
            }
        }
    }
}
