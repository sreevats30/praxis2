package com.companion.yogawellness.codex

/** The 32-pose Learn library (§5.6), aggregated from the six per-category files. */
object AsanaCatalog32 {
    val items: List<AsanaCodexItem> =
        (DESK_POSES + MORNING_POSES + STANDING_POSES + BALANCE_POSES + HIP_POSES + EVENING_POSES).sortedBy { it.number }

    private val byIdMap = items.associateBy { it.id }

    fun byId(id: String): AsanaCodexItem? = byIdMap[id]

    fun byCategory(): Map<AsanaCategory, List<AsanaCodexItem>> =
        AsanaCategory.entries.associateWith { c -> items.filter { it.category == c } }

    val liveItems: List<AsanaCodexItem> get() = items.filter { it.isLive }
}
