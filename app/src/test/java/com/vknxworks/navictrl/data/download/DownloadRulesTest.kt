package com.vknxworks.navictrl.data.download

import com.vknxworks.navictrl.data.api.Song
import com.vknxworks.navictrl.data.api.SongSort
import com.vknxworks.navictrl.ui.songs.sortDownloaded
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadRulesTest {

    @Test fun resumeOnlyWhenTheServerContinuesAtThePart() {
        assertTrue(DownloadRepository.resumeAccepted(206, "bytes 1000-4999/5000", 1000))
        assertTrue(DownloadRepository.resumeAccepted(206, "bytes 1000-4999/*", 1000))
        assertFalse(DownloadRepository.resumeAccepted(200, null, 1000)) // server ignored Range (e.g. transcoding)
        assertFalse(DownloadRepository.resumeAccepted(206, "bytes 0-4999/5000", 1000))
        assertFalse(DownloadRepository.resumeAccepted(206, null, 1000))
        assertFalse(DownloadRepository.resumeAccepted(206, "garbage", 1000))
    }

    @Test fun partTagNamesTheQuality() {
        assertEquals("orig", DownloadRepository.partTag(0, "mp3"))
        assertEquals("192mp3", DownloadRepository.partTag(192, "mp3"))
        assertEquals("128opus", DownloadRepository.partTag(128, "opus"))
        assertEquals("96audio", DownloadRepository.partTag(96, "../x"))
    }

    @Test fun expectedSize() {
        val song = Song(id = "1", size = 9_000_000, duration = 200)
        assertEquals(9_000_000L, DownloadRepository.expectedBytes(song, 0))
        assertEquals(320L * 1000 / 8 * 200, DownloadRepository.expectedBytes(song, 320))
        assertEquals(0L, DownloadRepository.expectedBytes(Song(id = "2"), 128))
    }

    @Test fun limit() {
        val gb = 1024L * 1024 * 1024
        assertFalse(DownloadRepository.exceedsLimit(5 * gb, 10 * gb, 0)) // no limit
        assertFalse(DownloadRepository.exceedsLimit(gb, gb, 2 * gb))
        assertTrue(DownloadRepository.exceedsLimit(gb, gb + 1, 2 * gb))
    }

    @Test fun safeExtensionAndPaths() {
        assertEquals("flac", DownloadRepository.safeExtension("FLAC"))
        assertEquals("audio", DownloadRepository.safeExtension("../../etc"))
        assertEquals("audio", DownloadRepository.safeExtension(null))
    }

    @Test fun downloadedSongsSortLikeTheServer() {
        val a = Song(id = "a", title = "beta", artist = "Zed", album = "One", track = 2, created = "2024-01-02T00:00:00Z")
        val b = Song(id = "b", title = "Alpha", artist = "abba", album = "Two", track = 1, created = "2024-03-01T00:00:00Z")
        val c = Song(id = "c", title = "gamma", artist = "Zed", album = "One", track = 1, created = "2023-12-31T00:00:00Z")
        val all = listOf(a, b, c)
        assertEquals(listOf("b", "a", "c"), sortDownloaded(all, SongSort.TITLE, true).map { it.id })
        assertEquals(listOf("c", "a", "b"), sortDownloaded(all, SongSort.TITLE, false).map { it.id })
        assertEquals(listOf("b", "c", "a"), sortDownloaded(all, SongSort.ARTIST, true).map { it.id })
        assertEquals(listOf("c", "a", "b"), sortDownloaded(all, SongSort.ALBUM, true).map { it.id })
        assertEquals(listOf("b", "a", "c"), sortDownloaded(all, SongSort.DATE_ADDED, false).map { it.id })
    }
}
