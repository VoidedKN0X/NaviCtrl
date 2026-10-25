package com.vknxworks.navictrl.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.vknxworks.navictrl.R

/** One Ctrl / Signal palette. The app has two: [CtrlColors.Dark] and [CtrlColors.Light]. */
@Immutable
data class CtrlPalette(
    val isDark: Boolean,
    /** Accent as a fill (hero cards, main buttons, the selected tab, switch tracks, PIN cells). */
    val accent: Color,
    /** Content on an accent fill. */
    val onAccent: Color,
    /** Accent as text or an icon on the background (links, status words, the wordmark slash, focus). */
    val accentText: Color,
    val background: Color,
    val surface: Color,
    val raised: Color,
    val outline: Color,
    val controlOutline: Color,
    val divider: Color,
    val text: Color,
    val muted: Color,
    val inactive: Color,
    val error: Color,
    val onError: Color,
    val switchTrackOff: Color,
    val progressTrack: Color,
    /** Progress fill when it isn't the main metric. */
    val progressFill: Color,
    val emptyCell: Color,
    val waveIdle: Color,
    /** Floating bars: the surface at ~92 % so content scrolling behind shows faintly. */
    val barFill: Color,
)

/** The palette of the current theme (light or dark, following the phone). */
val LocalCtrlColors = staticCompositionLocalOf { CtrlColors.Dark }

/**
 * Ctrl / Signal colours. [Accent] is the one accent of the app; both palettes use it, so changing it
 * here recolours everything. Screens read the current palette through the getters below.
 */
object CtrlColors {
    /** The one accent colour. */
    val AccentFill = Color(0xFFB48CFF)
    /** The accent as text on light backgrounds, where the fill colour would be too pale to read. */
    val AccentInk = Color(0xFF6A3BDB)
    private val Ink = Color(0xFF08080A)

    val Dark = CtrlPalette(
        isDark = true,
        accent = AccentFill,
        onAccent = Ink,
        accentText = AccentFill,
        background = Color(0xFF08080A),
        surface = Color(0xFF141417),
        raised = Color(0xFF1C1C21),
        outline = Color(0xFF232328),
        controlOutline = Color(0xFF2A2A2F),
        divider = Color(0xFF1E1E23),
        text = Color(0xFFF4F4F2),
        muted = Color(0xFFA3A3AB),
        inactive = Color(0xFF8C8C94),
        error = Color(0xFFFF6B6B),
        onError = Ink,
        switchTrackOff = Color(0xFF2A2A30),
        progressTrack = Color(0xFF26262C),
        progressFill = Color(0xFFF4F4F2),
        emptyCell = Color(0xFF3A3A41),
        waveIdle = Color(0xFF34343A),
        barFill = Color(0xFF141417).copy(alpha = 0.92f),
    )

    val Light = CtrlPalette(
        isDark = false,
        accent = AccentFill,
        onAccent = Ink,
        accentText = AccentInk,
        background = Color(0xFFF5F5F1),
        surface = Color(0xFFFFFFFF),
        raised = Color(0xFFECECE7),
        outline = Color(0xFFDEDED7),
        controlOutline = Color(0xFFD2D2CA),
        divider = Color(0xFFE6E6E0),
        text = Color(0xFF0B0B0D),
        muted = Color(0xFF5C5C64),
        inactive = Color(0xFF74747C),
        error = Color(0xFFD93636),
        onError = Color(0xFFFFFFFF),
        switchTrackOff = Color(0xFFDCDCD5),
        progressTrack = Color(0xFFE2E2DC),
        progressFill = Color(0xFF0B0B0D),
        emptyCell = Color(0xFFC9C9C1),
        waveIdle = Color(0xFFD2D2CA),
        barFill = Color(0xFFFFFFFF).copy(alpha = 0.92f),
    )

    private val p: CtrlPalette @Composable @ReadOnlyComposable get() = LocalCtrlColors.current

    val Accent: Color @Composable @ReadOnlyComposable get() = p.accent
    val OnAccent: Color @Composable @ReadOnlyComposable get() = p.onAccent
    val AccentText: Color @Composable @ReadOnlyComposable get() = p.accentText
    val Background: Color @Composable @ReadOnlyComposable get() = p.background
    val Surface: Color @Composable @ReadOnlyComposable get() = p.surface
    val Raised: Color @Composable @ReadOnlyComposable get() = p.raised
    val Outline: Color @Composable @ReadOnlyComposable get() = p.outline
    val ControlOutline: Color @Composable @ReadOnlyComposable get() = p.controlOutline
    val Divider: Color @Composable @ReadOnlyComposable get() = p.divider
    val Text: Color @Composable @ReadOnlyComposable get() = p.text
    val Muted: Color @Composable @ReadOnlyComposable get() = p.muted
    val Inactive: Color @Composable @ReadOnlyComposable get() = p.inactive
    val Error: Color @Composable @ReadOnlyComposable get() = p.error
    val OnError: Color @Composable @ReadOnlyComposable get() = p.onError
    val SwitchTrackOff: Color @Composable @ReadOnlyComposable get() = p.switchTrackOff
    val ProgressTrack: Color @Composable @ReadOnlyComposable get() = p.progressTrack
    val ProgressFill: Color @Composable @ReadOnlyComposable get() = p.progressFill
    val EmptyCell: Color @Composable @ReadOnlyComposable get() = p.emptyCell
    val WaveIdle: Color @Composable @ReadOnlyComposable get() = p.waveIdle
    val BarFill: Color @Composable @ReadOnlyComposable get() = p.barFill
}

