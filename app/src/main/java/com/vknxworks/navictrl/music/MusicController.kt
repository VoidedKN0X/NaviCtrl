package com.vknxworks.navictrl.music

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.vknxworks.navictrl.AppContainer
import com.vknxworks.navictrl.cacheTag
import com.vknxworks.navictrl.data.api.ApiJson
import com.vknxworks.navictrl.data.api.Song
import com.vknxworks.navictrl.data.api.SongSort
import com.vknxworks.navictrl.data.api.PlayQueue
import com.vknxworks.navictrl.data.api.playQueue
import com.vknxworks.navictrl.data.api.randomSongs
import com.vknxworks.navictrl.data.api.savePlayQueue
import com.vknxworks.navictrl.data.api.similarSongs
import com.vknxworks.navictrl.data.api.streamUrl
import com.vknxworks.navictrl.ui.components.friendlyMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.vknxworks.navictrl.R
import com.vknxworks.navictrl.str
import com.vknxworks.navictrl.plural

data class MusicState(
    val connected: Boolean = false,
    val current: Song? = null,
    val isPlaying: Boolean = false,
    val buffering: Boolean = false,
    val positionMs: Long = 0,
    val bufferedMs: Long = 0,
    val durationMs: Long = 0,
    val queue: List<Song> = emptyList(),
    val index: Int = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    /** True while the queue tops itself up (shuffle all, the songs list). */
    val endless: Boolean = false,
    val sleepAtMs: Long? = null,
    val sleepAtEndOfTrack: Boolean = false,
) {
    val hasMedia: Boolean get() = current != null
}

/** A queue saved on the server by another device or app that differs from what's here. */
data class RemoteQueueOffer(
    val accountKey: String,
    val queue: PlayQueue,
    val index: Int,
    val current: Song,
)

@Serializable
private data class SavedQueue(val accountKey: String, val songs: List<Song>, val index: Int, val positionMs: Long)

/**
 * App-side handle on the music service: turns songs into a queue, keeps it topped up for
 * endless modes, and exposes playback state to the UI.
 */
