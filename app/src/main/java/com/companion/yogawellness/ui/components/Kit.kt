package com.companion.yogawellness.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.NumericStyle

// =============================================================================================
// Screen structure
// =============================================================================================

/**
 * Standard screen: Material top app bar on the page background, a hairline once content scrolls
 * under it. [large] gives a large title that collapses on scroll (top-level and summary screens).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    large: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val c = AppTheme.colors
    val behavior = if (large) TopAppBarDefaults.exitUntilCollapsedScrollBehavior() else TopAppBarDefaults.pinnedScrollBehavior()
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = c.background,
        scrolledContainerColor = c.background,
        titleContentColor = c.text,
        navigationIconContentColor = c.text,
        actionIconContentColor = c.text
    )
    val navigation: @Composable () -> Unit = {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
        }
    }
    Scaffold(
        modifier = modifier.nestedScroll(behavior.nestedScrollConnection),
        containerColor = c.background,
        topBar = {
            Column {
                if (large) {
                    LargeTopAppBar(
                        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        navigationIcon = navigation, actions = actions, colors = colors, scrollBehavior = behavior
                    )
                } else {
                    TopAppBar(
                        title = { Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        navigationIcon = navigation, actions = actions, colors = colors, scrollBehavior = behavior
                    )
                }
                val scrolled = behavior.state.overlappedFraction > 0.01f || (large && behavior.state.collapsedFraction > 0.98f)
                HorizontalDivider(color = if (scrolled) c.border else Color.Transparent)
            }
        },
        bottomBar = bottomBar,
        content = content
    )
}

/** Scrolling page body with the standard gutters and rhythm. */
@Composable
fun ScreenColumn(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

/** Section label above a group of cards, sentence case. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 12.dp)
            .heightIn(min = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.textSecondary, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

// =============================================================================================
// Surfaces
// =============================================================================================

/** Flat card: surface colour, hairline border, 12dp corners. No shadow, no gradient. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = AppTheme.colors.surface,
    border: Color? = AppTheme.colors.border,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    val stroke = border?.let { BorderStroke(1.dp, it) }
    val inner: @Composable () -> Unit = {
        Column(Modifier.padding(contentPadding), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
    }
    if (onClick != null) Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = stroke) { inner() }
    else Surface(modifier = modifier, shape = shape, color = color, border = stroke) { inner() }
}

@Composable
fun Hairline(modifier: Modifier = Modifier, inset: Dp = 0.dp) {
    HorizontalDivider(modifier.padding(start = inset), thickness = 1.dp, color = AppTheme.colors.border)
}

@Composable
fun VerticalHairline(modifier: Modifier = Modifier) {
    VerticalDivider(modifier.fillMaxHeight(), thickness = 1.dp, color = AppTheme.colors.border)
}

/** Title + optional supporting line at the top of a card. */
@Composable
fun PanelHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.text)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textTertiary) }
        }
        trailing?.invoke()
    }
}

/** One row of a grouped list. Rows inside a [Panel] use zero panel padding and [Hairline] between them. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    chevron: Boolean = onClick != null,
    titleColor: Color = AppTheme.colors.text,
    onClickLabel: String? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            supporting?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textTertiary) }
        }
        trailing?.invoke()
        if (chevron) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = AppTheme.colors.textTertiary, modifier = Modifier.size(20.dp))
        }
    }
}

/** Leading icon for list rows: plain outlined glyph, no coloured badge. */
@Composable
fun RowIcon(icon: ImageVector, tint: Color = AppTheme.colors.textSecondary) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
}

// =============================================================================================
// Actions
// =============================================================================================

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    container: Color = AppTheme.colors.accent,
    content: Color = AppTheme.colors.onAccent
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container, contentColor = content,
            disabledContainerColor = AppTheme.colors.sunken, disabledContentColor = AppTheme.colors.textTertiary
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) {
        icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    content: Color = AppTheme.colors.text
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, AppTheme.colors.borderStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = content, containerColor = AppTheme.colors.surface),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    ) {
        icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Segmented control: recessed track, the selected option raised on a surface chip.
 * [onDark] is the variant used on the dark 3D viewer.
 */
@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onDark: Boolean = false,
    fill: Boolean = false
) {
    val c = AppTheme.colors
    val track = if (onDark) Color.White.copy(alpha = 0.10f) else c.sunken
    val chip = if (onDark) Color.White else c.surface
    val on = if (onDark) Color(0xFF111418) else c.text
    val off = if (onDark) Color.White.copy(alpha = 0.78f) else c.textSecondary
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(track)
            .padding(2.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { i, label ->
            val isOn = i == selected
            Box(
                (if (fill) Modifier.weight(1f) else Modifier)
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isOn) chip else Color.Transparent)
                    .then(if (isOn && !onDark) Modifier.border(1.dp, c.border, RoundedCornerShape(8.dp)) else Modifier)
                    .selectable(selected = isOn, role = Role.RadioButton, onClick = { onSelect(i) })
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (isOn) on else off, maxLines = 1)
            }
        }
    }
}

// =============================================================================================
// Status & data
// =============================================================================================

enum class Tone { NEUTRAL, ACCENT, POSITIVE, WARNING, NEGATIVE }

@Composable
private fun toneColors(tone: Tone): Pair<Color, Color> {
    val c = AppTheme.colors
    return when (tone) {
        Tone.NEUTRAL -> c.sunken to c.textSecondary
        Tone.ACCENT -> c.accentSoft to c.onAccentSoft
        Tone.POSITIVE -> c.positiveSoft to c.positive
        Tone.WARNING -> c.warningSoft to c.warning
        Tone.NEGATIVE -> c.negativeSoft to c.negative
    }
}

/** Small status tag ("Due", "Up to date", "Live-scored"). */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, tone: Tone = Tone.NEUTRAL) {
    val (bg, fg) = toneColors(tone)
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/** Inline message: tone-tinted background, icon, text, optional action row. */
@Composable
fun Notice(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.NEUTRAL,
    title: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val (bg, fg) = toneColors(tone)
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = AppTheme.colors.text) }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.text)
            action?.invoke()
        }
    }
}

/** A labelled number: "Practice / 192 min". Merged for screen readers. */
@Composable
fun Stat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    caption: String? = null,
    valueColor: Color = AppTheme.colors.text,
    valueSize: TextUnit = 24.sp
) {
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textTertiary, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = NumericStyle.copy(fontSize = valueSize, lineHeight = valueSize * 1.15f), color = valueColor, maxLines = 1)
            unit?.let {
                Text(
                    " $it", style = MaterialTheme.typography.labelLarge, color = AppTheme.colors.textTertiary,
                    modifier = Modifier.padding(bottom = 2.dp), maxLines = 1
                )
            }
        }
        caption?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textTertiary, maxLines = 2) }
    }
}

/** Thin horizontal meter (0..1). */
@Composable
fun Meter(progress: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 4.dp, track: Color = AppTheme.colors.sunken) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(500), label = "meter")
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        if (p > 0f) drawRoundRect(color, size = Size(size.width * p, size.height), cornerRadius = r)
    }
}

/** Score ring used for live and per-result scores. */
@Composable
fun Ring(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    track: Color = AppTheme.colors.sunken,
    stroke: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(500), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val s = stroke.toPx()
            val tl = androidx.compose.ui.geometry.Offset(s / 2, s / 2)
            val sz = Size(size.width - s, size.height - s)
            drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(s))
            if (p > 0f) drawArc(color, -90f, 360f * p, false, tl, sz, style = Stroke(s, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        content()
    }
}
