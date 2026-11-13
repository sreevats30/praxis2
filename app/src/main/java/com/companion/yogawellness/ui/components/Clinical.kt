package com.companion.yogawellness.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Brand
import com.companion.yogawellness.ui.theme.OverlineStyle

// ---------------------------------------------------------------------------------------------
// Structure
// ---------------------------------------------------------------------------------------------

/**
 * Brand top bar: navy→teal gradient with rounded lower corners, drawn behind the status bar.
 * [bottom] hosts extras such as a progress bar or a summary row.
 */
@Composable
fun ClinicalTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottom: (@Composable ColumnScope.() -> Unit)? = null
) {
    val c = AppTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(c.heroBrush)
    ) {
        HeroDecor(Modifier.matchParentSize(), intensity = 0.7f)
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = if (bottom != null) 18.dp else 14.dp)
        ) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                } else {
                    Spacer(Modifier.width(8.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(
                        title, style = MaterialTheme.typography.titleLarge, color = Color.White,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    subtitle?.let {
                        Text(
                            it, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { actions() }
            }
            if (bottom != null) {
                Column(Modifier.padding(horizontal = 10.dp).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bottom()
                }
            }
        }
    }
}

/** Translucent circular icon button for use on gradients and the camera feed. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    container: Color = Color.White.copy(alpha = 0.14f),
    size: Dp = 42.dp
) {
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .clip(CircleShape)
            .background(container)
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

/** Elevated white card with a hairline border and a soft navy-tinted shadow. */
@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.surface,
    brush: Brush? = null,
    shape: Shape = MaterialTheme.shapes.large,
    borderColor: Color? = AppTheme.colors.cardBorder,
    elevation: Dp = 10.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shadowed = if (elevation > 0.dp) {
        modifier.shadow(elevation, shape, ambientColor = Color(0x10071A2F), spotColor = Color(0x22071A2F))
    } else modifier
    val border = borderColor?.let { BorderStroke(1.dp, it) }
    val inner: @Composable () -> Unit = {
        Column(
            (if (brush != null) Modifier.background(brush) else Modifier).padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content
        )
    }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = shadowed, shape = shape, color = color, border = border) { inner() }
    } else {
        Surface(modifier = shadowed, shape = shape, color = color, border = border) { inner() }
    }
}

@Composable
fun Overline(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Text(text.uppercase(), style = OverlineStyle, color = color, modifier = modifier)
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            overline?.let { Overline(it, color = AppTheme.colors.inkMuted) }
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        }
        action?.invoke()
    }
}

// ---------------------------------------------------------------------------------------------
// Atoms
// ---------------------------------------------------------------------------------------------

@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = size * 0.55f,
    shape: Shape = RoundedCornerShape(size * 0.32f),
    filled: Boolean = false
) {
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(
                if (filled) Brush.linearGradient(listOf(tint, tint.copy(alpha = 0.78f)))
                else SolidColor(tint.copy(alpha = 0.13f))
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (filled) Color.White else tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    container: Color = color.copy(alpha = 0.12f),
    border: Color? = null,
    iconTint: Color = color
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(container)
            .then(if (border != null) Modifier.border(1.dp, border, CircleShape) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
    }
}

/** Frosted pill for text on the hero gradient or camera feed. */
@Composable
fun GlassPill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, iconTint: Color = Brand.Mint) {
    Pill(
        text, modifier, icon,
        color = Color.White,
        container = Color.White.copy(alpha = 0.13f),
        border = Color.White.copy(alpha = 0.16f),
        iconTint = iconTint
    )
}

