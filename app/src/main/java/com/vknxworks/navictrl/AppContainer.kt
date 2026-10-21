package com.vknxworks.navictrl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import coil3.SingletonImageLoader
import com.vknxworks.navictrl.data.api.SubsonicClient
import com.vknxworks.navictrl.data.appDataStore
import com.vknxworks.navictrl.data.auth.SessionManager
import com.vknxworks.navictrl.data.download.DownloadRepository
import com.vknxworks.navictrl.data.net.CleartextPolicy
import com.vknxworks.navictrl.data.net.RedirectGuard
import com.vknxworks.navictrl.data.library.StarStore
import com.vknxworks.navictrl.data.lock.LockVault
import com.vknxworks.navictrl.data.settings.SettingsRepository
import com.vknxworks.navictrl.music.MusicController
import com.vknxworks.navictrl.music.MusicService
import com.vknxworks.navictrl.ui.lock.AppLock
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/** Manual dependency container; one per process. */
class AppContainer(val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "NaviCtrl/${BuildConfig.VERSION_NAME} (Android)")
                    .build()
            )
        }
        // Plain HTTP only to the user's own server (after they chose it), and never via a redirect
        // from an HTTPS request.
        .addNetworkInterceptor(CleartextPolicy.interceptor)
        // The Navidrome session header and the sign-in request never follow a redirect to another server.
        .addNetworkInterceptor(RedirectGuard.interceptor)
        .build()

    val settings = SettingsRepository(context, appScope)

    /** The app lock's PIN, fingerprint key and the key that seals the saved passwords while locked. */
    val vault = LockVault(context, "navictrl")

    /** Read once, synchronously, so the app never shows unlocked for a moment at start. */
    val lockAtStart: Boolean = vault.enabled || runBlocking { runCatching { settings.stored.first().appLock }.getOrDefault(false) }

    val sessions = SessionManager(context, http, vault, appScope)
    val stars = StarStore(context, sessions, appScope)
    val downloads = DownloadRepository(context, appScope, sessions, settings, http)
    val music by lazy { MusicController(context, this) }

    private val _playlistEdits = MutableStateFlow(0)

    /** Goes up after every playlist change made in the app, so playlist lists and pages already open reload. */
    val playlistEdits: StateFlow<Int> = _playlistEdits.asStateFlow()

    fun playlistsChanged() = _playlistEdits.update { it + 1 }

    /**
     * Streamed songs are kept on disk so replays and skips back don't hit the network.
     * Size is read once at startup; null when turned off.
     */
    val playbackCache: SimpleCache? by lazy {
        val mb = runBlocking { context.appDataStore.data.first()[intPreferencesKey("cache_mb")] } ?: 1024
        if (mb <= 0) null
        else SimpleCache(File(context.cacheDir, "media"), LeastRecentlyUsedCacheEvictor(mb * 1024L * 1024L), StandaloneDatabaseProvider(context))
    }

    /**
     * Signs out of the account [key] and clears what the app kept for it on the phone apart from
     * downloads: its songs in the playback cache, cached artwork and its saved queue.
     */
    suspend fun signOut(key: String) {
        sessions.signOut(key)
        withContext(Dispatchers.IO) {
            runCatching {
                playbackCache?.let { cache ->
                    val prefix = cacheTag(key) + ":"
                    // Entries cached before keys carried the account are removed too.
                    cache.keys.filter { it.startsWith(prefix) || !TAGGED_KEY.containsMatchIn(it) }.forEach(cache::removeResource)
                }
            }
            runCatching { MusicController.forgetSavedQueue(context, key) }
            runCatching {
                SingletonImageLoader.get(context).let { loader ->
                    loader.memoryCache?.clear()
                    loader.diskCache?.clear()
                }
            }
        }
    }

    /** The app lock is on, either with a PIN or still the old one waiting for a PIN. */
    val lockEnabled: Boolean get() = vault.enabled || settings.settings.value.appLock

    /** Turns the app lock on with [pin]: the PIN seals the saved passwords from now on. */
    suspend fun enableLock(pin: String) {
        // Unlocked first, so the lock screen doesn't flash up while the PIN is being saved.
        AppLock.markUnlocked()
        withContext(Dispatchers.Default) { vault.setUp(pin) }
        settings.update { it.copy(appLock = true) }
        sessions.sealPasswords()
    }

    /** Turns the app lock off (after the PIN was confirmed): passwords go back to Keystore-only encryption. */
    suspend fun disableLock() {
        sessions.unsealPasswords()
        settings.update { it.copy(appLock = false) }
        vault.disable()
    }

    /**
     * "Forgot PIN?": the sealed passwords can't be read any more, so forget every account, and the
     * downloads, playback cache, saved queues and artwork kept for them.
     */
    suspend fun forgetEverything() {
        if (MusicService.running) runCatching { music.reset() }
        val keys = sessions.accounts.value.map { it.key }
        downloads.deleteEverything()
        sessions.forgetAllAccounts()
        withContext(Dispatchers.IO) {
            runCatching { playbackCache?.let { cache -> cache.keys.toList().forEach(cache::removeResource) } }
            keys.forEach { key -> runCatching { MusicController.forgetSavedQueue(context, key) } }
            runCatching {
                SingletonImageLoader.get(context).let { loader ->
                    loader.memoryCache?.clear()
                    loader.diskCache?.clear()
                }
            }
        }
        AppLock.markUnlocked()
        settings.update { it.copy(appLock = false) }
        vault.disable()
    }

    /** Locked: wipe the key from memory and drop the session, unless music or downloads need it. */
    fun onLocked() {
        vault.close()
        sessions.onLocked()
    }

    fun onUnlocked() {
        AppLock.markUnlocked()
        sessions.onUnlocked()
    }

    init {
        AppLock.onLocked = { onLocked() }
        sessions.mediaBusy = { MusicService.running || downloads.busy() }
        // The last download finished while the app is locked: nothing needs the sign-in any more.
        appScope.launch {
            downloads.entries.map { downloads.busy() }.distinctUntilChanged().collect { busy -> if (!busy) sessions.mediaStopped() }
        }
    }

    /** Client for the signed-in account. Screens are only shown while signed in. */
    val api: SubsonicClient
        get() = sessions.active.value?.client ?: error("Not signed in")

    fun isOnCellular(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && !caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}

/** Short, stable tag for an account in playback-cache keys, so accounts never share cached songs. */
fun cacheTag(accountKey: String): String = SubsonicClient.md5(accountKey).take(12)

private val TAGGED_KEY = Regex("^[0-9a-f]{12}:")

val Context.container: AppContainer
    get() = (applicationContext as NaviCtrlApp).container
