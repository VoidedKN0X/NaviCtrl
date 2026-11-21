package com.vknxworks.navictrl.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import com.vknxworks.navictrl.music.ParamEqMath
import com.vknxworks.navictrl.music.ParametricEq
import com.vknxworks.navictrl.music.PeqBand
import com.vknxworks.navictrl.music.PeqConfig
import com.vknxworks.navictrl.music.PeqType
import com.vknxworks.navictrl.ui.components.GroupRow
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.navictrl.R
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.settings.AppSettings
import com.vknxworks.navictrl.music.AudioFx
import com.vknxworks.navictrl.music.EqInfo
import com.vknxworks.navictrl.ui.components.CtrlChip
import com.vknxworks.navictrl.ui.components.CtrlLabel
import com.vknxworks.navictrl.ui.components.Group
import com.vknxworks.navictrl.ui.components.GroupSwitch
import com.vknxworks.navictrl.ui.components.MessageView
import com.vknxworks.navictrl.ui.components.ScreenTopBar
import com.vknxworks.navictrl.ui.components.topDivider
import com.vknxworks.navictrl.ui.theme.CtrlColors
import com.vknxworks.navictrl.ui.theme.CtrlType
import kotlinx.coroutines.launch

/** Equalizer: the phone's own presets or your own band levels, and bass boost when the phone has it. */
@Composable
fun EqualizerScreen() {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    val s by c.settings.settings.collectAsStateWithLifecycle()
    val info = remember { AudioFx.info(context) }
    fun update(t: (AppSettings) -> AppSettings) { scope.launch { c.settings.update(t) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(stringResource(R.string.equalizer)) },
    ) { padding ->
        if (info == null) {
            // No equalizer of the phone's own: the parametric equalizer is NaviCtrl's own and still works.
            Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
                Group {
                    GroupRow(stringResource(R.string.eq_unavailable), stringResource(R.string.eq_unavailable_body), icon = Icons.Outlined.GraphicEq)
                }
                ParametricSection(s) { t -> update(t) }
            }
            return@Scaffold
        }
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Group(footer = stringResource(R.string.eq_footer)) {
                GroupSwitch(stringResource(R.string.eq_use), stringResource(R.string.eq_use_subtitle), s.eqEnabled, icon = Icons.Outlined.GraphicEq) { v ->
                    update { it.copy(eqEnabled = v) }
                }
            }

            Group(stringResource(R.string.eq_presets)) {
                FlowRow(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CtrlChip(stringResource(R.string.eq_custom), selected = s.eqPreset !in info.presets.indices, onClick = {
                        // Custom starts from what's heard now, so picking it doesn't jump.
                        val from = info.presetLevels.getOrNull(s.eqPreset)
                        update { it.copy(eqPreset = -1, eqBands = from ?: it.eqBands) }
                    })
                    info.presets.forEachIndexed { i, name ->
                        CtrlChip(name, selected = s.eqPreset == i, onClick = { update { it.copy(eqPreset = i) } })
                    }
                }
            }

            Group(stringResource(R.string.eq_bands)) {
                val levels = currentLevels(s, info)
                info.bandHz.forEachIndexed { band, hz ->
                    BandRow(
                        label = formatHz(hz),
                        level = levels.getOrElse(band) { 0 },
                        min = info.minLevel,
                        max = info.maxLevel,
                        onPreview = { AudioFx.previewBand(band, it) },
                        onDone = { level ->
                            val start = currentLevels(c.settings.settings.value, info)
                            val next = info.bandHz.indices.map { if (it == band) level else start.getOrElse(it) { 0 } }
                            update { it.copy(eqPreset = -1, eqBands = next) }
                        },
                    )
                }
            }

            if (info.bassBoost) {
                Group(stringResource(R.string.bass_boost), footer = stringResource(R.string.bass_boost_footer)) {
                    var drag by remember { mutableStateOf<Float?>(null) }
                    val value = drag ?: s.bassBoost.toFloat()
                    Column(Modifier.fillMaxWidth().topDivider(start = 0.dp, end = 0.dp, color = CtrlColors.Outline).padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.bass_boost_strength), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(
                                if (value < 1f) stringResource(R.string.off) else "${(value / 10f).toInt()}%",
                                style = CtrlType.Mono,
                                color = CtrlColors.Muted,
                            )
                        }
                        Slider(
                            value = value,
                            onValueChange = { drag = it; AudioFx.previewBass(it.toInt()) },
                            onValueChangeFinished = {
                                val v = drag?.toInt() ?: s.bassBoost
                                drag = null
                                update { it.copy(bassBoost = v) }
                            },
                            valueRange = 0f..1000f,
                            colors = ctrlSliderColors(),
                        )
                    }
                }
            }

            ParametricSection(s) { t -> update(t) }
        }
    }
}

