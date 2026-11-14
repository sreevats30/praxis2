package com.companion.yogawellness.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.NumericStyle
import com.companion.yogawellness.ui.theme.Radius
import com.companion.yogawellness.ui.theme.SectionStyle
import com.companion.yogawellness.ui.theme.Space
import kotlin.math.roundToInt

// =============================================================================================
// Screen structure
// =============================================================================================

private val BarHeight = 52.dp

/**
 * Standard scrolling screen. A slim bar on the page colour holds back, a small title and actions.
 * Top-level screens ([largeTitle]) open with a large serif [headline] in the content; once it
 * scrolls away the bar's small title fades in and a hairline separates the bar from the content.
 * [eyebrow] is a short line shown in the bar while the large title is visible (Home's date).
 */
@Composable
fun AppScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    largeTitle: Boolean = onBack == null,
    headline: String = title,
    subtitle: String? = null,
    eyebrow: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollState: ScrollState = rememberScrollState(),
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }
    val collapsed by remember(scrollState) { derivedStateOf { scrollState.value > threshold } }
    val scrolled by remember(scrollState) { derivedStateOf { scrollState.value > 0 } }
    Column(modifier.fillMaxSize().background(AppTheme.colors.canvas)) {
        TopBar(title, onBack, largeTitle, eyebrow, showTitle = !largeTitle || collapsed, divider = if (largeTitle) collapsed else scrolled, actions = actions)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)) {
            if (largeTitle) LargeTitle(headline, subtitle = subtitle)
            content()
            Spacer(Modifier.height(Space.XXL))
            if (bottomBar == null) Spacer(Modifier.navigationBarsPadding())
        }
        bottomBar?.invoke()
    }
}

/** [AppScreen] for long lists: the large title is the list's first item. */
@Composable
fun AppLazyScreen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    largeTitle: Boolean = onBack == null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit
) {
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }
    val collapsed by remember(state) { derivedStateOf { state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > threshold } }
    val scrolled by remember(state) { derivedStateOf { state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0 } }
    Column(modifier.fillMaxSize().background(AppTheme.colors.canvas)) {
        TopBar(title, onBack, largeTitle, null, showTitle = !largeTitle || collapsed, divider = if (largeTitle) collapsed else scrolled, actions = actions)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = state, contentPadding = PaddingValues(bottom = Space.XXL)) {
            if (largeTitle) item(key = "large-title") { LargeTitle(title, subtitle = subtitle) }
            content()
            item(key = "nav-bar-space") { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    onBack: (() -> Unit)?,
    largeTitle: Boolean,
    eyebrow: String?,
    showTitle: Boolean,
    divider: Boolean,
    actions: @Composable RowScope.() -> Unit
) {
    val c = AppTheme.colors
    val titleAlpha by animateFloatAsState(if (showTitle) 1f else 0f, tween(180), label = "barTitle")
    val lineAlpha by animateFloatAsState(if (divider) 1f else 0f, tween(180), label = "barLine")
    Column(Modifier.fillMaxWidth().background(c.canvas).statusBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .padding(start = if (onBack != null) Space.XS else Space.Gutter, end = Space.XS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = c.text, modifier = Modifier.size(22.dp))
                }
            }
            Box(Modifier.weight(1f).padding(start = if (onBack != null) Space.XS else 0.dp), contentAlignment = Alignment.CenterStart) {
                if (eyebrow != null) {
                    Text(
                        eyebrow,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                        maxLines = 1,
                        modifier = Modifier.alpha(1f - titleAlpha)
                    )
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // On large-title screens this is a visual echo of the heading below: hidden from
                    // accessibility so the title is announced once.
                    modifier = Modifier
                        .alpha(titleAlpha)
                        .then(if (largeTitle) Modifier.clearAndSetSemantics {} else Modifier.semantics { heading() })
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line.copy(alpha = lineAlpha)))
    }
}

