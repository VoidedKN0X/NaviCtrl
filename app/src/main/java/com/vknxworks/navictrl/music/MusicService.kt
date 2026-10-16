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
import com.vknxworks.navictrl.MainActivity
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.api.scrobble
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

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            // Artwork is resolved in-process so authenticated cover URLs never reach other apps.
            .setBitmapLoader(CacheBitmapLoader(ArtworkBitmapLoader(this, c.http) { container.sessions.active.value?.client }))
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                startTrack(mediaItem)
            }
        })

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

    private fun startTrack(item: MediaItem?) {
        val id = item?.mediaId?.takeIf { it.isNotBlank() && !it.startsWith(RADIO_PREFIX) }
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
     * Only this app and the system's own media surfaces (notification, lock screen, Bluetooth,
     * Android Auto, apps holding MEDIA_CONTENT_CONTROL) may control playback. Other apps get nothing.
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        val s = session ?: return null
        val pkg = controllerInfo.packageName
        val uid = controllerInfo.uid
        // The package name is reported by the caller itself, so check it against the binder UID.
        val pkgVerified = uid >= 0 && packageManager.getPackagesForUid(uid)?.contains(pkg) == true
        val allowed = uid == Process.myUid() ||
            controllerInfo.isTrusted ||
            (pkgVerified && pkg in SYSTEM_CONTROLLERS) ||
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
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val RADIO_PREFIX = "radio:"
        private const val LEGACY_BROWSER_ACTION = "android.media.browse.MediaBrowserService"
        private val SYSTEM_CONTROLLERS = setOf(
            "com.android.systemui",
            "com.android.bluetooth",
            "com.google.android.bluetooth",
            "com.google.android.projection.gearhead",
        )
    }
}
