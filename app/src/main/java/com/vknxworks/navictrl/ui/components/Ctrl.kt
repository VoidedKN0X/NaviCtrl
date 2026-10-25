package com.vknxworks.navictrl.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vknxworks.navictrl.R
import com.vknxworks.navictrl.ui.nav.LocalNavigator
import com.vknxworks.navictrl.ui.theme.CtrlColors
import com.vknxworks.navictrl.ui.theme.CtrlShapes
import com.vknxworks.navictrl.ui.theme.CtrlType
import com.vknxworks.navictrl.ui.theme.Gutter

/**
 * Ctrl / Signal building blocks shared by every screen: top bar pieces, cards, the floating
 * bottom bar, switches, buttons, chips, dividers and progress bars.
 */

/**
 * Space taken at the bottom of the screen by floating bars. Scrolling screens add it to their
 * bottom content padding, so their last items can scroll clear of the bars.
 */
val LocalBottomClearance = compositionLocalOf { 0.dp }

// ---------- Text ----------

/** "NAVI/CTRL", with the slash in the accent colour. */
@Composable
fun CtrlWordmark(name: String = "NAVI", modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            append(name)
            withStyle(SpanStyle(color = CtrlColors.AccentText)) { append("/") }
            append("CTRL")
        },
        style = CtrlType.Wordmark,
        color = CtrlColors.Text,
        modifier = modifier,
    )
}

/** Section label: mono, upper case, wide tracking, muted. */
@Composable
fun CtrlLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = CtrlColors.Muted,
    maxLines: Int = 1,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
) {
    Text(text.uppercase(), style = CtrlType.Label, color = color, modifier = modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis, textAlign = textAlign)
}

// ---------- Square controls ----------

/** 44dp square control in the top-bar style: surface fill, hairline border. */
@Composable
fun CtrlSquareButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = CtrlShapes.Control,
    active: Boolean = false,
    enabled: Boolean = true,
    iconSize: Dp = 20.dp,
    tint: Color = if (active) CtrlColors.OnAccent else CtrlColors.Text,
) {
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(if (active) CtrlColors.Accent else CtrlColors.Surface)
            .border(1.dp, if (active) CtrlColors.Accent else CtrlColors.ControlOutline, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription, Modifier.size(iconSize), tint = tint) }
}

/** Icon-only control without a fill (secondary actions next to the main ones). */
@Composable
fun CtrlGhostButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = CtrlColors.Muted,
    iconSize: Dp = 20.dp,
) {
    Box(
        modifier.size(size).clip(CtrlShapes.Button).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription, Modifier.size(iconSize), tint = tint) }
}

/** Back button for detail and sub screens. */
@Composable
fun CtrlBackButton(onClick: (() -> Unit)? = null, icon: ImageVector = Icons.AutoMirrored.Outlined.ArrowBack, description: String? = null) {
    val nav = LocalNavigator.current
    CtrlSquareButton(icon, description ?: stringResource(R.string.back), onClick = onClick ?: { nav.back() })
}

/**
 * Top bar of the main tabs: wordmark on the left, then any actions and the profile picture.
 */
@Composable
fun CtrlTopBar(
    modifier: Modifier = Modifier,
    showProfile: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CtrlWordmark(modifier = Modifier.weight(1f))
        actions()
        if (showProfile) ProfileButton()
    }
}

/** Top bar of sub screens: square back button and a title. */
@Composable
fun CtrlSubTopBar(
    title: String,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showBack) CtrlBackButton()
        Text(
            title,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
            color = CtrlColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

// ---------- Cards ----------

/** Secondary card: surface fill, hairline border, 22dp corners. */
@Composable
fun CtrlCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(CtrlShapes.Card)
            .background(CtrlColors.Surface)
            .border(1.dp, CtrlColors.Outline, CtrlShapes.Card)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Hero card: the one most important thing on a screen, in the accent colour with dark content. */
@Composable
fun CtrlHeroCard(
    modifier: Modifier = Modifier,
    label: String? = null,
    trailingLabel: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides CtrlColors.OnAccent) {
        Column(
            modifier
                .clip(CtrlShapes.Hero)
                .background(CtrlColors.Accent)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (label != null || trailingLabel != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CtrlLabel(label.orEmpty(), Modifier.weight(1f), color = CtrlColors.OnAccent)
                    if (trailingLabel != null) CtrlLabel(trailingLabel, color = CtrlColors.OnAccent)
                }
            }
            content()
        }
    }
}

