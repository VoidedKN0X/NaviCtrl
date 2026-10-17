package com.vknxworks.navictrl.ui.detail

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.api.AlbumSort
import com.vknxworks.navictrl.data.api.Playlist
import com.vknxworks.navictrl.data.api.coverArtUrl
import com.vknxworks.navictrl.data.api.deletePlaylist
import com.vknxworks.navictrl.data.api.playlist
import com.vknxworks.navictrl.data.api.songsByGenre
import com.vknxworks.navictrl.data.api.starred
import com.vknxworks.navictrl.data.api.updatePlaylist
import com.vknxworks.navictrl.ui.components.AlbumRow
import com.vknxworks.navictrl.ui.components.ArtistRow
import com.vknxworks.navictrl.ui.components.CircleIconButton
import com.vknxworks.navictrl.ui.components.ConfirmDialog
import com.vknxworks.navictrl.ui.components.ErrorView
import com.vknxworks.navictrl.ui.components.LoadContent
import com.vknxworks.navictrl.ui.components.Loader
import com.vknxworks.navictrl.ui.components.LoadingView
import com.vknxworks.navictrl.ui.components.LocalBottomClearance
import com.vknxworks.navictrl.ui.components.MessageView
import com.vknxworks.navictrl.ui.components.PlayShuffleRow
import com.vknxworks.navictrl.ui.components.ScreenTopBar
import com.vknxworks.navictrl.ui.components.SongAction
import com.vknxworks.navictrl.ui.components.SongRow
import com.vknxworks.navictrl.ui.components.TabTitle
import androidx.compose.foundation.layout.statusBarsPadding
import com.vknxworks.navictrl.ui.components.TextInputDialog
import com.vknxworks.navictrl.ui.components.countLabel
import com.vknxworks.navictrl.ui.components.formatLength
import com.vknxworks.navictrl.ui.components.rememberLoader
import com.vknxworks.navictrl.ui.components.rememberPagedLoader
import com.vknxworks.navictrl.ui.components.runTask
import com.vknxworks.navictrl.ui.library.AlbumsSection
import com.vknxworks.navictrl.ui.nav.LocalNavigator
import com.vknxworks.navictrl.ui.nav.PlaylistRoute
import com.vknxworks.navictrl.ui.theme.ArtworkTint
import com.vknxworks.navictrl.ui.theme.Gutter
import com.vknxworks.navictrl.ui.theme.rememberArtworkColor
import kotlinx.coroutines.delay

// ---------- Playlist ----------

@Composable
fun PlaylistScreen(id: String) {
    val c = LocalContext.current.container
    val loader = rememberLoader("playlist-$id") { c.api.playlist(id) }
    LoadContent(loader) { p -> PlaylistContent(p, loader) }
}

@Composable
private fun PlaylistContent(p: Playlist, loader: Loader<Playlist>) {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val offline by c.downloads.completedIds.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val songs = p.entry
    val mine = p.owner == null || p.owner == c.api.username
    val allOffline = songs.isNotEmpty() && songs.all { it.id in offline }
    val seed = rememberArtworkColor(c.api.coverArtUrl(p.coverArt, 300))

    ArtworkTint(seed) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp + LocalBottomClearance.current)) {
            item(key = "header") {
                HeroHeader(
                    artUrl = c.api.coverArtUrl(p.coverArt, 1200),
                    title = p.name,
                    subtitle = p.comment,
                    meta = listOfNotNull(countLabel(songs.size, "song"), formatLength(p.duration), p.owner?.takeIf { !mine }?.let { "by $it" }).joinToString("  ·  "),
                    placeholderIcon = Icons.AutoMirrored.Filled.QueueMusic,
                ) {
                    CircleIconButton(
                        if (allOffline) Icons.Filled.DownloadDone else Icons.Filled.Download,
                        if (allOffline) "Remove download" else "Download",
                        onClick = { if (allOffline) c.downloads.remove(songs.map { it.id }) else c.downloads.enqueue(songs) },
                        active = allOffline,
                    )
                    CircleIconButton(Icons.Filled.MoreHoriz, "More", onClick = { menu = true })
                }
            }
            if (songs.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp)) {
                        MessageView(Icons.Outlined.MusicNote, "This playlist is empty", "Add songs from their menu, using Add to playlist.")
                    }
                }
            } else {
                item(key = "buttons") { PlayShuffleRow(onPlay = { c.music.play(songs) }, onShuffle = { c.music.shuffle(songs) }) }
            }
            itemsIndexed(songs, key = { i, s -> "$i-${s.id}" }) { i, s ->
                SongRow(
                    s,
                    onClick = { c.music.play(songs, i) },
                    extraActions = if (!mine) emptyList() else listOf(
                        SongAction("Remove from this playlist", Icons.Outlined.RemoveCircleOutline) {
                            c.runTask {
                                c.api.updatePlaylist(p.id, removeIndexes = listOf(i))
                                loader.reload(silent = true)
                            }
                        },
                    ),
                )
            }
        }
    }

    if (menu) {
        ModalBottomSheet(onDismissRequest = { menu = false }, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 8.dp)) {
                MenuRow(Icons.AutoMirrored.Filled.PlaylistPlay, "Play next") { menu = false; c.music.playNext(songs) }
                MenuRow(Icons.AutoMirrored.Filled.QueueMusic, "Add to queue") { menu = false; c.music.enqueue(songs) }
                if (mine) {
                    MenuRow(Icons.Filled.Edit, "Rename") { menu = false; renaming = true }
                    MenuRow(Icons.Outlined.Delete, "Delete playlist") { menu = false; deleting = true }
                }
            }
        }
    }
    if (renaming) {
        TextInputDialog("Rename playlist", "Name", "Save", initial = p.name, onDismiss = { renaming = false }) { name ->
            renaming = false
            c.runTask {
                c.api.updatePlaylist(p.id, name = name)
                loader.reload(silent = true)
            }
        }
    }
    if (deleting) {
        ConfirmDialog("Delete \"${p.name}\"?", "The songs stay in your library.", "Delete", onDismiss = { deleting = false }) {
            deleting = false
            c.runTask {
                c.api.deletePlaylist(p.id)
                Toast.makeText(context, "Deleted ${p.name}", Toast.LENGTH_SHORT).show()
                nav.close(PlaylistRoute::class)
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

// ---------- Genre ----------

@Composable
fun GenreScreen(name: String) {
    val c = LocalContext.current.container
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    var albums by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(name) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !albums, onClick = { albums = false }, label = { Text("Songs") })
                FilterChip(selected = albums, onClick = { albums = true }, label = { Text("Albums") })
            }
            if (albums) {
                AlbumsSection(AlbumSort.NAME, settings.albumGrid, genre = name)
            } else {
                GenreSongs(name)
            }
        }
    }
}

