package com.companion.yogawellness.ui.wellness

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.ui.components.Hairline
import com.companion.yogawellness.ui.components.ListRow
import com.companion.yogawellness.ui.components.Panel
import com.companion.yogawellness.ui.components.PrimaryButton
import com.companion.yogawellness.ui.components.RowIcon
import com.companion.yogawellness.ui.theme.AppTheme
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
        containerColor = AppTheme.colors.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        CrisisSupportContent(onDismiss)
    }
}

@Composable
fun CrisisSupportContent(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val c = AppTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("You don't have to go through this alone", style = MaterialTheme.typography.headlineSmall, color = c.text)
        Text(
            "Thank you for answering honestly. If you are having thoughts of hurting yourself, please talk to someone now. " +
                "Tele-MANAS is India's free, confidential, 24×7 mental-health helpline.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary
        )
        PrimaryButton(
            "Call Tele-MANAS ${CrisisLines.TELE_MANAS_SHORT}",
            { dial(context, CrisisLines.TELE_MANAS_SHORT) },
            Modifier.fillMaxWidth(),
            icon = Icons.Outlined.Call
        )
        Panel(contentPadding = PaddingValues(0.dp), spacing = 0.dp) {
            ListRow(
                "Call ${CrisisLines.TELE_MANAS_TOLL_FREE}",
                supporting = "Tele-MANAS toll-free · 24×7",
                leading = { RowIcon(Icons.Outlined.SupportAgent) },
                trailing = { RowIcon(Icons.Outlined.Call, c.accent) },
                chevron = false,
                onClick = { dial(context, CrisisLines.TELE_MANAS_TOLL_FREE_DIAL) }
            )
            Hairline(inset = 52.dp)
            ListRow(
                "Emergency services ${CrisisLines.EMERGENCY}",
                supporting = "If you are in immediate danger",
                leading = { RowIcon(Icons.Outlined.LocalHospital, c.negative) },
                trailing = { RowIcon(Icons.Outlined.Call, c.negative) },
                chevron = false,
                onClick = { dial(context, CrisisLines.EMERGENCY) }
            )
        }
        Text(
            "If you are in immediate danger, call ${CrisisLines.EMERGENCY} or go to the nearest hospital. " +
                "This app cannot contact anyone for you.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textTertiary
        )
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Close", style = MaterialTheme.typography.labelLarge, color = c.text)
        }
        Spacer(Modifier.height(12.dp))
    }
}
