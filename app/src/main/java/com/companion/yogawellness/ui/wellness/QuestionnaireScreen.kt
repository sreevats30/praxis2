package com.companion.yogawellness.ui.wellness

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.companion.yogawellness.data.db.QuestionnaireEntity
import com.companion.yogawellness.ui.components.Callout
import com.companion.yogawellness.ui.components.ClinicalTopBar
import com.companion.yogawellness.ui.components.GlassPill
import com.companion.yogawellness.ui.components.GradientButton
import com.companion.yogawellness.ui.components.IconBadge
import com.companion.yogawellness.ui.components.Overline
import com.companion.yogawellness.ui.components.PanelCard
import com.companion.yogawellness.ui.components.ProgressTrack
import com.companion.yogawellness.ui.components.RingGauge
import com.companion.yogawellness.ui.components.SoftButton
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Brand
import com.companion.yogawellness.ui.theme.NumericStyle
import com.companion.yogawellness.wellness.CrisisLines
import com.companion.yogawellness.wellness.QuestionnaireType
import com.companion.yogawellness.wellness.Questionnaires
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val SHORT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
fun formatEpochDay(day: Long): String = LocalDate.ofEpochDay(day).format(DATE)

private fun accentFor(type: QuestionnaireType) = if (type == QuestionnaireType.PHQ9) Brand.Mood else Color(0xFF2563EB)

