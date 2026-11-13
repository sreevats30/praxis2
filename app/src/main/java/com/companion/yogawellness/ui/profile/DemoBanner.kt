package com.companion.yogawellness.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.companion.yogawellness.YogaWellnessApp
import com.companion.yogawellness.data.ProfileIds

/** Amber banner colours; ink on background is ~11:1 contrast (>= 4.5:1 required). */
private val BannerBackground = Color(0xFFFFF1C7)
private val BannerEdge = Color(0xFFF5C451)
private val BannerInk = Color(0xFF3B2A00)

fun demoBannerText(profileId: String) =
    "Viewing demo data (${ProfileIds.displayName(profileId)}). Your own data is safe."

/** Shown only while a demo profile is active. */
@Composable
fun DemoBanner(profileId: String, onBackToMyAccount: () -> Unit, modifier: Modifier = Modifier) {
    if (!ProfileIds.isDemo(profileId)) return
    Surface(
        color = BannerBackground,
        contentColor = BannerInk,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BannerEdge),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.Science, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(demoBannerText(profileId), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = BannerInk)
                Surface(
                    onClick = onBackToMyAccount,
                    shape = RoundedCornerShape(12.dp),
                    color = BannerInk,
                    contentColor = Color.White
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Back to My Account", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
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