// ---------- Lists ----------

/** Draws a 1dp list divider along the top edge, inset by [start] and [end]. */
@Composable
fun Modifier.topDivider(start: Dp = Gutter, end: Dp = Gutter, color: Color = CtrlColors.Divider): Modifier = drawBehind {
    val y = 0.5.dp.toPx()
    drawLine(color, Offset(start.toPx(), y), Offset(size.width - end.toPx(), y), strokeWidth = 1.dp.toPx())
}

/** 8dp status square: accent when active, grey when not. */
@Composable
fun StatusDot(active: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).clip(CtrlShapes.Dot).background(if (active) CtrlColors.AccentText else CtrlColors.Inactive))
}

// ---------- Switch ----------

/**
 * Rounded-rect switch, 52x32dp. On: accent track with a dark knob; off: grey track and knob.
 * With [onCheckedChange] null it only draws (the row around it handles the toggle and its semantics).
 */
@Composable
fun CtrlSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val knobX by animateDpAsState(if (checked) 20.dp else 0.dp, label = "knob")
    val track by animateColorAsState(if (checked) CtrlColors.Accent else CtrlColors.SwitchTrackOff, label = "track")
    val knob by animateColorAsState(if (checked) CtrlColors.OnAccent else CtrlColors.Inactive, label = "knob-color")
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(shape)
            .background(track)
            .then(
                if (onCheckedChange != null) Modifier.toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                ) else Modifier,
            )
            .alpha(if (enabled) 1f else 0.45f)
            .padding(4.dp),
    ) {
        Box(Modifier.offset(x = knobX).size(24.dp).clip(RoundedCornerShape(7.dp)).background(knob))
    }
}

// ---------- Buttons ----------

/** Main action: accent fill, dark text, 56dp tall. [secondary] gives the surface + border variant. */
@Composable
fun CtrlButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    secondary: Boolean = false,
    height: Dp = 56.dp,
    contentColor: Color? = null,
    containerColor: Color? = null,
    borderColor: Color? = null,
) {
    val shape = CtrlShapes.MainButton
    val fg = contentColor ?: if (secondary) CtrlColors.Text else CtrlColors.OnAccent
    val edge = borderColor ?: if (secondary) CtrlColors.ControlOutline else null
    Row(
        modifier
            .heightIn(min = height)
            .clip(shape)
            .background(containerColor ?: if (secondary) CtrlColors.Surface else CtrlColors.Accent)
            .then(if (edge != null) Modifier.border(1.dp, edge, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp), tint = fg)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Small selectable chip (filters, sort order): accent when selected, outlined otherwise. */
@Composable
fun CtrlChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = CtrlShapes.Badge
    Box(
        modifier
            .heightIn(min = 36.dp)
            .clip(shape)
            .background(if (selected) CtrlColors.Accent else CtrlColors.Surface)
            .border(1.dp, if (selected) CtrlColors.Accent else CtrlColors.ControlOutline, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) CtrlColors.OnAccent else CtrlColors.Text, maxLines = 1)
    }
}

// ---------- Progress ----------

/** 6dp progress bar with 3dp corners. Light fill by default; [accent] for the main metric. */
@Composable
fun CtrlProgressBar(progress: Float, modifier: Modifier = Modifier, accent: Boolean = false, height: Dp = 6.dp) {
    val shape = RoundedCornerShape(height / 2)
    Box(modifier.fillMaxWidth().height(height).clip(shape).background(CtrlColors.ProgressTrack)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(height).clip(shape).background(if (accent) CtrlColors.Accent else CtrlColors.ProgressFill))
    }
}

