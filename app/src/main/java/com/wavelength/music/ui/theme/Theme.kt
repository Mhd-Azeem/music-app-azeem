package com.wavelength.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val WavelengthDarkColorScheme = darkColorScheme(
    primary = WavelengthGreen,
    secondary = WavelengthGreenLight,
    background = WavelengthBackground,
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
