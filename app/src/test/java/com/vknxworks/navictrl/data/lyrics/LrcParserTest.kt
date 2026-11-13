package com.vknxworks.navictrl.data.lyrics

import com.vknxworks.navictrl.data.api.LyricsSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test fun basicSyncedLines() {
        val l = LrcParser.parse("[00:01.00]One\n[00:02.50]Two\n[01:03.25]Three")!!
        assertTrue(l.synced)
        assertEquals(listOf(1000L, 2500L, 63250L), l.lines.map { it.start })
        assertEquals(listOf("One", "Two", "Three"), l.lines.map { it.value })
    }

    @Test fun fractionsOfOneTwoAndThreeDigits() {
        val l = LrcParser.parse("[00:01.5]a\n[00:01.05]b\n[00:01.005]c\n[00:02]d\n[00:03:20]e")!!
        assertEquals(listOf(1005L, 1050L, 1500L, 2000L, 3200L), l.lines.map { it.start })
        assertEquals(listOf("c", "b", "a", "d", "e"), l.lines.map { it.value })
    }

    @Test fun multipleTimestampsPerLineAreSortedByTime() {
        val l = LrcParser.parse("[00:10.00][00:30.00]Chorus\n[00:20.00]Verse")!!
        assertEquals(listOf(10_000L, 20_000L, 30_000L), l.lines.map { it.start })
        assertEquals(listOf("Chorus", "Verse", "Chorus"), l.lines.map { it.value })
    }

    @Test fun offsetTagShiftsLinesEarlierAndClampsAtZero() {
        val l = LrcParser.parse("[offset:+500]\n[00:00.20]Zero\n[00:02.00]Two")!!
        assertEquals(listOf(0L, 1500L), l.lines.map { it.start })
        val later = LrcParser.parse("[offset:-250]\n[00:01.00]x")!!
        assertEquals(1250L, later.lines.single().start)
    }

    @Test fun metadataTagsAreDroppedButSectionLabelsStayInPlainText() {
        val l = LrcParser.parse("[ar:Artist]\n[ti:Title]\n[al:Album]\n[by:someone]\n[length:03:20]\n[00:05.00]Words")!!
        assertEquals(1, l.lines.size)
        val plain = LrcParser.parse("[ar:Artist]\n[Chorus: Someone]\nla la")!!
        assertFalse(plain.synced)
        assertEquals(listOf("[Chorus: Someone]", "la la"), plain.lines.map { it.value })
    }

    @Test fun wordTimingsAndExtraSpacesAreRemoved() {
        val l = LrcParser.parse("[00:01.00]<00:01.00>Hello  <00:01.50>world\r\n")!!
        assertEquals("Hello world", l.lines.single().value)
    }

    @Test fun blankTimedLinesAreKeptAsBreaks() {
        val l = LrcParser.parse("[00:01.00]A\n[00:02.00]\n[00:03.00]B")!!
        assertEquals(listOf("A", "", "B"), l.lines.map { it.value })
    }

    @Test fun plainTextIsTrimmedAtBothEnds() {
        val l = LrcParser.parse("\uFEFF\n\nLine one\n\nLine two\n\n", LyricsSource.LRCLIB)!!
        assertFalse(l.synced)
        assertEquals(LyricsSource.LRCLIB, l.source)
        assertEquals(listOf("Line one", "", "Line two"), l.lines.map { it.value })
        assertTrue(l.lines.all { it.start == null })
    }

    @Test fun emptyOrWordlessInputGivesNull() {
        assertNull(LrcParser.parse(null))
        assertNull(LrcParser.parse("   \n "))
        assertNull(LrcParser.parse("[ar:Someone]\n[ti:Song]"))
        assertNull(LrcParser.parse("[00:01.00]\n[00:02.00]"))
    }

    @Test fun brokenTimestampsAreSkipped() {
        val l = LrcParser.parse("[00:75.00]bad seconds\n[00:01.00]good\n[xx:yy]junk")!!
        assertTrue(l.synced)
        assertEquals(listOf("good"), l.lines.map { it.value })
    }

    @Test fun hugeInputIsCapped() {
        val big = buildString { repeat(200_000) { append("[00:01.00]").append("x".repeat(20)).append('\n') } }
        val l = LrcParser.parse(big)!!
        assertTrue(l.lines.size <= LrcParser.MAX_LINES)
        val longLine = LrcParser.parse("[00:01.00]" + "y".repeat(50_000))!!
        assertEquals(LrcParser.MAX_LINE_CHARS, longLine.lines.single().value.length)
    }

    @Test fun minutesAboveSixty() {
        val l = LrcParser.parse("[75:00.00]late")!!
        assertEquals(75L * 60 * 1000, l.lines.single().start)
    }
}
