package com.companion.yogawellness.ui.profile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.data.ProfileIds
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.wellness.Persona
import kotlinx.coroutines.launch

/** Stateless row of profile chips. [busyId] shows a spinner while that demo is being seeded. */
@Composable
fun ProfileSelector(activeId: String, busyId: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (listOf(ProfileIds.MY_ACCOUNT) + ProfileIds.DEMO_IDS).forEach { id ->
            val selected = id == activeId
            val accent = if (ProfileIds.isDemo(id)) AppTheme.colors.warning else MaterialTheme.colorScheme.primary
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(if (selected) accent else MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (selected) accent else AppTheme.colors.cardBorder, CircleShape)
                    .selectable(selected = selected, enabled = busyId == null, role = Role.Tab, onClick = { onSelect(id) })
                    .semantics { contentDescription = "Show ${ProfileIds.displayName(id)}" }
                    .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White.copy(alpha = 0.25f) else accent.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (busyId == id) {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = if (selected) Color.White else accent)
                    } else {
                        Icon(
                            if (ProfileIds.isDemo(id)) Icons.Rounded.Science else Icons.Rounded.Person,
                            contentDescription = null,
                            tint = if (selected) Color.White else accent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Text(
                    chipText(id),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
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
