package com.companion.yogawellness.ui.profile

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.data.ProfileIds
import com.companion.yogawellness.ui.components.Notice
import com.companion.yogawellness.ui.components.TextAction
import com.companion.yogawellness.ui.components.Tone
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Space

fun demoBannerText(profileId: String) =
    "Viewing demo data (${ProfileIds.displayName(profileId)}). Your own data is safe."

/** Shown only while a demo profile is active. */
@Composable
fun DemoBanner(profileId: String, onBackToMyAccount: () -> Unit, modifier: Modifier = Modifier) {
    if (!ProfileIds.isDemo(profileId)) return
    Notice(
        demoBannerText(profileId),
        modifier.padding(vertical = Space.S),
        tone = Tone.WARNING,
        title = "Demo data",
        action = { TextAction("Back to My Account", onBackToMyAccount, Modifier.offset(x = (-8).dp), color = AppTheme.colors.text) }
    )
}

/** [DemoBanner] bound to the app's active profile. */
@Composable
fun DemoBannerBar(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as YogaWellnessApp
    val active by app.activeProfile.id.collectAsStateWithLifecycle()
    DemoBanner(active, onBackToMyAccount = app.activeProfile::returnToMyAccount, modifier = modifier)
}
