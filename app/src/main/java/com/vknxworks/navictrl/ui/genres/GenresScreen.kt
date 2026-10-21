package com.vknxworks.navictrl.ui.genres

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vknxworks.navictrl.R
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.api.Genre
import com.vknxworks.navictrl.data.api.genres
import com.vknxworks.navictrl.ui.components.LoadContent
import com.vknxworks.navictrl.ui.components.LocalBottomClearance
import com.vknxworks.navictrl.ui.components.MessageView
import com.vknxworks.navictrl.ui.components.TabTitle
import com.vknxworks.navictrl.ui.components.countLabel
import com.vknxworks.navictrl.ui.components.rememberLoader
import com.vknxworks.navictrl.ui.nav.GenreRoute
import com.vknxworks.navictrl.ui.nav.LocalNavigator
import com.vknxworks.navictrl.ui.theme.Gutter

/**
 * The Genres tab: every genre on the server, A to Z, with how many songs and albums it has.
 * Tapping one opens its songs and albums; the shuffle button starts an endless mix of it straight away.
 */
@Composable
fun GenresScreen() {
    val c = LocalContext.current.container
    // Navidrome lists songs without a genre as "<Empty>"; that isn't a genre you can open.
    val loader = rememberLoader("genres") {
        c.api.genres()
            .filter { it.value.isNotBlank() && it.value != "<Empty>" && it.songCount > 0 }
            .sortedBy { it.value.lowercase() }
    }
    var pulled by remember { mutableStateOf(false) }
    LaunchedEffect(loader.refreshing) { if (!loader.refreshing) pulled = false }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TabTitle(stringResource(R.string.genres))
        LoadContent(loader) { genres ->
            if (genres.isEmpty()) {
                MessageView(
                    Icons.Outlined.Category,
                    stringResource(R.string.no_genres_yet),
                    stringResource(R.string.no_genres_body),
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
                            pluralStringResource(R.plurals.count_genres, genres.size, genres.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = Gutter, end = Gutter, bottom = 4.dp),
                        )
                    }
                    items(genres, key = { it.value }) { g -> GenreRow(g) }
                }
            }
        }
    }
}

/** One genre: its name, song and album counts, and a shuffle button. */
@Composable
private fun GenreRow(g: Genre) {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { nav.navigate(GenreRoute(g.value)) }
            .padding(start = Gutter, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(56.dp).clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                g.value.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "#",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(g.value, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    countLabel(g.songCount, R.plurals.count_songs),
                    g.albumCount.takeIf { it > 0 }?.let { countLabel(it, R.plurals.count_albums) },
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = { c.music.shuffleGenre(g.value) }) {
            Icon(Icons.Filled.Shuffle, stringResource(R.string.shuffle_x, g.value), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