/** Fortnightly PHQ-9 / GAD-7 self-reflection (§9.3). Raw scores only; never clinical labels. */
@Composable
fun QuestionnaireScreen(type: QuestionnaireType, onDone: () -> Unit, vm: WellnessViewModel = viewModel()) {
    val answers = remember { mutableStateListOf<Int?>().apply { repeat(type.items.size) { add(null) } } }
    var showCrisis by rememberSaveable { mutableStateOf(false) }
    var crisisShownOnce by rememberSaveable { mutableStateOf(false) }
    var saved by remember { mutableStateOf<QuestionnaireEntity?>(null) }
    val history by vm.history(type).collectAsStateWithLifecycle()
    val answered = answers.count { it != null }
    val accent = accentFor(type)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ClinicalTopBar(
                title = type.title,
                subtitle = "Self-reflection · last 2 weeks",
                onBack = onDone,
                bottom = {
                    if (saved == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "$answered of ${type.items.size} answered",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            GlassPill("Private", icon = Icons.Rounded.Lock)
                        }
                        ProgressTrack(
                            answered / type.items.size.toFloat(), Brand.Mint,
                            trackColor = Color.White.copy(alpha = 0.16f), height = 6.dp,
                            brush = Brush.horizontalGradient(listOf(Brand.Mint, Brand.Cyan))
                        )
                    } else {
                        GlassPill("Saved on this phone", icon = Icons.Rounded.CheckCircle)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val result = saved
            if (result != null) {
                QuestionnaireResult(type, result, history, Questionnaires.needsCrisisSupport(type, answers), onDone) { showCrisis = true }
            } else {
                Callout(Questionnaires.NON_DIAGNOSTIC, Icons.Rounded.Info, AppTheme.colors.info)
                Text(
                    Questionnaires.STEM,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
                type.items.forEachIndexed { i, item ->
                    QuestionCard(i + 1, type.items.size, item, answers[i], accent) { value ->
                        answers[i] = value
                        if (Questionnaires.needsCrisisSupport(type, answers) && !crisisShownOnce) {
                            showCrisis = true
                            crisisShownOnce = true
                        }
                    }
                }
                GradientButton(
                    "Save my answers",
                    { vm.saveQuestionnaire(type, answers.toList()) { saved = it } },
                    Modifier.fillMaxWidth(),
                    enabled = Questionnaires.isComplete(type, answers),
                    icon = Icons.Rounded.CheckCircle
                )
                Text(
                    Questionnaires.ATTRIBUTION,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.inkMuted,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }

    if (showCrisis) CrisisSupportSheet { showCrisis = false }
}

@Composable
private fun QuestionCard(number: Int, total: Int, text: String, selected: Int?, accent: Color, onSelect: (Int) -> Unit) {
    val done = selected != null
    PanelCard(spacing = 12.dp, elevation = if (done) 4.dp else 8.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (done) accent else accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (done) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                else Text("$number", style = MaterialTheme.typography.labelLarge, color = accent)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Overline("Question $number of $total", color = AppTheme.colors.inkMuted)
                Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Questionnaires.OPTIONS.forEachIndexed { value, label ->
                AnswerOption(label, value, selected == value, accent) { onSelect(value) }
            }
        }
    }
}

@Composable
private fun AnswerOption(label: String, points: Int, on: Boolean, accent: Color, onClick: () -> Unit) {
    val border by animateColorAsState(if (on) accent else AppTheme.colors.cardBorder, label = "border")
    val bg by animateColorAsState(if (on) accent.copy(alpha = 0.08f) else Color.Transparent, label = "bg")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.5.dp, border, RoundedCornerShape(14.dp))
            .selectable(selected = on, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (on) accent else MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (on) Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Box(
            Modifier
                .semantics { contentDescription = "$points points" }
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("$points", style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.inkMuted)
        }
    }
}

@Composable
private fun QuestionnaireResult(
    type: QuestionnaireType,
    result: QuestionnaireEntity,
    history: List<QuestionnaireEntity>,
    showSupport: Boolean,
    onDone: () -> Unit,
    onOpenSupport: () -> Unit
) {
    val accent = accentFor(type)
    PanelCard(spacing = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            RingGauge(
                result.totalScore / type.maxScore.toFloat(), accent, Modifier.size(112.dp), stroke = 10.dp,
                brush = Brush.sweepGradient(listOf(accent.copy(alpha = 0.5f), accent))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${result.totalScore}", style = NumericStyle.copy(fontSize = MaterialTheme.typography.displaySmall.fontSize), color = MaterialTheme.colorScheme.onSurface)
                    Text("of ${type.maxScore}", style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.inkMuted)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IconBadge(if (type == QuestionnaireType.PHQ9) Icons.Rounded.Psychology else Icons.Rounded.Spa, accent, size = 34.dp)
                Text("Self-Reflection Check-In Score", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("${result.totalScore} / ${type.maxScore}", style = MaterialTheme.typography.labelLarge, color = accent)
            }
        }
        Text(Questionnaires.NON_DIAGNOSTIC, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Questionnaires.nextDueEpochDay(type, result.epochDay)?.let {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("Next ${type.shortName} reflection: ${formatEpochDay(it)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
    if (showSupport) {
        SoftButton(
            "Talk to someone — Tele-MANAS ${CrisisLines.TELE_MANAS_SHORT}", onOpenSupport, Modifier.fillMaxWidth(),
            icon = Icons.Rounded.Call, color = Brand.Mind
        )
    }
    if (history.size > 1) {
        PanelCard(spacing = 12.dp, elevation = 6.dp) {
            Overline("History")
            Text("Your scores over time", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            HistoryBars(history, type.maxScore, accent)
            history.takeLast(6).reversed().forEach {
                Row {
                    Text(formatEpochDay(it.epochDay), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text("${it.totalScore} / ${type.maxScore}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
    GradientButton("Done", onDone, Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun HistoryBars(history: List<QuestionnaireEntity>, max: Int, accent: Color) {
    val items = history.takeLast(8)
    val track = accent.copy(alpha = 0.10f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(90.dp)
                .semantics { contentDescription = "Bar chart of your last ${items.size} scores" }
        ) {
            val n = items.size
            val gap = 10.dp.toPx()
            val bw = (size.width - gap * (n - 1)) / n
            items.forEachIndexed { i, q ->
                val x = i * (bw + gap)
                val r = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                drawRoundRect(track, Offset(x, 0f), Size(bw, size.height), r)
                val h = size.height * q.totalScore / max.toFloat()
                if (h > 0f) {
                    drawRoundRect(
                        Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.6f)), startY = size.height - h, endY = size.height),
                        Offset(x, size.height - h), Size(bw, h), r,
                        alpha = if (i == n - 1) 1f else 0.7f
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            items.forEach {
                Text(
                    LocalDate.ofEpochDay(it.epochDay).format(SHORT),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.inkMuted,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
