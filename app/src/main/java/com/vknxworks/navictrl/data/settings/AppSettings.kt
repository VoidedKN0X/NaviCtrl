package com.vknxworks.navictrl.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vknxworks.navictrl.data.api.AlbumSort
import com.vknxworks.navictrl.data.api.SongSort
import com.vknxworks.navictrl.data.appDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class ThemeChoice(val label: String) { SYSTEM("Follow system"), DARK("Dark"), LIGHT("Light"), BLACK("Pure black") }

enum class TranscodeFormat(val param: String, val label: String) {
    MP3("mp3", "MP3"),
    OPUS("opus", "Opus"),
    AAC("aac", "AAC"),
}

data class AppSettings(
    /** kbps; 0 = original files. */
    val wifiBitrate: Int = 0,
    val mobileBitrate: Int = 320,
    val format: TranscodeFormat = TranscodeFormat.MP3,
    /** kbps for downloads; 0 = original files. */
    val downloadBitrate: Int = 0,
    val downloadWifiOnly: Boolean = true,
    /** MB of streamed music to keep on the phone; 0 = off. Takes effect after a restart. */
    val cacheMb: Int = 1024,
    val scrobble: Boolean = true,
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val dynamicColor: Boolean = false,
    val songSort: SongSort = SongSort.TITLE,
    val songSortAscending: Boolean = true,
    val albumSort: AlbumSort = AlbumSort.NAME,
    val albumGrid: Boolean = true,
)

class SettingsRepository(private val context: Context, scope: CoroutineScope) {
    private object K {
        val wifiBitrate = intPreferencesKey("wifi_bitrate")
        val mobileBitrate = intPreferencesKey("mobile_bitrate")
        val format = stringPreferencesKey("format")
        val downloadBitrate = intPreferencesKey("download_bitrate")
        val wifiOnly = booleanPreferencesKey("download_wifi_only")
        val cache = intPreferencesKey("cache_mb")
        val scrobble = booleanPreferencesKey("scrobble")
        val theme = stringPreferencesKey("theme")
        val dynamic = booleanPreferencesKey("dynamic_color")
        val songSort = stringPreferencesKey("song_sort")
        val songAsc = booleanPreferencesKey("song_sort_asc")
        val albumSort = stringPreferencesKey("album_sort")
        val albumGrid = booleanPreferencesKey("album_grid")
    }

    val settings: StateFlow<AppSettings> = context.appDataStore.data
        .map { it.toSettings() }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    private fun Preferences.toSettings() = AppSettings(
        wifiBitrate = this[K.wifiBitrate] ?: 0,
        mobileBitrate = this[K.mobileBitrate] ?: 320,
        format = enumOr(this[K.format], TranscodeFormat.MP3),
        downloadBitrate = this[K.downloadBitrate] ?: 0,
        downloadWifiOnly = this[K.wifiOnly] ?: true,
        cacheMb = this[K.cache] ?: 1024,
        scrobble = this[K.scrobble] ?: true,
        theme = enumOr(this[K.theme], ThemeChoice.SYSTEM),
        dynamicColor = this[K.dynamic] ?: false,
        songSort = enumOr(this[K.songSort], SongSort.TITLE),
        songSortAscending = this[K.songAsc] ?: true,
        albumSort = enumOr(this[K.albumSort], AlbumSort.NAME),
        albumGrid = this[K.albumGrid] ?: true,
    )

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.appDataStore.edit { p ->
            val s = transform(p.toSettings())
            p[K.wifiBitrate] = s.wifiBitrate
            p[K.mobileBitrate] = s.mobileBitrate
            p[K.format] = s.format.name
            p[K.downloadBitrate] = s.downloadBitrate
            p[K.wifiOnly] = s.downloadWifiOnly
            p[K.cache] = s.cacheMb
            p[K.scrobble] = s.scrobble
            p[K.theme] = s.theme.name
            p[K.dynamic] = s.dynamicColor
            p[K.songSort] = s.songSort.name
            p[K.songAsc] = s.songSortAscending
            p[K.albumSort] = s.albumSort.name
            p[K.albumGrid] = s.albumGrid
        }
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
}
