package com.vknxworks.navictrl.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.vknxworks.navictrl.MainActivity
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.api.StarKind
import com.vknxworks.navictrl.data.api.coverArtUrl
import com.vknxworks.navictrl.data.api.lyrics
import com.vknxworks.navictrl.ui.components.ArtImage
import com.vknxworks.navictrl.ui.components.Load
import com.vknxworks.navictrl.ui.components.MessageView
import com.vknxworks.navictrl.ui.components.SeekBar
import com.vknxworks.navictrl.ui.components.SongAction
import com.vknxworks.navictrl.ui.components.SongActionsSheet
import com.vknxworks.navictrl.ui.components.formatClock
import com.vknxworks.navictrl.ui.components.glass
import com.vknxworks.navictrl.ui.components.rememberLoader
import com.vknxworks.navictrl.ui.nav.LocalNavigator
import com.vknxworks.navictrl.ui.theme.ArtworkTint
import com.vknxworks.navictrl.ui.theme.rememberArtworkColor
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import com.vknxworks.navictrl.R
import com.vknxworks.navictrl.str
import androidx.compose.ui.res.pluralStringResource

/** Floating glass card above the tab island while music plays. */
@Composable
fun MiniPlayer(onOpen: () -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val music = c.music
    val s by music.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { music.connect() }
    LaunchedEffect(s.hasMedia) { if (s.hasMedia) (context as? MainActivity)?.ensureNotificationPermission() }
    val song = s.current ?: return
    val art = c.api.coverArtUrl(song.coverArt, 300)
    val seed = rememberArtworkColor(art)
    ArtworkTint(seed) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .glass(MaterialTheme.shapes.large, elevation = 12.dp, tint = MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp).clip(MaterialTheme.shapes.small)) {
                    ArtImage(art, Modifier.fillMaxSize(), placeholderIcon = Icons.Outlined.MusicNote)
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (s.buffering) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) }
                } else {
                    IconButton(onClick = { music.togglePlay() }) {
                        Icon(if (s.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (s.isPlaying) stringResource(R.string.pause) else stringResource(R.string.play))
                    }
                }
                IconButton(onClick = { music.next() }) { Icon(Icons.Filled.SkipNext, stringResource(R.string.next_song)) }
            }
            val p = if (s.durationMs > 0) (s.positionMs.toFloat() / s.durationMs).coerceIn(0f, 1f) else 0f
            Box(
                Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp).fillMaxWidth(p).height(2.dp)
                    .clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
