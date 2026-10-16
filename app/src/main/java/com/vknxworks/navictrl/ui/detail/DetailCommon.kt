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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vknxworks.navictrl.ui.components.ArtImage
import com.vknxworks.navictrl.ui.nav.LocalNavigator
import com.vknxworks.navictrl.ui.theme.Gutter

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
    artShape: Shape = MaterialTheme.shapes.large,
    placeholderIcon: ImageVector = Icons.Outlined.MusicNote,
    actions: @Composable () -> Unit = {},
) {
    val nav = LocalNavigator.current
    val wash = MaterialTheme.colorScheme.primaryContainer
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to wash, 1f to MaterialTheme.colorScheme.background)),
    ) {
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                IconButton(onClick = nav::back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            }
            Box(
                Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f)
                    .clip(artShape),
            ) { ArtImage(artUrl, Modifier.fillMaxSize(), placeholderIcon = placeholderIcon) }
            Spacer(Modifier.height(20.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = Gutter),
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (onSubtitle != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 4.dp, start = Gutter, end = Gutter)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .then(if (onSubtitle != null) Modifier.clickable(onClick = onSubtitle) else Modifier)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            if (!meta.isNullOrBlank()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, start = Gutter, end = Gutter),
                )
            }
            Row(
                Modifier.padding(top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { actions() }
        }
    }
}

/** Heading between discs on multi-disc albums. */
@Composable
fun DiscHeader(disc: Int) {
    Row(Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Spacer(Modifier.width(10.dp))
        Text("Disc $disc", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
