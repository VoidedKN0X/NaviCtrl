package com.vknxworks.navictrl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.vknxworks.navictrl.data.api.SubsonicClient
import com.vknxworks.navictrl.data.appDataStore
import com.vknxworks.navictrl.data.auth.SessionManager
import com.vknxworks.navictrl.data.download.DownloadRepository
import com.vknxworks.navictrl.data.library.StarStore
import com.vknxworks.navictrl.data.settings.SettingsRepository
import com.vknxworks.navictrl.music.MusicController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
        .build()

    val sessions = SessionManager(context, http, appScope)
    val settings = SettingsRepository(context, appScope)
    val stars = StarStore(context, sessions, appScope)
    val downloads = DownloadRepository(context, appScope, sessions, settings, http)
    val music by lazy { MusicController(context, this) }

    /**
     * Streamed songs are kept on disk so replays and skips back don't hit the network.
     * Size is read once at startup; null when turned off.
     */
    val playbackCache: SimpleCache? by lazy {
        val mb = runBlocking { context.appDataStore.data.first()[intPreferencesKey("cache_mb")] } ?: 1024
        if (mb <= 0) null
        else SimpleCache(File(context.cacheDir, "media"), LeastRecentlyUsedCacheEvictor(mb * 1024L * 1024L), StandaloneDatabaseProvider(context))
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

val Context.container: AppContainer
    get() = (applicationContext as NaviCtrlApp).container
