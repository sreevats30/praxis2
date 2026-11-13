package com.companion.yogawellness.ui.codex

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.codex.AsanaCatalog32
import com.companion.yogawellness.codex.AsanaCategory
import com.companion.yogawellness.codex.AsanaCodexItem
import com.companion.yogawellness.codex.Difficulty
import com.companion.yogawellness.ui.theme.ScoreColors

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

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Learn", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Text(
                    "32 poses with animated demos. Four of them can also be scored live with the camera.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Search poses") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filter == CodexFilter.ALL,
                    onClick = { liveOnly = false; categoryName = null },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = filter == CodexFilter.LIVE,
                    onClick = { liveOnly = true; categoryName = null },
                    label = { Text("Live-scored") }
                )
                AsanaCategory.entries.forEach { c ->
                    FilterChip(
                        selected = filter.category == c,
                        onClick = { liveOnly = false; categoryName = c.name },
                        label = { Text(c.shortTitle) }
                    )
                }
            }
        }
        if (groups.isEmpty()) {
            item {
                Text(
                    "No poses match “${query.trim()}”.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        }
        groups.forEach { (category, list) ->
            item(key = "header-${category.name}") {
                Column(Modifier.padding(top = 8.dp)) {
                    Text(
                        category.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(category.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(list, key = { it.id }) { item -> CodexRow(item, onClick = { onOpen(item.id) }) }
        }
    }
}

@Composable
private fun CodexRow(item: AsanaCodexItem, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "Open ${item.englishName}", onClick = onClick)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonThumbnail(item, item.defaultView, Modifier.width(56.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${item.number}. ${item.englishName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(item.sanskritName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(item.everydayBenefit, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(item.difficulty.label, if (item.difficulty == Difficulty.INTERMEDIATE) ScoreColors.Amber else MaterialTheme.colorScheme.outline)
                    if (item.isLive) Tag("Live-scored", ScoreColors.Green)
                }
            }
        }
    }
}

@Composable
internal fun Tag(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.14f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}
