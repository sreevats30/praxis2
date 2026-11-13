package com.companion.yogawellness.ui.wellness

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.ui.components.GradientButton
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Brand
import com.companion.yogawellness.wellness.CrisisLines

/** Opens the dialer pre-filled (no CALL_PHONE permission needed; the user presses call). */
fun dial(context: Context, number: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // No dialer (e.g. a tablet): the numbers stay visible on screen.
    }
}

/**
 * Calm, dismissible support sheet shown immediately when PHQ-9 item 9 is answered with
 * "Several days" or more (§9.3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrisisSupportSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        CrisisSupportContent(onDismiss)
    }
}

@Composable
fun CrisisSupportContent(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(72.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Brand.Mind.copy(alpha = 0.22f), Brand.Mind.copy(alpha = 0.06f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Brand.Mind, modifier = Modifier.size(34.dp))
        }
        Text(
            "You don't have to go through this alone",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Thank you for answering honestly. If you are having thoughts of hurting yourself, please talk to someone now. " +
                "Tele-MANAS is India's free, confidential, 24×7 mental-health helpline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        GradientButton(
            "Call Tele-MANAS ${CrisisLines.TELE_MANAS_SHORT}",
            { dial(context, CrisisLines.TELE_MANAS_SHORT) },
            Modifier.fillMaxWidth(),
            icon = Icons.Rounded.Call,
            height = 60.dp
        )
        HelplineRow(
            Icons.Rounded.SupportAgent, "Call ${CrisisLines.TELE_MANAS_TOLL_FREE}", "Tele-MANAS toll-free · free · 24×7",
            MaterialTheme.colorScheme.primary
        ) { dial(context, CrisisLines.TELE_MANAS_TOLL_FREE_DIAL) }
        HelplineRow(
            Icons.Rounded.Emergency, "Emergency services ${CrisisLines.EMERGENCY}", "If you are in immediate danger",
            AppTheme.colors.danger
        ) { dial(context, CrisisLines.EMERGENCY) }
        Text(
            "If you are in immediate danger, call ${CrisisLines.EMERGENCY} or go to the nearest hospital. " +
                "This app cannot contact anyone for you.",
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.inkMuted,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close", style = MaterialTheme.typography.labelLarge) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun HelplineRow(icon: ImageVector, title: String, sub: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.07f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.inkMuted)
            }
            Icon(Icons.Rounded.Call, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
    }
}
