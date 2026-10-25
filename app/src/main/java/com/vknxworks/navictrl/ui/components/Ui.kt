package com.vknxworks.navictrl.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.ui.nav.LocalNavigator
import com.vknxworks.navictrl.ui.nav.YouRoute
import com.vknxworks.navictrl.ui.theme.CtrlColors
import com.vknxworks.navictrl.ui.theme.CtrlShapes
import com.vknxworks.navictrl.ui.theme.CtrlType
import com.vknxworks.navictrl.ui.theme.Gutter
import androidx.compose.ui.res.stringResource
import com.vknxworks.navictrl.R
import com.vknxworks.navictrl.str

// ---------- Top bars ----------

/** Top bar for pushed screens: square back button and a title. */
@Composable
fun ScreenTopBar(
    title: String,
    showBack: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CtrlSubTopBar(
        title,
        Modifier.statusBarsPadding().padding(start = Gutter, end = Gutter, top = 12.dp, bottom = 12.dp),
        showBack = showBack,
        actions = actions,
    )
}

/**
 * Header of the main tabs: the NAVI/CTRL wordmark with any actions and your profile picture,
 * then the tab's big title (and an optional mono label above it).
 */
@Composable
fun TabTitle(
    title: String,
    modifier: Modifier = Modifier,
    padded: Boolean = true,
    showProfile: Boolean = true,
    label: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth().padding(horizontal = if (padded) Gutter else 0.dp).padding(top = 12.dp, bottom = 12.dp)) {
        CtrlTopBar(showProfile = showProfile, actions = trailing)
        Spacer(Modifier.height(20.dp))
        if (label != null) {
            CtrlLabel(label)
            Spacer(Modifier.height(6.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineLarge, color = CtrlColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Small square action in a top bar (sort, new playlist...). */
@Composable
fun TopBarAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    CtrlSquareButton(icon, description, onClick = onClick)
}

/** The signed-in user's initials in a 44dp square; tapping it opens the You page (settings live there). */
@Composable
fun ProfileButton() {
    val nav = LocalNavigator.current
    val session by LocalContext.current.container.sessions.active.collectAsStateWithLifecycle()
    val name = session?.account?.username.orEmpty()
    val shape = CtrlShapes.Control
    Box(Modifier.size(47.dp), contentAlignment = Alignment.TopStart) {
        Box(
            Modifier
                .size(44.dp)
                .clip(shape)
                .background(CtrlColors.Surface)
                .border(1.dp, CtrlColors.ControlOutline, shape)
                .clickable(role = Role.Button) { nav.navigate(YouRoute) }
                .semantics { contentDescription = str(R.string.you) },
            contentAlignment = Alignment.Center,
        ) {
            if (name.isEmpty()) {
                Icon(Icons.Outlined.AccountCircle, null, Modifier.size(22.dp), tint = CtrlColors.Text)
            } else {
                Text(initials(name), style = MaterialTheme.typography.labelLarge, color = CtrlColors.Text)
            }
        }
        // Connected: a small accent square in the corner.
        if (session != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CtrlColors.Accent)
                    .border(2.dp, CtrlColors.Background, RoundedCornerShape(4.dp)),
            )
        }
    }
}

/** Up to two letters for an avatar. */
fun initials(name: String): String = name.filter { it.isLetterOrDigit() }.take(2).uppercase().ifEmpty { name.take(1).uppercase() }

// ---------- Grouped lists (settings, profile, admin) ----------

/** A card of related rows separated by hairlines, with an optional mono label above it. */
@Composable
fun Group(
    title: String? = null,
    modifier: Modifier = Modifier,
    footer: String? = null,
    padded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = if (padded) Gutter else 0.dp, vertical = 8.dp)) {
        if (title != null) CtrlLabel(title, Modifier.padding(start = 4.dp, bottom = 8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(CtrlShapes.Card)
                .background(CtrlColors.Surface)
                .border(1.dp, CtrlColors.Outline, CtrlShapes.Card),
        ) {
            // Every row draws a hairline on its top edge; the first one sits on the card's own border.
            content()
        }
        if (footer != null) {
            Text(
                footer,
                style = MaterialTheme.typography.bodySmall,
                color = CtrlColors.Muted,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun GroupRowContent(
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    iconTint: Color,
    titleColor: Color,
    showChevron: Boolean,
    modifier: Modifier,
    trailing: (@Composable () -> Unit)?,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .topDivider(start = 0.dp, end = 0.dp, color = CtrlColors.Outline)
            .then(modifier)
            .heightIn(min = 62.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                Modifier.size(36.dp).clip(CtrlShapes.Badge).background(CtrlColors.Raised),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, Modifier.size(20.dp), tint = iconTint) }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = CtrlColors.Muted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        } else if (showChevron) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = CtrlColors.Muted)
        }
    }
}

@Composable
fun GroupRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = CtrlColors.Accent,
    titleColor: Color = CtrlColors.Text,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    GroupRowContent(
        title, subtitle, icon, iconTint, titleColor, showChevron,
        if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        trailing,
    )
}

/** A row with a Signal switch; the whole row toggles it and reads as one switch to TalkBack. */
@Composable
fun GroupSwitch(title: String, subtitle: String? = null, checked: Boolean, icon: ImageVector? = null, onChange: (Boolean) -> Unit) {
    GroupRowContent(
        title, subtitle, icon, CtrlColors.Accent, CtrlColors.Text, showChevron = false,
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    ) { CtrlSwitch(checked, onCheckedChange = null) }
}

