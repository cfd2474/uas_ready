package com.uasready.ui.theme

import androidx.compose.ui.graphics.Color
import com.uasready.domain.model.AssessmentStatus

// Public Safety Aviation Dark Palette
val AviationDarkBackground = Color(0xFF0A0E14)
val AviationDarkSurface = Color(0xFF121820)
val AviationDarkCard = Color(0xFF1A222D)
val AviationDarkBorder = Color(0xFF2B3644)
val AviationDarkSubtle = Color(0xFF3B485A)

// Text Colors
val TextPrimary = Color(0xFFF0F6FC)
val TextSecondary = Color(0xFF8B949E)
val TextMuted = Color(0xFF6E7681)

// Semantic Safety Colors
val SafetyGo = Color(0xFF2EA043)
val SafetyGoLight = Color(0xFF3FB950)
val SafetyGoBg = Color(0xFF0D2818)

val SafetyCaution = Color(0xFFD29922)
val SafetyCautionLight = Color(0xFFE3B341)
val SafetyCautionBg = Color(0xFF2E2305)

val SafetyNoGo = Color(0xFFDA3633)
val SafetyNoGoLight = Color(0xFFF85149)
val SafetyNoGoBg = Color(0xFF330D0D)

val SafetyUnavailable = Color(0xFF6E7681)
val SafetyUnavailableLight = Color(0xFF8B949E)
val SafetyUnavailableBg = Color(0xFF1E2228)

// Accent
val AviationCyan = Color(0xFF388BFD)
val AviationAccent = Color(0xFF58A6FF)

fun AssessmentStatus.toColor(): Color = when (this) {
    AssessmentStatus.GO -> SafetyGoLight
    AssessmentStatus.CAUTION -> SafetyCautionLight
    AssessmentStatus.NO_GO -> SafetyNoGoLight
    AssessmentStatus.DATA_UNAVAILABLE -> SafetyUnavailableLight
}

fun AssessmentStatus.toBgColor(): Color = when (this) {
    AssessmentStatus.GO -> SafetyGoBg
    AssessmentStatus.CAUTION -> SafetyCautionBg
    AssessmentStatus.NO_GO -> SafetyNoGoBg
    AssessmentStatus.DATA_UNAVAILABLE -> SafetyUnavailableBg
}
