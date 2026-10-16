package com.vknxworks.navictrl.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.api.Album
import com.vknxworks.navictrl.data.api.Song
import com.vknxworks.navictrl.data.api.StarKind
import com.vknxworks.navictrl.data.api.album
import com.vknxworks.navictrl.data.api.artist
import com.vknxworks.navictrl.data.api.artistInfo
import com.vknxworks.navictrl.data.api.coverArtUrl
import com.vknxworks.navictrl.data.api.stripHtml
import com.vknxworks.navictrl.data.api.topSongs
import com.vknxworks.navictrl.ui.components.AlbumActionsSheet
import com.vknxworks.navictrl.ui.components.AlbumCard
import com.vknxworks.navictrl.ui.components.ArtistTile
import com.vknxworks.navictrl.ui.components.CircleIconButton
import com.vknxworks.navictrl.ui.components.LoadContent
import com.vknxworks.navictrl.ui.components.LocalBottomClearance
import com.vknxworks.navictrl.ui.components.PillButton
import com.vknxworks.navictrl.ui.components.SectionHeader
import com.vknxworks.navictrl.ui.components.SongRow
import com.vknxworks.navictrl.ui.components.cardWidthFor
import com.vknxworks.navictrl.ui.components.countLabel
import com.vknxworks.navictrl.ui.components.formatDate
import com.vknxworks.navictrl.ui.components.formatLength
import com.vknxworks.navictrl.ui.components.rememberLoader
import com.vknxworks.navictrl.ui.components.runTask
import com.vknxworks.navictrl.ui.nav.LocalNavigator
import com.vknxworks.navictrl.ui.theme.ArtworkTint
import com.vknxworks.navictrl.ui.theme.Gutter
import com.vknxworks.navictrl.ui.theme.rememberArtworkColor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Composable
fun AlbumScreen(id: String) {
    val c = LocalContext.current.container
    val loader = rememberLoader("album-$id") { c.api.album(id) }
    LoadContent(loader) { album -> AlbumContent(album) }
}

@Composable
private fun AlbumContent(album: Album) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val stars by c.stars.overrides.collectAsStateWithLifecycle()
    val offline by c.downloads.completedIds.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }
    val songs = album.song
    val starred = stars[album.id] ?: (album.starred != null)
    val allOffline = songs.isNotEmpty() && songs.all { it.id in offline }
    val discs = songs.mapNotNull { it.discNumber }.distinct()
    val seed = rememberArtworkColor(c.api.coverArtUrl(album.coverArt, 300))

    ArtworkTint(seed) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp + LocalBottomClearance.current),
        ) {
            item(key = "header") {
                HeroHeader(
                    artUrl = c.api.coverArtUrl(album.coverArt, 1200),
                    title = album.name,
                    subtitle = album.artist,
                    onSubtitle = album.artistId?.let { aid -> { nav.artist(aid) } },
                    meta = listOfNotNull(album.year?.toString(), album.genre, countLabel(songs.size, "song"), formatLength(album.duration)).joinToString("  ·  "),
                ) {
                    CircleIconButton(
                        if (starred) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        if (starred) "Remove from favourites" else "Add to favourites",
                        onClick = { c.stars.set(StarKind.ALBUM, album.id, !starred) },
                        active = starred,
                    )
                    CircleIconButton(
                        if (allOffline) Icons.Filled.DownloadDone else Icons.Filled.Download,
                        if (allOffline) "Remove download" else "Download",
                        onClick = { if (allOffline) c.downloads.remove(songs.map { it.id }) else c.downloads.enqueue(songs) },
                        active = allOffline,
                    )
                    CircleIconButton(Icons.Filled.MoreHoriz, "More", onClick = { menu = true })
                }
            }
            item(key = "buttons") {
                com.vknxworks.navictrl.ui.components.PlayShuffleRow(
                    onPlay = { c.music.play(songs) },
                    onShuffle = { c.music.shuffle(songs) },
                )
            }
            var lastDisc: Int? = null
            songs.forEachIndexed { i, s ->
                if (discs.size > 1 && s.discNumber != lastDisc) {
                    lastDisc = s.discNumber
                    val d = s.discNumber ?: 1
                    item(key = "disc-$d-$i") { DiscHeader(d) }
                }
                item(key = "song-$i-${s.id}") {
                    SongRow(s, onClick = { c.music.play(songs, i) }, number = s.track ?: (i + 1), showAlbum = false)
                }
            }
            album.created?.let { created ->
                item(key = "footer") {
                    Text(
                        "Added ${formatDate(created).orEmpty()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Gutter, vertical = 16.dp),
                    )
                }
            }
        }
    }
    if (menu) AlbumActionsSheet(album, onDismiss = { menu = false })
}