/** The serif title that opens a top-level screen. */
@Composable
fun LargeTitle(text: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier.fillMaxWidth().padding(start = Space.Gutter, end = Space.Gutter, top = Space.XXS, bottom = Space.S)) {
        Text(text, style = MaterialTheme.typography.displaySmall, color = AppTheme.colors.text, modifier = Modifier.semantics { heading() })
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/**
 * A titled block of the page. Sections are separated by a full-width hairline and spacing, never
 * by a card. [gutter] = false lets list rows run edge to edge (they bring their own padding).
 */
@Composable
fun Section(
    modifier: Modifier = Modifier,
    title: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    divider: Boolean = true,
    gutter: Boolean = true,
    spacing: Dp = Space.M,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxWidth()) {
        if (divider) Hairline()
        Column(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = Space.XL)) {
            if (title != null) {
                SectionHeader(title, Modifier.padding(start = Space.Gutter, end = if (trailing != null) Space.S else Space.Gutter), trailing)
                Spacer(Modifier.height(if (gutter) Space.M else Space.XS))
            }
            Column(
                if (gutter) Modifier.fillMaxWidth().padding(horizontal = Space.Gutter) else Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing),
                content = content
            )
        }
    }
}

/** Small sentence-case heading with an optional trailing action. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text, style = SectionStyle, color = AppTheme.colors.textSecondary,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        trailing?.invoke()
    }
}

/** Lets a child run edge to edge inside a padded column (horizontal carousels). */
fun Modifier.bleed(h: Dp) = layout { measurable, constraints ->
    val extra = h.roundToPx() * 2
    val p = measurable.measure(constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra))
    layout(p.width - extra, p.height) { p.place(-h.roundToPx(), 0) }
}

@Composable
fun Hairline(modifier: Modifier = Modifier, inset: Dp = 0.dp) {
    Box(modifier.padding(start = inset).fillMaxWidth().height(1.dp).background(AppTheme.colors.line))
}

@Composable
fun VerticalHairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxHeight().width(1.dp).background(AppTheme.colors.line))
}

// =============================================================================================
// Rows
// =============================================================================================

/** One row of a flat list. Edge to edge, with the page gutter as its own padding. */
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
            .padding(horizontal = Space.Gutter, vertical = Space.M),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.L)
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

/** Inset that aligns a divider with the text of [ListRow]s that have a [RowIcon]. */
val RowIconInset = Space.Gutter + 20.dp + Space.L

/** Leading glyph for list rows: plain outlined icon, never a coloured badge. */
@Composable
fun RowIcon(icon: ImageVector, tint: Color = AppTheme.colors.textSecondary) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
}

/** "Label ........ value" line for small tables (key angles, versions). */
@Composable
fun ValueRow(label: String, value: String, modifier: Modifier = Modifier, supporting: String? = null) {
    Row(modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.text)
            supporting?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textTertiary) }
        }
        Text(value, style = NumericStyle.copy(fontSize = 15.sp), color = AppTheme.colors.text)
    }
}

@Composable
fun BulletList(items: List<String>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { text ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.padding(top = 9.dp).size(4.dp).clip(CircleShape).background(AppTheme.colors.textTertiary))
                Text(text, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.text, modifier = Modifier.weight(1f))
            }
        }
    }
}

// =============================================================================================
// Actions
// =============================================================================================

/** The one main action of a view: ivory fill, ink label. [compact] for inline use. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    compact: Boolean = false
) {
    val c = AppTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = if (compact) 40.dp else 48.dp),
        shape = RoundedCornerShape(Radius.M),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.inverse, contentColor = c.onInverse,
            disabledContainerColor = c.control, disabledContentColor = c.textDisabled
        ),
        contentPadding = PaddingValues(horizontal = if (compact) 16.dp else 20.dp, vertical = 0.dp)
    ) {
        icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(Space.S)) }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Secondary action: outlined, transparent. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    compact: Boolean = false,
    content: Color = AppTheme.colors.text
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = if (compact) 40.dp else 48.dp),
        shape = RoundedCornerShape(Radius.M),
        border = BorderStroke(1.dp, AppTheme.colors.lineStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = content, containerColor = Color.Transparent),
        contentPadding = PaddingValues(horizontal = if (compact) 16.dp else 20.dp, vertical = 0.dp)
    ) {
        icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(Space.S)) }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Inline text action ("Edit", "Show as table"): accent label, 48dp touch target. */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.accent,
    trailingIcon: ImageVector? = null
) {
    Row(
        modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(Radius.S))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.S, vertical = Space.XS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
        trailingIcon?.let { Icon(it, contentDescription = null, tint = color, modifier = Modifier.size(18.dp)) }
    }
}

@Composable
fun IconAction(icon: ImageVector, contentDescription: String, onClick: () -> Unit, tint: Color = AppTheme.colors.text) {
    IconButton(onClick = onClick) { Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp)) }
}