fun NowPlayingScreen() {
    val context = LocalContext.current
    val c = context.container
    val music = c.music
    val nav = LocalNavigator.current
    val s by music.state.collectAsStateWithLifecycle()
    val stars by c.stars.overrides.collectAsStateWithLifecycle()
    var panel by rememberSaveable { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    var sleepSheet by remember { mutableStateOf(false) }
    var scrub by remember { mutableStateOf<Long?>(null) }
    val song = s.current
    val art = song?.let { c.api.coverArtUrl(it.coverArt, 1200) }
    val seed = rememberArtworkColor(song?.let { c.api.coverArtUrl(it.coverArt, 300) })

    if (song == null) {
        MessageView(Icons.Outlined.MusicNote, stringResource(R.string.nothing_playing), stringResource(R.string.play_a_song_an_album_or))
        return
    }
    val starred = stars[song.id] ?: (song.starred != null)

    ArtworkTint(seed) {
        val top = MaterialTheme.colorScheme.primaryContainer
        val bg = MaterialTheme.colorScheme.background
        BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to top, 0.75f to bg))) {
            val wide = maxWidth > maxHeight && maxWidth >= 600.dp
            val player: @Composable (Modifier) -> Unit = { mod ->
                Column(mod.statusBarsPadding().padding(horizontal = 28.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = nav::back) { Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.close)) }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (s.endless) stringResource(R.string.endless_queue) else stringResource(R.string.playing_from),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                song.album.orEmpty(),
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable(enabled = !song.albumId.isNullOrBlank()) { nav.album(song.albumId) },
                            )
                        }
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more)) }
                    }
                    Spacer(Modifier.height(20.dp))
                    Box(
                        Modifier.widthIn(max = 440.dp).fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.large)
                            .clickable(enabled = !song.albumId.isNullOrBlank()) { nav.album(song.albumId) },
                    ) { ArtImage(art, Modifier.fillMaxSize(), placeholderIcon = Icons.Outlined.MusicNote) }
                    Spacer(Modifier.height(28.dp))
                    Row(Modifier.widthIn(max = 440.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                song.artist.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable(enabled = !song.artistId.isNullOrBlank()) { nav.artist(song.artistId) },
                            )
                        }
                        IconButton(onClick = { c.stars.set(StarKind.SONG, song.id, !starred) }) {
                            Icon(
                                if (starred) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                if (starred) stringResource(R.string.remove_from_favourites) else stringResource(R.string.add_to_favourites),
                                tint = if (starred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
                        SeekBar(
                            positionMs = scrub ?: s.positionMs,
                            durationMs = s.durationMs,
                            bufferedMs = s.bufferedMs,
                            onScrub = { scrub = it },
                            onSeek = { music.seekTo(it); scrub = null },
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            bufferColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                        )
                        Row(Modifier.fillMaxWidth()) {
                            Text(formatClock(scrub ?: s.positionMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.weight(1f))
                            Text(formatClock(s.durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.widthIn(max = 440.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(onClick = { music.toggleShuffle() }) {
                            Icon(Icons.Filled.Shuffle, stringResource(R.string.shuffle), tint = if (s.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { music.previous() }, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Filled.SkipPrevious, stringResource(R.string.previous), Modifier.size(36.dp))
                        }
                        Box(
                            Modifier.size(76.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable { music.togglePlay() },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (s.buffering && !s.isPlaying) {
                                CircularProgressIndicator(Modifier.size(32.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp)
                            } else {
                                Icon(
                                    if (s.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    if (s.isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                                    Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                        IconButton(onClick = { music.next() }, modifier = Modifier.size(56.dp)) {
                            Icon(Icons.Filled.SkipNext, stringResource(R.string.next), Modifier.size(36.dp))
                        }
                        IconButton(onClick = { music.cycleRepeat() }) {
                            Icon(
                                if (s.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                stringResource(R.string.repeat),
                                tint = if (s.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (!wide) {
                        Spacer(Modifier.weight(1f))
                        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            PanelButton(Icons.AutoMirrored.Filled.Subject, stringResource(R.string.lyrics)) { panel = "lyrics" }
                            PanelButton(Icons.AutoMirrored.Filled.QueueMusic, stringResource(R.string.up_next)) { panel = "queue" }
                            PanelButton(Icons.Filled.Bedtime, sleepLabel(s) ?: stringResource(R.string.sleep), selected = sleepLabel(s) != null) { sleepSheet = true }
                        }
                    }
                }
            }
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    player(Modifier.weight(1f).fillMaxSize())
                    Column(Modifier.weight(1f).fillMaxSize().statusBarsPadding()) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PanelButton(Icons.AutoMirrored.Filled.QueueMusic, stringResource(R.string.up_next), selected = panel != "lyrics") { panel = "queue" }
                            PanelButton(Icons.AutoMirrored.Filled.Subject, stringResource(R.string.lyrics), selected = panel == "lyrics") { panel = "lyrics" }
                            PanelButton(Icons.Filled.Bedtime, sleepLabel(s) ?: stringResource(R.string.sleep), selected = sleepLabel(s) != null) { sleepSheet = true }
                        }
                        if (panel == "lyrics") LyricsView(s) else QueueList(s)
                    }
                }
            } else {
                player(Modifier.fillMaxSize())
            }
        }

        if (!isWideScreen(context) && panel != null) {
            ModalBottomSheet(onDismissRequest = { panel = null }, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Box(Modifier.fillMaxWidth().height(560.dp)) {
                    if (panel == "lyrics") LyricsView(s) else QueueList(s)
                }
            }
        }
        if (sleepSheet) SleepSheet(s) { sleepSheet = false }
        if (menu) {
            SongActionsSheet(
                song,
                onDismiss = { menu = false },
                extra = listOf(SongAction(stringResource(R.string.stop_and_clear_the_queue), Icons.Filled.Stop) { music.stop(); nav.back() }),
            )
        }
    }
}

private fun sleepLabel(s: MusicState): String? = when {
    s.sleepAtEndOfTrack -> str(R.string.after_this_song)
    s.sleepAtMs != null -> {
        val min = ((s.sleepAtMs - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0) + 1
        str(R.string.x_min_left, min)
    }
    else -> null
}

/** Wide layouts show the queue and lyrics beside the player instead of in a sheet. */
private fun isWideScreen(context: android.content.Context): Boolean {
    val cfg = context.resources.configuration
    return cfg.screenWidthDp > cfg.screenHeightDp && cfg.screenWidthDp >= 600
}

@Composable
private fun PanelButton(icon: ImageVector, label: String, selected: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SleepSheet(s: MusicState, onDismiss: () -> Unit) {
    val music = LocalContext.current.container.music
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(stringResource(R.string.sleep_timer), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            sleepLabel(s)?.let {
                Text(stringResource(R.string.pausing_x, it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 24.dp))
            }
            listOf(5, 10, 15, 30, 45, 60, 90).forEach { m ->
                SheetOption(pluralStringResource(R.plurals.x_minutes, m, m)) { music.sleepIn(m); onDismiss() }
            }
            SheetOption(stringResource(R.string.at_the_end_of_this_song)) { music.sleepAtEndOfTrack(); onDismiss() }
            if (sleepLabel(s) != null) SheetOption(stringResource(R.string.turn_off), color = MaterialTheme.colorScheme.error) { music.cancelSleep(); onDismiss() }
        }
    }
}

@Composable
private fun SheetOption(label: String, color: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        color = color,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
    )
}

private val QueueRowHeight = 64.dp

/** Up next. Drag the handle to reorder; the cross removes a song. */
@Composable
private fun QueueList(s: MusicState) {
    val c = LocalContext.current.container
    val music = c.music
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (s.index - 1).coerceAtLeast(0))
    val rowPx = with(LocalDensity.current) { QueueRowHeight.toPx() }
    var dragFrom by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val target = dragFrom?.let { (it + (dragOffset / rowPx).roundToInt()).coerceIn(0, s.queue.lastIndex) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.up_next), style = MaterialTheme.typography.titleMedium)
                val left = (s.queue.size - s.index - 1).coerceAtLeast(0)
                Text(
                    if (s.endless) stringResource(R.string.x_queued_more_on_the_way, left) else pluralStringResource(R.plurals.count_songs, left, left),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (s.endless) Icon(Icons.Filled.AllInclusive, stringResource(R.string.endless), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
            TextButton(onClick = { music.clearUpNext() }) {
                Icon(Icons.Outlined.ClearAll, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.clear))
            }
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(s.queue, key = { i, e -> "$i-${e.id}" }) { i, e ->
                val current = i == s.index
                val dragging = i == dragFrom
                val shift = when {
                    dragFrom == null || target == null || dragging -> 0f
                    target > dragFrom!! && i in (dragFrom!! + 1)..target -> -rowPx
                    target < dragFrom!! && i in target until dragFrom!! -> rowPx
                    else -> 0f
                }
                Row(
                    Modifier
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer { translationY = if (dragging) dragOffset else shift }
                        .fillMaxWidth()
                        .height(QueueRowHeight)
                        .background(
                            when {
                                dragging -> MaterialTheme.colorScheme.surfaceContainerHighest
                                current -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else -> Color.Transparent
                            }
                        )
                        .clickable { music.skipTo(i) }
                        .padding(start = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(44.dp).pointerInput(i, s.queue.size) {
                            detectDragGestures(
                                onDragStart = { dragFrom = i; dragOffset = 0f },
                                onDragEnd = {
                                    // Read the live drag state here; values captured at composition would be stale.
                                    val from = dragFrom
                                    val to = from?.let { (it + (dragOffset / rowPx).roundToInt()).coerceIn(0, s.queue.lastIndex) }
                                    dragFrom = null
                                    dragOffset = 0f
                                    if (from != null && to != null && from != to) music.move(from, to)
                                },
                                onDragCancel = { dragFrom = null; dragOffset = 0f },
                                onDrag = { change, amount -> change.consume(); dragOffset += amount.y },
                            )
                        },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.DragHandle, stringResource(R.string.drag_to_reorder), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Box(Modifier.size(44.dp).clip(MaterialTheme.shapes.extraSmall)) {
                        ArtImage(c.api.coverArtUrl(e.coverArt, 160), Modifier.fillMaxSize())
                        if (current) {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.GraphicEq, stringResource(R.string.playing), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            e.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        e.artist?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    if (!current) {
                        IconButton(onClick = { music.removeAt(i) }) { Icon(Icons.Filled.Close, stringResource(R.string.remove_from_queue), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsView(s: MusicState) {
    val c = LocalContext.current.container
    val song = s.current ?: return
    val loader = rememberLoader("lyrics-${song.id}") { c.api.lyrics(song) }
    val lyrics = loader.value
    if (lyrics == null || lyrics.lines.isEmpty()) {
        MessageView(
            Icons.Outlined.MusicNote,
            if (loader.state is Load.Loading) stringResource(R.string.looking_for_lyrics) else stringResource(R.string.no_lyrics_for_this_song),
            if (loader.state is Load.Loading) null else stringResource(R.string.navidrome_reads_lyrics_from_the_files),
        )
        return
    }
    val currentIndex = if (lyrics.synced) lyrics.lines.indexOfLast { (it.start ?: Long.MAX_VALUE) <= s.positionMs } else -1
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) { if (currentIndex > 2) listState.animateScrollToItem(currentIndex - 2) }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp)) {
        itemsIndexed(lyrics.lines) { i, line ->
            val active = i == currentIndex
            Text(
                line.value.ifBlank { "♪" },
                style = MaterialTheme.typography.headlineSmall,
                color = if (active || !lyrics.synced) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable(enabled = line.start != null) { line.start?.let { c.music.seekTo(it) } },
            )
        }
    }
}
