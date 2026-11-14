package com.companion.yogawellness.ui.profile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.data.ProfileIds
import com.companion.yogawellness.ui.components.Chip
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Space
import com.companion.yogawellness.wellness.Persona
import kotlinx.coroutines.launch

/**
 * Stateless row of profile chips, scrolling edge to edge with the page gutter as padding.
 * [busyId] shows a spinner while that demo is being seeded.
 */
@Composable
fun ProfileSelector(activeId: String, busyId: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Row(
        modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Space.Gutter, vertical = Space.XS),
        horizontalArrangement = Arrangement.spacedBy(Space.S),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (listOf(ProfileIds.MY_ACCOUNT) + ProfileIds.DEMO_IDS).forEach { id ->
            val selected = id == activeId
            Chip(
                chipText(id),
                selected = selected,
                enabled = busyId == null,
                onClick = { onSelect(id) },
                modifier = Modifier.semantics { contentDescription = "Show ${ProfileIds.displayName(id)}" },
                leading = if (busyId == id) ({
                    CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp, color = if (selected) c.onInverse else c.textSecondary)
                }) else null
            )
        }
    }
}

/** Chip text: "My Account" first, then the short demo labels ("Demo A" …). */
private fun chipText(profileId: String): String =
    if (profileId == ProfileIds.MY_ACCOUNT) "My Account" else Persona.forProfileId(profileId)?.shortLabel ?: profileId

/**
 * Profile chips bound to the app's [com.companion.yogawellness.data.ActiveProfile]. A new account
 * starts at zero with no demo affordances: the chips only appear once demo data has been loaded
 * (Settings → Demo data) or a demo is being viewed.
 */
@Composable
fun ProfileSelectorBar(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as YogaWellnessApp
    val active by app.activeProfile.id.collectAsStateWithLifecycle()
    val hasDemoData by remember { app.repository.observeHasDemoData() }.collectAsStateWithLifecycle(initialValue = false)
    var busy by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    if (!hasDemoData && !ProfileIds.isDemo(active)) return
    ProfileSelector(
        activeId = active,
        busyId = busy,
        onSelect = { id ->
            if (busy == null && id != active) {
                scope.launch {
                    busy = id
                    try {
                        app.profileSwitcher.select(id)
                    } finally {
                        busy = null
                    }
                }
            }
        },
        modifier = modifier
    )
}
