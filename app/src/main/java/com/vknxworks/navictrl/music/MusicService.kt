package com.vknxworks.navictrl.music

import android.app.PendingIntent
import android.content.Intent
import android.os.IBinder
import android.os.Process
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.vknxworks.navictrl.MainActivity
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.api.scrobble
import com.vknxworks.navictrl.data.settings.AppSettings
import com.vknxworks.navictrl.data.settings.ReplayGainMode
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Background playback with the media notification, lock-screen and Bluetooth controls, and scrobbling. */
class MusicService : MediaSessionService() {

    private var session: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Scrobble bookkeeping for the current track.
    private var trackId: String? = null
    private var listenedMs = 0L
    private var submitted = false

    // Loudness: the ReplayGain part of the volume (1 or lower) and whether the song fades in.
    private var settingsNow = AppSettings()
    private var rgVolume = 1f
    private var fadeIn = false
    private var lastItem: MediaItem? = null

    override fun onCreate() {
        super.onCreate()
        val c = container
        // Local files go straight to disk; streams go through the playback cache when it's on.
        val network: DataSource.Factory = OkHttpDataSource.Factory(c.http)
        val remote: DataSource.Factory = c.playbackCache?.let { cache ->
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(network)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        } ?: network

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(this, remote)))
            .build()
        settingsNow = c.settings.settings.value
        applyGapless(player)

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            // Artwork is resolved in-process so authenticated cover URLs never reach other apps.
            .setBitmapLoader(CacheBitmapLoader(ArtworkBitmapLoader(this, c.http) { container.sessions.active.value?.client }))
            .setCallback(QueueGuard)
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                startTrack(mediaItem)
                // Crossfade: a song that follows on its own fades in, unless it carries on the same album gaplessly.
                val prev = lastItem
                fadeIn = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && prev != null && mediaItem != null &&
                    settingsNow.crossfadeSec > 0 && !(settingsNow.gapless && continuesAlbum(prev, mediaItem))
                lastItem = mediaItem
                updateGain(player)
            }

            override fun onEvents(player: Player, events: Player.Events) {
                updateRunning(player)
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                AudioFx.attach(audioSessionId, settingsNow)
                updateGain(player)
            }
        })
        AudioFx.attach(player.audioSessionId, settingsNow)
        lastItem = player.currentMediaItem

        // Settings changed in the app apply straight away.
        scope.launch {
            c.settings.settings.collect { s ->
                settingsNow = s
                applyGapless(player)
                AudioFx.apply(s)
                updateGain(player)
            }
        }

        // Volume ramps for crossfade, often while a fade can happen and rarely otherwise.
        scope.launch {
            while (isActive) {
                applyVolume(player)
                delay(if (player.isPlaying && settingsNow.crossfadeSec > 0) 50 else 250)
            }
        }

        // Count real listening time once a second; submit at half the track or four minutes.
        scope.launch {
            while (isActive) {
                delay(1000)
                if (!player.isPlaying) continue
                if (trackId == null) startTrack(player.currentMediaItem)
                listenedMs += 1000
                val id = trackId ?: continue
                val duration = player.duration
                if (!submitted && duration > 30_000 && listenedMs >= minOf(duration / 2, 240_000L)) {
                    submitted = true
                    send(id, submission = true)
                }
            }
        }
    }

    /** Gapless: load the next song well ahead, so it follows without a pause even on a slow connection. */
    private fun applyGapless(player: ExoPlayer) {
        player.setPreloadConfiguration(
            if (settingsNow.gapless) ExoPlayer.PreloadConfiguration(PRELOAD_US) else ExoPlayer.PreloadConfiguration.DEFAULT,
        )
    }

    /** ReplayGain for the current song: turned down with the player volume, raised with the loudness enhancer. */
    private fun updateGain(player: Player) {
        val s = settingsNow
        val ex = player.currentMediaItem?.mediaMetadata?.extras
        var db: Double? = null
        if (s.replayGain != ReplayGainMode.OFF && ex != null) {
            val track = ex.dbl(RG_TRACK_GAIN)
            val album = ex.dbl(RG_ALBUM_GAIN)
            val trackPeak = ex.dbl(RG_TRACK_PEAK)
            val albumPeak = ex.dbl(RG_ALBUM_PEAK)
            val gain = if (s.replayGain == ReplayGainMode.ALBUM) album ?: track else track ?: album
            val peak = if (s.replayGain == ReplayGainMode.ALBUM) albumPeak ?: trackPeak else trackPeak ?: albumPeak
            // Songs without ReplayGain tags keep their own loudness, unless the server suggests a fallback gain.
            db = (gain ?: ex.dbl(RG_FALLBACK_GAIN))?.plus(s.replayGainPreamp)
            if (db != null && s.preventClipping && peak != null && peak > 0.0) db = min(db, -20.0 * log10(peak))
        }
        val d = (db ?: 0.0).coerceIn(-30.0, 15.0)
        if (d > 0.0 && AudioFx.canBoost) {
            rgVolume = 1f
            AudioFx.setBoost((d * 100).roundToInt())
        } else {
            AudioFx.setBoost(0)
            rgVolume = 10.0.pow(min(d, 0.0) / 20.0).toFloat()
        }
        applyVolume(player)
    }

    private fun applyVolume(player: Player) {
        val v = (rgVolume * fadeFactor(player)).coerceIn(0f, 1f)
        if (abs(player.volume - v) > 0.002f) player.volume = v
    }

    /** Crossfade: 1 normally, ramping to 0 over the last seconds of a song and back up at the start of the next. */
    private fun fadeFactor(player: Player): Float {
        val fadeMs = settingsNow.crossfadeSec * 1000L
        if (fadeMs <= 0) {
            fadeIn = false
            return 1f
        }
        var f = 1f
        val duration = player.duration
        val pos = player.currentPosition
        if (duration != C.TIME_UNSET && duration > fadeMs * 2 && fadesIntoNext(player)) {
            val remaining = duration - pos
            if (remaining < fadeMs) f = (remaining.toFloat() / fadeMs).coerceIn(0f, 1f)
        }
        if (fadeIn) {
            if (pos >= fadeMs) fadeIn = false else f = min(f, (pos.toFloat() / fadeMs).coerceIn(0f, 1f))
        }
        return f
    }

    private fun fadesIntoNext(player: Player): Boolean {
        if (player.repeatMode == Player.REPEAT_MODE_ONE) return false
        val next = player.nextMediaItemIndex
        if (next == C.INDEX_UNSET) return false
        val current = player.currentMediaItem ?: return false
        return !(settingsNow.gapless && continuesAlbum(current, player.getMediaItemAt(next)))
    }

    /** [b] is the next song of [a]'s album (by track and disc number when the songs have them). */
    private fun continuesAlbum(a: MediaItem, b: MediaItem): Boolean {
        val albumA = a.mediaMetadata.extras?.getString("albumId")
        val albumB = b.mediaMetadata.extras?.getString("albumId")
        if (albumA.isNullOrBlank() || albumA != albumB) return false
        val ta = a.mediaMetadata.trackNumber
        val tb = b.mediaMetadata.trackNumber
        if (ta == null || tb == null) return true
        val da = a.mediaMetadata.discNumber ?: 1
        val db = b.mediaMetadata.discNumber ?: 1
        return (db == da && tb == ta + 1) || (db == da + 1 && tb == 1)
    }

    private fun android.os.Bundle.dbl(key: String): Double? =
        if (containsKey(key)) getDouble(key).takeUnless { it.isNaN() || it.isInfinite() } else null

    private fun startTrack(item: MediaItem?) {
        val id = item?.mediaId?.takeIf { it.isNotBlank() }
        trackId = id
        listenedMs = 0
        submitted = false
        // "Now playing" for the server's activity view and Last.fm/ListenBrainz.
        if (id != null) send(id, submission = false)
    }

    private fun send(id: String, submission: Boolean) {
        val c = container
        if (!c.settings.settings.value.scrobble) return
        val client = c.sessions.active.value?.client ?: return
        c.appScope.launch { runCatching { client.scrobble(id, submission) } }
    }

    /**
     * Only this app and the system's own media surfaces (notification, lock screen, Bluetooth, car
     * and voice controls built into the phone, apps holding MEDIA_CONTENT_CONTROL) may control
     * playback. Other apps get nothing.
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        val s = session ?: return null
        val pkg = controllerInfo.packageName
        val uid = controllerInfo.uid
        // The package name is reported by the caller itself, so check it against the binder UID.
        val pkgVerified = uid >= 0 && packageManager.getPackagesForUid(uid)?.contains(pkg) == true
        val allowed = uid == Process.myUid() ||
            controllerInfo.isTrusted ||
            (pkgVerified && isSystemApp(pkg)) ||
            // Media button intents delivered to the service by the platform (no binder caller).
            (uid < 0 && pkg == MediaSession.ControllerInfo.LEGACY_CONTROLLER_PACKAGE_NAME &&
                controllerInfo.controllerVersion == MediaSession.ControllerInfo.LEGACY_CONTROLLER_VERSION) ||
            (pkgVerified && (
                s.isMediaNotificationController(controllerInfo) ||
                    s.isAutomotiveController(controllerInfo) ||
                    s.isAutoCompanionController(controllerInfo)
                ))
        return if (allowed) s else null
    }

    /** Apps built into the phone's firmware (system UI, Bluetooth, car mode); a sideloaded app never is one. */
    private fun isSystemApp(pkg: String): Boolean = runCatching {
        val ai = packageManager.getApplicationInfo(pkg, 0)
        ai.flags and (android.content.pm.ApplicationInfo.FLAG_SYSTEM or android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }.getOrDefault(false)

    /**
     * Only the app itself puts songs in the queue. Other allowed controllers (system UI, Bluetooth,
     * car mode) can play, pause and skip, but can't make the player open their own addresses or
     * files from the app's private storage.
     */
    private object QueueGuard : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ) = CallbackToFutureAdapter.getFuture<MutableList<MediaItem>> { completer ->
            if (controller.uid != Process.myUid() || mediaItems.any { it.localConfiguration == null }) {
                completer.setException(UnsupportedOperationException("Not allowed"))
            } else {
                completer.set(mediaItems)
            }
            "onAddMediaItems"
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // The legacy MediaBrowserService interface isn't declared in the manifest; don't hand out
        // the session's legacy binder to anyone who binds with that action explicitly.
        if (intent?.action == LEGACY_BROWSER_ACTION) return null
        return super.onBind(intent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        AudioFx.release()
        scope.cancel()
        if (running) {
            running = false
            // With the app locked, the sign-in was only kept in memory for playback.
            container.sessions.mediaStopped()
        }
        super.onDestroy()
    }

    /** Music is playing or paused with a queue (its notification is up): that keeps the sign-in while locked. */
    private fun updateRunning(player: Player) {
        val now = player.mediaItemCount > 0 && player.playbackState != Player.STATE_IDLE && player.playbackState != Player.STATE_ENDED
        if (now == running) return
        running = now
        if (!now) container.sessions.mediaStopped()
    }

    companion object {
        /** Music is playing or paused with a queue: it keeps the sign-in in memory while the app is locked. */
        @Volatile var running = false
            private set

        const val RG_TRACK_GAIN = "rgTrackGain"
        const val RG_ALBUM_GAIN = "rgAlbumGain"
        const val RG_TRACK_PEAK = "rgTrackPeak"
        const val RG_ALBUM_PEAK = "rgAlbumPeak"
        const val RG_FALLBACK_GAIN = "rgFallbackGain"

        /** How much of the next song gapless playback loads ahead: 30 seconds. */
        private const val PRELOAD_US = 30_000_000L

        private const val LEGACY_BROWSER_ACTION = "android.media.browse.MediaBrowserService"
    }
}
