package com.companion.yogawellness.ui.wellness

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        CrisisSupportContent(onDismiss)
    }
}

@Composable
fun CrisisSupportContent(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("You don't have to go through this alone", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "Thank you for answering honestly. If you are having thoughts of hurting yourself, please talk to someone now. " +
                "Tele-MANAS is India's free, confidential, 24×7 mental-health helpline.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = { dial(context, CrisisLines.TELE_MANAS_SHORT) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Call, contentDescription = null)
            Text("  Call Tele-MANAS ${CrisisLines.TELE_MANAS_SHORT}")
        }
        FilledTonalButton(onClick = { dial(context, CrisisLines.TELE_MANAS_TOLL_FREE_DIAL) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Call, contentDescription = null)
            Text("  Call ${CrisisLines.TELE_MANAS_TOLL_FREE}")
        }
        FilledTonalButton(onClick = { dial(context, CrisisLines.EMERGENCY) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Call, contentDescription = null)
            Text("  Emergency services ${CrisisLines.EMERGENCY}")
        }
        Text(
            "If you are in immediate danger, call ${CrisisLines.EMERGENCY} or go to the nearest hospital. " +
                "This app cannot contact anyone for you.",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        Spacer(Modifier.height(16.dp))
    }
}
