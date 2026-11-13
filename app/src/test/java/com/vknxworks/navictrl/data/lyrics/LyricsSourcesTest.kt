package com.vknxworks.navictrl.data.lyrics

import com.vknxworks.navictrl.data.api.LyricLine
import com.vknxworks.navictrl.data.api.Lyrics
import com.vknxworks.navictrl.data.api.LyricsSource
import com.vknxworks.navictrl.data.api.Song
import com.vknxworks.navictrl.data.api.StructuredLyrics
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class LyricsSourcesTest {
    private val serverLyrics = Lyrics(true, listOf(LyricLine(0, "server")), LyricsSource.SERVER)
    private val onlineLyrics = Lyrics(true, listOf(LyricLine(0, "online")), LyricsSource.LRCLIB)
    private val song = Song(id = "s1", title = "I Want to Live", artist = "Borislav Slavov", album = "Baldur's Gate 3 (Original Game Soundtrack)", duration = 233)

    // ---- Source selection ----

    @Test fun serverWinsAndLrclibIsNotAsked() = runBlocking {
        var asked = false
        val r = LyricsResolver.resolve(1, server = { LyricsLookup.Found(serverLyrics) }, online = { asked = true; LyricsLookup.Found(onlineLyrics) })
        assertSame(serverLyrics, r.lyrics)
        assertFalse(asked)
        assertSame(serverLyrics, r.cache!!.lyrics)
    }

    @Test fun lrclibOnlyWhenServerHasNone() = runBlocking {
        val r = LyricsResolver.resolve(1, server = { LyricsLookup.None }, online = { LyricsLookup.Found(onlineLyrics) })
        assertSame(onlineLyrics, r.lyrics)
        assertTrue(r.cache!!.askedOnline)
    }

    @Test fun serverErrorStillTriesLrclib() = runBlocking {
        val r = LyricsResolver.resolve(1, server = { throw IOException("offline") }, online = { LyricsLookup.Found(onlineLyrics) })
        assertSame(onlineLyrics, r.lyrics)
    }

    @Test fun noneEverywhereIsCachedAsNone() = runBlocking {
        val r = LyricsResolver.resolve(5, server = { LyricsLookup.None }, online = { LyricsLookup.None })
        assertNull(r.lyrics)
        assertNull(r.cache!!.lyrics)
        assertEquals(5L, r.cache!!.at)
        assertTrue(r.cache!!.askedOnline)
    }

    @Test fun onlineOffNeverCallsLrclib() = runBlocking {
        val r = LyricsResolver.resolve(1, server = { LyricsLookup.None }, online = null)
        assertNull(r.lyrics)
        assertFalse(r.cache!!.askedOnline)
    }

    @Test fun aFailureIsNeverCachedAsNone() = runBlocking {
        val r = LyricsResolver.resolve(1, server = { LyricsLookup.None }, online = { throw IOException("429") })
        assertNull(r.lyrics)
        assertNull(r.cache)
        val r2 = LyricsResolver.resolve(1, server = { throw IOException("down") }, online = { LyricsLookup.None })
        assertNull(r2.cache)
    }

    @Test fun everySourceFailingThrows() {
        runBlocking {
            try {
                LyricsResolver.resolve(1, server = { throw IOException("server down") }, online = { throw IOException("lrclib down") })
                fail()
            } catch (e: IOException) {
                assertEquals("server down", e.message)
            }
            try {
                LyricsResolver.resolve(1, server = { throw IOException("server down") }, online = null)
                fail()
            } catch (e: IOException) {
                assertEquals("server down", e.message)
            }
        }
    }

    // ---- Cache rules ----

    @Test fun cachedLyricsStayValid() {
        assertTrue(LyricsResolver.cacheValid(CachedLyrics(serverLyrics, at = 0), now = Long.MAX_VALUE / 2, onlineEnabled = true))
    }

    @Test fun noneExpiresAfterAWeek() {
        val none = CachedLyrics(null, at = 1_000, askedOnline = true)
        assertTrue(LyricsResolver.cacheValid(none, 1_000 + LyricsResolver.NONE_TTL_MS - 1, true))
        assertFalse(LyricsResolver.cacheValid(none, 1_000 + LyricsResolver.NONE_TTL_MS, true))
        assertFalse(LyricsResolver.cacheValid(none, 500, true)) // clock went back
        assertFalse(LyricsResolver.cacheValid(null, 0, true))
    }

    @Test fun serverOnlyNoneIsRetriedOnceOnlineLookupsAreOn() {
        val none = CachedLyrics(null, at = 1_000, askedOnline = false)
        assertTrue(LyricsResolver.cacheValid(none, 2_000, onlineEnabled = false))
        assertFalse(LyricsResolver.cacheValid(none, 2_000, onlineEnabled = true))
    }

    // ---- Server structured lyrics ----

    @Test fun structuredPrefersSyncedAndAppliesOffset() {
        val plain = StructuredLyrics(synced = false, line = listOf(LyricLine(null, "plain")))
        val synced = StructuredLyrics(synced = true, offset = 200, line = listOf(LyricLine(1_000, "b"), LyricLine(100, "a")))
        val l = LyricsResolver.fromStructured(listOf(plain, synced))!!
        assertTrue(l.synced)
        assertEquals(listOf(0L, 800L), l.lines.map { it.start })
        assertEquals(listOf("a", "b"), l.lines.map { it.value })
    }

    @Test fun structuredEmptyIsNone() {
        assertNull(LyricsResolver.fromStructured(emptyList()))
        assertNull(LyricsResolver.fromStructured(listOf(StructuredLyrics(synced = true, line = listOf(LyricLine(0, " "))))))
    }

    // ---- LRCLIB requests and answers ----

    @Test fun getUrlCarriesOnlyTitleArtistAlbumAndDuration() {
        val url = Lrclib.getUrl(Lrclib.BASE, song)!!
        assertEquals("https://lrclib.net/api/get", url.newBuilder().query(null).build().toString())
        assertEquals(setOf("track_name", "artist_name", "album_name", "duration"), url.queryParameterNames)
        assertEquals("I Want to Live", url.queryParameter("track_name"))
        assertEquals("Borislav Slavov", url.queryParameter("artist_name"))
        assertEquals("Baldur's Gate 3 (Original Game Soundtrack)", url.queryParameter("album_name"))
        assertEquals("233", url.queryParameter("duration"))
    }

    @Test fun getUrlLeavesOutUnknownAlbumAndOutOfRangeDuration() {
        val url = Lrclib.getUrl(Lrclib.BASE, song.copy(album = " ", duration = 0))!!
        assertEquals(setOf("track_name", "artist_name"), url.queryParameterNames)
        assertNull(Lrclib.getUrl(Lrclib.BASE, song.copy(artist = null)))
        assertNull(Lrclib.getUrl(Lrclib.BASE, song.copy(title = "")))
        assertEquals(setOf("track_name", "artist_name"), Lrclib.getUrl(Lrclib.BASE, song.copy(duration = 4000))!!.queryParameterNames - "album_name")
    }

    @Test fun searchUrl() {
        val url = Lrclib.searchUrl("https://lrclib.net/".toHttpUrl(), song)!!
        assertEquals("/api/search", url.encodedPath)
        assertEquals(setOf("track_name", "artist_name"), url.queryParameterNames)
    }

    @Test fun userAgentNamesTheProject() {
        assertEquals("NaviCtrl/0.9.0 (https://github.com/VoidedKN0X/NaviCtrl)", Lrclib.userAgent("0.9.0"))
    }

    @Test fun recordPrefersSyncedThenPlain() {
        val both = LrclibRecord(syncedLyrics = "[00:01.00]synced", plainLyrics = "plain")
        assertEquals("synced", Lrclib.toLyrics(both)!!.lines.single().value)
        assertEquals(LyricsSource.LRCLIB, Lrclib.toLyrics(both)!!.source)
        val plainOnly = LrclibRecord(syncedLyrics = null, plainLyrics = "plain one\nplain two")
        val l = Lrclib.toLyrics(plainOnly)!!
        assertFalse(l.synced)
        assertEquals(2, l.lines.size)
        assertTrue(Lrclib.toLyrics(LrclibRecord(instrumental = true))!!.instrumental)
        assertNull(Lrclib.toLyrics(LrclibRecord()))
        assertNull(Lrclib.toLyrics(null))
    }

    @Test fun recordedGetAnswerParses() {
        // Shape of LRCLIB's documented /api/get answer.
        val body = """{"id":3396226,"name":"I Want to Live","trackName":"I Want to Live","artistName":"Borislav Slavov",
            "albumName":"Baldur's Gate 3 (Original Game Soundtrack)","duration":233,"instrumental":false,"hasWordSync":false,
            "plainLyrics":"Line one\nLine two","syncedLyrics":"[00:17.12] Line one\n[00:20.50] Line two","lyricsfile":"x: y"}"""
        val rec = com.vknxworks.navictrl.data.api.ApiJson.decodeFromString(LrclibRecord.serializer(), body)
        val l = Lrclib.toLyrics(rec)!!
        assertTrue(l.synced)
        assertEquals(listOf(17_120L, 20_500L), l.lines.map { it.start })
        assertEquals("Line one", l.lines[0].value)
    }

    @Test fun searchPicksSyncedWithinDurationTolerance() {
        val results = listOf(
            LrclibRecord(id = 1, duration = 300.0, syncedLyrics = "[00:01.00]too long"),
            LrclibRecord(id = 2, duration = 234.0, plainLyrics = "plain"),
            LrclibRecord(id = 3, duration = 231.0, syncedLyrics = "[00:01.00]synced"),
            LrclibRecord(id = 4, duration = 233.0),
        )
        assertEquals(3L, Lrclib.pickBest(results, song)!!.id)
        assertEquals(2L, Lrclib.pickBest(results.filter { it.id != 3L }, song)!!.id)
        assertNull(Lrclib.pickBest(listOf(results[0], results[3]), song))
        // Without a known length, any result can match.
        assertEquals(1L, Lrclib.pickBest(results, song.copy(duration = null))!!.id)
    }
}
