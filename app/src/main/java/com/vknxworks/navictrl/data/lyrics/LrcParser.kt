package com.vknxworks.navictrl.data.lyrics

import com.vknxworks.navictrl.data.api.LyricLine
import com.vknxworks.navictrl.data.api.Lyrics
import com.vknxworks.navictrl.data.api.LyricsSource

/**
 * Reads LRC lyrics ("[01:02.34]words") and plain text. Lyrics come from the server or from LRCLIB,
 * so the input is untrusted: it's size-capped, and anything that doesn't parse is skipped rather
 * than failing the whole song.
 *
 * Supported: several timestamps on one line ("[00:12.00][01:30.50]chorus"), 1 to 3 digit fractions
 * ("[00:12.5]", "[00:12.05]", "[00:12.005]") and "[mm:ss:cc]", minutes above 59, "[offset:±ms]",
 * metadata tags ("[ar:…]", "[ti:…]" …, dropped), and word timings ("<00:12.30>") which are removed.
 */
object LrcParser {
    /** Bigger input is cut off here (about 10 times the longest real lyrics). */
    const val MAX_CHARS = 256 * 1024
    const val MAX_LINES = 5_000
    const val MAX_LINE_CHARS = 1_000
    private const val MAX_STAMPS_PER_LINE = 64
    /** Ten hours: anything later is a broken timestamp. */
    private const val MAX_MS = 10L * 60 * 60 * 1000

    // [mm:ss], [mm:ss.f], [mm:ss.ff], [mm:ss.fff] and [mm:ss:ff].
    private val STAMP = Regex("""\[(\d{1,4}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val LEADING_STAMPS = Regex("""^(?:\s*\[\d{1,4}:\d{1,2}(?:[.:]\d{1,3})?])+""")
    // Only the standard LRC metadata names, so a plain-text line like "[Chorus: Artist]" stays.
    private val TAG = Regex("""^\s*\[(ar|ti|al|au|by|re|ve|tool|length|offset|la|lang|id|#):(.*)]\s*$""", RegexOption.IGNORE_CASE)
    private val WORD_STAMP = Regex("""<\d{1,4}:\d{1,2}(?:[.:]\d{1,3})?>""")
    private val SPACES = Regex("""\s+""")
    private const val BOM = "\uFEFF"

    /**
     * Parses [raw]. Returns synced lyrics when at least one line has a timestamp, plain lyrics
     * otherwise, or null when there are no words at all.
     */
    fun parse(raw: String?, source: LyricsSource = LyricsSource.SERVER): Lyrics? {
        if (raw.isNullOrBlank()) return null
        val text = raw.take(MAX_CHARS).removePrefix(BOM)
        var offsetMs = 0L
        val timed = ArrayList<Pair<Long, String>>()
        val plain = ArrayList<String>()
        for (rawLine in text.lineSequence()) {
            if (timed.size >= MAX_LINES && plain.size >= MAX_LINES) break
            val line = rawLine.trimEnd('\r')
            val stamps = LEADING_STAMPS.find(line)
            if (stamps != null) {
                val words = clean(line.substring(stamps.range.last + 1))
                STAMP.findAll(stamps.value).take(MAX_STAMPS_PER_LINE).forEach { m ->
                    val ms = stampMs(m) ?: return@forEach
                    if (timed.size < MAX_LINES) timed += ms to words
                }
                continue
            }
            val tag = TAG.matchEntire(line)
            if (tag != null) {
                if (tag.groupValues[1].equals("offset", ignoreCase = true)) {
                    offsetMs = tag.groupValues[2].trim().removePrefix("+").toLongOrNull()?.coerceIn(-MAX_MS, MAX_MS) ?: 0L
                }
                continue
            }
            if (plain.size < MAX_LINES) plain += clean(line)
        }
        if (timed.isNotEmpty()) {
            // Stable sort: lines with the same time keep their order in the file.
            val lines = timed
                .map { (ms, words) -> LyricLine((ms - offsetMs).coerceIn(0, MAX_MS), words) }
                .sortedBy { it.start }
            if (lines.all { it.value.isBlank() }) return null
            return Lyrics(synced = true, lines = lines, source = source)
        }
        val lines = plain.dropWhile { it.isBlank() }.dropLastWhile { it.isBlank() }
        if (lines.isEmpty()) return null
        return Lyrics(synced = false, lines = lines.map { LyricLine(null, it) }, source = source)
    }

    /** A timestamp in ms, or null when it's out of range. */
    internal fun stampMs(m: MatchResult): Long? {
        val min = m.groupValues[1].toLongOrNull() ?: return null
        val sec = m.groupValues[2].toLongOrNull() ?: return null
        if (sec > 59) return null
        val frac = m.groupValues[3]
        val fracMs = when (frac.length) {
            0 -> 0L
            1 -> frac.toLong() * 100
            2 -> frac.toLong() * 10
            else -> frac.toLong()
        }
        val ms = (min * 60 + sec) * 1000 + fracMs
        return ms.takeIf { it <= MAX_MS }
    }

    private fun clean(s: String): String =
        s.replace(WORD_STAMP, "").replace(SPACES, " ").trim().take(MAX_LINE_CHARS)
}
