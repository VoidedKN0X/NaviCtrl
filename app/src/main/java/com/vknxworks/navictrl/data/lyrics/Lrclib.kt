package com.vknxworks.navictrl.data.lyrics

import com.vknxworks.navictrl.data.api.ApiJson
import com.vknxworks.navictrl.data.api.Lyrics
import com.vknxworks.navictrl.data.api.LyricsSource
import com.vknxworks.navictrl.data.api.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.math.abs

/** One record from LRCLIB's /api/get or /api/search. Only the fields NaviCtrl uses. */
@Serializable
data class LrclibRecord(
    val id: Long = 0,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    /** Seconds. */
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
)

/**
 * LRCLIB (https://lrclib.net), a free, open lyrics database without accounts or keys.
 * Privacy: a request only ever carries the song's title, artist, album and duration, plus a
 * User-Agent naming NaviCtrl, as LRCLIB asks. No account, server address or anything else.
 */
class Lrclib(private val http: OkHttpClient, private val userAgent: String, private val base: HttpUrl = BASE) {

    /** Lyrics for [song], or [LyricsLookup.None] when LRCLIB has none. Network trouble throws. */
    suspend fun find(song: Song): LyricsLookup = withContext(Dispatchers.IO) {
        val title = song.title.trim()
        val artist = song.artist?.trim().orEmpty()
        if (title.isEmpty() || artist.isEmpty()) return@withContext LyricsLookup.None
        getUrl(base, song)?.let { url ->
            val body = fetch(url)
            if (body != null) {
                val rec = runCatching { ApiJson.decodeFromString(LrclibRecord.serializer(), body) }.getOrNull()
                toLyrics(rec)?.let { return@withContext LyricsLookup.Found(it) }
            }
        }
        val list = fetch(searchUrl(base, song) ?: return@withContext LyricsLookup.None)
            ?.let { runCatching { ApiJson.decodeFromString(ListSerializer(LrclibRecord.serializer()), it) }.getOrNull() }
            .orEmpty()
        toLyrics(pickBest(list, song))?.let { LyricsLookup.Found(it) } ?: LyricsLookup.None
    }

    /** The body of a successful answer, null for 404. Other errors throw, so nothing is cached for them. */
    private fun fetch(url: HttpUrl): String? {
        val req = Request.Builder().url(url).header("User-Agent", userAgent).header("Accept", "application/json").get().build()
        http.newCall(req).execute().use { resp ->
            if (resp.code == 404) return null
            if (!resp.isSuccessful) throw IOException("LRCLIB ${resp.code}")
            val body = resp.body ?: return null
            // Lyrics are small; a huge answer is not something to read into memory.
            if (body.contentLength() > MAX_BODY) throw IOException("LRCLIB answer too big")
            val source = body.source()
            source.request(MAX_BODY + 1)
            if (source.buffer.size > MAX_BODY) throw IOException("LRCLIB answer too big")
            return source.buffer.readUtf8()
        }
    }

    companion object {
        val BASE: HttpUrl = "https://lrclib.net/".toHttpUrl()
        const val MAX_BODY = 2L * 1024 * 1024
        /** LRCLIB matches /api/get within ±2 s; search results get a little more room. */
        const val SEARCH_TOLERANCE_SEC = 3.0

        fun userAgent(version: String) = "NaviCtrl/$version (https://github.com/VoidedKN0X/NaviCtrl)"

        /** /api/get with title, artist, album (when known) and duration (when 1–3600 s). Null without title or artist. */
        fun getUrl(base: HttpUrl, song: Song): HttpUrl? {
            val title = song.title.trim().takeIf { it.isNotEmpty() } ?: return null
            val artist = song.artist?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return base.newBuilder().addPathSegments("api/get")
                .addQueryParameter("track_name", title)
                .addQueryParameter("artist_name", artist)
                .apply {
                    song.album?.trim()?.takeIf { it.isNotEmpty() }?.let { addQueryParameter("album_name", it) }
                    song.duration?.takeIf { it in 1..3600 }?.let { addQueryParameter("duration", it.toString()) }
                }
                .build()
        }

        /** /api/search by title and artist (album and duration are left out to find more; duration picks the best). */
        fun searchUrl(base: HttpUrl, song: Song): HttpUrl? {
            val title = song.title.trim().takeIf { it.isNotEmpty() } ?: return null
            val artist = song.artist?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return base.newBuilder().addPathSegments("api/search")
                .addQueryParameter("track_name", title)
                .addQueryParameter("artist_name", artist)
                .build()
        }

        /**
         * The best search result for [song]: within a few seconds of its length (when both are known),
         * synced lyrics before plain ones, then the closest length.
         */
        fun pickBest(results: List<LrclibRecord>, song: Song): LrclibRecord? {
            val dur = song.duration?.takeIf { it > 0 }?.toDouble()
            return results
                .filter { r -> r.instrumental || !r.syncedLyrics.isNullOrBlank() || !r.plainLyrics.isNullOrBlank() }
                .filter { r -> dur == null || r.duration == null || abs(r.duration - dur) <= SEARCH_TOLERANCE_SEC }
                .sortedWith(
                    compareBy<LrclibRecord> { if (it.instrumental) 2 else if (!it.syncedLyrics.isNullOrBlank()) 0 else 1 }
                        .thenBy { r -> if (dur == null || r.duration == null) 0.0 else abs(r.duration - dur) },
                )
                .firstOrNull()
        }

        /** Synced lyrics when the record has them, otherwise plain ones; instrumental songs are marked as such. */
        fun toLyrics(rec: LrclibRecord?): Lyrics? {
            rec ?: return null
            LrcParser.parse(rec.syncedLyrics, LyricsSource.LRCLIB)?.takeIf { it.synced }?.let { return it }
            LrcParser.parse(rec.plainLyrics, LyricsSource.LRCLIB)?.let { return it.copy(synced = false, lines = it.lines.map { l -> l.copy(start = null) }) }
            if (rec.instrumental) return Lyrics(synced = false, lines = emptyList(), source = LyricsSource.LRCLIB, instrumental = true)
            return null
        }
    }
}
