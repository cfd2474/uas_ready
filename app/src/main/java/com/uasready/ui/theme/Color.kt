package com.uasready.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.uasready.domain.model.AssessmentStatus

data class AviationColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val border: Color,
    val subtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val cyan: Color,
    val safetyGo: Color,
    val safetyGoBg: Color,
    val safetyCaution: Color,
    val safetyCautionBg: Color,
    val safetyNoGo: Color,
    val safetyNoGoBg: Color,
    val safetyUnavailable: Color,
    val safetyUnavailableBg: Color
)

val DarkAviationColors = AviationColors(
    background = Color(0xFF0A0E14),
    surface = Color(0xFF121820),
    card = Color(0xFF1A222D),
    border = Color(0xFF2B3644),
    subtle = Color(0xFF3B485A),
    textPrimary = Color(0xFFF0F6FC),
    textSecondary = Color(0xFF8B949E),
    textMuted = Color(0xFF6E7681),
    accent = Color(0xFF58A6FF),
    cyan = Color(0xFF388BFD),
    safetyGo = Color(0xFF3FB950),
    safetyGoBg = Color(0xFF0D2818),
    safetyCaution = Color(0xFFE3B341),
    safetyCautionBg = Color(0xFF2E2305),
    safetyNoGo = Color(0xFFF85149),
    safetyNoGoBg = Color(0xFF330D0D),
    safetyUnavailable = Color(0xFF8B949E),
    safetyUnavailableBg = Color(0xFF1E2228)
)

val LightAviationColors = AviationColors(
    background = Color(0xFFF6F8FA),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    border = Color(0xFFD0D7DE),
    subtle = Color(0xFFEAEEF2),
    textPrimary = Color(0xFF1F2328),
    textSecondary = Color(0xFF57606A),
    textMuted = Color(0xFF6E7781),
    accent = Color(0xFF0969DA),
    cyan = Color(0xFF0550AE),
    safetyGo = Color(0xFF1A7F37),
    safetyGoBg = Color(0xFFDAFBE1),
    safetyCaution = Color(0xFF9A6700),
    safetyCautionBg = Color(0xFFFFF8C5),
    safetyNoGo = Color(0xFFCF222E),
    safetyNoGoBg = Color(0xFFFFEBE9),
    safetyUnavailable = Color(0xFF656D76),
    safetyUnavailableBg = Color(0xFFF6F8FA)
)

val LocalAviationColors = staticCompositionLocalOf { DarkAviationColors }

// Dynamic reactive theme color accessors for Composable callers
val AviationDarkBackground: Color @Composable get() = LocalAviationColors.current.background
val AviationDarkSurface: Color @Composable get() = LocalAviationColors.current.surface
val AviationDarkCard: Color @Composable get() = LocalAviationColors.current.card
val AviationDarkBorder: Color @Composable get() = LocalAviationColors.current.border
val AviationDarkSubtle: Color @Composable get() = LocalAviationColors.current.subtle

val TextPrimary: Color @Composable get() = LocalAviationColors.current.textPrimary
val TextSecondary: Color @Composable get() = LocalAviationColors.current.textSecondary
val TextMuted: Color @Composable get() = LocalAviationColors.current.textMuted
val AviationAccent: Color @Composable get() = LocalAviationColors.current.accent
val AviationCyan: Color @Composable get() = LocalAviationColors.current.cyan

val SafetyGo: Color @Composable get() = LocalAviationColors.current.safetyGo
val SafetyGoLight: Color @Composable get() = LocalAviationColors.current.safetyGo
val SafetyGoBg: Color @Composable get() = LocalAviationColors.current.safetyGoBg

val SafetyCaution: Color @Composable get() = LocalAviationColors.current.safetyCaution
val SafetyCautionLight: Color @Composable get() = LocalAviationColors.current.safetyCaution
val SafetyCautionBg: Color @Composable get() = LocalAviationColors.current.safetyCautionBg

val SafetyNoGo: Color @Composable get() = LocalAviationColors.current.safetyNoGo
val SafetyNoGoLight: Color @Composable get() = LocalAviationColors.current.safetyNoGo
val SafetyNoGoBg: Color @Composable get() = LocalAviationColors.current.safetyNoGoBg

val SafetyUnavailable: Color @Composable get() = LocalAviationColors.current.safetyUnavailable
val SafetyUnavailableLight: Color @Composable get() = LocalAviationColors.current.safetyUnavailable
val SafetyUnavailableBg: Color @Composable get() = LocalAviationColors.current.safetyUnavailableBg

@Composable
fun AssessmentStatus.toColor(): Color = when (this) {
    AssessmentStatus.GO -> LocalAviationColors.current.safetyGo
    AssessmentStatus.CAUTION -> LocalAviationColors.current.safetyCaution
    AssessmentStatus.NO_GO -> LocalAviationColors.current.safetyNoGo
    AssessmentStatus.DATA_UNAVAILABLE -> LocalAviationColors.current.safetyUnavailable
}

@Composable
fun AssessmentStatus.toBgColor(): Color = when (this) {
    AssessmentStatus.GO -> LocalAviationColors.current.safetyGoBg
    AssessmentStatus.CAUTION -> LocalAviationColors.current.safetyCautionBg
    AssessmentStatus.NO_GO -> LocalAviationColors.current.safetyNoGoBg
    AssessmentStatus.DATA_UNAVAILABLE -> LocalAviationColors.current.safetyUnavailableBg
}
