package com.uasready.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
enum class AppThemeMode(val displayName: String, val description: String) {
    DARK("Dark / Night Mode", "High-contrast dark palette for night and low-light operations"),
    LIGHT("Light / Day Mode", "High-visibility bright palette for direct sunlight outdoor readability"),
    AUTO("Auto / System Theme", "Automatically matches your device's system theme")
}

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

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0969DA),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDF4FF),
    onPrimaryContainer = Color(0xFF0969DA),
    secondary = Color(0xFF1A7F37),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF1F2328),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2328),
    surfaceVariant = Color(0xFFEAEEF2),
    onSurfaceVariant = Color(0xFF57606A),
    outline = Color(0xFFD0D7DE)
)

@Composable
fun UASReadyTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.AUTO -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
