package com.vknxworks.navictrl.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
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

/**
 * Ctrl / Signal: the shared look of the Ctrl series. Dark only, near-black base, one accent.
 * Change [Accent] to recolour the whole app.
 */
object CtrlColors {
    /** The one accent colour. Everything accent-coloured reads it from here. */
    val Accent = Color(0xFFB48CFF)
    val OnAccent = Color(0xFF08080A)

    val Background = Color(0xFF08080A)
    val Surface = Color(0xFF141417)
    val Raised = Color(0xFF1C1C21)
    val Outline = Color(0xFF232328)
    val ControlOutline = Color(0xFF2A2A2F)
    val Divider = Color(0xFF1E1E23)
    val Text = Color(0xFFF4F4F2)
    val Muted = Color(0xFFA3A3AB)
    val Inactive = Color(0xFF8C8C94)
    val Error = Color(0xFFFF6B6B)

    val SwitchTrackOff = Color(0xFF2A2A30)
    val ProgressTrack = Color(0xFF26262C)
    val EmptyCell = Color(0xFF3A3A41)
    val WaveIdle = Color(0xFF34343A)

    /** Floating bars: the surface at ~92 % so content scrolling behind shows faintly. */
    val BarFill = Surface.copy(alpha = 0.92f)
}


private val SignalScheme = darkColorScheme(
    primary = CtrlColors.Accent,
    onPrimary = CtrlColors.OnAccent,
    primaryContainer = CtrlColors.Accent,
    onPrimaryContainer = CtrlColors.OnAccent,
    inversePrimary = CtrlColors.Accent,
    secondary = CtrlColors.Text,
    onSecondary = CtrlColors.OnAccent,
    secondaryContainer = CtrlColors.Surface,
    onSecondaryContainer = CtrlColors.Text,
    tertiary = CtrlColors.Accent,
    onTertiary = CtrlColors.OnAccent,
    tertiaryContainer = CtrlColors.Raised,
    onTertiaryContainer = CtrlColors.Text,
    background = CtrlColors.Background,
    onBackground = CtrlColors.Text,
    surface = CtrlColors.Background,
    onSurface = CtrlColors.Text,
    surfaceVariant = CtrlColors.Raised,
    onSurfaceVariant = CtrlColors.Muted,
    surfaceTint = Color.Transparent,
    inverseSurface = CtrlColors.Text,
    inverseOnSurface = CtrlColors.Background,
    surfaceBright = CtrlColors.Raised,
    surfaceDim = CtrlColors.Background,
    surfaceContainerLowest = CtrlColors.Background,
    surfaceContainerLow = CtrlColors.Surface,
    surfaceContainer = CtrlColors.Surface,
    surfaceContainerHigh = CtrlColors.Raised,
    surfaceContainerHighest = CtrlColors.Raised,
    outline = CtrlColors.ControlOutline,
    outlineVariant = CtrlColors.Outline,
    error = CtrlColors.Error,
    onError = CtrlColors.OnAccent,
    errorContainer = CtrlColors.Raised,
    onErrorContainer = CtrlColors.Error,
    scrim = Color.Black,
)

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

/** The app theme: always Ctrl / Signal dark. No light theme and no wallpaper colours. */
@Composable
fun NaviCtrlTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SignalScheme, typography = AppTypography, shapes = AppShapes, content = content)
}
