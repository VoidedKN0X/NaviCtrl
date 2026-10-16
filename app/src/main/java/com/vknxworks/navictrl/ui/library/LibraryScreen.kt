package com.vknxworks.navictrl.ui.library

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.api.AlbumSort
import com.vknxworks.navictrl.data.api.Genre
import com.vknxworks.navictrl.data.api.Playlist
import com.vknxworks.navictrl.data.api.RadioStation
import com.vknxworks.navictrl.data.api.albumList
import com.vknxworks.navictrl.data.api.artists
import com.vknxworks.navictrl.data.api.coverArtUrl
import com.vknxworks.navictrl.data.api.createPlaylist
import com.vknxworks.navictrl.data.api.genres
import com.vknxworks.navictrl.data.api.playlists
import com.vknxworks.navictrl.data.api.radioStations
import com.vknxworks.navictrl.ui.components.AlbumCard
import com.vknxworks.navictrl.ui.components.AlbumRow
import com.vknxworks.navictrl.ui.components.ArtImage
import com.vknxworks.navictrl.ui.components.ArtistRow
import com.vknxworks.navictrl.ui.components.ChoiceSheet
import com.vknxworks.navictrl.ui.components.ErrorView
import com.vknxworks.navictrl.ui.components.LoadContent
import com.vknxworks.navictrl.ui.components.LoadingView
import com.vknxworks.navictrl.ui.components.LocalBottomClearance
import com.vknxworks.navictrl.ui.components.MessageView
import com.vknxworks.navictrl.ui.components.PagedLoader
import com.vknxworks.navictrl.ui.components.TabTitle
import com.vknxworks.navictrl.ui.components.TextInputDialog
import com.vknxworks.navictrl.ui.components.countLabel
import com.vknxworks.navictrl.ui.components.formatLength
import com.vknxworks.navictrl.ui.components.friendlyMessage
import com.vknxworks.navictrl.ui.components.rememberLoader
import com.vknxworks.navictrl.ui.components.rememberPagedLoader
import com.vknxworks.navictrl.ui.components.sortLetter
import com.vknxworks.navictrl.ui.nav.GenreRoute
import com.vknxworks.navictrl.ui.nav.LocalNavigator
import com.vknxworks.navictrl.ui.nav.PlaylistRoute
import com.vknxworks.navictrl.ui.theme.Gutter
import kotlinx.coroutines.launch

private enum class Section(val label: String) { ALBUMS("Albums"), ARTISTS("Artists"), PLAYLISTS("Playlists"), GENRES("Genres"), RADIO("Radio") }

@Composable
fun LibraryScreen() {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(Section.ALBUMS) }
    var sortSheet by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TabTitle("Library") {
            if (section == Section.ALBUMS) {
                IconButton(onClick = { scope.launch { c.settings.update { it.copy(albumGrid = !it.albumGrid) } } }) {
                    Icon(if (settings.albumGrid) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView, if (settings.albumGrid) "Show as list" else "Show as grid")
                }
                IconButton(onClick = { sortSheet = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Sort albums") }
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Section.entries) { s ->
                FilterChip(selected = s == section, onClick = { section = s }, label = { Text(s.label) })
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (section) {
                Section.ALBUMS -> AlbumsSection(settings.albumSort, settings.albumGrid)
                Section.ARTISTS -> ArtistsSection()
                Section.PLAYLISTS -> PlaylistsSection()
                Section.GENRES -> GenresSection()
                Section.RADIO -> RadioSection()
            }
        }
    }

    if (sortSheet) {
        ChoiceSheet(
            "Sort albums by",
            AlbumSort.entries.map { it to it.label },
            settings.albumSort,
            onDismiss = { sortSheet = false },
        ) { v ->
            sortSheet = false
            scope.launch { c.settings.update { it.copy(albumSort = v) } }
        }
    }
}

/** Grid or list of albums that loads more as you scroll. Shared with the "See all" pages. */
@Composable
fun AlbumsSection(sort: AlbumSort, grid: Boolean, genre: String? = null) {
    val c = LocalContext.current.container
    val loader = rememberPagedLoader("albums-${sort.name}-$genre", pageSize = 60) { start, limit ->
        val page = c.api.albumList(sort, limit, start, genre)
        // The API doesn't say how many there are; a short page means we've reached the end.
        page to if (page.size < limit) start + page.size else Int.MAX_VALUE
    }
    PagedContent(loader, emptyTitle = "No albums") {
        val bottom = 24.dp + LocalBottomClearance.current
        if (grid) {
            val state = rememberLazyGridState()
            LoadMoreWhenNearEnd(loader) { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            LazyVerticalGrid(
                GridCells.Adaptive(150.dp),
                state = state,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 12.dp, bottom = bottom),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                itemsIndexed(loader.items, key = { _, a -> a.id }) { _, a -> AlbumCard(a) }
                if (loader.loading) item(span = { GridItemSpan(maxLineSpan) }) { LoadingMore() }
            }
        } else {
            val state = rememberLazyListState()
            LoadMoreWhenNearEnd(loader) { state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = bottom)) {
                itemsIndexed(loader.items, key = { _, a -> a.id }) { _, a -> AlbumRow(a) }
                if (loader.loading) item { LoadingMore() }
            }
        }
    }
}

