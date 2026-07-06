package com.wavelength.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WavelengthDarkColorScheme = darkColorScheme(
    primary = WavelengthGreen,
    secondary = WavelengthGreenLight,
    // Transparent so the app-wide background photo (drawn once, behind WavelengthNavHost in
    // MainActivity) shows through every screen's Scaffold instead of being painted over.
    background = Color.Transparent,
    surface = WavelengthSurface,
    surfaceVariant = WavelengthSurfaceVariant,
    onBackground = WavelengthOnBackground,
    onSurface = WavelengthOnBackground,
    error = WavelengthError
)

// Wavelength always renders in dark theme, regardless of system setting.
@Composable
fun WavelengthTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WavelengthDarkColorScheme,
        typography = WavelengthTypography,
        content = content
    )
}
