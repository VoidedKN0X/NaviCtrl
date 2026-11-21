package com.vknxworks.navictrl.music

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import com.vknxworks.navictrl.data.settings.AppSettings
import java.nio.ByteBuffer

/**
 * The parametric equalizer's current settings, shared between the settings screen (main thread) and
 * the player's audio thread. Changing [config] is heard within the next audio buffer.
 */
object ParametricEq {
    @Volatile var config: PeqConfig = PeqConfig.OFF
        private set

    private var saved: PeqConfig = PeqConfig.OFF

    /** The saved settings. */
    fun apply(s: AppSettings) {
        val now = saved
        if (now.enabled == s.peqEnabled && now.preampTenths == s.peqPreamp && now.bands == s.peqBands) {
            // Nothing changed: also ends a preview that wasn't saved.
            config = now
            return
        }
        saved = PeqConfig(s.peqEnabled, s.peqPreamp, s.peqBands)
        config = saved
    }

    /** Heard straight away while a slider is dragged; saved when it's let go. */
    fun preview(enabled: Boolean, preampTenths: Int, bands: List<PeqBand>) {
        config = PeqConfig(enabled, preampTenths, bands)
    }
}

/**
 * Runs the player's sound through the parametric equalizer: a chain of second-order filters, one per
 * band, on every channel. Works on the 16-bit (or float) PCM the player hands to Android, inside the
 * app, so it doesn't depend on the phone's own equalizer. With the equalizer off the sound passes
 * through untouched.
 */
class ParamEqProcessor : BaseAudioProcessor() {
    private var applied: PeqConfig? = null
    private var sampleRate = 0
    private var channels = 0
    private var bands = 0
    private var preamp = 1.0

    /** Five numbers per band: b0, b1, b2, a1, a2. */
    private var coef = DoubleArray(0)

    /** Two numbers of memory per band and channel. */
    private var state = DoubleArray(0)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
        // Other formats (compressed audio handed straight to Android) pass by untouched.
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT || inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) inputAudioFormat
        else AudioProcessor.AudioFormat.NOT_SET

    private fun rebuild(cfg: PeqConfig, rate: Int, channelCount: Int) {
        val active = cfg.active
        val sameShape = active.size == bands && channelCount == channels && rate == sampleRate
        applied = cfg
        sampleRate = rate
        channels = channelCount
        bands = active.size
        preamp = cfg.preampLinear
        coef = DoubleArray(bands * 5)
        active.forEachIndexed { i, band ->
            val q = ParamEqMath.biquad(band, rate)
            coef[i * 5] = q.b0
            coef[i * 5 + 1] = q.b1
            coef[i * 5 + 2] = q.b2
            coef[i * 5 + 3] = q.a1
            coef[i * 5 + 4] = q.a2
        }
        // A slider moving keeps the filters' memory, so dragging doesn't click.
        if (!sameShape) state = DoubleArray(bands * channelCount * 2)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val out = replaceOutputBuffer(size)
        val format = inputAudioFormat
        val cfg = ParametricEq.config
        if (cfg !== applied || format.sampleRate != sampleRate || format.channelCount != channels) {
            rebuild(cfg, format.sampleRate, format.channelCount)
        }
        val n = bands
        val ch = channels
        if ((n == 0 && preamp == 1.0) || ch <= 0) {
            out.put(inputBuffer)
            out.flip()
            return
        }
        val c = coef
        val z = state
        val gain = preamp
        var channel = 0
        if (format.encoding == C.ENCODING_PCM_16BIT) {
            while (inputBuffer.remaining() >= 2) {
                var x = inputBuffer.getShort() * gain
                var b = 0
                while (b < n) {
                    val k = b * 5
                    val s = (b * ch + channel) * 2
                    val y = c[k] * x + z[s]
                    z[s] = c[k + 1] * x - c[k + 3] * y + z[s + 1]
                    z[s + 1] = c[k + 2] * x - c[k + 4] * y
                    x = y
                    b++
                }
                val v = if (x >= 32767.0) 32767 else if (x <= -32768.0) -32768 else Math.round(x).toInt()
                out.putShort(v.toShort())
                channel++
                if (channel == ch) channel = 0
            }
        } else {
            while (inputBuffer.remaining() >= 4) {
                var x = inputBuffer.getFloat() * gain
                var b = 0
                while (b < n) {
                    val k = b * 5
                    val s = (b * ch + channel) * 2
                    val y = c[k] * x + z[s]
                    z[s] = c[k + 1] * x - c[k + 3] * y + z[s + 1]
                    z[s + 1] = c[k + 2] * x - c[k + 4] * y
                    x = y
                    b++
                }
                out.putFloat(x.toFloat())
                channel++
                if (channel == ch) channel = 0
            }
        }
        // Never leave bytes behind (a buffer is always whole samples; this is only a safeguard).
        if (inputBuffer.hasRemaining()) out.put(inputBuffer)
        out.flip()
    }
}
