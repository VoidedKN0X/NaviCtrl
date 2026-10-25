package com.vknxworks.navictrl.music

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import com.vknxworks.navictrl.data.settings.AppSettings

/** What the phone's equalizer offers: bands, their level range, built-in presets and bass boost. */
data class EqInfo(
    /** Centre frequency of each band, in Hz. */
    val bandHz: List<Int>,
    val minLevel: Int,
    val maxLevel: Int,
    val presets: List<String>,
    /** Band levels (millibels) of each preset, in the same order as [presets]. */
    val presetLevels: List<List<Int>>,
    val bassBoost: Boolean,
)

/**
 * The phone's own audio effects (android.media.audiofx) on the player's audio session: equalizer,
 * bass boost, and a loudness enhancer for ReplayGain values above 0 dB (the player's volume can
 * only turn songs down). Everything runs on the main thread; any effect the phone doesn't have is
 * simply left out.
 */
object AudioFx {
    private var session = 0
    private var equalizer: Equalizer? = null
    private var bass: BassBoost? = null
    private var loudness: LoudnessEnhancer? = null
    private var last: AppSettings? = null
    private var cachedInfo: EqInfo? = null
    private var infoChecked = false

    /** Puts the effects on [audioSessionId] (from the player); called again when the session changes. */
    fun attach(audioSessionId: Int, settings: AppSettings) {
        if (audioSessionId <= 0) return
        if (audioSessionId == session && equalizer != null) {
            apply(settings)
            return
        }
        release()
        session = audioSessionId
        equalizer = runCatching { Equalizer(0, audioSessionId) }.getOrNull()
        bass = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
        loudness = runCatching { LoudnessEnhancer(audioSessionId) }.getOrNull()
        last = null
        apply(settings)
    }

    /** Applies the equalizer and bass boost settings. */
    fun apply(s: AppSettings) {
        val prev = last
        last = s
        equalizer?.let { eq ->
            runCatching {
                if (prev == null || prev.eqPreset != s.eqPreset || prev.eqBands != s.eqBands || prev.eqEnabled != s.eqEnabled) {
                    val presets = eq.numberOfPresets.toInt()
                    if (s.eqPreset in 0 until presets) {
                        eq.usePreset(s.eqPreset.toShort())
                    } else {
                        val range = eq.bandLevelRange
                        for (b in 0 until eq.numberOfBands.toInt()) {
                            val level = s.eqBands.getOrNull(b) ?: 0
                            eq.setBandLevel(b.toShort(), level.coerceIn(range[0].toInt(), range[1].toInt()).toShort())
                        }
                    }
                }
                if (eq.enabled != s.eqEnabled) eq.setEnabled(s.eqEnabled)
            }
        }
        bass?.let { bb ->
            runCatching {
                val on = s.eqEnabled && s.bassBoost > 0 && bb.strengthSupported
                if (on) bb.setStrength(s.bassBoost.coerceIn(0, 1000).toShort())
                if (bb.enabled != on) bb.setEnabled(on)
            }
        }
    }

    /** Sets one band straight away while its slider is dragged (saved when it's let go). */
    fun previewBand(band: Int, level: Int) {
        val eq = equalizer ?: return
        runCatching {
            if (band in 0 until eq.numberOfBands.toInt()) eq.setBandLevel(band.toShort(), level.toShort())
        }
    }

    /** Bass boost strength straight away while its slider is dragged. */
    fun previewBass(strength: Int) {
        val bb = bass ?: return
        runCatching {
            if (bb.strengthSupported && last?.eqEnabled == true) {
                bb.setStrength(strength.coerceIn(0, 1000).toShort())
                if (bb.enabled != strength > 0) bb.setEnabled(strength > 0)
            }
        }
    }

    /** Positive ReplayGain in millibels; 0 turns the enhancer off. */
    fun setBoost(millibels: Int) {
        val le = loudness ?: return
        runCatching {
            if (millibels > 0) {
                le.setTargetGain(millibels)
                if (!le.enabled) le.setEnabled(true)
            } else if (le.enabled) {
                le.setEnabled(false)
            }
        }
    }

    /** True when the loudness enhancer is available, so ReplayGain can also raise quiet songs. */
    val canBoost: Boolean get() = loudness != null

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bass?.release() }
        runCatching { loudness?.release() }
        equalizer = null
        bass = null
        loudness = null
        session = 0
        last = null
    }

    /**
     * The phone's equalizer bands and presets, read once from a throwaway equalizer on its own
     * session so the music isn't touched. Null when the phone has no equalizer.
     */
    fun info(context: Context): EqInfo? {
        if (infoChecked) return cachedInfo
        infoChecked = true
        val am = context.getSystemService(AudioManager::class.java)
        val id = am?.generateAudioSessionId() ?: return null
        if (id <= 0) return null
        cachedInfo = runCatching {
            val eq = Equalizer(0, id)
            try {
                val bands = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val presetNames = (0 until eq.numberOfPresets.toInt()).map { eq.getPresetName(it.toShort()) }
                val presetLevels = presetNames.indices.map { p ->
                    eq.usePreset(p.toShort())
                    (0 until bands).map { b -> eq.getBandLevel(b.toShort()).toInt() }
                }
                val bassOk = runCatching {
                    val bb = BassBoost(0, id)
                    try { bb.strengthSupported } finally { bb.release() }
                }.getOrDefault(false)
                EqInfo(
                    bandHz = (0 until bands).map { eq.getCenterFreq(it.toShort()) / 1000 },
                    minLevel = range[0].toInt(),
                    maxLevel = range[1].toInt(),
                    presets = presetNames,
                    presetLevels = presetLevels,
                    bassBoost = bassOk,
                )
            } finally {
                eq.release()
            }
        }.getOrNull()?.takeIf { it.bandHz.isNotEmpty() && it.maxLevel > it.minLevel }
        return cachedInfo
    }
}