/** A row showing the current value; tapping opens a sheet to pick another. */
@Composable
fun <T> GroupChoice(
    title: String,
    options: List<Pair<T, String>>,
    selected: T?,
    icon: ImageVector? = null,
    fallbackLabel: String = "",
    onPick: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected }?.second ?: fallbackLabel
    GroupRow(title, current, icon, showChevron = true, onClick = { open = true })
    if (open) {
        ChoiceSheet(title, options, selected, onDismiss = { open = false }) { onPick(it); open = false }
    }
}

@Composable
fun <T> ChoiceSheet(
    title: String,
    options: List<Pair<T, String>>,
    selected: T?,
    onDismiss: () -> Unit,
    onPick: (T) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CtrlColors.Surface, modifier = Modifier.ctrlSheetBorder()) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        LazyColumn(Modifier.navigationBarsPadding(), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(options, key = { it.second }) { (value, label) ->
                val isSel = value == selected
                Row(
                    Modifier.fillMaxWidth().topDivider(start = 24.dp, end = 24.dp).heightIn(min = 52.dp).clickable { onPick(value) }.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSel) CtrlColors.Accent else CtrlColors.Text,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSel) Icon(Icons.Outlined.Check, stringResource(R.string.selected), tint = CtrlColors.Accent)
                }
            }
        }
    }
}

// ---------- Buttons ----------

/** The one main action on a screen (accent), or a secondary one (surface + border) with [secondary]. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    secondary: Boolean = false,
) {
    CtrlButton(text, onClick, modifier, icon = icon, enabled = enabled, secondary = secondary)
}

/** Square secondary action with a short label underneath. */
@Composable
fun RoundAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.widthIn(min = 64.dp).clip(CtrlShapes.Button).clickable(onClick = onClick).padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CtrlSquareButton(icon, label, onClick = onClick, size = 48.dp, shape = CtrlShapes.Button, active = active)
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) CtrlColors.Accent else CtrlColors.Muted,
            maxLines = 1,
        )
    }
}

// ---------- Text helpers ----------

/** Facts about an item laid out with space between them instead of separator characters. */
@Composable
fun MetaRow(parts: List<String>, modifier: Modifier = Modifier, color: Color = CtrlColors.Muted) {
    val visible = parts.filter { it.isNotBlank() }
    if (visible.isEmpty()) return
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        visible.forEach { Text(it, style = CtrlType.Mono, color = color) }
    }
}

// ---------- Seek bar ----------

/** Bar heights of the now-playing waveform (a fixed pattern; NaviCtrl doesn't analyse the audio). */
private val WaveHeights = floatArrayOf(
    .30f, .50f, .70f, .45f, .90f, .60f, .35f, .80f, .55f, .40f, .75f, .95f, .65f, .50f, .30f, .60f,
    .85f, .70f, .45f, .55f, 1f, .75f, .50f, .40f, .65f, .80f, .55f, .35f, .70f, .90f, .60f, .45f,
    .30f, .55f, .75f, .50f, .40f, .65f, .85f, .60f, .45f, .35f, .55f, .70f, .40f, .30f, .50f, .35f,
)

/**
 * Seek bar with a generous touch area. Tap to jump, drag to scrub.
 * [onScrub] fires while dragging; [onSeek] when the finger lifts.
 * With [wave] it draws as a row of bars (the now-playing screen); otherwise as a slim line.
 */
@Composable
fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onScrub: (Long) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    bufferedMs: Long = 0,
    activeColor: Color = CtrlColors.Accent,
    trackColor: Color = CtrlColors.ProgressTrack,
    bufferColor: Color = CtrlColors.WaveIdle,
    height: Dp = 6.dp,
    wave: Boolean = false,
    waveSeed: Int = 0,
) {
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val dur by rememberUpdatedState(durationMs)
    val fraction = if (dragging) dragFraction else if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val buffered = if (durationMs > 0) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Box(
        modifier
            .fillMaxWidth()
            .height(if (wave) 44.dp else 32.dp)
            .pointerInput(Unit) {
                detectTapGestures { o ->
                    if (dur > 0) onSeek((o.x / size.width * dur).toLong().coerceIn(0, dur))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> dragging = true; dragFraction = (o.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { dragging = false; onSeek((dragFraction * dur).toLong()) },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onScrub((dragFraction * dur).toLong())
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (wave) {
            Canvas(Modifier.fillMaxWidth().height(36.dp)) {
                val n = WaveHeights.size
                val gap = 3.dp.toPx()
                val w = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
                val r = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                for (i in 0 until n) {
                    val h = size.height * WaveHeights[(i + waveSeed).mod(n)]
                    val x = i * (w + gap)
                    val played = (x + w / 2) / size.width <= fraction
                    drawRoundRect(if (played) activeColor else bufferColor, Offset(x, (size.height - h) / 2), Size(w, h), r)
                }
            }
        } else {
            Canvas(Modifier.fillMaxWidth().height(if (dragging) height * 2 else height + 10.dp)) {
                val h = (if (dragging) height * 1.5f else height).toPx()
                val y = (size.height - h) / 2
                val r = CornerRadius(h / 2, h / 2)
                drawRoundRect(trackColor, Offset(0f, y), Size(size.width, h), r)
                if (buffered > 0f) drawRoundRect(bufferColor, Offset(0f, y), Size(size.width * buffered, h), r)
                drawRoundRect(activeColor, Offset(0f, y), Size(size.width * fraction, h), r)
            }
        }
    }
}
