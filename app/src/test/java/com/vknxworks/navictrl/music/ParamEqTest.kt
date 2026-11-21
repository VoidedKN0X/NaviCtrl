package com.vknxworks.navictrl.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ParamEqTest {

    @Test fun aPeakBandGivesItsGainAtItsFrequencyAndNothingFarAway() {
        val band = PeqBand(PeqType.PEAK, 1000, 60, 100)
        val q = ParamEqMath.biquad(band, 48_000)
        assertEquals(6.0, q.gainDbAt(1000.0, 48_000), 0.01)
        assertTrue(abs(q.gainDbAt(20.0, 48_000)) < 0.1)
        assertTrue(abs(q.gainDbAt(18_000.0, 48_000)) < 0.3)
    }

    @Test fun aCutMirrorsABoost() {
        val up = ParamEqMath.biquad(PeqBand(PeqType.PEAK, 500, 45, 140), 44_100)
        val down = ParamEqMath.biquad(PeqBand(PeqType.PEAK, 500, -45, 140), 44_100)
        for (hz in listOf(100.0, 400.0, 500.0, 700.0, 5000.0)) {
            assertEquals(0.0, up.gainDbAt(hz, 44_100) + down.gainDbAt(hz, 44_100), 0.001)
        }
    }

    @Test fun shelvesReachTheirGainOnTheirSideOnly() {
        val low = ParamEqMath.biquad(PeqBand(PeqType.LOW_SHELF, 200, 80, 70), 48_000)
        assertEquals(8.0, low.gainDbAt(20.0, 48_000), 0.2)
        assertEquals(4.0, low.gainDbAt(200.0, 48_000), 0.1)
        assertTrue(abs(low.gainDbAt(10_000.0, 48_000)) < 0.1)
        val high = ParamEqMath.biquad(PeqBand(PeqType.HIGH_SHELF, 4000, -60, 70), 48_000)
        assertEquals(-6.0, high.gainDbAt(20_000.0, 48_000), 0.2)
        assertEquals(-3.0, high.gainDbAt(4000.0, 48_000), 0.1)
        assertTrue(abs(high.gainDbAt(50.0, 48_000)) < 0.1)
    }

    @Test fun theFiltersAreStable() {
        for (type in PeqType.entries) for (hz in listOf(20, 1000, 20_000)) for (gain in listOf(-150, 150)) for (qh in listOf(20, 1000)) {
            for (rate in listOf(22_050, 44_100, 96_000)) {
                val q = ParamEqMath.biquad(PeqBand(type, hz, gain, qh), rate)
                // Both poles inside the unit circle.
                assertTrue("$type $hz $gain $qh $rate", abs(q.a2) < 1.0 && abs(q.a1) < 1.0 + q.a2)
            }
        }
    }

    @Test fun responseAddsBandsAndPreamp() {
        val bands = listOf(PeqBand(PeqType.PEAK, 1000, 60, 100), PeqBand(PeqType.PEAK, 1000, 30, 100), PeqBand(PeqType.PEAK, 5000, 0, 100))
        assertEquals(6.0, ParamEqMath.responseDb(bands, -30, 1000.0), 0.02)
        assertEquals(6.0, ParamEqMath.peakDb(bands, -30), 0.1)
        assertEquals(-3.0, ParamEqMath.peakDb(emptyList(), -30), 0.0001)
    }

    @Test fun bandsSurviveTheSettingsFile() {
        val bands = listOf(PeqBand(PeqType.LOW_SHELF, 80, 45, 70), PeqBand(PeqType.PEAK, 2500, -35, 220), PeqBand(PeqType.HIGH_SHELF, 9000, 20, 71))
        assertEquals(bands, PeqBand.decode(PeqBand.encode(bands)))
        assertEquals(emptyList<PeqBand>(), PeqBand.decode(""))
        assertEquals(emptyList<PeqBand>(), PeqBand.decode(null))
        // Rubbish is left out, and values are brought into range.
        assertEquals(listOf(PeqBand(PeqType.PEAK, 20_000, 150, 20)), PeqBand.decode("X,1,2,3;P,99999,999,1;nonsense;P,1,2"))
        assertEquals(PeqBand.MAX_BANDS, PeqBand.decode((1..30).joinToString(";") { "P,${it * 100},10,100" }).size)
    }

    @Test fun anOffEqualizerDoesNothing() {
        val cfg = PeqConfig(false, -60, listOf(PeqBand(PeqType.PEAK, 1000, 60, 100)))
        assertTrue(cfg.active.isEmpty())
        assertEquals(1.0, cfg.preampLinear, 0.0)
        val on = PeqConfig(true, -60, listOf(PeqBand(PeqType.PEAK, 1000, 60, 100), PeqBand(PeqType.PEAK, 2000, 0, 100)))
        assertEquals(1, on.active.size)
        assertEquals(0.501, on.preampLinear, 0.001)
    }
}