@Composable
fun ArtistScreen(id: String) {
    val c = LocalContext.current.container
    val loader = rememberLoader("artist-$id") { c.api.artist(id) }
    LoadContent(loader) { artist ->
        val info = rememberLoader("artist-info-$id") { c.api.artistInfo(id) }
        val top = rememberLoader("artist-top-$id") { c.api.topSongs(artist.name, 20) }
        val stars by c.stars.overrides.collectAsStateWithLifecycle()
        var showAllTop by remember { mutableStateOf(false) }
        var bioOpen by remember { mutableStateOf(false) }
        val starred = stars[artist.id] ?: (artist.starred != null)
        val albums = artist.album.sortedByDescending { it.year ?: 0 }
        val image = info.value?.largeImageUrl?.takeIf { it.isNotBlank() } ?: c.api.coverArtUrl(artist.coverArt ?: "ar-${artist.id}", 1200)
        val seed = rememberArtworkColor(image)

        // Every song by the artist, gathered album by album, for "Shuffle".
        suspend fun allSongs(): List<Song> = coroutineScope {
            albums.map { a -> async { runCatching { c.api.album(a.id).song }.getOrDefault(emptyList()) } }.awaitAll().flatten()
        }

        ArtworkTint(seed) {
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                val cardW = cardWidthFor(maxWidth)
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp + LocalBottomClearance.current)) {
                    item(key = "header") {
                        HeroHeader(
                            artUrl = image,
                            title = artist.name,
                            meta = countLabel(artist.albumCount.takeIf { it > 0 } ?: albums.size, "album"),
                            artShape = androidx.compose.foundation.shape.CircleShape,
                            placeholderIcon = Icons.Filled.Person,
                        ) {
                            CircleIconButton(
                                if (starred) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                if (starred) "Remove from favourites" else "Add to favourites",
                                onClick = { c.stars.set(StarKind.ARTIST, artist.id, !starred) },
                                active = starred,
                            )
                        }
                    }
                    item(key = "buttons") {
                        androidx.compose.foundation.layout.Row(
                            Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            PillButton(
                                "Play",
                                onClick = {
                                    val t = top.value.orEmpty()
                                    if (t.isNotEmpty()) c.music.play(t) else c.runTask { c.music.play(allSongs()) }
                                },
                                icon = Icons.Filled.PlayArrow,
                                modifier = Modifier.weight(1f),
                            )
                            PillButton(
                                "Shuffle",
                                onClick = { c.runTask { c.music.shuffle(allSongs()) } },
                                icon = Icons.Filled.Shuffle,
                                modifier = Modifier.weight(1f),
                                container = MaterialTheme.colorScheme.secondaryContainer,
                                content = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    val topSongs = top.value.orEmpty()
                    if (topSongs.isNotEmpty()) {
                        item(key = "top-h") { SectionHeader("Popular") }
                        val shown = if (showAllTop) topSongs else topSongs.take(5)
                        shown.forEachIndexed { i, s ->
                            item(key = "top-$i-${s.id}") { SongRow(s, onClick = { c.music.play(topSongs, i) }) }
                        }
                        if (topSongs.size > 5) {
                            item(key = "top-more") {
                                TextButton(onClick = { showAllTop = !showAllTop }, modifier = Modifier.padding(horizontal = 8.dp)) {
                                    Text(if (showAllTop) "Show less" else "Show all ${topSongs.size}")
                                }
                            }
                        }
                    }
                    if (albums.isNotEmpty()) {
                        item(key = "albums") {
                            SectionHeader("Albums")
                            LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(albums, key = { it.id }) { AlbumCard(it, width = cardW) }
                            }
                        }
                    }
                    val similar = info.value?.similarArtist.orEmpty()
                    if (similar.isNotEmpty()) {
                        item(key = "similar") {
                            SectionHeader("Similar artists")
                            LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(similar, key = { it.id }) { ArtistTile(it) }
                            }
                        }
                    }
                    val bio = info.value?.biography?.stripHtml()
                    if (!bio.isNullOrBlank()) {
                        item(key = "bio") {
                            SectionHeader("About")
                            SelectionContainer {
                                Text(
                                    bio,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = if (bioOpen) Int.MAX_VALUE else 5,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = Gutter),
                                )
                            }
                            TextButton(onClick = { bioOpen = !bioOpen }, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text(if (bioOpen) "Show less" else "Read more")
                            }
                        }
                    }
                }
            }
        }
    }
}