private fun schemeFor(c: CtrlPalette): ColorScheme {
    val base = if (c.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.accent,
        onPrimaryContainer = c.onAccent,
        inversePrimary = c.accent,
        secondary = c.text,
        onSecondary = c.background,
        secondaryContainer = c.surface,
        onSecondaryContainer = c.text,
        // Accent as text/icon colour, for Material parts that use tertiary.
        tertiary = c.accentText,
        onTertiary = if (c.isDark) c.onAccent else Color.White,
        tertiaryContainer = c.raised,
        onTertiaryContainer = c.text,
        background = c.background,
        onBackground = c.text,
        surface = c.background,
        onSurface = c.text,
        surfaceVariant = c.raised,
        onSurfaceVariant = c.muted,
        surfaceTint = Color.Transparent,
        inverseSurface = c.text,
        inverseOnSurface = c.background,
        surfaceBright = c.surface,
        surfaceDim = c.background,
        surfaceContainerLowest = c.background,
        surfaceContainerLow = c.surface,
        surfaceContainer = c.surface,
        surfaceContainerHigh = c.raised,
        surfaceContainerHighest = c.raised,
        outline = c.controlOutline,
        outlineVariant = c.outline,
        error = c.error,
        onError = c.onError,
        errorContainer = c.raised,
        onErrorContainer = c.error,
        scrim = Color.Black,
    )
}

private val DarkScheme = schemeFor(CtrlColors.Dark)
private val LightScheme = schemeFor(CtrlColors.Light)

/** Space Grotesk: all UI text and headings. */
val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

/** JetBrains Mono: numbers, data readouts, section labels and the wordmark. */
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
)

private fun sg(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = SpaceGrotesk,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.em,
)

private val AppTypography = Typography(
    displayLarge = sg(56, 56, FontWeight.Bold, -0.04),
    displayMedium = sg(48, 50, FontWeight.Bold, -0.04),
    displaySmall = sg(40, 42, FontWeight.Bold, -0.035),
    headlineLarge = sg(40, 42, FontWeight.Bold, -0.035),
    headlineMedium = sg(30, 32, FontWeight.Bold, -0.03),
    headlineSmall = sg(24, 28, FontWeight.Bold, -0.02),
    titleLarge = sg(18, 24, FontWeight.SemiBold),
    titleMedium = sg(16, 22, FontWeight.SemiBold),
    titleSmall = sg(14, 20, FontWeight.SemiBold),
    bodyLarge = sg(16, 22, FontWeight.Normal),
    bodyMedium = sg(14, 20, FontWeight.Normal),
    bodySmall = sg(12, 16, FontWeight.Normal),
    labelLarge = sg(14, 20, FontWeight.SemiBold),
    labelMedium = sg(12, 16, FontWeight.Medium),
    labelSmall = sg(11, 14, FontWeight.Medium),
)

/** Extra text styles of the Signal type scale that Material's Typography has no slot for. */
object CtrlType {
    /** Section labels: mono, upper case, wide tracking, muted. */
    val Label = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.16.em)
    /** "NAVI/CTRL" in the top bar. */
    val Wordmark = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.14.em)
    /** Small mono readouts: times, counts, track numbers, server addresses. */
    val Mono = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal)
    val MonoStrong = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
    /** Big numeric readouts. */
    val BigNumber = TextStyle(fontFamily = SpaceGrotesk, fontSize = 64.sp, lineHeight = 60.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.05).em)
    /** PIN keypad digits. */
    val Key = TextStyle(fontFamily = JetBrainsMono, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium)
}

/** Corner radii of the Signal shapes. */
object CtrlShapes {
    val Card = RoundedCornerShape(22.dp)
    val Hero = RoundedCornerShape(26.dp)
    val Control = RoundedCornerShape(14.dp)
    val Button = RoundedCornerShape(16.dp)
    val MainButton = RoundedCornerShape(18.dp)
    val Badge = RoundedCornerShape(10.dp)
    val Chip = RoundedCornerShape(9.dp)
    val Dot = RoundedCornerShape(2.dp)
    val Art = RoundedCornerShape(10.dp)
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(9.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

/** Horizontal page margin used everywhere. */
val Gutter = 20.dp

/**
 * The app theme: Ctrl / Signal, light or dark following the phone's dark theme setting. There is no
 * in-app theme choice and no wallpaper (Material You) colours.
 */
@Composable
fun NaviCtrlTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val palette = if (dark) CtrlColors.Dark else CtrlColors.Light
    val selection = TextSelectionColors(handleColor = palette.accentText, backgroundColor = palette.accent.copy(alpha = 0.35f))
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, typography = AppTypography, shapes = AppShapes) {
        CompositionLocalProvider(LocalCtrlColors provides palette, LocalTextSelectionColors provides selection, content = content)
    }
}
