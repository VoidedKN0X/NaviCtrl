package com.vknxworks.navictrl.data.settings

import com.vknxworks.navictrl.music.PeqBand
import com.vknxworks.navictrl.music.PeqConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * NaviCtrl's settings as a JSON file the user keeps: streaming and download quality, playback, both
 * equalizers, lyrics, listening history, the look and the sort order.
 *
 * Never in the file, and never changed by an import: accounts, server addresses and passwords, the
 * app lock and its PIN, "Lock again", "Hide in screenshots" and "Hide in recent apps", downloads and
 * anything else stored on the phone. So a settings file can't sign in anywhere, allow an unencrypted
 * connection, trust a certificate or weaken the app's protection.
 */
@Serializable
data class SettingsBackup(
    val format: String = FORMAT,
    val version: Int = VERSION,
    /** NaviCtrl version that wrote the file, for information. */
    val appVersion: String? = null,
    /** ISO time the file was written. */
    val createdAt: String? = null,
    val settings: BackupSettings = BackupSettings(),
) {
    companion object {
        const val FORMAT = "navictrl-settings"
        const val VERSION = 1
    }
}

/** Every field is optional, so files from older or newer versions still restore what they can. */
@Serializable
data class BackupSettings(
    val wifiBitrate: Int? = null,
    val mobileBitrate: Int? = null,
    val format: String? = null,
    val cacheMb: Int? = null,
    val preloadMobile: Boolean? = null,
    val downloadBitrate: Int? = null,
    val downloadFormat: String? = null,
    val downloadWifiOnly: Boolean? = null,
    val downloadLimitMb: Int? = null,
    val downloadedOnly: Boolean? = null,
    val lyricsOnline: Boolean? = null,
    val scrobble: Boolean? = null,
    val queueSync: Boolean? = null,
    val theme: String? = null,
    val dynamicColor: Boolean? = null,
    val songSort: String? = null,
    val songSortAscending: Boolean? = null,
    val albumSort: String? = null,
    val albumGrid: Boolean? = null,
    val gapless: Boolean? = null,
    val crossfadeSec: Int? = null,
    val replayGain: String? = null,
    val replayGainPreamp: Int? = null,
    val preventClipping: Boolean? = null,
    val eqEnabled: Boolean? = null,
    val eqPreset: Int? = null,
    val eqBands: List<Int>? = null,
    val bassBoost: Int? = null,
    val peqEnabled: Boolean? = null,
    val peqPreamp: Int? = null,
    /** The parametric bands in the settings file's own text form (see PeqBand.encode). */
    val peqBands: String? = null,
)

/** Thrown for a file that isn't a NaviCtrl settings file, or one from a newer, incompatible version. */
class BackupFormatException(val newer: Boolean) : Exception()

