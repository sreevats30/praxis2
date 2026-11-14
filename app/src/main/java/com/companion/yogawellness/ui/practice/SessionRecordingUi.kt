package com.companion.yogawellness.ui.practice

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.data.SessionAutoRecorder
import com.companion.yogawellness.data.SessionReceipt
import com.companion.yogawellness.ui.theme.AppTheme
import java.util.Locale

/** "Remove animations" accessibility setting. */
@Composable
fun animationsDisabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        runCatching { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrDefault(1f) == 0f
    }
}

fun formatMmSs(seconds: Int): String = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)

fun recBadgeText(rec: SessionAutoRecorder.Rec): String =
    "REC ${formatMmSs(rec.elapsedSec)} · ${rec.posesSaved} ${if (rec.posesSaved == 1) "pose" else "poses"} saved"

fun recContentDescription(rec: SessionAutoRecorder.Rec): String =
    "Recording session, ${rec.elapsedSec} seconds, ${rec.posesSaved} ${if (rec.posesSaved == 1) "pose" else "poses"} saved"

/** `● REC 00:42 · 2 poses saved` with a slow pulse (static when animations are off). */
@Composable
fun RecBadge(rec: SessionAutoRecorder.Rec, modifier: Modifier = Modifier, compact: Boolean = false) {
    val static = animationsDisabled()
    val dotAlpha = if (static) 1f else {
        val transition = rememberInfiniteTransition(label = "rec")
        transition.animateFloat(
            initialValue = 1f, targetValue = 0.3f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
            label = "recDot"
        ).value
    }
    Surface(
        color = Color(0xB8000000),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        modifier = modifier.clearAndSetSemantics { contentDescription = recContentDescription(rec) }
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(9.dp)
                    .alpha(dotAlpha)
                    .background(Color(0xFFE5484D), CircleShape)
            )
            Text(
                if (compact) recBadgeText(rec).removeSuffix(" saved") else recBadgeText(rec),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

fun receiptLine(p: com.companion.yogawellness.data.ReceiptPose): String {
    val name = p.name.substringBefore(" (") + (p.sideLabel?.let { " · $it" } ?: "")
    val align = p.meanScore?.let { ", ${it.toInt()}% alignment" } ?: ""
    return String.format(Locale.US, "%s — %.1f s held, %.1f s at ≥75%%%s", name, p.lockedSec, p.qualitySec, align)
}

@Composable
fun SessionReceiptDialog(receipt: SessionReceipt, onViewHistory: () -> Unit, onKeepPractising: () -> Unit) {
    val c = AppTheme.colors
    AlertDialog(
        onDismissRequest = onKeepPractising,
        title = { Text("Session saved") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                receipt.poses.forEach { Text(receiptLine(it), style = MaterialTheme.typography.bodyMedium, color = c.text) }
                HorizontalDivider(color = c.border)
                Text(
                    String.format(Locale.US, "Total %.1f s", receipt.totalSec) +
                        (receipt.meanScore?.let { " · mean alignment ${it.toInt()}%" } ?: ""),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.text
                )
            }
        },
        confirmButton = { TextButton(onClick = onViewHistory) { Text("View History", color = c.accent) } },
        dismissButton = { TextButton(onClick = onKeepPractising) { Text("Keep practising", color = c.text) } }
    )
}