// ---------- Text fields ----------

/** Text field colours: surface fill, hairline border, accent border and cursor when focused. */
@Composable
fun ctrlTextFieldColors(container: Color = CtrlColors.Surface): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = container,
    unfocusedContainerColor = container,
    disabledContainerColor = container,
    focusedBorderColor = CtrlColors.AccentText,
    unfocusedBorderColor = CtrlColors.ControlOutline,
    disabledBorderColor = CtrlColors.Outline,
    cursorColor = CtrlColors.AccentText,
    focusedLabelColor = CtrlColors.AccentText,
    unfocusedLabelColor = CtrlColors.Muted,
    focusedTextColor = CtrlColors.Text,
    unfocusedTextColor = CtrlColors.Text,
    focusedLeadingIconColor = CtrlColors.Muted,
    unfocusedLeadingIconColor = CtrlColors.Muted,
    focusedTrailingIconColor = CtrlColors.Muted,
    unfocusedTrailingIconColor = CtrlColors.Muted,
    focusedPlaceholderColor = CtrlColors.Muted,
    unfocusedPlaceholderColor = CtrlColors.Muted,
)

/** Shape for text fields. */
val CtrlFieldShape = RoundedCornerShape(16.dp)

// ---------- Sheets and dialogs ----------

/** Hairline edge for bottom sheets (26dp top corners). */
@Composable
fun Modifier.ctrlSheetBorder(): Modifier =
    border(BorderStroke(1.dp, CtrlColors.Outline), RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))

/** Hairline edge for dialogs (26dp corners). */
@Composable
fun Modifier.ctrlDialogBorder(): Modifier = border(BorderStroke(1.dp, CtrlColors.Outline), CtrlShapes.Hero)

// ---------- Bottom bar ----------

data class TabItem(@StringRes val label: Int, val icon: ImageVector, val selectedIcon: ImageVector)

/**
 * Floating bottom bar: 64dp, 22dp corners, translucent surface with a hairline border. The selected
 * tab is an accent block with its icon and label; the others are icon-only.
 */
@Composable
fun CtrlBottomBar(
    items: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val shape = CtrlShapes.Card
    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .background(CtrlColors.BarFill)
            .border(1.dp, CtrlColors.ControlOutline, shape)
            .padding(horizontal = 8.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        items.forEachIndexed { i, item ->
            val selected = i == selectedIndex
            val label = stringResource(item.label)
            val itemShape = CtrlShapes.Button
            Row(
                Modifier
                    .height(48.dp)
                    .then(if (selected) Modifier else Modifier.width(48.dp))
                    .clip(itemShape)
                    .background(if (selected) CtrlColors.Accent else Color.Transparent)
                    .selectable(
                        selected = selected,
                        onClick = {
                            if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(i)
                        },
                        role = Role.Tab,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                    )
                    .padding(horizontal = if (selected) 18.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    if (selected) item.selectedIcon else item.icon,
                    contentDescription = if (selected) null else label,
                    tint = if (selected) CtrlColors.OnAccent else CtrlColors.Muted,
                    modifier = Modifier.size(if (selected) 18.dp else 20.dp),
                )
                if (selected) {
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.labelLarge, color = CtrlColors.OnAccent, maxLines = 1)
                }
            }
        }
    }
}

/** Square tile in the Signal style, used for empty-state icons and the lock badge. */
@Composable
fun CtrlIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    accent: Boolean = false,
    iconSize: Dp = 30.dp,
    content: (@Composable BoxScope.() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(if (accent) CtrlColors.Accent else CtrlColors.Surface)
            .then(if (accent) Modifier else Modifier.border(1.dp, CtrlColors.Outline, shape)),
        contentAlignment = Alignment.Center,
    ) {
        if (content != null) content()
        else Icon(icon, null, Modifier.size(iconSize), tint = if (accent) CtrlColors.OnAccent else CtrlColors.Muted)
    }
}
