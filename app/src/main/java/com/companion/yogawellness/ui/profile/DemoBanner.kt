package com.companion.yogawellness.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.data.ProfileIds

/** Amber banner colours; ink on background is ~11:1 contrast (>= 4.5:1 required). */
private val BannerBackground = Color(0xFFFFE7A3)
private val BannerInk = Color(0xFF3B2A00)

fun demoBannerText(profileId: String) =
    "Viewing demo data (${ProfileIds.displayName(profileId)}). Your own data is safe."

/** Shown only while a demo profile is active. */
@Composable
fun DemoBanner(profileId: String, onBackToMyAccount: () -> Unit, modifier: Modifier = Modifier) {
    if (!ProfileIds.isDemo(profileId)) return
    Surface(color = BannerBackground, contentColor = BannerInk, shape = RoundedCornerShape(12.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(demoBannerText(profileId), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = BannerInk)
            OutlinedButton(
                onClick = onBackToMyAccount,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BannerInk)
            ) { Text("Back to My Account") }
        }
    }
}

/** [DemoBanner] bound to the app's active profile. */
@Composable
fun DemoBannerBar(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as YogaWellnessApp
    val active by app.activeProfile.id.collectAsStateWithLifecycle()
    DemoBanner(active, onBackToMyAccount = app.activeProfile::returnToMyAccount, modifier = modifier)
}