@Composable
private fun GenreSongs(name: String) {
    val c = LocalContext.current.container
    val loader = rememberPagedLoader("genre-songs-$name", pageSize = 100) { start, limit ->
        val page = c.api.songsByGenre(name, limit, start)
        page to if (page.size < limit) start + page.size else Int.MAX_VALUE
    }
    val state = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(loader) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (last >= loader.items.size - 10) loader.loadMore()
        }
    }
    when {
        loader.items.isEmpty() && loader.error != null -> ErrorView(loader.error!!) { loader.refresh() }
        loader.items.isEmpty() && !loader.endReached -> LoadingView()
        loader.items.isEmpty() -> MessageView(Icons.Outlined.MusicNote, "No songs in $name")
        else -> LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp + LocalBottomClearance.current)) {
            item(key = "buttons") {
                PlayShuffleRow(onPlay = { c.music.play(loader.items.toList()) }, onShuffle = { c.music.shuffleGenre(name) })
            }
            itemsIndexed(loader.items, key = { i, s -> "$i-${s.id}" }) { i, s ->
                SongRow(s, onClick = { c.music.play(loader.items.toList(), i) })
            }
            if (loader.loading) item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
    }
}

// ---------- Favourites ----------

private enum class FavTab(val label: String) { SONGS("Songs"), ALBUMS("Albums"), ARTISTS("Artists") }

@Composable
fun FavouritesScreen() {
    val c = LocalContext.current.container
    val loader = rememberLoader("starred") { c.api.starred() }
    var tab by rememberSaveable { mutableStateOf(FavTab.SONGS) }
    // Hearts changed anywhere (including here) refresh the list.
    val overrides by c.stars.overrides.collectAsStateWithLifecycle()
    LaunchedEffect(overrides) { if (overrides.isNotEmpty()) { delay(800); loader.reload(silent = true) } }
    // A main tab: big title, no back arrow, content scrolls under the glass island.
    run {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TabTitle("Favourites")
            LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FavTab.entries) { t -> FilterChip(selected = t == tab, onClick = { tab = t }, label = { Text(t.label) }) }
            }
            LoadContent(loader) { starred ->
                val bottom = PaddingValues(bottom = 24.dp + LocalBottomClearance.current)
                when (tab) {
                    FavTab.SONGS -> if (starred.song.isEmpty()) Empty("Tap the heart on any song to keep it here.") else
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = bottom) {
                            item { PlayShuffleRow(onPlay = { c.music.play(starred.song) }, onShuffle = { c.music.shuffle(starred.song) }) }
                            itemsIndexed(starred.song, key = { _, s -> s.id }) { i, s -> SongRow(s, onClick = { c.music.play(starred.song, i) }) }
                        }
                    FavTab.ALBUMS -> if (starred.album.isEmpty()) Empty("Favourite albums show up here.") else
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = bottom) { items(starred.album, key = { it.id }) { AlbumRow(it) } }
                    FavTab.ARTISTS -> if (starred.artist.isEmpty()) Empty("Favourite artists show up here.") else
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = bottom) { items(starred.artist, key = { it.id }) { ArtistRow(it) } }
                }
            }
        }
    }
}

@Composable
private fun Empty(body: String) = MessageView(Icons.Outlined.FavoriteBorder, "Nothing here yet", body)
