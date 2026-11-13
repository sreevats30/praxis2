package com.companion.yogawellness.ui.consent

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.ui.components.BrandMark
import com.companion.yogawellness.ui.components.Callout
import com.companion.yogawellness.ui.components.FeatureRow
import com.companion.yogawellness.ui.components.GlassPill
import com.companion.yogawellness.ui.components.GradientButton
import com.companion.yogawellness.ui.components.HeroDecor
import com.companion.yogawellness.ui.components.IconBadge
import com.companion.yogawellness.ui.components.Overline
import com.companion.yogawellness.ui.components.PanelCard
import com.companion.yogawellness.ui.components.ProgressTrack
import com.companion.yogawellness.ui.components.PulseLine
import com.companion.yogawellness.ui.components.Reveal
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Brand
import com.companion.yogawellness.wellness.CrisisLines

const val NON_DIAGNOSTIC_STATEMENT =
    "This app is a wellness and self-reflection companion, not a medical or diagnostic device."
const val PHYSICAL_SAFETY_STATEMENT =
    "Practice on a non-slip surface. Stop immediately if you feel any sharp pain, dizziness, or joint strain."

/** First-run consent, non-diagnostic disclaimer and physical safety screen (§9.1). */
@Composable
fun ConsentAndSafetyScreen(onAccepted: () -> Unit) {
    var notMedical by rememberSaveable { mutableStateOf(false) }
    var dataConsent by rememberSaveable { mutableStateOf(false) }
    var safety by rememberSaveable { mutableStateOf(false) }
    val confirmed = listOf(notMedical, dataConsent, safety).count { it }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        WelcomeHero()
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Reveal(0) { Pillars() }
            Reveal(1) {
                Callout(
                    NON_DIAGNOSTIC_STATEMENT,
                    Icons.Rounded.Info,
                    AppTheme.colors.info,
                    title = "Important"
                )
            }
            Reveal(2) {
                SectionCard("Practise safely", "Physical safety", Brand.Stress) {
                    FeatureRow(Icons.Rounded.WarningAmber, PHYSICAL_SAFETY_STATEMENT, Brand.Stress)
                    FeatureRow(Icons.Rounded.Accessibility, "Clear a space around you and keep your phone propped up, not held.", Brand.Stress)
                    FeatureRow(
                        Icons.Rounded.Favorite,
                        "If you are pregnant, injured, recovering from surgery or have a health condition, check with a doctor before practising.",
                        Brand.Stress
                    )
                    FeatureRow(Icons.Rounded.Timer, "Hold poses only as long as is comfortable. Chair Pose is capped at 30 seconds.", Brand.Stress)
                }
            }
            Reveal(3) {
                SectionCard("Your data stays on this phone", "Privacy", MaterialTheme.colorScheme.primary) {
                    FeatureRow(
                        Icons.Rounded.VisibilityOff,
                        "The camera image is analysed on this phone to estimate your pose. Frames are never saved or sent anywhere.",
                        MaterialTheme.colorScheme.primary
                    )
                    FeatureRow(
                        Icons.Rounded.CloudOff,
                        "The app has no internet permission. Check-ins, questionnaire answers and practice history are stored only on this device and are excluded from backups.",
                        MaterialTheme.colorScheme.primary
                    )
                    FeatureRow(
                        Icons.Rounded.DeleteForever,
                        "You can erase everything at any time with \"Delete All My Data\" in Settings.",
                        MaterialTheme.colorScheme.primary
                    )
                }
            }
            Reveal(4) {
                Callout(
                    "If you are going through a difficult time, India's Tele-MANAS helpline is free and confidential: " +
                        "${CrisisLines.TELE_MANAS_SHORT} or ${CrisisLines.TELE_MANAS_TOLL_FREE}. In an emergency, call ${CrisisLines.EMERGENCY}.",
                    Icons.Rounded.Call,
                    Brand.Mind,
                    title = "Support is always available"
                )
            }

            PanelCard(spacing = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Overline("Before you begin")
                        Text("Please confirm", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        "$confirmed of 3",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (confirmed == 3) AppTheme.colors.success else AppTheme.colors.inkMuted
                    )
                }
                ProgressTrack(confirmed / 3f, if (confirmed == 3) AppTheme.colors.success else MaterialTheme.colorScheme.primary, height = 6.dp)
                ConsentRow(notMedical, { notMedical = it }, "I understand this app does not diagnose or treat any condition.")
                ConsentRow(dataConsent, { dataConsent = it }, "I agree to on-device camera processing and to storing my data only on this device.")
                ConsentRow(safety, { safety = it }, "I will practise safely and stop if anything hurts.")
            }

            GradientButton(
                "Agree and continue",
                onAccepted,
                Modifier.fillMaxWidth(),
                enabled = notMedical && dataConsent && safety,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward
            )
            Text(
                "You can review these statements any time in Settings & privacy.",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.inkMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun WelcomeHero() {
    val c = AppTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
            .background(c.heroBrush)
    ) {
        HeroDecor(Modifier.matchParentSize())
        PulseLine(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
                .fillMaxWidth()
                .height(36.dp)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BrandMark(size = 64.dp)
            Spacer(Modifier.height(6.dp))
            Overline("Welcome", color = Brand.Mint)
            Text("Yoga Wellness Companion", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Text(
                "AI-guided posture coaching and validated self-reflection check-ins — private, on-device and built around your safety.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.78f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                GlassPill("Private", icon = Icons.Rounded.Lock)
                GlassPill("Offline", icon = Icons.Rounded.CloudOff)
                GlassPill("Free", icon = Icons.Rounded.Favorite, iconTint = Brand.Mint)
            }
        }
    }
}

@Composable
private fun Pillars() {
    PanelCard(spacing = 16.dp) {
        Overline("What you get")
        Pillar(Icons.Rounded.SelfImprovement, Brand.Practice, "Real-time posture analysis", "33 body landmarks, joint angles and a live alignment score for 4 asanas, with a voice coach.")
        Pillar(Icons.Rounded.Psychology, Brand.Mood, "Mind check-ins", "Daily mood, stress and sleep in 30 seconds; PHQ-9 and GAD-7 reflections every 14 days.")
        Pillar(Icons.Rounded.Insights, AppTheme.colors.info, "Honest insights", "Weekly trends with statistics that show their uncertainty — and a 1-page PDF report.")
    }
}

@Composable
private fun Pillar(icon: ImageVector, tint: Color, title: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconBadge(icon, tint, size = 44.dp, filled = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionCard(title: String, overline: String, accent: Color, content: @Composable () -> Unit) {
    PanelCard(spacing = 14.dp, elevation = 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Waves, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Column {
                Overline(overline, color = accent)
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        content()
    }
}

@Composable
private fun ConsentRow(checked: Boolean, onChange: (Boolean) -> Unit, text: String) {
    val accent = MaterialTheme.colorScheme.primary
    val border by animateColorAsState(if (checked) accent else AppTheme.colors.cardBorder, label = "border")
    val bg by animateColorAsState(if (checked) accent.copy(alpha = 0.07f) else Color.Transparent, label = "bg")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (checked) accent else Color.Transparent)
                .border(2.dp, if (checked) accent else MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
    }
}
