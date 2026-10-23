package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun buildDarkColorScheme(accent: AccentColor) = darkColorScheme(
    primary = accent.primaryDark,
    onPrimary = accent.onPrimaryDark,
    primaryContainer = accent.primaryContainerDark,
    onPrimaryContainer = accent.onPrimaryContainerDark,
    secondary = EmeraldAccent,
    onSecondary = Color(0xFF003822),
    secondaryContainer = Color(0xFF005234),
    onSecondaryContainer = Color(0xFF6CF8B8),
    tertiary = AmberWarning,
    onTertiary = Color(0xFF452B00),
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkOutline,
    error = RedAlert,
    onError = Color.White
)

fun buildLightColorScheme(accent: AccentColor) = lightColorScheme(
    primary = accent.primaryLight,
    onPrimary = accent.onPrimaryLight,
    primaryContainer = accent.primaryContainerLight,
    onPrimaryContainer = accent.onPrimaryContainerLight,
    secondary = EmeraldPrimaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB1F7D1),
    onSecondaryContainer = Color(0xFF002113),
    tertiary = AmberWarning,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightOutline,
    error = RedAlert,
    onError = Color.White
)

@Composable
fun IoTSentinelTheme(
    darkTheme: Boolean = true, // Default to dark theme as requested in spec
    accentColor: AccentColor = AccentColor.CYAN, // Default to Cian Tecnológico
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        buildDarkColorScheme(accentColor)
    } else {
        buildLightColorScheme(accentColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
