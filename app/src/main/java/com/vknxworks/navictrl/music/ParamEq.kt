package com.vknxworks.navictrl.music

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// The parametric equalizer's own maths: no Android classes here, so it can be unit-tested on its own.

/** The shape of one band: a bell around a frequency, or a shelf that lifts or lowers everything below or above it. */
enum class PeqType(val code: String) { PEAK("P"), LOW_SHELF("L"), HIGH_SHELF("H") }

/**
 * One band of the parametric equalizer: where ([hz]), how much ([gainTenths], tenths of a dB) and how
 * wide ([qHundredths], hundredths of Q: higher is narrower).
 */
data class PeqBand(
    val type: PeqType = PeqType.PEAK,
    val hz: Int = 1000,
    val gainTenths: Int = 0,
    val qHundredths: Int = 100,
) {
    val gainDb: Double get() = gainTenths / 10.0
    val q: Double get() = qHundredths / 100.0

    /** The same band with every value inside what the equalizer accepts. */
    fun clean(): PeqBand = copy(
        hz = hz.coerceIn(MIN_HZ, MAX_HZ),
        gainTenths = gainTenths.coerceIn(-MAX_GAIN_TENTHS, MAX_GAIN_TENTHS),
        qHundredths = qHundredths.coerceIn(MIN_Q_HUNDREDTHS, MAX_Q_HUNDREDTHS),
    )

    companion object {
        const val MIN_HZ = 20
        const val MAX_HZ = 20_000
        const val MAX_GAIN_TENTHS = 150
        const val MIN_Q_HUNDREDTHS = 20
        const val MAX_Q_HUNDREDTHS = 1000
        const val MAX_BANDS = 10

        /** Bands as one line of text for the settings file, e.g. "P,1000,30,100;L,80,45,70". */
        fun encode(bands: List<PeqBand>): String =
            bands.take(MAX_BANDS).joinToString(";") { "${it.type.code},${it.hz},${it.gainTenths},${it.qHundredths}" }

        /** Reads [encode]'s text back. Anything that isn't a band is left out; values are brought into range. */
        fun decode(text: String?): List<PeqBand> = text.orEmpty().split(';').mapNotNull { part ->
            val f = part.trim().split(',')
            if (f.size != 4) return@mapNotNull null
            val type = PeqType.entries.firstOrNull { it.code == f[0] } ?: return@mapNotNull null
            val hz = f[1].toIntOrNull() ?: return@mapNotNull null
            val gain = f[2].toIntOrNull() ?: return@mapNotNull null
            val q = f[3].toIntOrNull() ?: return@mapNotNull null
            PeqBand(type, hz, gain, q).clean()
        }.take(MAX_BANDS)
    }
}

/** What the parametric equalizer does right now. A new object for every change, so the player sees it at once. */
class PeqConfig(val enabled: Boolean, val preampTenths: Int, val bands: List<PeqBand>) {
    /** Bands that change the sound (a band at 0 dB does nothing). */
    val active: List<PeqBand> get() = if (enabled) bands.filter { it.gainTenths != 0 } else emptyList()
    val preampLinear: Double get() = if (enabled) 10.0.pow(preampTenths.coerceIn(MIN_PREAMP_TENTHS, 0) / 200.0) else 1.0

    companion object {
        const val MIN_PREAMP_TENTHS = -150
        val OFF = PeqConfig(false, 0, emptyList())
    }
}

/** One second-order filter section, normalised so the first feedback coefficient is 1. */
class Biquad(val b0: Double, val b1: Double, val b2: Double, val a1: Double, val a2: Double) {
    /** How much this section raises or lowers a tone of [hz], in dB. */
    fun gainDbAt(hz: Double, sampleRate: Int): Double {
        val w = 2.0 * PI * hz / sampleRate
        val c1 = cos(w)
        val s1 = sin(w)
        val c2 = cos(2 * w)
        val s2 = sin(2 * w)
        val numRe = b0 + b1 * c1 + b2 * c2
        val numIm = -(b1 * s1 + b2 * s2)
        val denRe = 1.0 + a1 * c1 + a2 * c2
        val denIm = -(a1 * s1 + a2 * s2)
        val num = numRe * numRe + numIm * numIm
        val den = denRe * denRe + denIm * denIm
        if (num <= 0.0 || den <= 0.0) return 0.0
        return 10.0 * log10(num / den)
    }
}

object ParamEqMath {
    /**
     * The filter for [band] at [sampleRate], after Robert Bristow-Johnson's Audio EQ Cookbook
     * (peaking EQ, low shelf, high shelf). The frequency stays below 45% of the sample rate.
     */
    fun biquad(band: PeqBand, sampleRate: Int): Biquad {
        val b = band.clean()
        val a = 10.0.pow(b.gainDb / 40.0)
        val f = b.hz.toDouble().coerceAtMost(sampleRate * 0.45)
        val w0 = 2.0 * PI * f / sampleRate
        val cosw = cos(w0)
        val alpha = sin(w0) / (2.0 * b.q)
        val b0: Double
        val b1: Double
        val b2: Double
        val a0: Double
        val a1: Double
        val a2: Double
        when (b.type) {
            PeqType.PEAK -> {
                b0 = 1 + alpha * a
                b1 = -2 * cosw
                b2 = 1 - alpha * a
                a0 = 1 + alpha / a
                a1 = -2 * cosw
                a2 = 1 - alpha / a
            }
            PeqType.LOW_SHELF -> {
                val r = 2 * sqrt(a) * alpha
                b0 = a * ((a + 1) - (a - 1) * cosw + r)
                b1 = 2 * a * ((a - 1) - (a + 1) * cosw)
                b2 = a * ((a + 1) - (a - 1) * cosw - r)
                a0 = (a + 1) + (a - 1) * cosw + r
                a1 = -2 * ((a - 1) + (a + 1) * cosw)
                a2 = (a + 1) + (a - 1) * cosw - r
            }
            PeqType.HIGH_SHELF -> {
                val r = 2 * sqrt(a) * alpha
                b0 = a * ((a + 1) + (a - 1) * cosw + r)
                b1 = -2 * a * ((a - 1) + (a + 1) * cosw)
                b2 = a * ((a + 1) + (a - 1) * cosw - r)
                a0 = (a + 1) - (a - 1) * cosw + r
                a1 = 2 * ((a - 1) - (a + 1) * cosw)
                a2 = (a + 1) - (a - 1) * cosw - r
            }
        }
        return Biquad(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    /** The whole equalizer's gain at [hz] in dB: every band plus the preamp. For the curve on screen. */
    fun responseDb(bands: List<PeqBand>, preampTenths: Int, hz: Double, sampleRate: Int = 48_000): Double =
        preampTenths / 10.0 + bands.filter { it.gainTenths != 0 }.sumOf { biquad(it, sampleRate).gainDbAt(hz, sampleRate) }

    /** The highest point of the curve in dB (20 Hz to 20 kHz): above 0 means loud passages can distort. */
    fun peakDb(bands: List<PeqBand>, preampTenths: Int, sampleRate: Int = 48_000): Double {
        var max = -1000.0
        for (i in 0..120) {
            val hz = 20.0 * 1000.0.pow(i / 120.0)
            val v = responseDb(bands, preampTenths, hz, sampleRate)
            if (v > max) max = v
        }
        return max
    }
}
