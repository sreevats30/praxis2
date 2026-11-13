package com.companion.yogawellness.ui.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.codex.AsanaCatalog32
import com.companion.yogawellness.codex.AsanaCategory
import com.companion.yogawellness.codex.AsanaCodexItem
import com.companion.yogawellness.codex.Difficulty
import com.companion.yogawellness.ui.components.ClinicalTopBar
import com.companion.yogawellness.ui.components.Overline
import com.companion.yogawellness.ui.components.Pill
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Brand
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

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ClinicalTopBar(
                title = "Learn",
                subtitle = "${AsanaCatalog32.items.size} poses · animated demos",
                bottom = { SearchField(query) { query = it } }
            )
        }
    ) { inner ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "32 poses with animated demos. Four of them can also be scored live with the camera.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            item {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPill("All", filter == CodexFilter.ALL) { liveOnly = false; categoryName = null }
                    FilterPill("Live-scored", filter == CodexFilter.LIVE, live = true) { liveOnly = true; categoryName = null }
                    AsanaCategory.entries.forEach { c ->
                        FilterPill(c.shortTitle, filter.category == c) { liveOnly = false; categoryName = c.name }
                    }
                }
            }
            if (groups.isEmpty()) {
                item {
                    Text(
                        "No poses match “${query.trim()}”.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.inkMuted,
                        modifier = Modifier.padding(vertical = 24.dp, horizontal = 4.dp)
                    )
                }
            }
            groups.forEach { (category, list) ->
                item(key = "header-${category.name}") {
                    Column(Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Overline("${list.size} ${if (list.size == 1) "pose" else "poses"}", color = AppTheme.colors.inkMuted)
                        Text(
                            category.title, style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(category.subtitle, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.inkMuted)
                    }
                }
                items(list, key = { it.id }) { item -> CodexRow(item, onClick = { onOpen(item.id) }) }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onChange,
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) ({
            IconButton(onClick = { onChange("") }) { Icon(Icons.Rounded.Close, contentDescription = "Clear search") }
        }) else null,
        placeholder = { Text("Search poses") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White.copy(alpha = 0.94f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = Brand.Navy900,
            unfocusedTextColor = Brand.Navy900,
            focusedLeadingIconColor = Brand.Teal600,
            unfocusedLeadingIconColor = Brand.Teal600,
            focusedPlaceholderColor = Color(0xFF64748B),
            unfocusedPlaceholderColor = Color(0xFF64748B),
            cursorColor = Brand.Teal600
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FilterPill(text: String, selected: Boolean, live: Boolean = false, onClick: () -> Unit) {
    val accent = if (live) ScoreColors.Green else MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (selected) accent else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) accent else AppTheme.colors.cardBorder, CircleShape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (live) Icon(Icons.Rounded.Videocam, contentDescription = null, tint = if (selected) Color.White else accent, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CodexRow(item: AsanaCodexItem, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, ambientColor = Color(0x10071A2F), spotColor = Color(0x22071A2F))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppTheme.colors.cardBorder, shape)
            .clickable(role = Role.Button, onClickLabel = "Open ${item.englishName}", onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box {
            SkeletonThumbnail(item, item.defaultView, Modifier.width(68.dp))
            if (item.isLive) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(ScoreColors.Green),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("${item.number}. ${item.englishName}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(item.sanskritName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(item.everydayBenefit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                Tag(item.difficulty.label, if (item.difficulty == Difficulty.INTERMEDIATE) ScoreColors.Amber else AppTheme.colors.inkMuted)
                if (item.isLive) Tag("Live-scored", ScoreColors.Green)
            }
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = AppTheme.colors.inkMuted)
    }
}

@Composable
internal fun Tag(text: String, color: Color) {
    Pill(text, color = color, container = color.copy(alpha = 0.12f))
}