/** Primary call to action: brand gradient, gentle press-scale, disabled state that still reads clearly. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    brush: Brush = AppTheme.colors.ctaBrush,
    contentColor: Color = Color.White,
    height: Dp = 56.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")
    val shape = RoundedCornerShape(18.dp)
    val disabledBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val fg = if (enabled) contentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = Color.Transparent,
        interactionSource = interaction,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (enabled) Modifier.shadow(12.dp, shape, ambientColor = Brand.Teal600.copy(alpha = 0.25f), spotColor = Brand.Teal600.copy(alpha = 0.35f))
                else Modifier
            )
            .height(height)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(if (enabled) brush else SolidColor(disabledBg)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                icon?.let { Icon(it, contentDescription = null, tint = fg, modifier = Modifier.size(22.dp)) }
                Text(
                    text, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp)
                )
                trailingIcon?.let { Icon(it, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

/** Secondary action: tinted fill in the accent colour. */
@Composable
fun SoftButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    container: Color = color.copy(alpha = 0.11f),
    enabled: Boolean = true,
    height: Dp = 48.dp
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = container,
        contentColor = color,
        modifier = modifier.height(height)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp)) }
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Data display
// ---------------------------------------------------------------------------------------------

/** Circular progress ring that sweeps in from zero; [content] sits in the middle. */
@Composable
fun RingGauge(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = color.copy(alpha = 0.14f),
    stroke: Dp = 8.dp,
    brush: Brush? = null,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        anim.animateTo(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing))
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val s = stroke.toPx()
            val topLeft = Offset(s / 2, s / 2)
            val arc = Size(size.width - s, size.height - s)
            drawArc(trackColor, 0f, 360f, false, topLeft, arc, style = Stroke(s))
            val sweep = 360f * anim.value
            if (sweep > 0.5f) {
                rotate(-90f) {
                    if (brush != null) drawArc(brush, 0f, sweep, false, topLeft, arc, style = Stroke(s, cap = StrokeCap.Round))
                    else drawArc(color, 0f, sweep, false, topLeft, arc, style = Stroke(s, cap = StrokeCap.Round))
                }
            }
        }
        content()
    }
}

/** Thin rounded progress bar with an animated fill. */
@Composable
fun ProgressTrack(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = color.copy(alpha = 0.14f),
    height: Dp = 8.dp,
    brush: Brush? = null
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) { anim.animateTo(progress.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing)) }
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(trackColor, cornerRadius = r)
        val w = size.width * anim.value
        if (w > 0f) {
            val fillSize = Size(maxOf(w, size.height), size.height)
            if (brush != null) drawRoundRect(brush, size = fillSize, cornerRadius = r)
            else drawRoundRect(color, size = fillSize, cornerRadius = r)
        }
    }
}

/** KPI tile: icon badge, big value, label and an optional supporting line or delta pill. */
@Composable
fun MetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    sub: String? = null,
    delta: String? = null,
    deltaPositive: Boolean? = null
) {
    PanelCard(modifier, contentPadding = PaddingValues(16.dp), spacing = 10.dp, elevation = 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, accent, size = 36.dp)
            Spacer(Modifier.weight(1f))
            delta?.let {
                val c = when (deltaPositive) {
                    true -> AppTheme.colors.success
                    false -> AppTheme.colors.warning
                    null -> AppTheme.colors.inkMuted
                }
                Pill(it, color = c)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.inkMuted, maxLines = 1)
            sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.inkMuted, maxLines = 2) }
        }
    }
}

/** Icon + text row used for safety / privacy statements. */
@Composable
fun FeatureRow(icon: ImageVector, text: String, tint: Color, modifier: Modifier = Modifier, title: String? = null) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        IconBadge(icon, tint, size = 34.dp)
        Column(Modifier.weight(1f).padding(top = if (title == null) 7.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface) }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Callout with a coloured accent edge (info / warning / success). */
@Composable
fun Callout(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.09f))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface) }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f))
            content()
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Motion
// ---------------------------------------------------------------------------------------------

/** Staggered fade + rise entrance for stacked sections. */
@Composable
fun Reveal(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier,
        enter = fadeIn(tween(420, delayMillis = 55 * index)) +
            slideInVertically(tween(480, delayMillis = 55 * index, easing = FastOutSlowInEasing)) { it / 8 }
    ) { content() }
}