// =============================================================================================
// Selection
// =============================================================================================

/**
 * Segmented control: a recessed track; the selected option is ivory with ink text, so the choice
 * reads at a glance. Radii are concentric (8 outside, 5 inside the 3dp track padding).
 */
@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = false
) {
    val c = AppTheme.colors
    Row(
        modifier
            .clip(RoundedCornerShape(Radius.M))
            .background(c.control)
            .padding(3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { i, label ->
            val isOn = i == selected
            Box(
                (if (fill) Modifier.weight(1f) else Modifier)
                    .heightIn(min = 38.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isOn) c.inverse else Color.Transparent)
                    .selectable(selected = isOn, role = Role.RadioButton, onClick = { onSelect(i) })
                    .padding(horizontal = if (fill) 6.dp else 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isOn) FontWeight.SemiBold else FontWeight.Medium),
                    color = if (isOn) c.onInverse else c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Filter / choice chip: ivory when selected, recessed otherwise. No borders. */
@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null
) {
    val c = AppTheme.colors
    Row(
        modifier
            .clip(RoundedCornerShape(Radius.M))
            .background(if (selected) c.inverse else c.control)
            .selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        leading?.invoke()
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = if (selected) c.onInverse else c.textSecondary,
            maxLines = 1
        )
    }
}

/**
 * 1–10 rating as ten cells. The chosen value is solid [color]; the cells below it are tinted so the
 * amount reads as a level. With [rangeSemantics] the row is announced and adjustable like a slider.
 * [clearable] lets a second tap on the chosen value clear it (optional ratings).
 */
@Composable
fun ScaleSelector(
    value: Int?,
    onChange: (Int?) -> Unit,
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
    rangeSemantics: Boolean = true,
    clearable: Boolean = false,
    ramp: Boolean = true
) {
    val c = AppTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .then(
                if (rangeSemantics) Modifier.semantics {
                    contentDescription = label
                    stateDescription = value?.let { "$it of 10" } ?: "$label not rated yet"
                    progressBarRangeInfo = ProgressBarRangeInfo(value?.toFloat() ?: 0f, 0f..10f, steps = 9)
                    setProgress { v -> onChange(v.roundToInt().coerceIn(1, 10)); true }
                } else Modifier.selectableGroup()
            ),
    ) {
        (1..10).forEach { n ->
            val chosen = value == n
            val below = ramp && value != null && n < value
            // The touch area spans the gaps between cells, so there is no dead zone between numbers.
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .selectable(selected = chosen, role = Role.RadioButton) { onChange(if (chosen && clearable) null else n) }
                    .padding(horizontal = 2.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(Radius.S))
                    .background(
                        when {
                            chosen -> color
                            below -> color.copy(alpha = 0.22f)
                            else -> c.control
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$n",
                    style = NumericStyle.copy(fontSize = 13.sp, fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Medium),
                    color = when {
                        chosen -> c.onInverse
                        below -> c.text
                        else -> c.textTertiary
                    }
                )
            }
        }
    }
}

@Composable
fun CheckRow(checked: Boolean, onChange: (Boolean) -> Unit, text: String, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .heightIn(min = 56.dp)
            .padding(start = Space.Gutter - 12.dp, end = Space.Gutter, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked, onCheckedChange = null, modifier = Modifier.padding(horizontal = 12.dp),
            colors = CheckboxDefaults.colors(checkedColor = c.accent, checkmarkColor = c.onAccent, uncheckedColor = c.lineStrong)
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.text, modifier = Modifier.weight(1f))
    }
}

@Composable
fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    val c = AppTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(start = Space.Gutter - 12.dp, end = Space.Gutter),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 12.dp),
            colors = RadioButtonDefaults.colors(selectedColor = c.accent, unselectedColor = c.lineStrong)
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = if (selected) c.text else c.textSecondary, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun AppSwitch(checked: Boolean) {
    val c = AppTheme.colors
    Switch(
        checked = checked, onCheckedChange = null,
        colors = SwitchDefaults.colors(
            checkedTrackColor = c.accent, checkedThumbColor = c.onAccent, checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = c.control, uncheckedThumbColor = c.textTertiary, uncheckedBorderColor = c.lineStrong
        )
    )
}

