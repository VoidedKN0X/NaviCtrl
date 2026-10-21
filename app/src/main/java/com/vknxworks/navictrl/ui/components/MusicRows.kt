package com.vknxworks.navictrl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.api.Album
import com.vknxworks.navictrl.data.api.Artist
import com.vknxworks.navictrl.data.api.Song
import com.vknxworks.navictrl.data.api.coverArtUrl
import com.vknxworks.navictrl.ui.nav.LocalNavigator
import com.vknxworks.navictrl.ui.theme.Gutter
import androidx.compose.ui.res.stringResource
import com.vknxworks.navictrl.R

/**
 * One song in a list. Tap plays, long-press or the menu opens its actions.
 * [number] shows a track number in place of artwork (album pages).
 */
@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    number: Int? = null,
    showAlbum: Boolean = true,
    extraActions: List<SongAction> = emptyList(),
) {
    val c = LocalContext.current.container
    val playingId by c.music.currentId.collectAsStateWithLifecycle()
    val stars by c.stars.overrides.collectAsStateWithLifecycle()
    val offline by c.downloads.completedIds.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }
    val playing = playingId == song.id
    val starred = stars[song.id] ?: (song.starred != null)

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .combinedClickable(onClick = onClick, onLongClick = { menu = true })
            .padding(start = Gutter, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (number != null) {
            Box(Modifier.width(32.dp), contentAlignment = Alignment.CenterStart) {
                if (playing) Icon(Icons.Filled.GraphicEq, stringResource(R.string.playing), Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                else Text(
                    "$number",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Box(Modifier.size(48.dp).clip(MaterialTheme.shapes.extraSmall)) {
                ArtImage(c.api.coverArtUrl(song.coverArt, 160), Modifier.fillMaxSize())
                if (playing) {
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.GraphicEq, stringResource(R.string.playing), tint = MaterialTheme.colorScheme.primary) }
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song.id in offline) {
                    Icon(Icons.Filled.DownloadDone, stringResource(R.string.downloaded), Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                }
                if (starred) {
                    Icon(Icons.Filled.Favorite, stringResource(R.string.favourite), Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    song.subtitle(showAlbum).ifBlank { stringResource(R.string.unknown_artist) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        song.duration?.takeIf { it > 0 && number != null }?.let {
            Text(formatClock(it * 1000L), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { menu = true }) {
            Icon(Icons.Filled.MoreVert, stringResource(R.string.more_for_x, song.title), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (menu) SongActionsSheet(song, onDismiss = { menu = false }, extra = extraActions)
}

/** Placeholder row shown while a page of the songs list loads. */
@Composable
fun SongRowPlaceholder() {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = Gutter, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.surfaceContainerHigh))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.fillMaxWidth(0.6f).height(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh))
            Box(Modifier.fillMaxWidth(0.4f).height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer))
        }
    }
}

/** Square album card for grids and rows. */
@Composable
fun AlbumCard(album: Album, modifier: Modifier = Modifier, width: Dp? = null) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    var menu by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.small
    Column(
        (if (width != null) modifier.width(width) else modifier.fillMaxWidth())
            .clip(shape)
            .combinedClickable(onClick = { nav.album(album.id) }, onLongClick = { menu = true }),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(shape)) {
            ArtImage(c.api.coverArtUrl(album.coverArt, 300), Modifier.fillMaxSize(), placeholderText = album.name)
        }
        Text(
            album.name,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            listOfNotNull(album.artist, album.year?.toString()).joinToString("  ·  "),
            modifier = Modifier.padding(horizontal = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (menu) AlbumActionsSheet(album, onDismiss = { menu = false })
}

/** Album as a list row (artist pages on narrow screens, favourites). */
@Composable
fun AlbumRow(album: Album) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = { nav.album(album.id) }, onLongClick = { menu = true })
            .padding(horizontal = Gutter, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(MaterialTheme.shapes.extraSmall)) { ArtImage(c.api.coverArtUrl(album.coverArt, 160), Modifier.fillMaxSize()) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(album.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(album.artist, album.year?.toString(), countLabel(album.songCount, R.plurals.count_songs)).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (menu) AlbumActionsSheet(album, onDismiss = { menu = false })
}

@Composable
fun ArtistRow(artist: Artist) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = { nav.artist(artist.id) }).padding(horizontal = Gutter, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp).clip(CircleShape)) {
            ArtImage(c.api.coverArtUrl(artist.coverArt ?: "ar-${artist.id}", 160), Modifier.fillMaxSize(), placeholderIcon = Icons.Filled.Person)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(artist.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (artist.albumCount > 0) {
                Text(countLabel(artist.albumCount, R.plurals.count_albums), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Round artist tile for horizontal rows. */
@Composable
fun ArtistTile(artist: Artist, size: Dp = 112.dp) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    Column(
        Modifier.width(size).clip(MaterialTheme.shapes.medium).combinedClickable(onClick = { nav.artist(artist.id) }),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(size).clip(CircleShape)) {
            ArtImage(c.api.coverArtUrl(artist.coverArt ?: "ar-${artist.id}", 300), Modifier.fillMaxSize(), placeholderIcon = Icons.Filled.Person)
        }
        Text(
            artist.name,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Play and shuffle side by side, used at the top of song lists. */
@Composable
fun PlayShuffleRow(onPlay: () -> Unit, onShuffle: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillButton(stringResource(R.string.play), onClick = onPlay, icon = Icons.Filled.PlayArrow, modifier = Modifier.weight(1f))
        PillButton(
            stringResource(R.string.shuffle),
            onClick = onShuffle,
            icon = Icons.Filled.Shuffle,
            modifier = Modifier.weight(1f),
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        trailing()
    }
}

/** Small round icon button with a tinted background, for header actions. */
@Composable
fun CircleIconButton(icon: ImageVector, label: String, onClick: () -> Unit, active: Boolean = false) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Icon(icon, label, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

/** Section heading inside scrolling pages. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(start = Gutter, end = 8.dp, top = 20.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            androidx.compose.material3.TextButton(onClick = onAction) { Text(action) }
        }
    }
}

/** Width that fits the given number of cards across, used by horizontal rows. */
fun cardWidthFor(screenWidth: Dp, perRow: Float = 2.4f): Dp = ((screenWidth - Gutter * 2 - 12.dp * perRow.toInt()) / perRow).coerceIn(120.dp, 200.dp)
