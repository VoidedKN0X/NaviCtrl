package com.vknxworks.navictrl.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vknxworks.navictrl.ui.components.ArtImage
import com.vknxworks.navictrl.ui.components.CtrlBackButton
import com.vknxworks.navictrl.ui.components.CtrlLabel
import com.vknxworks.navictrl.ui.theme.CtrlColors
import com.vknxworks.navictrl.ui.theme.CtrlShapes
import com.vknxworks.navictrl.ui.theme.CtrlType
import androidx.compose.foundation.border
import com.vknxworks.navictrl.ui.theme.Gutter
import androidx.compose.ui.res.stringResource
import com.vknxworks.navictrl.R

/**
 * Top of album, artist and playlist pages: artwork over a wash of its own colour, the title,
 * a tappable subtitle and a row of actions.
 */
@Composable
fun HeroHeader(
    artUrl: String?,
    title: String,
    subtitle: String? = null,
    onSubtitle: (() -> Unit)? = null,
    meta: String? = null,
    artShape: Shape = CtrlShapes.Hero,
    placeholderIcon: ImageVector = Icons.Outlined.MusicNote,
    label: String? = null,
    actions: @Composable () -> Unit = {},
) {
    Column(
        Modifier.fillMaxWidth().statusBarsPadding().padding(top = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Gutter), verticalAlignment = Alignment.CenterVertically) {
            CtrlBackButton()
        }
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .padding(horizontal = Gutter)
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .align(Alignment.CenterHorizontally)
                .clip(artShape)
                .background(CtrlColors.Surface)
                .border(1.dp, CtrlColors.Outline, artShape),
        ) { ArtImage(artUrl, Modifier.fillMaxSize(), placeholderIcon = placeholderIcon) }
        Spacer(Modifier.height(24.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = Gutter), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!label.isNullOrBlank()) CtrlLabel(label)
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                color = CtrlColors.Text,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (onSubtitle != null) CtrlColors.AccentText else CtrlColors.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(CtrlShapes.Chip)
                        .then(if (onSubtitle != null) Modifier.clickable(onClick = onSubtitle) else Modifier),
                )
            }
            if (!meta.isNullOrBlank()) {
                Text(meta.uppercase(), style = CtrlType.Mono, color = CtrlColors.Muted)
            }
        }
        Row(
            Modifier.padding(start = Gutter, end = Gutter, top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { actions() }
    }
}

/** Heading between discs on multi-disc albums. */
@Composable
fun DiscHeader(disc: Int) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CtrlShapes.Dot).background(CtrlColors.AccentText))
        Spacer(Modifier.width(10.dp))
        CtrlLabel(stringResource(R.string.disc_x, disc))
    }
}
