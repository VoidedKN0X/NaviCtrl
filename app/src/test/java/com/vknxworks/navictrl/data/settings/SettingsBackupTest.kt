package com.vknxworks.navictrl.data.settings

import com.vknxworks.navictrl.data.api.SongSort
import com.vknxworks.navictrl.music.PeqBand
import com.vknxworks.navictrl.music.PeqType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SettingsBackupTest {

    private val mine = AppSettings(
        wifiBitrate = 192,
        mobileBitrate = 96,
        format = TranscodeFormat.OPUS,
        crossfadeSec = 4,
        songSort = SongSort.DATE_ADDED,
        theme = ThemeChoice.BLACK,
        peqEnabled = true,
        peqPreamp = -30,
        peqBands = listOf(PeqBand(PeqType.LOW_SHELF, 80, 40, 70)),
        appLock = true,
        lockTimeout = LockTimeout.MINUTES_5,
        hideScreenshots = true,
        hideRecents = true,
    )

    @Test fun whatIsExportedComesBack() {
        val file = SettingsBackups.export(mine, "1.0.0", "2026-11-21T10:00:00+01:00")
        val restored = SettingsBackups.apply(AppSettings(), SettingsBackups.parse(file).settings)
        // Everything except the privacy settings, which are not in the file.
        assertEquals(mine.copy(appLock = false, lockTimeout = LockTimeout.IMMEDIATELY, hideScreenshots = false, hideRecents = false), restored)
    }

    @Test fun theLockAndPrivacySettingsAreNeverInTheFileOrChanged() {
        val file = SettingsBackups.export(mine, "1.0.0", "now")
        assertFalse(file.contains("appLock"))
        assertFalse(file.contains("lockTimeout"))
        assertFalse(file.contains("hideScreenshots"))
        assertFalse(file.contains("hideRecents"))
        // A file from a phone without a lock doesn't turn the lock or the hiding off here.
        val other = SettingsBackups.parse(SettingsBackups.export(AppSettings(), "1.0.0", "now")).settings
        val after = SettingsBackups.apply(mine, other)
        assertTrue(after.appLock)
        assertEquals(LockTimeout.MINUTES_5, after.lockTimeout)
        assertTrue(after.hideScreenshots)
        assertTrue(after.hideRecents)
    }

    @Test fun onlineLyricsAndScrobblingCanBeSwitchedOffButNotOn() {
        val off = SettingsBackups.parse(SettingsBackups.export(AppSettings(lyricsOnline = false, scrobble = false), "1.0.0", "now")).settings
        val on = SettingsBackups.parse(SettingsBackups.export(AppSettings(lyricsOnline = true, scrobble = true), "1.0.0", "now")).settings

        val turnedOff = SettingsBackups.apply(AppSettings(lyricsOnline = true, scrobble = true), off)
        assertFalse(turnedOff.lyricsOnline)
        assertFalse(turnedOff.scrobble)

        val stayOff = SettingsBackups.apply(AppSettings(lyricsOnline = false, scrobble = false), on)
        assertFalse(stayOff.lyricsOnline)
        assertFalse(stayOff.scrobble)

        val stayOn = SettingsBackups.apply(AppSettings(lyricsOnline = true, scrobble = true), on)
        assertTrue(stayOn.lyricsOnline)
        assertTrue(stayOn.scrobble)
    }

    @Test fun valuesTheSettingsDoNotOfferAreLeftOut() {
        val odd = BackupSettings(wifiBitrate = 12345, format = "FLAC", crossfadeSec = 99, replayGainPreamp = 40, theme = "PINK", peqPreamp = 50)
        assertEquals(mine, SettingsBackups.apply(mine, odd))
    }

    @Test fun otherFilesAreRefused() {
        for (text in listOf("", "hello", "[]", "{\"format\":\"something-else\",\"version\":1}")) {
            try {
                SettingsBackups.parse(text)
                fail("accepted: $text")
            } catch (e: BackupFormatException) {
                assertFalse(e.newer)
            }
        }
        try {
            SettingsBackups.parse("{\"format\":\"navictrl-settings\",\"version\":2}")
            fail("accepted a newer file")
        } catch (e: BackupFormatException) {
            assertTrue(e.newer)
        }
    }
}
