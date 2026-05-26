package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ElectricTeal, // Deep dark purple text over primary lavender
    secondary = SmoothPink, // Light lavender for accents
    onSecondary = ElectricTeal,
    tertiary = SmoothPink,
    background = CosmicBackground,
    onBackground = CrispWhite,
    surface = CosmicSurface,
    onSurface = CrispWhite,
    surfaceVariant = CosmicSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = Color(0xFFFF5252)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = ElectricTeal,
    onSecondary = Color.Black,
    tertiary = SmoothPink,
    background = PaleBackground,
    onBackground = NeutralDark,
    surface = PaleSurface,
    onSurface = NeutralDark,
    surfaceVariant = Color(0xFFEDF2F7),
    onSurfaceVariant = Color(0xFF718096),
    error = Color(0xFFD32F2F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Theme by default for gorgeous wallpaper previews!
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
