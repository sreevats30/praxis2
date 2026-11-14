package com.companion.yogawellness.ui.codex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.codex.AsanaCatalog32
import com.companion.yogawellness.codex.AsanaCategory
import com.companion.yogawellness.codex.AsanaCodexItem
import com.companion.yogawellness.ui.components.AppLazyScreen
import com.companion.yogawellness.ui.components.Chip
import com.companion.yogawellness.ui.components.EmptyState
import com.companion.yogawellness.ui.components.Hairline
import com.companion.yogawellness.ui.components.SearchField
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.SanskritStyle
import com.companion.yogawellness.ui.theme.SectionStyle
import com.companion.yogawellness.ui.theme.Space

/** List filter: all, live-scored only, or one category. */
data class CodexFilter(val liveOnly: Boolean = false, val category: AsanaCategory? = null) {
    companion object {
        val ALL = CodexFilter()
        val LIVE = CodexFilter(liveOnly = true)
    }
}

/** Search + filter, grouped by category in catalog order; empty groups are dropped. */
fun filterCodex(
    items: List<AsanaCodexItem>,
    query: String,
    filter: CodexFilter
): List<Pair<AsanaCategory, List<AsanaCodexItem>>> {
    val q = query.trim().lowercase()
    val kept = items.filter { item ->
        (!filter.liveOnly || item.isLive) &&
            (filter.category == null || item.category == filter.category) &&
            (q.isEmpty() || listOf(item.englishName, item.sanskritName, item.everydayBenefit, item.id.replace('_', ' '))
                .any { it.lowercase().contains(q) })
    }
    return AsanaCategory.entries.mapNotNull { c -> kept.filter { it.category == c }.takeIf { it.isNotEmpty() }?.let { c to it } }
}

/** The Learn tab: 32 poses with search, filters and static peak-pose thumbnails. */
@Composable
fun AsanaCodexScreen(onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    var liveOnly by rememberSaveable { mutableStateOf(false) }
    var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val filter = CodexFilter(liveOnly, categoryName?.let { AsanaCategory.valueOf(it) })
    val groups = remember(query, filter) { filterCodex(AsanaCatalog32.items, query, filter) }

    AppLazyScreen(
        title = "Learn",
        subtitle = "${AsanaCatalog32.items.size} poses with animated demos you can turn in 3D. Four are scored live with the camera.",
        modifier = modifier
    ) {
        item(key = "search") {
            SearchField(query, { query = it }, "Search by English or Sanskrit name", Modifier.padding(start = Space.Gutter, end = Space.Gutter, top = Space.S))
        }
        item(key = "filters") {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Space.Gutter, vertical = Space.M),
                horizontalArrangement = Arrangement.spacedBy(Space.S)
            ) {
                Chip("All", filter == CodexFilter.ALL, { liveOnly = false; categoryName = null })
                Chip("Scored live", filter == CodexFilter.LIVE, { liveOnly = true; categoryName = null })
                AsanaCategory.entries.forEach { cat ->
                    Chip(cat.shortTitle, filter.category == cat, { liveOnly = false; categoryName = cat.name })
                }
            }
        }
        if (groups.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    "No matching poses",
                    "Nothing matches “${query.trim()}”. Try the English or Sanskrit name, or clear the filter.",
                    Modifier.padding(horizontal = Space.Gutter, vertical = Space.L)
                )
            }
        }
        groups.forEach { (category, list) ->
            item(key = "header-${category.name}") { CategoryHeader(category, list.size) }
            itemsIndexed(list, key = { _, it -> it.id }) { i, item ->
                Column {
                    if (i > 0) Hairline(inset = Space.Gutter + THUMB_WIDTH + Space.L)
                    CodexRow(item, onClick = { onOpen(item.id) })
                }
            }
        }
    }
}

private val THUMB_WIDTH = 52.dp

@Composable
private fun CategoryHeader(category: AsanaCategory, count: Int) {
    Column(
        Modifier.fillMaxWidth().padding(start = Space.Gutter, end = Space.Gutter, top = Space.XL, bottom = Space.XS),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(category.title, style = SectionStyle, color = AppTheme.colors.text, modifier = Modifier.weight(1f).semantics { heading() })
            Text("$count", style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"), color = AppTheme.colors.textTertiary, modifier = Modifier.padding(start = Space.M))
        }
        Text(category.subtitle, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textTertiary)
    }
}

@Composable
private fun CodexRow(item: AsanaCodexItem, onClick: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "Open ${item.englishName}", onClick = onClick)
            .padding(horizontal = Space.Gutter, vertical = Space.M),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.L)
    ) {
        SkeletonThumbnail(item, item.defaultView, Modifier.width(THUMB_WIDTH))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${item.number}. ${item.englishName}", style = MaterialTheme.typography.titleMedium, color = c.text)
            Text(item.sanskritName, style = SanskritStyle, color = c.textSecondary)
            Text(
                buildAnnotatedString {
                    append(item.difficulty.label)
                    append(" · hold ${item.recommendedHoldSec} s")
                    if (item.isLive) {
                        append(" · ")
                        withStyle(SpanStyle(color = c.accent)) { append("Scored live") }
                    }
                },
                style = MaterialTheme.typography.labelMedium,
                color = c.textTertiary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
