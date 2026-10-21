package com.vknxworks.navictrl.ui.songs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.api.Song
import com.vknxworks.navictrl.data.api.SongSort
import com.vknxworks.navictrl.ui.components.ErrorView
import com.vknxworks.navictrl.ui.components.LoaderStore
import com.vknxworks.navictrl.ui.components.LoadingView
import com.vknxworks.navictrl.ui.components.LocalBottomClearance
import com.vknxworks.navictrl.ui.components.MessageView
import com.vknxworks.navictrl.ui.components.PillButton
import com.vknxworks.navictrl.ui.components.SongRow
import com.vknxworks.navictrl.ui.components.SongRowPlaceholder
import com.vknxworks.navictrl.ui.components.TabTitle
import com.vknxworks.navictrl.ui.components.friendlyMessage
import com.vknxworks.navictrl.ui.components.isoToEpochMs
import com.vknxworks.navictrl.ui.components.sortLetter
import com.vknxworks.navictrl.ui.theme.Gutter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import com.vknxworks.navictrl.R
import com.vknxworks.navictrl.str
import androidx.compose.ui.res.pluralStringResource

private const val PAGE = 200

/**
 * Every song in the library, fetched a page at a time as it scrolls into view, so the list can
 * jump anywhere (fast scroll) without loading the whole library first.
 */
class SongPager(
    private val scope: CoroutineScope,
    private val fetch: suspend (start: Int, count: Int) -> Pair<List<Song>, Int>,
) {
    var total by mutableIntStateOf(-1)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private val pages = mutableStateMapOf<Int, List<Song>>()
    private val inFlight = HashSet<Int>()

    init {
        load(0)
    }

    fun peek(index: Int): Song? = pages[index / PAGE]?.getOrNull(index % PAGE)

    fun load(page: Int) {
        if (page in inFlight || pages.containsKey(page)) return
        inFlight += page
        scope.launch {
            try {
                val (items, count) = fetch(page * PAGE, PAGE)
                pages[page] = items
                total = count
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (total < 0) error = e.friendlyMessage()
            } finally {
                inFlight -= page
            }
        }
    }

    fun ensure(index: Int) = load(index / PAGE)
}

@Composable
fun SongsScreen() {
    val c = LocalContext.current.container
    val settings by c.settings.settings.collectAsStateWithLifecycle()
    val sort = settings.songSort
    val asc = settings.songSortAscending
    val store: LoaderStore = viewModel()
    val scope = rememberCoroutineScope()
    var refreshCount by remember { mutableIntStateOf(0) }
    var sortSheet by remember { mutableStateOf(false) }
    val pagerKey = "songs:${sort.name}:$asc:$refreshCount"
    val pager = remember(pagerKey) {
        store.map.getOrPut(pagerKey) {
            SongPager(store.viewModelScope) { start, count -> c.api.songsPage(start, count, sort, asc) }
        } as SongPager
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TabTitle(stringResource(R.string.songs)) {
            IconButton(onClick = { sortSheet = true }) { Icon(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.sort_songs)) }
        }
        val total = pager.total
        when {
            total < 0 && pager.error != null -> ErrorView(pager.error!!) { refreshCount++ }
            total < 0 -> LoadingView()
            total == 0 -> MessageView(Icons.Outlined.MusicNote, stringResource(R.string.no_songs_yet), stringResource(R.string.once_navidrome_has_scanned_your_music))
            else -> key(pagerKey) {
                val listState = rememberLazyListState()
                PullToRefreshBox(isRefreshing = false, onRefresh = { refreshCount++ }, modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp + LocalBottomClearance.current),
                    ) {
                        item(key = "header") {
                            Column(Modifier.fillMaxWidth()) {
                                Text(
                                    pluralStringResource(R.plurals.songs_sorted, total, NumberFormat.getIntegerInstance().format(total), sortDescription(sort, asc)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = Gutter),
                                )
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    PillButton(stringResource(R.string.shuffle_all), onClick = { c.music.shuffleAll() }, icon = Icons.Filled.Shuffle, modifier = Modifier.weight(1f))
                                    Spacer(Modifier.width(12.dp))
                                    PillButton(
                                        stringResource(R.string.play),
                                        onClick = { c.music.playSongList(sort, asc, 0) },
                                        icon = Icons.Filled.PlayArrow,
                                        container = MaterialTheme.colorScheme.secondaryContainer,
                                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                        }
                        items(total) { i ->
                            val song = pager.peek(i)
                            if (song == null) {
                                LaunchedEffect(i / PAGE) { pager.ensure(i) }
                                SongRowPlaceholder()
                            } else {
                                SongRow(song, onClick = { c.music.playSongList(sort, asc, i) })
                            }
                        }
                    }
                    FastScroller(
                        state = listState,
                        total = total,
                        headerItems = 1,
                        label = { i -> pager.peek(i)?.let { scrollLabel(it, sort) } },
                        onJump = { i -> pager.ensure(i); scope.launch { listState.scrollToItem(i + 1) } },
                        modifier = Modifier.align(Alignment.TopEnd).padding(bottom = LocalBottomClearance.current),
                    )
                }
            }
        }
    }

    if (sortSheet) {
        SortSheet(sort, asc, onDismiss = { sortSheet = false }) { newSort, newAsc ->
            scope.launch { c.settings.update { it.copy(songSort = newSort, songSortAscending = newAsc) } }
        }
    }
}

