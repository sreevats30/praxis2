package com.companion.yogawellness.wellness

import com.companion.yogawellness.data.ProfileIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.util.Locale

class MockPersonaSeederTest {

    companion object {
        private fun f(v: Double?) = v?.let { String.format(Locale.US, "%.6f", it) } ?: "null"

        /** SHA-256 over every generated field except profileId (and auto ids). */
        fun fingerprint(p: Persona): String {
            val d = MockPersonaSeeder.generate(p, 20_000L)
            val sb = StringBuilder()
            d.checkIns.forEach {
                sb.append("C${it.epochDay},${it.mood},${it.stress},${it.sleepQuality},${it.preTension},${it.postTension},${it.updatedAtMs};")
            }
            d.sessions.forEach { s ->
                val x = s.session
                sb.append("S${x.epochDay},${x.startedAtMs},${x.endedAtMs},${f(x.activeSec)},${f(x.meanScore)},${x.profile};")
                s.poses.forEach { r ->
                    sb.append("P${r.asana},${r.side},${f(r.lockedSec)},${f(r.meanScore)},${f(r.bestStrictHoldSec)},${f(r.bestGraceHoldSec)},${f(r.meanStability)};")
                }
            }
            d.questionnaires.forEach { sb.append("Q${it.type},${it.epochDay},${it.createdAtMs},${it.answers},${it.totalScore};") }
            return MessageDigest.getInstance("SHA-256").digest(sb.toString().toByteArray()).joinToString("") { "%02x".format(it) }
        }

        /** Captured from the seeder before the multi-profile change (commit acaa099). */
        private val BASELINE = mapOf(
            Persona.A_CONSISTENT to "db64651b79cb22270f64172f1f432822a1049a096678915a9ca43c2788fe78a3",
            Persona.B_EXAM_GAP to "036496c803e2a4ad4333d6239433664583a9fe6bfd13536544af8fc3c5c48169",
            Persona.C_NULL to "cdd77de145b4db99516c62da8600512c2dabed7eb882313b9bd4696dec91b09e"
        )
    }

    @Test
    fun outputIsUnchangedApartFromProfileId() {
        Persona.entries.forEach { assertEquals(it.name, BASELINE[it], fingerprint(it)) }
    }

    @Test
    fun everyEntityCarriesThePersonaProfileId() {
        assertEquals(
            listOf(ProfileIds.PERSONA_A, ProfileIds.PERSONA_B, ProfileIds.PERSONA_C),
            Persona.entries.map { it.profileId }
        )
        for (p in Persona.entries) {
            val d = MockPersonaSeeder.generate(p, 20_000L)
            assertTrue(d.checkIns.all { it.profileId == p.profileId })
            assertTrue(d.sessions.all { it.session.profileId == p.profileId })
            assertTrue(d.questionnaires.all { it.profileId == p.profileId })
            assertTrue(ProfileIds.isDemo(p.profileId))
            assertEquals(p, Persona.forProfileId(p.profileId))
        }
        assertEquals(listOf("Demo A", "Demo B", "Demo C"), Persona.entries.map { it.shortLabel })
    }
}
