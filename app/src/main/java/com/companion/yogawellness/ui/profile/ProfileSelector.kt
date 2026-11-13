package com.companion.yogawellness.ui.profile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
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
import com.companion.yogawellness.wellness.Persona
import kotlinx.coroutines.launch

/** Chip label: "👤 My Account" first, then the short demo labels. */
fun profileChipLabel(profileId: String): String =
    if (profileId == ProfileIds.MY_ACCOUNT) "👤 My Account" else Persona.forProfileId(profileId)?.shortLabel ?: profileId

/** Stateless row of profile chips. [busyId] shows a spinner while that demo is being seeded. */
@Composable
fun ProfileSelector(activeId: String, busyId: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (listOf(ProfileIds.MY_ACCOUNT) + ProfileIds.DEMO_IDS).forEach { id ->
            FilterChip(
                selected = id == activeId,
                onClick = { onSelect(id) },
                enabled = busyId == null,
                label = { Text(profileChipLabel(id)) },
                leadingIcon = if (busyId == id) {
                    { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) }
                } else null,
                modifier = Modifier.semantics { contentDescription = "Show ${ProfileIds.displayName(id)}" }
            )
        }
    }
}

/** Profile chips bound to the app's [com.companion.yogawellness.data.ActiveProfile]. */
@Composable
fun ProfileSelectorBar(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as YogaWellnessApp
    val active by app.activeProfile.id.collectAsStateWithLifecycle()
    var busy by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
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
