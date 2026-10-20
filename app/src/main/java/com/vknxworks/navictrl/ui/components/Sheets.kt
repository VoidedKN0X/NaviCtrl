package com.vknxworks.navictrl.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.navictrl.AppContainer
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.api.Album
import com.vknxworks.navictrl.data.api.Playlist
import com.vknxworks.navictrl.data.api.Song
import com.vknxworks.navictrl.data.api.StarKind
import com.vknxworks.navictrl.data.api.album
import com.vknxworks.navictrl.data.api.coverArtUrl
import com.vknxworks.navictrl.data.api.createPlaylist
import com.vknxworks.navictrl.data.api.deletePlaylist
import com.vknxworks.navictrl.data.api.playlist
import com.vknxworks.navictrl.data.api.playlists
import com.vknxworks.navictrl.data.api.updatePlaylist
import com.vknxworks.navictrl.ui.nav.LocalNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SongAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** Runs a background task that outlives the sheet that started it, reporting failures as a toast. */
fun AppContainer.runTask(block: suspend () -> Unit) {
    appScope.launch(Dispatchers.Main) {
        runCatching { block() }.onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
    }
}

@Composable
private fun SheetHeader(artUrl: String?, title: String, subtitle: String?) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(56.dp).clip(MaterialTheme.shapes.small)) { ArtImage(artUrl, Modifier.fillMaxSize()) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Format, bitrate and length, e.g. "FLAC  ·  1011 kbps  ·  3:42". */
private fun Song.techLine(): String = listOfNotNull(
    suffix?.uppercase(),
    bitRate?.takeIf { it > 0 }?.let { "$it kbps" },
    duration?.takeIf { it > 0 }?.let { formatClock(it * 1000L) },
).joinToString("  ·  ")

@Composable
fun SongActionsSheet(song: Song, onDismiss: () -> Unit, extra: List<SongAction> = emptyList()) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val stars by c.stars.overrides.collectAsStateWithLifecycle()
    val offline by c.downloads.completedIds.collectAsStateWithLifecycle()
    var addTo by remember { mutableStateOf(false) }
    if (addTo) {
        AddToPlaylistSheet(listOf(song), onDismiss = { addTo = false; onDismiss() })
        return
    }
    val starred = stars[song.id] ?: (song.starred != null)
    fun act(block: () -> Unit) { block(); onDismiss() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 8.dp)) {
            SheetHeader(
                c.api.coverArtUrl(song.coverArt, 160),
                song.title,
                listOf(song.subtitle(), song.techLine()).filter { it.isNotBlank() }.joinToString("\n"),
            )
            ActionItem(Icons.AutoMirrored.Filled.PlaylistPlay, "Play next") { act { c.music.playNext(listOf(song)) } }
            ActionItem(Icons.AutoMirrored.Filled.QueueMusic, "Add to queue") { act { c.music.enqueue(listOf(song)) } }
            ActionItem(Icons.Filled.AutoAwesome, "Start a mix from this song") { act { c.music.startMix(song) } }
            ActionItem(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist") { addTo = true }
            ActionItem(
                if (starred) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                if (starred) "Remove from favourites" else "Add to favourites",
            ) { act { c.stars.set(StarKind.SONG, song.id, !starred) } }
            if (song.id in offline) {
                ActionItem(Icons.Outlined.Delete, "Remove download") { act { c.downloads.remove(listOf(song.id)) } }
            } else {
                ActionItem(Icons.Filled.Download, "Download") { act { c.downloads.enqueue(listOf(song)) } }
            }
            if (!song.albumId.isNullOrBlank()) ActionItem(Icons.Filled.Album, "Go to album") { act { nav.album(song.albumId) } }
            if (!song.artistId.isNullOrBlank()) ActionItem(Icons.Filled.Person, "Go to artist") { act { nav.artist(song.artistId) } }
            extra.forEach { a -> ActionItem(a.icon, a.label) { act(a.onClick) } }
        }
    }
}

@Composable
fun AlbumActionsSheet(album: Album, onDismiss: () -> Unit) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val stars by c.stars.overrides.collectAsStateWithLifecycle()
    var addTo by remember { mutableStateOf<List<Song>?>(null) }
    addTo?.let { songs ->
        AddToPlaylistSheet(songs, onDismiss = { addTo = null; onDismiss() })
        return
    }
    val starred = stars[album.id] ?: (album.starred != null)
    // Album lists don't include tracks, so fetch them when an action needs them.
    fun withSongs(block: (List<Song>) -> Unit) {
        onDismiss()
        c.runTask { block(album.song.ifEmpty { c.api.album(album.id).song }) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 8.dp)) {
            SheetHeader(c.api.coverArtUrl(album.coverArt, 160), album.name, listOfNotNull(album.artist, album.year?.toString()).joinToString("  ·  "))
            ActionItem(Icons.Filled.PlayArrow, "Play") { withSongs { c.music.play(it) } }
            ActionItem(Icons.Filled.Shuffle, "Shuffle") { withSongs { c.music.shuffle(it) } }
            ActionItem(Icons.AutoMirrored.Filled.PlaylistPlay, "Play next") { withSongs { c.music.playNext(it) } }
            ActionItem(Icons.AutoMirrored.Filled.QueueMusic, "Add to queue") { withSongs { c.music.enqueue(it) } }
            ActionItem(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist") {
                c.runTask { addTo = album.song.ifEmpty { c.api.album(album.id).song } }
            }
            ActionItem(
                if (starred) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                if (starred) "Remove from favourites" else "Add to favourites",
            ) { c.stars.set(StarKind.ALBUM, album.id, !starred); onDismiss() }
            ActionItem(Icons.Filled.Download, "Download album") { withSongs { c.downloads.enqueue(it) } }
            if (!album.artistId.isNullOrBlank()) ActionItem(Icons.Filled.Person, "Go to artist") { onDismiss(); nav.artist(album.artistId) }
        }
    }
}