class MusicController(private val context: Context, private val c: AppContainer) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private val _state = MutableStateFlow(MusicState())
    val state: StateFlow<MusicState> = _state.asStateFlow()

    /** Just the playing song's id; long lists watch this instead of the full, fast-changing state. */
    private val _currentId = MutableStateFlow<String?>(null)
    val currentId: StateFlow<String?> = _currentId.asStateFlow()

    /** Song details by id, so the queue shows full info and actions can reach album and artist. */
    private val known = HashMap<String, Song>()

    /** Fetches more songs when the queue runs low; null when the queue is fixed. */
    private var continuation: (suspend () -> List<Song>)? = null
    private var extending = false

    /** Queue order before shuffle was turned on, to put it back when it's turned off. */
    private var unshuffled: List<String>? = null

    private var sleepJob: Job? = null
    private var pauseAfterTrack = false
    private var sleepAtMs: Long? = null

    private val savedQueueFile = File(context.filesDir, QUEUE_FILE)

    init {
        // Songs from one account can't play (or be scrobbled) on another: whenever the signed-in
        // account changes, including signing in to a new server, the old queue is cleared.
        scope.launch {
            var last = c.sessions.active.value?.account?.key
            c.sessions.active.map { it?.account?.key }.distinctUntilChanged().collect { key ->
                // The app lock dropped the session (nothing was playing): the same account comes back on unlock.
                if (key == null && c.sessions.isLocked) return@collect
                if (key != last) {
                    last = key
                    reset()
                }
            }
        }
    }

    // ---------- Connection ----------

    private val connecting = Mutex()

    private suspend fun controller(): MediaController = connecting.withLock {
        controller?.let { existing ->
            if (existing.isConnected) return@withLock existing
            // The music service stopped (closed from Recents, or ended by Android) while the app stayed
            // in memory: this connection is dead, so make a new one, which starts the service again.
            controller = null
            runCatching { existing.release() }
        }
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token)
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) = onServiceGone(controller)
            })
            .buildAsync()
        val mc = suspendCancellableCoroutine<MediaController> { cont ->
            cont.invokeOnCancellation { MediaController.releaseFuture(future) }
            future.addListener({
                runCatching { future.get() }
                    .onSuccess { if (cont.isActive) cont.resume(it) else it.release() }
                    .onFailure { if (cont.isActive) cont.resumeWithException(it) }
            }, ContextCompat.getMainExecutor(context))
        }
        controller = mc
        mc.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = refresh()

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (pauseAfterTrack && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    pauseAfterTrack = false
                    mc.pause()
                    refresh()
                }
                maybeExtend()
                saveQueueSoon()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) touched = true
                if (!isPlaying) saveQueueSoon()
            }
        })
        startPolling()
        mc
    }

    /** The music service went away: forget the connection; the next action connects again. */
    private fun onServiceGone(gone: MediaController) {
        if (controller !== gone) return
        controller = null
        runCatching { gone.release() }
        // A save still waiting (the pause as the service stopped) is written now, from the last state seen.
        if (saveJob?.isActive == true) {
            saveJob?.cancel()
            val last = _state.value
            val key = c.sessions.active.value?.account?.key
            if (key != null && last.queue.isNotEmpty()) {
                scope.launch { writeLocal(key, last.queue, last.index, last.positionMs) }
            }
        }
        sleepJob?.cancel()
        sleepJob = null
        sleepAtMs = null
        pauseAfterTrack = false
        _currentId.value = null
        _state.value = MusicState()
    }

    private var polling = false
    private fun startPolling() {
        if (polling) return
        polling = true
        scope.launch {
            var ticks = 0
            while (true) {
                if (controller?.isPlaying != true) {
                    // Nothing playing: sleep until something does, instead of waking up twice a second in the background.
                    _state.first { it.isPlaying }
                    delay(500)
                    continue
                }
                refresh()
                // Every 30 seconds while playing, the server's copy of the queue gets the new position.
                if (++ticks >= 60) { ticks = 0; saveQueueSoon() }
                delay(500)
            }
        }
    }

    private fun refresh() {
        val p = controller ?: return
        val queue = (0 until p.mediaItemCount).map { songOf(p.getMediaItemAt(it)) }
        val current = p.currentMediaItem?.let(::songOf)
        _currentId.value = current?.id
        _state.value = MusicState(
            connected = true,
            current = current,
            isPlaying = p.isPlaying,
            buffering = p.playbackState == Player.STATE_BUFFERING,
            positionMs = p.currentPosition.coerceAtLeast(0),
            bufferedMs = p.bufferedPosition.coerceAtLeast(0),
            durationMs = p.duration.coerceAtLeast(0),
            queue = queue,
            index = p.currentMediaItemIndex,
            shuffle = unshuffled != null,
            repeatMode = p.repeatMode,
            endless = continuation != null,
            sleepAtMs = sleepAtMs,
            sleepAtEndOfTrack = pauseAfterTrack,
        )
    }

    // ---------- Media items ----------

    private fun mediaItem(song: Song): MediaItem {
        known[song.id] = song
        val client = c.api
        val local = c.downloads.completedFile(song.id)
        val s = c.settings.settings.value
        val kbps = if (c.isOnCellular()) s.mobileBitrate else s.wifiBitrate
        val uri = local?.let { Uri.fromFile(it).toString() } ?: client.streamUrl(song.id, kbps, s.format.param)
        val extras = Bundle().apply {
            putString("albumId", song.albumId)
            putString("artistId", song.artistId)
            putString("coverArt", song.coverArt)
            putInt("duration", song.duration ?: 0)
            // OpenSubsonic ReplayGain values for the player service; left out when the server has none.
            song.replayGain?.let { rg ->
                rg.trackGain?.let { putDouble(MusicService.RG_TRACK_GAIN, it) }
                rg.albumGain?.let { putDouble(MusicService.RG_ALBUM_GAIN, it) }
                rg.trackPeak?.let { putDouble(MusicService.RG_TRACK_PEAK, it) }
                rg.albumPeak?.let { putDouble(MusicService.RG_ALBUM_PEAK, it) }
                rg.fallbackGain?.let { putDouble(MusicService.RG_FALLBACK_GAIN, it) }
            }
        }
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(uri)
            // Keyed per account, so one account's cached songs never play for another and are removed on sign-out.
            .setCustomCacheKey("${c.sessions.active.value?.account?.key?.let(::cacheTag) ?: "none"}:${song.id}:${if (kbps > 0) "$kbps${s.format.param}" else "raw"}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setTrackNumber(song.track)
                    .setDiscNumber(song.discNumber)
                    .setReleaseYear(song.year)
                    .setGenre(song.genre)
                    // A credential-free placeholder URI; MusicService's bitmap loader turns it into the
                    // authenticated request in-process, so other apps never see the auth token.
                    .setArtworkUri(ArtworkUris.forCover(song.coverArt, 600))
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    private fun songOf(item: MediaItem): Song {
        known[item.mediaId]?.let { return it }
        val md = item.mediaMetadata
        val ex = md.extras
        return Song(
            id = item.mediaId,
            title = md.title?.toString().orEmpty(),
            artist = md.artist?.toString(),
            album = md.albumTitle?.toString(),
            albumId = ex?.getString("albumId"),
            artistId = ex?.getString("artistId"),
            coverArt = ex?.getString("coverArt"),
            duration = ex?.getInt("duration")?.takeIf { it > 0 },
            track = md.trackNumber,
            year = md.releaseYear,
        )
    }

    private fun launch(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Toast.makeText(context, e.friendlyMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------- Starting playback ----------

    /** Replaces the queue and plays from [startIndex]. [shuffle] mixes the songs first. */
    fun play(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        startQueue(songs, startIndex, shuffle)
    }

    /** Replaces the queue and plays from [startIndex]. [shuffle] mixes the songs first. [more] keeps the queue topped up. */
    private fun startQueue(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false, more: (suspend () -> List<Song>)? = null) = launch {
        if (songs.isEmpty()) return@launch
        var list = songs
        var start = startIndex.coerceIn(0, songs.lastIndex)
        unshuffled = null
        if (shuffle) {
            unshuffled = songs.map { it.id }
            list = songs.shuffled()
            start = 0
        }
        continuation = more
        touched = true
        val mc = controller()
        mc.setMediaItems(list.map(::mediaItem), start, 0)
        mc.prepare()
        mc.play()
        refresh()
    }

    fun shuffle(songs: List<Song>) = play(songs, shuffle = true)

    /** Plays the whole library in random order, topping up as it goes. */
    fun shuffleAll() = launch {
        val first = c.api.randomSongs(200)
        if (first.isEmpty()) throw IllegalStateException(str(R.string.your_library_has_no_songs_yet))
        startQueue(first, more = randomMore())
    }

    private fun randomMore(): suspend () -> List<Song> = {
        val have = state.value.queue.map { it.id }.toHashSet()
        c.api.randomSongs(150).filterNot { it.id in have }
    }

    /**
     * Plays the all-songs list from [index] in the chosen order. The queue is filled a page at a
     * time from the server, so even very large libraries start instantly.
     */
    fun playSongList(sort: SongSort, ascending: Boolean, index: Int, pageSize: Int = 200) = launch {
        val (page, total) = c.api.songsPage(index, pageSize, sort, ascending)
        if (page.isEmpty()) return@launch
        var next = index + page.size
        startQueue(page, more = {
            if (next >= total) emptyList()
            else c.api.songsPage(next, pageSize, sort, ascending).first.also { next += it.size }
        })
    }

    /** A queue of songs like this one, from the server's similar-song data. */
    fun startMix(song: Song) = launch {
        val similar = runCatching { c.api.similarSongs(song.id, 80) }.getOrDefault(emptyList())
        if (similar.isEmpty()) throw IllegalStateException(str(R.string.no_similar_songs_found_the_server))
        startQueue(listOf(song) + similar.filterNot { it.id == song.id })
    }

    fun enqueue(songs: List<Song>) = launch {
        touched = true
        if (songs.isEmpty()) return@launch
        val mc = controller()
        val wasEmpty = mc.mediaItemCount == 0
        mc.addMediaItems(songs.map(::mediaItem))
        unshuffled = unshuffled?.plus(songs.map { it.id })
        if (wasEmpty) { mc.prepare(); mc.play() }
        Toast.makeText(context, plural(R.plurals.added_to_queue, songs.size, songs.size), Toast.LENGTH_SHORT).show()
        refresh()
    }

    fun playNext(songs: List<Song>) = launch {
        touched = true
        if (songs.isEmpty()) return@launch
        val mc = controller()
        val wasEmpty = mc.mediaItemCount == 0
        mc.addMediaItems((mc.currentMediaItemIndex + 1).coerceAtMost(mc.mediaItemCount), songs.map(::mediaItem))
        unshuffled = unshuffled?.plus(songs.map { it.id })
        if (wasEmpty) { mc.prepare(); mc.play() }
        else Toast.makeText(context, plural(R.plurals.plays_next, songs.size, songs.size), Toast.LENGTH_SHORT).show()
        refresh()
    }

    // ---------- Transport ----------

    fun togglePlay() = launch {
        val mc = controller()
        if (mc.mediaItemCount == 0) restoreQueue(mc, autoplay = true)
        if (mc.isPlaying) mc.pause() else {
            if (mc.playbackState == Player.STATE_ENDED) mc.seekTo(0, 0)
            if (mc.playbackState == Player.STATE_IDLE) mc.prepare()
            mc.play()
        }
    }

    fun next() = launch { controller().seekToNextMediaItem() }
    fun previous() = launch { controller().seekToPrevious() }
    fun seekTo(ms: Long) = launch { controller().seekTo(ms) }
    fun skipTo(index: Int) = launch { controller().seekTo(index, 0); controller().play() }
    fun removeAt(index: Int) = launch {
        val mc = controller()
        if (index in 0 until mc.mediaItemCount) mc.removeMediaItem(index)
        touched = true
        saveQueueSoon()
        refresh()
    }
    fun move(from: Int, to: Int) = launch {
        val mc = controller()
        if (from == to || from !in 0 until mc.mediaItemCount || to !in 0 until mc.mediaItemCount) return@launch
        mc.moveMediaItem(from, to)
        touched = true
        saveQueueSoon()
        refresh()
    }

    /** Clears everything after the current song. */
    fun clearUpNext() = launch {
        val mc = controller()
        val from = mc.currentMediaItemIndex + 1
        if (from < mc.mediaItemCount) mc.removeMediaItems(from, mc.mediaItemCount)
        continuation = null
        refresh()
    }

    /**
     * Shuffle reorders the songs still to come so the queue shows what's actually next.
     * Turning it off puts the original order back.
     */
    fun toggleShuffle() = launch {
        val mc = controller()
        val count = mc.mediaItemCount
        if (count < 2) {
            unshuffled = if (unshuffled == null) emptyList() else null
            refresh()
            return@launch
        }
        val idx = mc.currentMediaItemIndex
        val items = (0 until count).map { mc.getMediaItemAt(it) }
        val current = items[idx]
        val others: List<MediaItem>
        val before: List<MediaItem>
        if (unshuffled == null) {
            unshuffled = items.map { it.mediaId }
            before = emptyList()
            others = (items.take(idx) + items.drop(idx + 1)).shuffled()
        } else {
            val order = unshuffled.orEmpty()
            unshuffled = null
            val byId = items.groupBy { it.mediaId }.mapValues { it.value.toMutableList() }
            val restored = order.mapNotNull { id -> byId[id]?.removeFirstOrNull() } +
                byId.values.flatten() // songs added since, in their current order
            val pos = restored.indexOfFirst { it.mediaId == current.mediaId }.coerceAtLeast(0)
            before = restored.take(pos)
            others = restored.drop(pos + 1)
        }
        // Keep the playing item in place so playback isn't interrupted.
        if (idx + 1 < count) mc.removeMediaItems(idx + 1, count)
        if (idx > 0) mc.removeMediaItems(0, idx)
        if (others.isNotEmpty()) mc.addMediaItems(others)
        if (before.isNotEmpty()) mc.addMediaItems(0, before)
        refresh()
    }

    fun cycleRepeat() = launch {
        controller().let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun stop() = launch {
        controller().let { it.stop(); it.clearMediaItems() }
        continuation = null
        unshuffled = null
        cancelSleep()
        runCatching { savedQueueFile.delete() }
        refresh()
    }

    // ---------- Sleep timer ----------

    fun sleepIn(minutes: Int) {
        cancelSleep()
        val at = System.currentTimeMillis() + minutes * 60_000L
        sleepAtMs = at
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            controller?.pause()
            sleepAtMs = null
            refresh()
        }
        refresh()
    }

    fun sleepAtEndOfTrack() {
        cancelSleep()
        pauseAfterTrack = true
        refresh()
    }

    fun cancelSleep() {
        sleepJob?.cancel()
        sleepJob = null
        sleepAtMs = null
        pauseAfterTrack = false
        refresh()
    }

    // ---------- Endless queues ----------

    private fun maybeExtend() {
        val more = continuation ?: return
        val mc = controller ?: return
        if (extending || mc.mediaItemCount - mc.currentMediaItemIndex > 5) return
        extending = true
        scope.launch {
            try {
                val songs = more()
                if (songs.isEmpty()) continuation = null
                else {
                    mc.addMediaItems(songs.map(::mediaItem))
                    unshuffled = unshuffled?.plus(songs.map { it.id })
                }
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            } finally {
                extending = false
                refresh()
            }
        }
    }

    // ---------- Saved queue ----------

    private var saveJob: Job? = null

    private fun saveQueueSoon() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(1500)
            val mc = controller ?: return@launch
            val key = c.sessions.active.value?.account?.key ?: return@launch
            val songs = (0 until mc.mediaItemCount).map { songOf(mc.getMediaItemAt(it)) }
            if (songs.isEmpty()) return@launch
            writeLocal(key, songs, mc.currentMediaItemIndex, mc.currentPosition.coerceAtLeast(0))
            syncToServer(key, songs.map { it.id }, mc.currentMediaItemIndex, mc.currentPosition.coerceAtLeast(0))
        }
    }

    /** The queue on the phone, brought back after a restart. */
    private suspend fun writeLocal(key: String, songs: List<Song>, index: Int, positionMs: Long) {
        if (songs.isEmpty()) return
        // Keep a window around the current song so very long endless queues stay small on disk.
        val from = (index - 500).coerceAtLeast(0)
        val kept = songs.drop(from).take(2000)
        if (kept.isEmpty()) return
        val saved = SavedQueue(key, kept, (index - from).coerceIn(0, kept.lastIndex), positionMs.coerceAtLeast(0))
        withContext(Dispatchers.IO) {
            runCatching { savedQueueFile.writeText(ApiJson.encodeToString(SavedQueue.serializer(), saved)) }
        }
    }

    // ---------- Play queue sync between devices (savePlayQueue / getPlayQueue) ----------

    /** Something was played or changed in this app session, so the server's copy should follow. */
    private var touched = false

    /** The server's queue was checked (and any offer answered); until then this phone doesn't overwrite it. */
    private var remoteCheckedFor: String? = null
    private var lastSynced: Triple<List<String>, Int, Long>? = null

    private val _remoteOffer = MutableStateFlow<RemoteQueueOffer?>(null)

    /** A queue from another device to continue; the app shows it as a question. */
    val remoteOffer: StateFlow<RemoteQueueOffer?> = _remoteOffer.asStateFlow()

    /** How this phone signs the queues it saves, so other devices can say where a queue came from. */
    val deviceName: String = "${SubsonicClientName} (${android.os.Build.MODEL.trim().ifBlank { "Android" }.take(40)})"

    private suspend fun syncToServer(accountKey: String, ids: List<String>, index: Int, positionMs: Long) {
        if (!c.settings.settings.value.queueSync || !touched || ids.isEmpty()) return
        // The check at start failed (offline): what you've started playing here since then wins.
        if (remoteCheckedFor != accountKey && _remoteOffer.value == null && startCheckFor == accountKey) remoteCheckedFor = accountKey
        if (remoteCheckedFor != accountKey || _remoteOffer.value != null) return
        val session = c.sessions.active.value ?: return
        if (session.account.key != accountKey) return
        val now = Triple(ids, index, positionMs / 5000)
        if (now == lastSynced) return
        runCatching { session.client.savePlayQueue(ids, index, positionMs, deviceName) }
            .onSuccess { lastSynced = now }
    }

    /** Saves straight away (the app is going to the background). */
    fun saveNow() {
        if (controller == null) return
        scope.launch {
            val mc = controller ?: return@launch
            val key = c.sessions.active.value?.account?.key ?: return@launch
            val ids = (0 until mc.mediaItemCount).map { mc.getMediaItemAt(it).mediaId }
            syncToServer(key, ids, mc.currentMediaItemIndex, mc.currentPosition.coerceAtLeast(0))
        }
    }

    /**
     * Looks at the queue saved on the server. When it differs from the one here (and, unless
     * [manual], wasn't saved by this phone), it's offered in [remoteOffer]. Returns false when
     * there's nothing to continue.
     */
    private suspend fun checkRemote(manual: Boolean): Boolean {
        val session = c.sessions.active.value ?: return false
        val key = session.account.key
        val mc = controller()
        val remote = try {
            session.client.playQueue()
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            if (manual) throw e
            // Offline or the server is down: look again later, and don't overwrite its queue meanwhile.
            return false
        }
        if (c.sessions.active.value?.account?.key != key) return false
        val offer = remote?.let { q ->
            val idx = q.entry.indexOfFirst { it.id == q.current }.coerceAtLeast(0)
            val current = q.entry[idx]
            val localId = mc.currentMediaItem?.mediaId
            val samePlace = localId == current.id && kotlin.math.abs(mc.currentPosition - q.position) < 15_000
            val ours = q.changedBy == deviceName
            when {
                samePlace -> null
                ours && !manual && mc.mediaItemCount > 0 -> null
                else -> RemoteQueueOffer(key, q, idx, current)
            }
        }
        _remoteOffer.value = offer
        if (offer == null) remoteCheckedFor = key
        return offer != null
    }

    /** "Resume from another device": checks the server now, even with sync off. */
    fun resumeFromOtherDevice() = launch {
        if (!checkRemote(manual = true)) {
            Toast.makeText(context, str(R.string.queue_sync_nothing), Toast.LENGTH_SHORT).show()
        }
    }

    /** Continue the offered queue here: it replaces this phone's queue at the same song and position. */
    fun acceptRemote() = launch {
        val offer = _remoteOffer.value ?: return@launch
        _remoteOffer.value = null
        remoteCheckedFor = offer.accountKey
        if (c.sessions.active.value?.account?.key != offer.accountKey) return@launch
        val mc = controller()
        continuation = null
        unshuffled = null
        touched = true
        mc.setMediaItems(offer.queue.entry.map(::mediaItem), offer.index, offer.queue.position.coerceAtLeast(0))
        mc.prepare()
        mc.play()
        refresh()
    }

    /** Keep this phone's queue; it's saved to the server again the next time it changes. */
    fun declineRemote() {
        val offer = _remoteOffer.value ?: return
        _remoteOffer.value = null
        remoteCheckedFor = offer.accountKey
    }

    private suspend fun restoreQueue(mc: MediaController, autoplay: Boolean) {
        if (mc.mediaItemCount > 0) return
        val key = c.sessions.active.value?.account?.key ?: return
        val saved = withContext(Dispatchers.IO) {
            runCatching { ApiJson.decodeFromString(SavedQueue.serializer(), savedQueueFile.readText()) }.getOrNull()
        } ?: return
        if (saved.accountKey != key || saved.songs.isEmpty()) return
        mc.setMediaItems(saved.songs.map(::mediaItem), saved.index.coerceIn(0, saved.songs.lastIndex), saved.positionMs)
        mc.prepare()
        if (autoplay) mc.play()
    }

    /** Reconnects to a running service, or brings back the last queue (paused) after a restart. */
    fun connect() = launch {
        val mc = controller()
        restoreQueue(mc, autoplay = false)
        refresh()
        // Once per account and app start: is there a queue from another device to continue?
        val key = c.sessions.active.value?.account?.key
        if (key != null && key != startCheckFor) {
            startCheckFor = key
            if (c.settings.settings.value.queueSync) checkRemote(manual = false) else remoteCheckedFor = key
        }
    }

    private var startCheckFor: String? = null

    companion object {
        private const val QUEUE_FILE = "queue.json"
        private const val SubsonicClientName = com.vknxworks.navictrl.data.api.SubsonicClient.CLIENT

        /** Deletes the saved queue if it belongs to [accountKey] (called on sign-out). */
        fun forgetSavedQueue(context: Context, accountKey: String) {
            val file = File(context.filesDir, QUEUE_FILE)
            if (!file.exists()) return
            val owner = runCatching { ApiJson.decodeFromString(SavedQueue.serializer(), file.readText()).accountKey }.getOrNull()
            if (owner == null || owner == accountKey) file.delete()
        }
    }

    /** Called when switching accounts: the old account's queue can't play on the new one. */
    fun reset() {
        touched = false
        lastSynced = null
        remoteCheckedFor = null
        startCheckFor = null
        _remoteOffer.value = null
        val mc = controller ?: return
        mc.stop()
        mc.clearMediaItems()
        continuation = null
        unshuffled = null
        known.clear()
        refresh()
    }
}