/** Search input: 44dp, recessed, no underline. */
@Composable
fun SearchField(query: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    BasicTextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.text),
        cursorBrush = SolidColor(c.accent),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { field ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(Radius.M))
                    .background(c.control)
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = c.textTertiary)
                    field()
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onChange("") }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = c.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    )
}

// =============================================================================================
// Status
// =============================================================================================

enum class Tone { NEUTRAL, ACCENT, POSITIVE, WARNING, NEGATIVE }

@Composable
fun toneColor(tone: Tone): Color {
    val c = AppTheme.colors
    return when (tone) {
        Tone.NEUTRAL -> c.textSecondary
        Tone.ACCENT, Tone.POSITIVE -> c.accent
        Tone.WARNING -> c.warning
        Tone.NEGATIVE -> c.negative
    }
}

@Composable
private fun toneFill(tone: Tone): Color {
    val c = AppTheme.colors
    return when (tone) {
        Tone.NEUTRAL -> c.control
        Tone.ACCENT, Tone.POSITIVE -> c.accentMuted
        Tone.WARNING -> c.warningMuted
        Tone.NEGATIVE -> c.negativeMuted
    }
}

/** Small label ("Scored live", "Critical"). Used sparingly; most metadata is plain text. */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, tone: Tone = Tone.NEUTRAL) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = toneColor(tone),
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(Radius.S))
            .background(toneFill(tone))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/** Dot + label status ("Due", "Done"): lighter than a tag. */
@Composable
fun StatusLabel(text: String, tone: Tone, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(toneColor(tone)))
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (tone == Tone.NEUTRAL) AppTheme.colors.textSecondary else toneColor(tone))
    }
}

/**
 * Inline message: a 2dp rule in the tone colour beside the text. No box, no icon — it sits in the
 * reading flow like a margin note.
 */
@Composable
fun Notice(
    text: String,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.NEUTRAL,
    title: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val c = AppTheme.colors
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            Modifier
                .width(2.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(1.dp))
                .background(if (tone == Tone.NEUTRAL) c.lineStrong else toneColor(tone))
        )
        Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = if (tone == Tone.NEUTRAL) c.text else toneColor(tone)) }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            action?.invoke()
        }
    }
}

/** Title, explanation and an optional action — the empty state of any list or dashboard. */
@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.S)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = AppTheme.colors.text)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary)
        action?.let { Spacer(Modifier.height(Space.S)); it() }
    }
}

@Composable
fun LoadingState(text: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(vertical = Space.XL), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = AppTheme.colors.textTertiary, trackColor = AppTheme.colors.control)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textTertiary)
    }
}

// =============================================================================================
// Data
// =============================================================================================

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
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textTertiary, maxLines = 2)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = NumericStyle.copy(fontSize = valueSize, lineHeight = valueSize * 1.2f), color = valueColor, maxLines = 2)
            unit?.let {
                Text(
                    " $it", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textTertiary,
                    modifier = Modifier.padding(bottom = 3.dp), maxLines = 1
                )
            }
        }
        caption?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textTertiary, maxLines = 2) }
    }
}

/** Thin horizontal meter (0..1). */
@Composable
fun Meter(progress: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 4.dp, track: Color = AppTheme.colors.control) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(500), label = "meter")
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        if (p > 0f) drawRoundRect(color, size = Size(size.width * p, size.height), cornerRadius = r)
    }
}

/** A 1–10 value as ten short segments. */
@Composable
fun SegmentMeter(value: Int, color: Color, modifier: Modifier = Modifier, max: Int = 10) {
    val track = AppTheme.colors.control
    Canvas(modifier.fillMaxWidth().height(3.dp)) {
        val gap = 2.dp.toPx()
        val w = (size.width - gap * (max - 1)) / max
        for (i in 0 until max) {
            drawRoundRect(
                if (i < value) color else track, Offset(i * (w + gap), 0f), Size(w, size.height),
                CornerRadius(size.height / 2, size.height / 2)
            )
        }
    }
}

/** Score ring used for live and per-result scores. */
@Composable
fun Ring(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    track: Color = AppTheme.colors.control,
    stroke: Dp = 5.dp,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(500), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val s = stroke.toPx()
            val tl = Offset(s / 2, s / 2)
            val sz = Size(size.width - s, size.height - s)
            drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(s))
            if (p > 0f) drawArc(color, -90f, 360f * p, false, tl, sz, style = Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}