/** A playlist's menu in the Playlists tab: play it without opening it, or rename or delete it if it's yours. */
@Composable
fun PlaylistActionsSheet(pl: Playlist, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    if (renaming) {
        TextInputDialog("Rename playlist", "Name", "Save", initial = pl.name, onDismiss = onDismiss) { name ->
            onDismiss()
            c.runTask {
                c.api.updatePlaylist(pl.id, name = name)
                c.playlistsChanged()
            }
        }
        return
    }
    if (deleting) {
        ConfirmDialog("Delete \"${pl.name}\"?", "The songs stay in your library.", "Delete", onDismiss = onDismiss) {
            onDismiss()
            c.runTask {
                c.api.deletePlaylist(pl.id)
                c.playlistsChanged()
                Toast.makeText(context, "Deleted ${pl.name}", Toast.LENGTH_SHORT).show()
            }
        }
        return
    }
    val mine = pl.ownedBy(c.api.username)
    // The list of playlists doesn't include their songs, so fetch them when an action needs them.
    fun withSongs(block: (List<Song>) -> Unit) {
        onDismiss()
        c.runTask {
            val songs = c.api.playlist(pl.id).entry
            if (songs.isEmpty()) Toast.makeText(context, "${pl.name} is empty", Toast.LENGTH_SHORT).show()
            else block(songs)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 8.dp)) {
            SheetHeader(
                c.api.coverArtUrl(pl.coverArt, 160),
                pl.name,
                listOfNotNull(countLabel(pl.songCount, "song"), formatLength(pl.duration)).joinToString("  ·  "),
            )
            ActionItem(Icons.Filled.PlayArrow, "Play") { withSongs { c.music.play(it) } }
            ActionItem(Icons.Filled.Shuffle, "Shuffle") { withSongs { c.music.shuffle(it) } }
            ActionItem(Icons.AutoMirrored.Filled.PlaylistPlay, "Play next") { withSongs { c.music.playNext(it) } }
            ActionItem(Icons.AutoMirrored.Filled.QueueMusic, "Add to queue") { withSongs { c.music.enqueue(it) } }
            if (mine) {
                ActionItem(Icons.Filled.Edit, "Rename") { renaming = true }
                ActionItem(Icons.Outlined.Delete, "Delete playlist") { deleting = true }
            }
        }
    }
}

/** Pick a playlist to add songs to, or make a new one. */
@Composable
fun AddToPlaylistSheet(songs: List<Song>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    // Only playlists songs can be added to: your own, and not smart or synced ones.
    val lists = rememberLoader("playlists-picker") { c.api.playlists().filter { it.songsEditableBy(c.api.username) } }
    // Song counts change as songs are added, so the list is fetched again each time it opens.
    LaunchedEffect(Unit) { if (lists.state is Load.Ok) lists.reload(silent = true) }
    var naming by remember { mutableStateOf(false) }
    val label = if (songs.size == 1) "\"${songs.first().title}\"" else "${songs.size} songs"

    if (naming) {
        TextInputDialog(
            title = "New playlist",
            label = "Name",
            confirm = "Create",
            onDismiss = { naming = false },
        ) { name ->
            naming = false
            onDismiss()
            c.runTask {
                c.api.createPlaylist(name, songs.map { it.id })
                c.playlistsChanged()
                Toast.makeText(context, "Created $name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Text("Add $label to", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
        ActionItem(Icons.Filled.Add, "New playlist") { naming = true }
        when (val s = lists.state) {
            is Load.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is Load.Err -> Text(s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
            is Load.Ok -> LazyColumn(Modifier.navigationBarsPadding(), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(s.value, key = { it.id }) { p ->
                    PlaylistPickRow(p) {
                        onDismiss()
                        c.runTask {
                            c.api.updatePlaylist(p.id, add = songs.map { it.id })
                            c.playlistsChanged()
                            Toast.makeText(context, "Added to ${p.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistPickRow(p: Playlist, onClick: () -> Unit) {
    val c = LocalContext.current.container
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(MaterialTheme.shapes.extraSmall)) {
            ArtImage(c.api.coverArtUrl(p.coverArt, 160), Modifier.fillMaxSize(), placeholderIcon = Icons.AutoMirrored.Filled.QueueMusic)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(countLabel(p.songCount, "song"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    confirm: String,
    initial: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it }, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