private fun sortDescription(sort: SongSort, asc: Boolean): String = when (sort) {
    SongSort.TITLE -> if (asc) str(R.string.a_to_z) else str(R.string.z_to_a)
    SongSort.DATE_ADDED -> if (asc) str(R.string.oldest_added_first) else str(R.string.newest_added_first)
    SongSort.ARTIST -> if (asc) str(R.string.by_artist_a_to_z) else str(R.string.by_artist_z_to_a)
    SongSort.ALBUM -> if (asc) str(R.string.by_album_a_to_z) else str(R.string.by_album_z_to_a)
}

private val monthYear = DateTimeFormatter.ofPattern("MMM yyyy").withZone(ZoneId.systemDefault())

private fun scrollLabel(song: Song, sort: SongSort): String = when (sort) {
    SongSort.TITLE -> sortLetter(song.title)
    SongSort.ARTIST -> sortLetter(song.artist)
    SongSort.ALBUM -> sortLetter(song.album)
    SongSort.DATE_ADDED -> isoToEpochMs(song.created)?.let { monthYear.format(Instant.ofEpochMilli(it)) } ?: "…"
}

@Composable
private fun SortSheet(sort: SongSort, asc: Boolean, onDismiss: () -> Unit, onPick: (SongSort, Boolean) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(stringResource(R.string.sort_songs_by), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            SongSort.entries.forEach { s ->
                val selected = s == sort
                Row(
                    Modifier.fillMaxWidth().clickable {
                        // Date added reads best newest first; names read best A to Z.
                        onPick(s, if (s == sort) asc else s != SongSort.DATE_ADDED)
                    }.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(s.label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) Icon(Icons.Filled.Check, stringResource(R.string.selected), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(stringResource(R.string.order), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 4.dp))
            Row(Modifier.padding(horizontal = 24.dp)) {
                val (first, second) = if (sort == SongSort.DATE_ADDED) stringResource(R.string.newest_first) to stringResource(R.string.oldest_first) else stringResource(R.string.a_to_z) to stringResource(R.string.z_to_a)
                // For dates, "newest first" is descending.
                val firstAsc = sort != SongSort.DATE_ADDED
                FilterChip(selected = asc == firstAsc, onClick = { onPick(sort, firstAsc) }, label = { Text(first) })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = asc != firstAsc, onClick = { onPick(sort, !firstAsc) }, label = { Text(second) })
            }
        }
    }
}

/**
 * A thumb on the right edge for jumping through long lists. Dragging shows a bubble with the
 * letter (or month) at that point.
 */
@Composable
fun FastScroller(
    state: LazyListState,
    total: Int,
    headerItems: Int,
    label: (Int) -> String?,
    onJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (total < 60) return
    val density = LocalDensity.current
    var trackPx by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val thumbH = 48.dp
    val thumbPx = with(density) { thumbH.toPx() }
    val scrolling = state.isScrollInProgress || dragging
    val listFraction = ((state.firstVisibleItemIndex - headerItems).coerceAtLeast(0).toFloat() / (total - 1)).coerceIn(0f, 1f)
    val fraction = if (dragging) dragFraction else listFraction
    val thumbY = ((trackPx - thumbPx) * fraction).roundToInt().coerceAtLeast(0)
    val index = (fraction * (total - 1)).roundToInt()

    Box(
        modifier
            .fillMaxHeight()
            .width(36.dp)
            .padding(vertical = 8.dp)
            .onSizeChanged { trackPx = it.height }
            .pointerInput(total) {
                fun jump(y: Float) {
                    dragFraction = ((y - thumbPx / 2) / (trackPx - thumbPx).coerceAtLeast(1f)).coerceIn(0f, 1f)
                    onJump((dragFraction * (total - 1)).roundToInt())
                }
                detectVerticalDragGestures(
                    onDragStart = { o -> dragging = true; jump(o.y) },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onVerticalDrag = { change, _ -> change.consume(); jump(change.position.y) },
                )
            },
    ) {
        AnimatedVisibility(scrolling, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.offset { IntOffset(0, thumbY) }.align(Alignment.TopEnd)) {
            Box(
                Modifier
                    .padding(end = 4.dp)
                    .size(width = 6.dp, height = thumbH)
                    .clip(CircleShape)
                    .background(if (dragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)),
            )
        }
    }
    if (dragging) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(-with(density) { 56.dp.roundToPx() }, thumbY + with(density) { 8.dp.roundToPx() }) }
                    .size(64.dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 32.dp, bottomEnd = 8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                val text = label(index) ?: "…"
                Text(
                    text,
                    style = if (text.length <= 2) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