@Composable
private fun <T> PagedContent(loader: PagedLoader<T>, emptyTitle: String, content: @Composable () -> Unit) {
    when {
        loader.items.isEmpty() && loader.error != null -> ErrorView(loader.error!!) { loader.refresh() }
        loader.items.isEmpty() && !loader.endReached -> LoadingView()
        loader.items.isEmpty() -> MessageView(Icons.Outlined.Album, emptyTitle)
        else -> content()
    }
}

@Composable
private fun <T> LoadMoreWhenNearEnd(loader: PagedLoader<T>, lastVisible: () -> Int) {
    LaunchedEffect(loader) {
        snapshotFlow { lastVisible() }.collect { last ->
            if (last >= loader.items.size - 12) loader.loadMore()
        }
    }
}

@Composable
private fun LoadingMore() {
    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
}

@Composable
private fun ArtistsSection() {
    val c = LocalContext.current.container
    val loader = rememberLoader("artists") { c.api.artists().sortedBy { it.name.lowercase().removePrefix("the ") } }
    LoadContent(loader) { artists ->
        if (artists.isEmpty()) {
            MessageView(Icons.Outlined.Person, "No artists")
            return@LoadContent
        }
        val grouped = remember(artists) { artists.groupBy { sortLetter(it.name) } }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp + LocalBottomClearance.current)) {
            grouped.forEach { (letter, list) ->
                stickyHeader(key = "h-$letter") { LetterHeader(letter) }
                items(list, key = { it.id }) { ArtistRow(it) }
            }
        }
    }
}

@Composable
private fun LetterHeader(letter: String) {
    Text(
        letter,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = Gutter, vertical = 6.dp),
    )
}

@Composable
private fun PlaylistsSection() {
    val context = LocalContext.current
    val c = context.container
    val loader = rememberLoader("playlists") { c.api.playlists() }
    var creating by remember { mutableStateOf(false) }
    LoadContent(loader) { lists ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp + LocalBottomClearance.current)) {
            item(key = "new") {
                Row(
                    Modifier.fillMaxWidth().clickable { creating = true }.padding(horizontal = Gutter, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(56.dp).clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary) }
                    Spacer(Modifier.width(14.dp))
                    Text("New playlist", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            items(lists, key = { it.id }) { PlaylistRow(it) }
        }
    }
    if (creating) {
        TextInputDialog("New playlist", "Name", "Create", onDismiss = { creating = false }) { name ->
            creating = false
            c.appScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                runCatching { c.api.createPlaylist(name) }
                    .onSuccess { loader.reload(silent = true) }
                    .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
            }
        }
    }
}

@Composable
fun PlaylistRow(p: Playlist) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    Row(
        Modifier.fillMaxWidth().clickable { nav.navigate(PlaylistRoute(p.id)) }.padding(horizontal = Gutter, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(MaterialTheme.shapes.extraSmall)) {
            ArtImage(c.api.coverArtUrl(p.coverArt, 160), Modifier.fillMaxSize(), placeholderIcon = Icons.AutoMirrored.Filled.QueueMusic)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(countLabel(p.songCount, "song"), formatLength(p.duration), p.owner?.takeIf { it != c.api.username }?.let { "by $it" }).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GenresSection() {
    val c = LocalContext.current.container
    val loader = rememberLoader("genres") { c.api.genres().sortedByDescending { it.songCount } }
    LoadContent(loader) { genres ->
        if (genres.isEmpty()) {
            MessageView(Icons.Outlined.Category, "No genres", "Genres come from your music's tags.")
            return@LoadContent
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp + LocalBottomClearance.current)) {
            items(genres, key = { it.value }) { GenreRow(it) }
        }
    }
}

@Composable
private fun GenreRow(g: Genre) {
    val nav = LocalNavigator.current
    Row(
        Modifier.fillMaxWidth().clickable { nav.navigate(GenreRoute(g.value)) }.padding(horizontal = Gutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(sortLetter(g.value), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(g.value, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${countLabel(g.songCount, "song")}  ·  ${countLabel(g.albumCount, "album")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RadioSection() {
    val c = LocalContext.current.container
    val loader = rememberLoader("radio") { c.api.radioStations() }
    LoadContent(loader) { stations ->
        if (stations.isEmpty()) {
            MessageView(Icons.Filled.Radio, "No radio stations", "Add internet radio stations in Navidrome's web interface and they appear here.")
            return@LoadContent
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp + LocalBottomClearance.current)) {
            items(stations, key = { it.id }) { RadioRow(it) }
        }
    }
}

@Composable
private fun RadioRow(s: RadioStation) {
    val c = LocalContext.current.container
    Row(
        Modifier.fillMaxWidth().clickable { c.music.playRadio(s) }.padding(horizontal = Gutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Radio, null, tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(s.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(s.homePageUrl ?: s.streamUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
