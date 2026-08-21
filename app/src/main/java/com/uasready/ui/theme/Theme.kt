package com.uasready.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AviationCyan,
    onPrimary = TextPrimary,
    primaryContainer = AviationDarkCard,
    onPrimaryContainer = AviationAccent,
    secondary = SafetyGoLight,
    onSecondary = TextPrimary,
    background = AviationDarkBackground,
    onBackground = TextPrimary,
    surface = AviationDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = AviationDarkCard,
    onSurfaceVariant = TextSecondary,
    outline = AviationDarkBorder
)

@Composable
fun UASReadyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
