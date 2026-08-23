package com.uasready.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
enum class AppThemeMode(val displayName: String, val description: String) {
    AUTO("System Default (Auto)", "Automatically matches device day/night system setting"),
    DARK("Dark / Night Mode", "High-contrast dark palette for night and low-light operations"),
    LIGHT("Light / Day Mode", "High-visibility bright palette for direct sunlight outdoor readability")
}

private val DarkMaterialColorScheme = darkColorScheme(
    primary = Color(0xFF388BFD),
    onPrimary = Color(0xFFF0F6FC),
    primaryContainer = Color(0xFF1A222D),
    onPrimaryContainer = Color(0xFF58A6FF),
    secondary = Color(0xFF3FB950),
    onSecondary = Color(0xFFF0F6FC),
    background = Color(0xFF0A0E14),
    onBackground = Color(0xFFF0F6FC),
    surface = Color(0xFF121820),
    onSurface = Color(0xFFF0F6FC),
    surfaceVariant = Color(0xFF1A222D),
    onSurfaceVariant = Color(0xFF8B949E),
    outline = Color(0xFF2B3644)
)

private val LightMaterialColorScheme = lightColorScheme(
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
    themeMode: AppThemeMode = AppThemeMode.AUTO,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.AUTO -> isSystemInDarkTheme()
    }

    val aviationColors = if (isDark) DarkAviationColors else LightAviationColors
    val materialColorScheme = if (isDark) DarkMaterialColorScheme else LightMaterialColorScheme

    CompositionLocalProvider(
        LocalAviationColors provides aviationColors
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography,
            content = content
        )
    }
}