/**
 * The parametric equalizer: NaviCtrl's own filters inside the player. Any number of bands up to ten, each a
 * bell or a shelf with its own frequency, gain and width (Q), and a preamp to make room for boosts.
 */
@Composable
private fun ParametricSection(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    var editing by remember { mutableStateOf<Int?>(null) }
    // What's shown and heard while a slider moves or a save is on its way; null = the saved settings.
    var draftBands by remember { mutableStateOf<List<PeqBand>?>(null) }
    var draftPreamp by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(s.peqBands) { draftBands = null }
    LaunchedEffect(s.peqPreamp) { draftPreamp = null }
    val bands = draftBands ?: s.peqBands
    val preamp = draftPreamp ?: s.peqPreamp
    val peak = remember(bands, preamp) { ParamEqMath.peakDb(bands, preamp) }

    fun saveBands(next: List<PeqBand>) {
        draftBands = next
        update { it.copy(peqBands = next) }
    }

    Group(stringResource(R.string.peq), footer = stringResource(R.string.peq_footer)) {
        GroupSwitch(stringResource(R.string.peq_use), stringResource(R.string.peq_use_subtitle), s.peqEnabled, icon = Icons.Outlined.Tune) { v ->
            update { it.copy(peqEnabled = v) }
        }
        ResponseCurve(bands, preamp)
        Column(Modifier.fillMaxWidth().topDivider(start = 0.dp, end = 0.dp, color = CtrlColors.Outline).padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.peq_preamp), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.x_db, "%+.1f".format(preamp / 10f)), style = CtrlType.Mono, color = CtrlColors.Muted)
            }
            Slider(
                value = preamp.toFloat(),
                onValueChange = {
                    val v = (it / 5f).roundToInt() * 5
                    draftPreamp = v
                    ParametricEq.preview(s.peqEnabled, v, bands)
                },
                onValueChangeFinished = { draftPreamp?.let { v -> update { st -> st.copy(peqPreamp = v) } } },
                valueRange = PeqConfig.MIN_PREAMP_TENTHS.toFloat()..0f,
                colors = ctrlSliderColors(),
            )
            if (peak > 0.05) {
                Text(
                    stringResource(R.string.peq_clip_warning, "%.1f".format(peak)),
                    style = MaterialTheme.typography.bodySmall,
                    color = CtrlColors.Error,
                )
            }
        }
        bands.forEachIndexed { i, band ->
            GroupRow(
                stringResource(band.type.label()) + "  ·  " + formatHz(band.hz),
                stringResource(R.string.peq_band_summary, "%+.1f".format(band.gainTenths / 10f), "%.2f".format(band.qHundredths / 100f)),
                showChevron = true,
                onClick = { editing = i },
            )
        }
        if (bands.size < PeqBand.MAX_BANDS) {
            GroupRow(
                stringResource(R.string.peq_add_band),
                icon = Icons.Filled.Add,
                onClick = {
                    saveBands(bands + PeqBand())
                    editing = bands.size
                },
            )
        }
    }

    val index = editing
    val band = index?.let { bands.getOrNull(it) }
    if (index != null && band != null) {
        fun change(next: PeqBand, save: Boolean) {
            val list = bands.toMutableList().also { it[index] = next.clean() }
            if (save) saveBands(list) else {
                draftBands = list
                ParametricEq.preview(s.peqEnabled, preamp, list)
            }
        }
        ModalBottomSheet(onDismissRequest = { editing = null }, containerColor = CtrlColors.Surface) {
            Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 16.dp)) {
                Text(stringResource(R.string.peq_band_x, index + 1), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PeqType.entries.forEach { t ->
                        CtrlChip(stringResource(t.label()), selected = band.type == t, onClick = { change(band.copy(type = t), save = true) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                SheetSlider(
                    stringResource(R.string.peq_frequency),
                    formatHz(band.hz),
                    hzToPos(band.hz),
                    onChange = { change(band.copy(hz = posToHz(it)), save = false) },
                    onDone = { change(bands.getOrNull(index) ?: band, save = true) },
                )
                SheetSlider(
                    stringResource(R.string.peq_gain),
                    stringResource(R.string.x_db, "%+.1f".format(band.gainTenths / 10f)),
                    (band.gainTenths + PeqBand.MAX_GAIN_TENTHS) / (2f * PeqBand.MAX_GAIN_TENTHS),
                    onChange = { change(band.copy(gainTenths = ((it * 2f * PeqBand.MAX_GAIN_TENTHS - PeqBand.MAX_GAIN_TENTHS) / 5f).roundToInt() * 5), save = false) },
                    onDone = { change(bands.getOrNull(index) ?: band, save = true) },
                )
                SheetSlider(
                    stringResource(R.string.peq_width),
                    "Q %.2f".format(band.qHundredths / 100f),
                    qToPos(band.qHundredths),
                    onChange = { change(band.copy(qHundredths = posToQ(it)), save = false) },
                    onDone = { change(bands.getOrNull(index) ?: band, save = true) },
                )
                Text(stringResource(R.string.peq_width_hint), style = MaterialTheme.typography.bodySmall, color = CtrlColors.Muted)
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        editing = null
                        saveBands(bands.filterIndexed { i, _ -> i != index })
                    },
                ) { Text(stringResource(R.string.peq_remove_band), color = CtrlColors.Error) }
            }
        }
    }
}

