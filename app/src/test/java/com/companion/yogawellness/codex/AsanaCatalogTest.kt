package com.companion.yogawellness.codex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AsanaCatalogTest {
    private val items = AsanaCatalog32.items

    /** The ids of §5.6, in catalog order. */
    private val specIds = listOf(
        "cat_cow", "seated_twist", "cow_face_arms", "eagle_pose", "puppy_pose", "gentle_cobra",
        "mountain", "upward_salute", "standing_forward_fold", "halfway_lift", "low_lunge", "downward_dog",
        "warrior_1", "warrior_2", "chair_pose", "high_lunge", "plank_pose", "boat_pose",
        "tree_pose", "warrior_3", "half_moon", "dancer_pose", "side_plank",
        "triangle_pose", "extended_side_angle", "yogic_squat", "bound_angle", "wide_forward_fold",
        "childs_pose", "seated_forward_bend", "bridge_pose", "corpse_relaxation"
    )

    @Test fun exactly32ItemsWithTheSpecIdsAndNumbers() {
        assertEquals(32, items.size)
        assertEquals(32, items.map { it.id }.toSet().size)
        assertEquals(specIds, items.map { it.id })
        assertEquals((1..32).toList(), items.map { it.number })
    }

    @Test fun categoryCountsAre6_6_6_5_5_4() {
        val counts = AsanaCategory.entries.map { c -> items.count { it.category == c } }
        assertEquals(listOf(6, 6, 6, 5, 5, 4), counts)
    }

    @Test fun contentShapeRules() {
        for (it in items) {
            assertTrue("${it.id}: steps ${it.steps.size}", it.steps.size in 3..5)
            assertTrue("${it.id}: mistakes ${it.commonMistakes.size}", it.commonMistakes.size in 2..3)
            assertTrue("${it.id}: safety", it.safety.isNotBlank())
            assertTrue("${it.id}: benefit", it.everydayBenefit.isNotBlank())
            assertTrue("${it.id}: callouts", it.callouts.isNotEmpty())
            val hold = it.recommendedHoldSec
            if (it.id == "corpse_relaxation") assertTrue(hold in 60..180) else assertTrue("${it.id}: hold $hold", hold in 15..45)
        }
    }

    @Test fun noMedicalClaimWordsAnywhere() {
        val banned = listOf("cure", "treat", "heal", "diagnos", "therapy")
        fun texts(i: AsanaCodexItem) = listOf(i.sanskritName, i.englishName, i.everydayBenefit, i.safety) +
            i.steps + i.commonMistakes + i.modifications + i.callouts.map { it.label } + i.category.title + i.category.subtitle
        val all = items.flatMap { i -> texts(i).map { i.id to it } } +
            listOf("global" to CodexText.GLOBAL_DISCLAIMER, "global" to CodexText.FOLLOW_ALONG_NOTE)
        for ((id, text) in all) {
            for (w in banned) assertFalse("$id contains '$w': $text", text.lowercase().contains(w))
        }
    }

    @Test fun exactlyFourLiveItems_14_15_19_24() {
        val live = items.filter { it.liveAsana != null }
        assertEquals(listOf(14, 15, 19, 24), live.map { it.number })
        assertNull("Mountain is calibration-only", AsanaCatalog32.byId("mountain")!!.liveAsana)
        assertTrue(live.all { it.liveAsana!!.isScored })
    }

    @Test fun byIdLooksUpEveryItem() {
        items.forEach { assertNotNull(AsanaCatalog32.byId(it.id)) }
        assertNull(AsanaCatalog32.byId("no_such_pose"))
    }

    @Test fun safetyNotesNameTheMainCaution() {
        fun s(id: String) = AsanaCatalog32.byId(id)!!.safety.lowercase()
        listOf("plank_pose", "downward_dog", "cat_cow").forEach { assertTrue(it, s(it).contains("wrist")) }
        listOf("low_lunge", "yogic_squat").forEach { assertTrue(it, s(it).contains("knee")) }
        listOf("bridge_pose", "gentle_cobra").forEach { assertTrue(it, s(it).contains("neck")) }
        listOf("boat_pose", "standing_forward_fold", "seated_forward_bend").forEach { assertTrue(it, s(it).contains("lower back")) }
        listOf("tree_pose", "warrior_3", "half_moon", "dancer_pose", "eagle_pose").forEach { assertTrue(it, s(it).contains("balance")) }
        listOf("boat_pose", "gentle_cobra", "seated_twist").forEach { assertTrue(it, s(it).contains("pregnan")) }
        listOf("standing_forward_fold", "wide_forward_fold", "downward_dog").forEach { assertTrue(it, s(it).contains("head below heart")) }
    }
}
