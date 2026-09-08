package com.wavelength.music.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun schemeFor(theme: AppTheme, customAccent: Color? = null): ColorScheme = when (theme) {
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
    AppTheme.OCEAN -> darkColorScheme(
        primary = Color(0xFF22D3EE),
        secondary = Color(0xFF0EA5E9),
        background = Color.Transparent,
        surface = Color(0xFF0F2027),
        surfaceVariant = Color(0xFF1B3A42),
        onBackground = WavelengthOnBackground,
        onSurface = WavelengthOnBackground,
        error = WavelengthError
    )
    AppTheme.SUNSET -> darkColorScheme(
        primary = Color(0xFFFF7A45),
        secondary = Color(0xFFFFB84D),
        background = Color.Transparent,
        surface = Color(0xFF2A1810),
        surfaceVariant = Color(0xFF3D2418),
        onBackground = WavelengthOnBackground,
        onSurface = WavelengthOnBackground,
        error = WavelengthError
    )
    AppTheme.LIQUID -> darkColorScheme(
        primary = customAccent ?: Color(0xFF7DD3FC),
        secondary = customAccent?.copy(alpha = 0.75f) ?: Color(0xFFC4B5FD),
        background = Color.Transparent,
        surface = Color(0xFF1C1C1E),
        surfaceVariant = Color(0xFF2A2A2C),
        onBackground = Color(0xFFF5F5F7),
        onSurface = Color(0xFFF5F5F7),
        error = WavelengthError
    )
    AppTheme.LIQUID_PURPLE -> darkColorScheme(
        primary = customAccent ?: Color(0xFFC084FC),
        secondary = customAccent?.copy(alpha = 0.75f) ?: Color(0xFFF0ABFC),
        background = Color.Transparent,
        surface = Color(0xFF1C1C1E),
        surfaceVariant = Color(0xFF2A2A2C),
        onBackground = Color(0xFFF5F5F7),
        onSurface = Color(0xFFF5F5F7),
        error = WavelengthError
    )
    AppTheme.LIQUID_ROSE -> darkColorScheme(
        primary = customAccent ?: Color(0xFFFB7185),
        secondary = customAccent?.copy(alpha = 0.75f) ?: Color(0xFFFDA4AF),
        background = Color.Transparent,
        surface = Color(0xFF1C1C1E),
        surfaceVariant = Color(0xFF2A2A2C),
        onBackground = Color(0xFFF5F5F7),
        onSurface = Color(0xFFF5F5F7),
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
    AppTheme.OCEAN -> Color(0xFF22D3EE)
    AppTheme.SUNSET -> Color(0xFFFF7A45)
    AppTheme.LIQUID -> Color(0xFF7DD3FC)
    AppTheme.LIQUID_PURPLE -> Color(0xFFC084FC)
    AppTheme.LIQUID_ROSE -> Color(0xFFFB7185)
}

// Wavelength always renders in dark mode; only the accent palette changes between themes.
// Every scheme's background is transparent so the app-wide background photo (drawn once, behind
// WavelengthNavHost in MainActivity) shows through every screen's Scaffold.
@Composable
fun WavelengthTheme(
    theme: AppTheme = AppTheme.CLASSIC,
    customLiquidAccent: Color? = null,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = schemeFor(theme, if (theme.isGlass) customLiquidAccent else null),
        typography = WavelengthTypography,
        content = content
    )
}
