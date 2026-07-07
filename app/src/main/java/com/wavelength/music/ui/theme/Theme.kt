package com.wavelength.music.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun schemeFor(theme: AppTheme): ColorScheme = when (theme) {
    AppTheme.CLASSIC -> darkColorScheme(
        primary = WavelengthGreen,
        secondary = WavelengthGreenLight,
        background = Color.Transparent,
        surface = WavelengthSurface,
        surfaceVariant = WavelengthSurfaceVariant,
        onBackground = WavelengthOnBackground,
        onSurface = WavelengthOnBackground,
        error = WavelengthError
    )
    AppTheme.MARVEL -> darkColorScheme(
        primary = Color(0xFFED1D24),
        secondary = Color(0xFFFFD700),
        background = Color.Transparent,
        surface = Color(0xFF1A1414),
        surfaceVariant = Color(0xFF2A1E1E),
        onBackground = WavelengthOnBackground,
        onSurface = WavelengthOnBackground,
        error = WavelengthError
    )
    AppTheme.PINK -> darkColorScheme(
        primary = Color(0xFFEC4899),
        secondary = Color(0xFFF9A8D4),
        background = Color.Transparent,
        surface = Color(0xFF1E1418),
        surfaceVariant = Color(0xFF2A1E24),
        onBackground = WavelengthOnBackground,
        onSurface = WavelengthOnBackground,
        error = WavelengthError
    )
    AppTheme.BLACK -> darkColorScheme(
        primary = Color(0xFFE0E0E0),
        secondary = Color(0xFF9E9E9E),
        background = Color.Transparent,
        surface = Color(0xFF000000),
        surfaceVariant = Color(0xFF1A1A1A),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFFFFFFF),
        error = WavelengthError
    )
    AppTheme.GREY -> darkColorScheme(
        primary = Color(0xFF9E9E9E),
        secondary = Color(0xFFBDBDBD),
        background = Color.Transparent,
        surface = Color(0xFF232323),
        surfaceVariant = Color(0xFF2E2E2E),
        onBackground = Color(0xFFF0F0F0),
        onSurface = Color(0xFFF0F0F0),
        error = WavelengthError
    )
}

/** The primary swatch color shown for each theme option in Settings. */
fun AppTheme.swatchColor(): Color = when (this) {
    AppTheme.CLASSIC -> WavelengthGreen
    AppTheme.MARVEL -> Color(0xFFED1D24)
    AppTheme.PINK -> Color(0xFFEC4899)
    AppTheme.BLACK -> Color(0xFFE0E0E0)
    AppTheme.GREY -> Color(0xFF9E9E9E)
}

// Wavelength always renders in dark mode; only the accent palette changes between themes.
// Every scheme's background is transparent so the app-wide background photo (drawn once, behind
// WavelengthNavHost in MainActivity) shows through every screen's Scaffold.
@Composable
fun WavelengthTheme(theme: AppTheme = AppTheme.CLASSIC, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = schemeFor(theme),
        typography = WavelengthTypography,
        content = content
    )
}
