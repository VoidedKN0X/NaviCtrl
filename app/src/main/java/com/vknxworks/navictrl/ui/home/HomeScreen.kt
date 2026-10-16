package com.vknxworks.navictrl.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.api.AlbumSort
import com.vknxworks.navictrl.data.api.Playlist
import com.vknxworks.navictrl.data.api.albumList
import com.vknxworks.navictrl.data.api.coverArtUrl
import com.vknxworks.navictrl.data.api.playlists
import com.vknxworks.navictrl.ui.components.AlbumCard
import com.vknxworks.navictrl.ui.components.ArtImage
import com.vknxworks.navictrl.ui.components.Load
import com.vknxworks.navictrl.ui.components.Loader
import com.vknxworks.navictrl.ui.components.LocalBottomClearance
import com.vknxworks.navictrl.ui.components.MessageView
import com.vknxworks.navictrl.ui.components.SectionHeader
import com.vknxworks.navictrl.ui.components.TabTitle
import com.vknxworks.navictrl.ui.components.cardWidthFor
import com.vknxworks.navictrl.ui.components.countLabel
import com.vknxworks.navictrl.ui.components.rememberLoader
import com.vknxworks.navictrl.ui.nav.AlbumListRoute
import com.vknxworks.navictrl.ui.nav.FavouritesRoute
import com.vknxworks.navictrl.ui.nav.LibraryRoute
import com.vknxworks.navictrl.ui.nav.LocalNavigator
import com.vknxworks.navictrl.ui.nav.PlaylistRoute
import com.vknxworks.navictrl.ui.theme.Gutter
import java.time.LocalTime

private val HomeRows = listOf(
    AlbumSort.NEWEST to "Recently added",
    AlbumSort.RECENT to "Recently played",
    AlbumSort.FREQUENT to "Most played",
    AlbumSort.RANDOM to "Rediscover",
    AlbumSort.STARRED to "Favourite albums",
)

@Composable
fun HomeScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val rows = HomeRows.map { (sort, title) -> Triple(sort, title, rememberLoader("home-${sort.name}") { c.api.albumList(sort, 20) }) }
    val playlists = rememberLoader("home-playlists") { c.api.playlists() }
    val all: List<Loader<*>> = rows.map { it.third } + playlists
    val offline = all.all { it.state is Load.Err }

    PullToRefreshBox(
        isRefreshing = all.any { it.refreshing },
        onRefresh = { all.forEach { it.reload(silent = true) } },
        modifier = Modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val cardW = cardWidthFor(maxWidth)
            LazyColumn(
                Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(bottom = 24.dp + LocalBottomClearance.current),
            ) {
                item(key = "title") { TabTitle(greeting()) }
                item(key = "quick") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        QuickTile(Icons.Filled.Shuffle, "Shuffle all", Modifier.weight(1f)) { c.music.shuffleAll() }
                        QuickTile(Icons.Filled.Favorite, "Favourites", Modifier.weight(1f)) { nav.navigate(FavouritesRoute) }
                    }
                }
                if (offline) {
                    item(key = "offline") {
                        Box(Modifier.fillMaxWidth().height(320.dp)) {
                            MessageView(
                                Icons.Outlined.CloudOff,
                                "Can't reach your server",
                                (rows.first().third.state as? Load.Err)?.message,
                                actionLabel = "Retry",
                                onAction = { all.forEach { it.reload() } },
                            )
                        }
                    }
                }
                rows.forEach { (sort, title, loader) ->
                    val albums = loader.value.orEmpty()
                    if (loader.state is Load.Loading || albums.isNotEmpty()) {
                        item(key = "row-${sort.name}") {
                            SectionHeader(title, action = "See all", onAction = { nav.navigate(AlbumListRoute(sort.name, title)) })
                            LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (loader.state is Load.Loading) {
                                    items(4) { PlaceholderCard(cardW) }
                                } else {
                                    items(albums, key = { it.id }) { AlbumCard(it, width = cardW) }
                                }
                            }
                        }
                    }
                    if (sort == AlbumSort.RECENT) {
                        val lists = playlists.value.orEmpty()
                        if (lists.isNotEmpty()) {
                            item(key = "playlists") {
                                SectionHeader("Playlists", action = "See all", onAction = { nav.navigate(LibraryRoute) })
                                LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(lists, key = { it.id }) { PlaylistCard(it, cardW) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
private fun QuickTile(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlaceholderCard(width: Dp) {
    Column(Modifier.width(width)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerHigh))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(0.7f).height(12.dp).clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.surfaceContainer))
    }
}

@Composable
fun PlaylistCard(p: Playlist, width: Dp?) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val shape = MaterialTheme.shapes.small
    Column((if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()).clip(shape).clickable { nav.navigate(PlaylistRoute(p.id)) }) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(shape)) {
            ArtImage(c.api.coverArtUrl(p.coverArt, 300), Modifier.fillMaxSize(), placeholderIcon = Icons.AutoMirrored.Filled.QueueMusic)
        }
        Text(p.name, modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            countLabel(p.songCount, "song"),
            modifier = Modifier.padding(horizontal = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