@Composable
private fun SheetSlider(label: String, value: String, position: Float, onChange: (Float) -> Unit, onDone: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = CtrlType.Mono, color = CtrlColors.Muted)
    }
    Slider(
        value = position.coerceIn(0f, 1f),
        onValueChange = onChange,
        onValueChangeFinished = onDone,
        valueRange = 0f..1f,
        colors = ctrlSliderColors(),
    )
}

/** The curve of the whole parametric equalizer from 20 Hz to 20 kHz, between -18 and +18 dB. */
@Composable
private fun ResponseCurve(bands: List<PeqBand>, preampTenths: Int) {
    val line = CtrlColors.AccentText
    val grid = CtrlColors.Outline
    val points = remember(bands, preampTenths) {
        FloatArray(CURVE_POINTS + 1) { i -> ParamEqMath.responseDb(bands, preampTenths, 20.0 * 1000.0.pow(i / CURVE_POINTS.toDouble())).toFloat() }
    }
    Column(Modifier.fillMaxWidth().topDivider(start = 0.dp, end = 0.dp, color = CtrlColors.Outline).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val w = size.width
            val h = size.height
            fun y(db: Float) = h / 2f - (db.coerceIn(-CURVE_DB, CURVE_DB) / CURVE_DB) * (h / 2f)
            // 0 dB, and the decades 100 Hz, 1 kHz and 10 kHz.
            drawLine(grid, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1.dp.toPx())
            for (hz in listOf(100.0, 1000.0, 10_000.0)) {
                val x = (ln(hz / 20.0) / ln(1000.0)).toFloat() * w
                drawLine(grid, Offset(x, 0f), Offset(x, h), strokeWidth = 1.dp.toPx())
            }
            val path = Path()
            points.forEachIndexed { i, db ->
                val x = w * i / CURVE_POINTS
                if (i == 0) path.moveTo(x, y(db)) else path.lineTo(x, y(db))
            }
            drawPath(path, line, style = Stroke(width = 2.dp.toPx()))
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("20 Hz", style = CtrlType.Mono, color = CtrlColors.Muted, modifier = Modifier.weight(1f))
            Text("±${CURVE_DB.toInt()} dB", style = CtrlType.Mono, color = CtrlColors.Muted)
            Text("20 kHz", style = CtrlType.Mono, color = CtrlColors.Muted, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
    }
}

private const val CURVE_POINTS = 96
private const val CURVE_DB = 18f

private fun PeqType.label(): Int = when (this) {
    PeqType.PEAK -> R.string.peq_type_peak
    PeqType.LOW_SHELF -> R.string.peq_type_low_shelf
    PeqType.HIGH_SHELF -> R.string.peq_type_high_shelf
}

/** Slider positions (0..1) for the frequency, on a scale where every octave is as wide as the next. */
private fun hzToPos(hz: Int): Float = (ln(hz.coerceIn(PeqBand.MIN_HZ, PeqBand.MAX_HZ) / 20.0) / ln(1000.0)).toFloat()

private fun posToHz(pos: Float): Int {
    val hz = 20.0 * 1000.0.pow(pos.coerceIn(0f, 1f).toDouble())
    val step = if (hz < 100) 1 else if (hz < 1000) 10 else 100
    return ((hz / step).roundToInt() * step).coerceIn(PeqBand.MIN_HZ, PeqBand.MAX_HZ)
}

private fun qToPos(qHundredths: Int): Float =
    (ln(qHundredths.coerceIn(PeqBand.MIN_Q_HUNDREDTHS, PeqBand.MAX_Q_HUNDREDTHS) / PeqBand.MIN_Q_HUNDREDTHS.toDouble()) / ln(PeqBand.MAX_Q_HUNDREDTHS / PeqBand.MIN_Q_HUNDREDTHS.toDouble())).toFloat()

private fun posToQ(pos: Float): Int {
    val q = PeqBand.MIN_Q_HUNDREDTHS * (PeqBand.MAX_Q_HUNDREDTHS / PeqBand.MIN_Q_HUNDREDTHS.toDouble()).pow(pos.coerceIn(0f, 1f).toDouble())
    return ((q / 5.0).roundToInt() * 5).coerceIn(PeqBand.MIN_Q_HUNDREDTHS, PeqBand.MAX_Q_HUNDREDTHS)
}

private fun currentLevels(s: AppSettings, info: EqInfo): List<Int> =
    info.presetLevels.getOrNull(s.eqPreset) ?: info.bandHz.indices.map { s.eqBands.getOrElse(it) { 0 } }

@Composable
private fun BandRow(label: String, level: Int, min: Int, max: Int, onPreview: (Int) -> Unit, onDone: (Int) -> Unit) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val value = drag ?: level.toFloat()
    Row(
        Modifier.fillMaxWidth().topDivider(start = 0.dp, end = 0.dp, color = CtrlColors.Outline).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CtrlLabel(label, Modifier.width(64.dp))
        Slider(
            value = value.coerceIn(min.toFloat(), max.toFloat()),
            onValueChange = { drag = it; onPreview(it.toInt()) },
            onValueChangeFinished = {
                val v = drag?.toInt() ?: level
                drag = null
                onDone(v)
            },
            valueRange = min.toFloat()..max.toFloat(),
            colors = ctrlSliderColors(),
            modifier = Modifier.weight(1f),
        )
        Text(
            "%+.1f dB".format(value / 100f),
            style = CtrlType.Mono,
            color = CtrlColors.Muted,
            modifier = Modifier.width(72.dp).padding(start = 8.dp),
        )
    }
}

@Composable
fun ctrlSliderColors() = SliderDefaults.colors(
    thumbColor = CtrlColors.Accent,
    activeTrackColor = CtrlColors.Accent,
    inactiveTrackColor = CtrlColors.ProgressTrack,
    activeTickColor = CtrlColors.OnAccent,
    inactiveTickColor = CtrlColors.Muted,
)

private fun formatHz(hz: Int): String = if (hz >= 1000) {
    val k = hz / 1000f
    if (k >= 10 || k == k.toInt().toFloat()) "${k.toInt()} kHz" else "%.1f kHz".format(k)
} else "$hz Hz"
