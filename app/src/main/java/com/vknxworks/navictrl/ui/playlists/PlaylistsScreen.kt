package com.vknxworks.navictrl.ui.playlists

import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.api.Playlist
import com.vknxworks.navictrl.data.api.coverArtUrl
import com.vknxworks.navictrl.data.api.createPlaylist
import com.vknxworks.navictrl.data.api.playlists
import com.vknxworks.navictrl.ui.components.ArtImage
import com.vknxworks.navictrl.ui.components.LoadContent
import com.vknxworks.navictrl.ui.components.LocalBottomClearance
import com.vknxworks.navictrl.ui.components.MessageView
import com.vknxworks.navictrl.ui.components.PlaylistActionsSheet
import com.vknxworks.navictrl.ui.components.TabTitle
import com.vknxworks.navictrl.ui.components.TextInputDialog
import com.vknxworks.navictrl.ui.components.countLabel
import com.vknxworks.navictrl.ui.components.formatLength
import com.vknxworks.navictrl.ui.components.rememberLoader
import com.vknxworks.navictrl.ui.components.runTask
import com.vknxworks.navictrl.ui.nav.LocalNavigator
import com.vknxworks.navictrl.ui.nav.PlaylistRoute
import com.vknxworks.navictrl.ui.theme.Gutter
import androidx.compose.ui.res.stringResource
import com.vknxworks.navictrl.R
import com.vknxworks.navictrl.str

/** The Playlists tab: your playlists and the ones others shared, with a button to make a new one. */
@Composable
fun PlaylistsScreen() {
    val context = LocalContext.current
    val c = context.container
    val loader = rememberLoader("playlists") { c.api.playlists() }
    // Playlists made, renamed, deleted or added to anywhere in the app show up here without a manual refresh.
    val edits by c.playlistEdits.collectAsStateWithLifecycle()
    var seenEdits by rememberSaveable { mutableIntStateOf(edits) }
    LaunchedEffect(edits) {
        if (edits != seenEdits) {
            seenEdits = edits
            loader.reload(silent = true)
        }
    }
    var pulled by remember { mutableStateOf(false) }
    LaunchedEffect(loader.refreshing) { if (!loader.refreshing) pulled = false }
    var creating by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<Playlist?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TabTitle(stringResource(R.string.playlists)) {
            IconButton(onClick = { creating = true }) { Icon(Icons.Filled.Add, stringResource(R.string.new_playlist)) }
        }
        LoadContent(loader) { lists ->
            if (lists.isEmpty()) {
                MessageView(
                    Icons.AutoMirrored.Filled.QueueMusic,
                    stringResource(R.string.no_playlists_yet),
                    stringResource(R.string.make_one_here_or_use_add),
                    actionLabel = stringResource(R.string.new_playlist),
                    onAction = { creating = true },
                )
                return@LoadContent
            }
            PullToRefreshBox(
                isRefreshing = pulled && loader.refreshing,
                onRefresh = { pulled = true; loader.reload(silent = true) },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp + LocalBottomClearance.current)) {
                    item(key = "header") {
                        Text(
                            countLabel(lists.size, R.plurals.count_playlists),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = Gutter, end = Gutter, bottom = 4.dp),
                        )
                    }
                    items(lists, key = { it.id }) { p -> PlaylistRow(p, onMore = { menuFor = p }) }
                }
            }
        }
    }

    if (creating) {
        TextInputDialog(stringResource(R.string.new_playlist), stringResource(R.string.name), stringResource(R.string.create), onDismiss = { creating = false }) { name ->
            creating = false
            c.runTask {
                c.api.createPlaylist(name)
                c.playlistsChanged()
                Toast.makeText(context, str(R.string.created_x, name), Toast.LENGTH_SHORT).show()
            }
        }
    }
    menuFor?.let { p -> PlaylistActionsSheet(p, onDismiss = { menuFor = null }) }
}

/** One playlist: its cover, name, song count and length, and who made it when it isn't yours. */
@Composable
private fun PlaylistRow(p: Playlist, onMore: () -> Unit) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { nav.navigate(PlaylistRoute(p.id)) }, onLongClick = onMore)
            .padding(start = Gutter, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(MaterialTheme.shapes.extraSmall)) {
            ArtImage(c.api.coverArtUrl(p.coverArt, 160), Modifier.fillMaxSize(), placeholderIcon = Icons.AutoMirrored.Filled.QueueMusic)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    countLabel(p.songCount, R.plurals.count_songs),
                    formatLength(p.duration),
                    p.owner?.takeIf { !p.ownedBy(c.api.username) }?.let { stringResource(R.string.by_x, it) },
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onMore) {
            Icon(Icons.Filled.MoreVert, stringResource(R.string.more_for_x, p.name), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
