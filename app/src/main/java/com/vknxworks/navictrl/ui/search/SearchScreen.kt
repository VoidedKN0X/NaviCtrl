package com.vknxworks.navictrl.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.api.SearchResult
import com.vknxworks.navictrl.data.api.search
import com.vknxworks.navictrl.ui.components.AlbumCard
import com.vknxworks.navictrl.ui.components.ArtistTile
import com.vknxworks.navictrl.ui.components.LocalBottomClearance
import com.vknxworks.navictrl.ui.components.MessageView
import com.vknxworks.navictrl.ui.components.SectionHeader
import com.vknxworks.navictrl.ui.components.SongRow
import com.vknxworks.navictrl.ui.components.TabTitle
import com.vknxworks.navictrl.ui.components.cardWidthFor
import com.vknxworks.navictrl.ui.components.friendlyMessage
import com.vknxworks.navictrl.ui.theme.Gutter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun SearchScreen() {
    val c = LocalContext.current.container
    val focus = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<SearchResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        snapshotFlow { query.trim() }.debounce(300).distinctUntilChanged().collectLatest { q ->
            if (q.isEmpty()) {
                result = null
                error = null
                return@collectLatest
            }
            busy = true
            try {
                result = c.api.search(q)
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                error = e.friendlyMessage()
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TabTitle("Search")
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Songs, albums, artists") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, "Clear") }
            },
            singleLine = true,
            shape = CircleShape,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter),
        )
        Box(Modifier.fillMaxWidth().padding(horizontal = Gutter + 16.dp, vertical = 4.dp)) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        val r = result
        when {
            error != null -> MessageView(Icons.Outlined.SearchOff, "Search failed", error)
            query.isBlank() -> MessageView(Icons.Filled.Search, "Search your library", "Find any song, album or artist on your server.")
            r == null -> Unit
            r.song.isEmpty() && r.album.isEmpty() && r.artist.isEmpty() -> MessageView(Icons.Outlined.SearchOff, "Nothing found", "Try fewer or different words.")
            else -> BoxWithConstraints(Modifier.fillMaxSize()) {
                val cardW = cardWidthFor(maxWidth)
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp + LocalBottomClearance.current)) {
                    if (r.artist.isNotEmpty()) {
                        item(key = "artists") {
                            SectionHeader("Artists")
                            LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(r.artist, key = { it.id }) { ArtistTile(it, size = 96.dp) }
                            }
                        }
                    }
                    if (r.album.isNotEmpty()) {
                        item(key = "albums") {
                            SectionHeader("Albums")
                            LazyRow(contentPadding = PaddingValues(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(r.album, key = { it.id }) { AlbumCard(it, width = cardW) }
                            }
                        }
                    }
                    if (r.song.isNotEmpty()) {
                        item(key = "songs-h") { SectionHeader("Songs") }
                        itemsIndexed(r.song, key = { _, s -> "s-${s.id}" }) { i, s ->
                            SongRow(s, onClick = { c.music.play(r.song, i) })
                        }
                    }
                }
            }
        }
    }
}
