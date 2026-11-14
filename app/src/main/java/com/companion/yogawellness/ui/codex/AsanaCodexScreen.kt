package com.companion.yogawellness.ui.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.codex.AsanaCatalog32
import com.companion.yogawellness.codex.AsanaCategory
import com.companion.yogawellness.codex.AsanaCodexItem
import com.companion.yogawellness.codex.Difficulty
import com.companion.yogawellness.ui.components.AppScreen
import com.companion.yogawellness.ui.components.Hairline
import com.companion.yogawellness.ui.components.Panel
import com.companion.yogawellness.ui.components.Tag
import com.companion.yogawellness.ui.components.Tone
import com.companion.yogawellness.ui.theme.AppTheme

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
    val c = AppTheme.colors

    AppScreen(title = "Learn", modifier = modifier, large = true) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "32 poses with animated demos. Four of them can also be scored live with the camera.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            item { SearchField(query) { query = it } }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip("All", filter == CodexFilter.ALL) { liveOnly = false; categoryName = null }
                    FilterChip("Live-scored", filter == CodexFilter.LIVE) { liveOnly = true; categoryName = null }
                    AsanaCategory.entries.forEach { cat ->
                        FilterChip(cat.shortTitle, filter.category == cat) { liveOnly = false; categoryName = cat.name }
                    }
                }
            }
            if (groups.isEmpty()) {
                item {
                    Text(
                        "No poses match “${query.trim()}”.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textTertiary,
                        modifier = Modifier.padding(vertical = 24.dp, horizontal = 4.dp)
                    )
                }
            }
            groups.forEach { (category, list) ->
                item(key = "header-${category.name}") {
                    Column(Modifier.padding(start = 4.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            category.title, style = MaterialTheme.typography.titleSmall, color = c.text,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text("${category.subtitle} · ${list.size} poses", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                    }
                }
                item(key = "group-${category.name}") {
                    Panel(contentPadding = PaddingValues(0.dp), spacing = 0.dp) {
                        list.forEachIndexed { i, item ->
                            if (i > 0) Hairline(inset = 84.dp)
                            CodexRow(item, onClick = { onOpen(item.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    val c = AppTheme.colors
    TextField(
        value = query,
        onValueChange = onChange,
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) ({
            IconButton(onClick = { onChange("") }) { Icon(Icons.Outlined.Close, contentDescription = "Clear search") }
        }) else null,
        placeholder = { Text("Search poses") },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = c.sunken,
            unfocusedContainerColor = c.sunken,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = c.text,
            unfocusedTextColor = c.text,
            focusedLeadingIconColor = c.textTertiary,
            unfocusedLeadingIconColor = c.textTertiary,
            focusedPlaceholderColor = c.textTertiary,
            unfocusedPlaceholderColor = c.textTertiary,
            cursorColor = c.accent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = AppTheme.colors
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) c.surface else c.text,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) c.text else c.surface)
            .border(1.dp, if (selected) c.text else c.border, RoundedCornerShape(8.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun CodexRow(item: AsanaCodexItem, onClick: () -> Unit) {
    val c = AppTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "Open ${item.englishName}", onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SkeletonThumbnail(item, item.defaultView, Modifier.width(58.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${item.number}. ${item.englishName}", style = MaterialTheme.typography.titleSmall, color = c.text)
            Text(item.sanskritName, style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            Text(item.everydayBenefit, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                Tag(item.difficulty.label, tone = if (item.difficulty == Difficulty.INTERMEDIATE) Tone.WARNING else Tone.NEUTRAL)
                if (item.isLive) Tag("Live-scored", tone = Tone.ACCENT)
            }
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
    }
}
