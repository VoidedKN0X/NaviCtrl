package com.vknxworks.navictrl.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
            Box(Modifier.padding(padding).fillMaxSize()) {
                MessageView(Icons.Outlined.GraphicEq, stringResource(R.string.eq_unavailable), stringResource(R.string.eq_unavailable_body))
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
        }
    }
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