object SettingsBackups {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = false
    }

    /** The bitrates the settings offer; 0 is "original files". */
    private val BITRATES = setOf(0, 96, 128, 192, 256, 320)

    fun export(s: AppSettings, appVersion: String, now: String): String = json.encodeToString(
        SettingsBackup.serializer(),
        SettingsBackup(
            appVersion = appVersion,
            createdAt = now,
            settings = BackupSettings(
                wifiBitrate = s.wifiBitrate,
                mobileBitrate = s.mobileBitrate,
                format = s.format.name,
                cacheMb = s.cacheMb,
                preloadMobile = s.preloadMobile,
                downloadBitrate = s.downloadBitrate,
                downloadFormat = s.downloadFormat.name,
                downloadWifiOnly = s.downloadWifiOnly,
                downloadLimitMb = s.downloadLimitMb,
                downloadedOnly = s.downloadedOnly,
                lyricsOnline = s.lyricsOnline,
                scrobble = s.scrobble,
                queueSync = s.queueSync,
                theme = s.theme.name,
                dynamicColor = s.dynamicColor,
                songSort = s.songSort.name,
                songSortAscending = s.songSortAscending,
                albumSort = s.albumSort.name,
                albumGrid = s.albumGrid,
                gapless = s.gapless,
                crossfadeSec = s.crossfadeSec,
                replayGain = s.replayGain.name,
                replayGainPreamp = s.replayGainPreamp,
                preventClipping = s.preventClipping,
                eqEnabled = s.eqEnabled,
                eqPreset = s.eqPreset,
                eqBands = s.eqBands,
                bassBoost = s.bassBoost,
                peqEnabled = s.peqEnabled,
                peqPreamp = s.peqPreamp,
                peqBands = PeqBand.encode(s.peqBands),
            ),
        ),
    )

    /** Reads and checks a settings file. */
    fun parse(text: String): SettingsBackup {
        val b = runCatching { json.decodeFromString(SettingsBackup.serializer(), text) }.getOrNull()
            ?: throw BackupFormatException(newer = false)
        if (b.format != SettingsBackup.FORMAT) throw BackupFormatException(newer = false)
        if (b.version > SettingsBackup.VERSION) throw BackupFormatException(newer = true)
        return b
    }

    /**
     * [cur] with what the file has. A value the file doesn't have, or one outside what the settings offer,
     * leaves the current one. The app lock and the other privacy settings are never touched.
     */
    fun apply(cur: AppSettings, s: BackupSettings): AppSettings = cur.copy(
        wifiBitrate = s.wifiBitrate?.takeIf { it in BITRATES } ?: cur.wifiBitrate,
        mobileBitrate = s.mobileBitrate?.takeIf { it in BITRATES } ?: cur.mobileBitrate,
        format = enumOr(s.format, cur.format),
        cacheMb = s.cacheMb?.takeIf { it in 0..8192 } ?: cur.cacheMb,
        preloadMobile = s.preloadMobile ?: cur.preloadMobile,
        downloadBitrate = s.downloadBitrate?.takeIf { it in BITRATES } ?: cur.downloadBitrate,
        downloadFormat = enumOr(s.downloadFormat, cur.downloadFormat),
        downloadWifiOnly = s.downloadWifiOnly ?: cur.downloadWifiOnly,
        downloadLimitMb = s.downloadLimitMb?.takeIf { it in 0..(1024 * 1024) } ?: cur.downloadLimitMb,
        downloadedOnly = s.downloadedOnly ?: cur.downloadedOnly,
        lyricsOnline = s.lyricsOnline ?: cur.lyricsOnline,
        scrobble = s.scrobble ?: cur.scrobble,
        queueSync = s.queueSync ?: cur.queueSync,
        theme = enumOr(s.theme, cur.theme),
        dynamicColor = s.dynamicColor ?: cur.dynamicColor,
        songSort = enumOr(s.songSort, cur.songSort),
        songSortAscending = s.songSortAscending ?: cur.songSortAscending,
        albumSort = enumOr(s.albumSort, cur.albumSort),
        albumGrid = s.albumGrid ?: cur.albumGrid,
        gapless = s.gapless ?: cur.gapless,
        crossfadeSec = s.crossfadeSec?.takeIf { it in 0..12 } ?: cur.crossfadeSec,
        replayGain = enumOr(s.replayGain, cur.replayGain),
        replayGainPreamp = s.replayGainPreamp?.takeIf { it in -15..15 } ?: cur.replayGainPreamp,
        preventClipping = s.preventClipping ?: cur.preventClipping,
        eqEnabled = s.eqEnabled ?: cur.eqEnabled,
        eqPreset = s.eqPreset?.takeIf { it in -1..100 } ?: cur.eqPreset,
        eqBands = s.eqBands?.takeIf { it.size <= 32 }?.map { it.coerceIn(-3000, 3000) } ?: cur.eqBands,
        bassBoost = s.bassBoost?.takeIf { it in 0..1000 } ?: cur.bassBoost,
        peqEnabled = s.peqEnabled ?: cur.peqEnabled,
        peqPreamp = s.peqPreamp?.takeIf { it in PeqConfig.MIN_PREAMP_TENTHS..0 } ?: cur.peqPreamp,
        peqBands = s.peqBands?.let { PeqBand.decode(it) } ?: cur.peqBands,
    )

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
}
