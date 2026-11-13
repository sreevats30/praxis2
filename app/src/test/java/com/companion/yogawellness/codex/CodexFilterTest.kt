package com.companion.yogawellness.codex

import com.companion.yogawellness.scoring.Asana
import com.companion.yogawellness.ui.codex.CodexFilter
import com.companion.yogawellness.ui.codex.filterCodex
import com.companion.yogawellness.ui.practice.targetHint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodexFilterTest {
    private val items = AsanaCatalog32.items

    @Test fun allShowsSixGroupsOf32() {
        val g = filterCodex(items, "", CodexFilter.ALL)
        assertEquals(AsanaCategory.entries, g.map { it.first })
        assertEquals(32, g.sumOf { it.second.size })
    }

    @Test fun liveFilterShowsTheFourScoredPoses() {
        val ids = filterCodex(items, "", CodexFilter.LIVE).flatMap { it.second }.map { it.id }
        assertEquals(listOf("warrior_2", "chair_pose", "tree_pose", "triangle_pose"), ids)
    }

    @Test fun categoryAndSearch() {
        assertEquals(4, filterCodex(items, "", CodexFilter(category = AsanaCategory.EVENING)).single().second.size)
        assertEquals(listOf("downward_dog"), filterCodex(items, "  adho mukha ", CodexFilter.ALL).flatMap { it.second }.map { it.id })
        assertEquals(listOf("warrior_1", "warrior_2", "warrior_3"), filterCodex(items, "WARRIOR", CodexFilter.ALL).flatMap { it.second }.map { it.id })
        assertTrue(filterCodex(items, "zzz", CodexFilter.ALL).isEmpty())
    }

    @Test fun practiceTargetHintIsAHintOnly() {
        assertTrue(targetHint(Asana.UTKATASANA).contains("Utkatasana"))
        assertTrue(targetHint(Asana.UTKATASANA).contains("turn 45°"))
        assertTrue(targetHint(Asana.VIRABHADRASANA_II).endsWith("Any of the four poses is still detected."))
    }
}
