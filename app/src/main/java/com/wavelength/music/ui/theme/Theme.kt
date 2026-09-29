package com.wavelength.music.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wavelength.music.data.repository.VisualThemeMode

private fun schemeFor(theme: AppTheme, customAccent: Color? = null): ColorScheme = when (theme) {
    AppTheme.CLASSIC -> darkColorScheme(
        primary = customAccent ?: WavelengthGreen,
        secondary = customAccent?.copy(alpha = 0.78f) ?: WavelengthGreenLight,
        background = Color.Transparent,
        surface = WavelengthSurface,
        surfaceVariant = WavelengthSurfaceVariant,
        onBackground = WavelengthOnBackground,
        onSurface = WavelengthOnBackground,
        error = WavelengthError
    )
    AppTheme.MARVEL -> darkColorScheme(
        primary = customAccent ?: Color(0xFFED1D24),
        secondary = customAccent?.copy(alpha = 0.78f) ?: Color(0xFFFFD700),
        background = Color.Transparent,
        surface = Color(0xFF1A1414),
        surfaceVariant = Color(0xFF2A1E1E),
        onBackground = WavelengthOnBackground,
        onSurface = WavelengthOnBackground,
        error = WavelengthError
    )
    AppTheme.PINK -> darkColorScheme(
        primary = customAccent ?: Color(0xFFEC4899),
        secondary = customAccent?.copy(alpha = 0.78f) ?: Color(0xFFF9A8D4),
        background = Color.Transparent,
        surface = Color(0xFF1E1418),
        surfaceVariant = Color(0xFF2A1E24),
        onBackground = WavelengthOnBackground,
        onSurface = WavelengthOnBackground,
        error = WavelengthError
    )
    AppTheme.BLACK -> darkColorScheme(
        primary = customAccent ?: Color(0xFFE0E0E0),
        secondary = customAccent?.copy(alpha = 0.78f) ?: Color(0xFF9E9E9E),
        background = Color.Transparent,
        surface = Color(0xFF000000),
        surfaceVariant = Color(0xFF1A1A1A),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFFFFFFF),
        error = WavelengthError
    )
    AppTheme.GREY -> darkColorScheme(
        primary = customAccent ?: Color(0xFF9E9E9E),
        secondary = customAccent?.copy(alpha = 0.78f) ?: Color(0xFFBDBDBD),
        background = Color.Transparent,
        surface = Color(0xFF232323),
        surfaceVariant = Color(0xFF2E2E2E),
        onBackground = Color(0xFFF0F0F0),
        onSurface = Color(0xFFF0F0F0),
        error = WavelengthError
    )
    AppTheme.OCEAN -> darkColorScheme(
        primary = customAccent ?: Color(0xFF22D3EE),
        secondary = customAccent?.copy(alpha = 0.78f) ?: Color(0xFF0EA5E9),
        background = Color.Transparent,
        surface = Color(0xFF0F2027),
        surfaceVariant = Color(0xFF1B3A42),
        onBackground = WavelengthOnBackground,
        onSurface = WavelengthOnBackground,
        error = WavelengthError
    )
    AppTheme.SUNSET -> darkColorScheme(
        primary = customAccent ?: Color(0xFFFF7A45),
        secondary = customAccent?.copy(alpha = 0.78f) ?: Color(0xFFFFB84D),
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
    glassmorphismEnabled: Boolean = false,
    neomorphismEnabled: Boolean = false,
    amoledEnabled: Boolean = false,
    visualThemeMode: VisualThemeMode = VisualThemeMode.SOLID,
    animateTransitions: Boolean = true,
    content: @Composable () -> Unit
) {
    val baseScheme = schemeFor(theme, customLiquidAccent)
    val appScheme = if (glassmorphismEnabled) {
        baseScheme.copy(
            background = Color.Transparent,
            surface = Color(0xFF102238).copy(alpha = 0.72f),
            surfaceVariant = Color(0xFF17324D).copy(alpha = 0.66f),
            surfaceContainer = Color(0xFF102A42).copy(alpha = 0.64f),
            surfaceContainerLow = Color(0xFF0C2035).copy(alpha = 0.58f),
            surfaceContainerHigh = Color(0xFF1B3853).copy(alpha = 0.70f),
            outline = Color.White.copy(alpha = 0.34f),
            outlineVariant = Color.White.copy(alpha = 0.20f),
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color.White.copy(alpha = 0.78f)
        )
    } else if (neomorphismEnabled) {
        baseScheme.copy(
            background = Color.Transparent,
            surface = Color(0xFF222833),
            surfaceVariant = Color(0xFF2A313D),
            surfaceContainerLowest = Color(0xFF171C24),
            surfaceContainerLow = Color(0xFF1E242E),
            surfaceContainer = Color(0xFF252C37),
            surfaceContainerHigh = Color(0xFF2B3340),
            surfaceContainerHighest = Color(0xFF323B49),
            outline = Color(0xFF4B5667).copy(alpha = 0.52f),
            outlineVariant = Color.White.copy(alpha = 0.10f),
            onBackground = Color(0xFFF1F4F8),
            onSurface = Color(0xFFF1F4F8),
            onSurfaceVariant = Color(0xFFBFC8D6)
        )
    } else if (amoledEnabled) {
        baseScheme.copy(
            background = Color.Transparent,
            surface = Color.Black,
            surfaceVariant = Color(0xFF090909),
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainer = Color(0xFF050505),
            surfaceContainerHigh = Color(0xFF0B0B0B),
            surfaceContainerHighest = Color(0xFF111111),
            outline = Color.White.copy(alpha = 0.24f),
            outlineVariant = Color.White.copy(alpha = 0.10f),
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFCECECE)
        )
    } else if (visualThemeMode == VisualThemeMode.LIQUID) {
        baseScheme.copy(
            background = Color.Transparent,
            surface = Color(0xFF101827).copy(alpha = 0.84f),
            surfaceVariant = Color(0xFF17243A).copy(alpha = 0.80f),
            surfaceContainer = Color(0xFF132037).copy(alpha = 0.82f),
            surfaceContainerHigh = Color(0xFF1D2D48).copy(alpha = 0.86f),
            outline = baseScheme.primary.copy(alpha = 0.42f),
            outlineVariant = Color.White.copy(alpha = 0.16f)
        )
    } else if (visualThemeMode == VisualThemeMode.ALBUM_ADAPTIVE) {
        // Keep large surfaces neutral so the accent color never becomes a full-screen
        // tint over the selected wallpaper. The adaptive/accent color is reserved for
        // controls, highlights and outlines.
        baseScheme.copy(
            background = Color.Transparent,
            surface = Color(0xFF101820).copy(alpha = 0.58f),
            surfaceVariant = Color(0xFF17232D).copy(alpha = 0.64f),
            surfaceContainer = Color(0xFF121D26).copy(alpha = 0.60f),
            surfaceContainerHigh = Color(0xFF1B2934).copy(alpha = 0.68f),
            outline = baseScheme.primary.copy(alpha = 0.70f),
            outlineVariant = Color.White.copy(alpha = 0.18f)
        )
    } else if (visualThemeMode == VisualThemeMode.AURORA) {
        baseScheme.copy(
            background = Color.Transparent,
            surface = Color(0xFF12152D).copy(alpha = 0.88f),
            surfaceVariant = Color(0xFF242052).copy(alpha = 0.84f),
            surfaceContainer = Color(0xFF171B3A).copy(alpha = 0.86f),
            surfaceContainerHigh = Color(0xFF302665).copy(alpha = 0.86f),
            outline = Color(0xFF7DD3FC).copy(alpha = 0.54f),
            outlineVariant = Color(0xFFC084FC).copy(alpha = 0.30f)
        )
    } else {
        baseScheme
    }

    val visualShapes = when (visualThemeMode) {
        VisualThemeMode.SOLID -> Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(10.dp),
            large = RoundedCornerShape(14.dp),
            extraLarge = RoundedCornerShape(18.dp)
        )
        VisualThemeMode.LIQUID -> Shapes(
            extraSmall = RoundedCornerShape(14.dp),
            small = RoundedCornerShape(18.dp),
            medium = RoundedCornerShape(26.dp),
            large = RoundedCornerShape(34.dp),
            extraLarge = RoundedCornerShape(42.dp)
        )
        VisualThemeMode.GLASSMORPHISM -> Shapes(
            extraSmall = RoundedCornerShape(18.dp),
            small = RoundedCornerShape(22.dp),
            medium = RoundedCornerShape(30.dp),
            large = RoundedCornerShape(38.dp),
            extraLarge = RoundedCornerShape(48.dp)
        )
        VisualThemeMode.NEOMORPHISM -> Shapes(
            extraSmall = RoundedCornerShape(12.dp),
            small = RoundedCornerShape(18.dp),
            medium = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(30.dp),
            extraLarge = RoundedCornerShape(36.dp)
        )
        VisualThemeMode.AMOLED -> Shapes(
            extraSmall = RoundedCornerShape(0.dp),
            small = RoundedCornerShape(2.dp),
            medium = RoundedCornerShape(4.dp),
            large = RoundedCornerShape(6.dp),
            extraLarge = RoundedCornerShape(8.dp)
        )
        VisualThemeMode.ALBUM_ADAPTIVE -> Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(34.dp)
        )
        VisualThemeMode.AURORA -> Shapes(
            extraSmall = RoundedCornerShape(20.dp),
            small = RoundedCornerShape(26.dp),
            medium = RoundedCornerShape(34.dp),
            large = RoundedCornerShape(42.dp),
            extraLarge = RoundedCornerShape(52.dp)
        )
    }

    val duration = if (animateTransitions) 420 else 0
    val animatedScheme = appScheme.copy(
        primary = animateColorAsState(appScheme.primary, tween(duration), label = "themePrimary").value,
        secondary = animateColorAsState(appScheme.secondary, tween(duration), label = "themeSecondary").value,
        surface = animateColorAsState(appScheme.surface, tween(duration), label = "themeSurface").value,
        surfaceVariant = animateColorAsState(appScheme.surfaceVariant, tween(duration), label = "themeSurfaceVariant").value,
        surfaceContainer = animateColorAsState(appScheme.surfaceContainer, tween(duration), label = "themeSurfaceContainer").value,
        surfaceContainerHigh = animateColorAsState(appScheme.surfaceContainerHigh, tween(duration), label = "themeSurfaceContainerHigh").value,
        onSurface = animateColorAsState(appScheme.onSurface, tween(duration), label = "themeOnSurface").value,
        onSurfaceVariant = animateColorAsState(appScheme.onSurfaceVariant, tween(duration), label = "themeOnSurfaceVariant").value
    )

    MaterialTheme(
        colorScheme = animatedScheme,
        typography = WavelengthTypography,
        shapes = visualShapes,
        content = content
    )
}
