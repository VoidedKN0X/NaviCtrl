package com.vknxworks.navictrl.data.lyrics

import com.vknxworks.navictrl.data.api.LyricLine
import com.vknxworks.navictrl.data.api.Lyrics
import com.vknxworks.navictrl.data.api.LyricsSource
import com.vknxworks.navictrl.data.api.StructuredLyrics
import kotlinx.serialization.Serializable
import kotlin.coroutines.cancellation.CancellationException

/** What one lyrics source answered. */
sealed interface LyricsLookup {
    data class Found(val lyrics: Lyrics) : LyricsLookup
    /** The source answered: it has no lyrics for this song. */
    data object None : LyricsLookup
}

/** A cached answer for one song: lyrics, or "none found" (which expires). */
@Serializable
data class CachedLyrics(
    val lyrics: Lyrics? = null,
    /** When this was looked up, ms since 1970. */
    val at: Long = 0,
    /** LRCLIB was asked too (so a "none" can be retried once online lookups are turned on). */
    val askedOnline: Boolean = false,
)

/** Result of [LyricsResolver.resolve]: the lyrics (or null) and what to cache, if anything. */
data class Resolved(val lyrics: Lyrics?, val cache: CachedLyrics?)

/**
 * Picks where a song's lyrics come from: the server first, LRCLIB only when the server has none
 * (and online lookups are on). Kept free of Android so it can be unit-tested with fake sources.
 */
object LyricsResolver {
    /** "No lyrics found" is trusted for a week; then the song is looked up again. */
    const val NONE_TTL_MS = 7L * 24 * 60 * 60 * 1000

    /**
     * True when [cached] can be shown without asking again: lyrics are kept until "Search again",
     * a "none" until it expires or online lookups were turned on since.
     */
    fun cacheValid(cached: CachedLyrics?, now: Long, onlineEnabled: Boolean): Boolean {
        cached ?: return false
        if (cached.lyrics != null) return true
        if (onlineEnabled && !cached.askedOnline) return false
        return now - cached.at in 0 until NONE_TTL_MS
    }

    /**
     * Asks [server], then (when it has none and [online] isn't null) [online]. A source that fails
     * (network, server error) counts as "unknown": if nothing is found, nothing is cached, so the
     * song is looked up again next time. When every source asked fails, the first error is thrown
     * so the screen can say why.
     */
    suspend fun resolve(
        now: Long,
        server: suspend () -> LyricsLookup,
        online: (suspend () -> LyricsLookup)?,
    ): Resolved {
        var failure: Throwable? = null
        val fromServer = try {
            server()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            failure = e
            null
        }
        if (fromServer is LyricsLookup.Found) return found(fromServer.lyrics, now, askedOnline = false)
        if (online == null) {
            if (failure != null) throw failure
            return Resolved(null, CachedLyrics(null, now, askedOnline = false))
        }
        val fromOnline = try {
            online()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (failure == null) failure = e
            null
        }
        if (fromOnline is LyricsLookup.Found) return found(fromOnline.lyrics, now, askedOnline = true)
        // Something failed: don't remember "none", it may just be the network.
        if (failure != null) {
            if (fromServer == null && fromOnline == null) throw failure
            return Resolved(null, null)
        }
        return Resolved(null, CachedLyrics(null, now, askedOnline = true))
    }

    private fun found(l: Lyrics, now: Long, askedOnline: Boolean) = Resolved(l, CachedLyrics(l, now, askedOnline))

    /**
     * The server's structured lyrics (OpenSubsonic getLyricsBySongId): synced ones first, with the
     * offset applied (positive offset = lines show earlier), lines sorted by time.
     */
    fun fromStructured(list: List<StructuredLyrics>): Lyrics? {
        val best = list.firstOrNull { it.synced && it.line.any { l -> l.value.isNotBlank() } }
            ?: list.firstOrNull { it.line.any { l -> l.value.isNotBlank() } }
            ?: return null
        val lines = best.line.take(LrcParser.MAX_LINES).map { l ->
            LyricLine(
                start = if (best.synced) ((l.start ?: 0L) - best.offset).coerceAtLeast(0) else null,
                value = l.value.take(LrcParser.MAX_LINE_CHARS),
            )
        }
        return Lyrics(
            synced = best.synced,
            lines = if (best.synced) lines.sortedBy { it.start } else lines,
            source = LyricsSource.SERVER,
        )
    }
}
