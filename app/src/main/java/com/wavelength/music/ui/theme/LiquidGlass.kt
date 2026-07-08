package com.wavelength.music.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

/** Frosted-glass style for a glass surface (mini player, bottom nav, Now Playing control
 * clusters) — a real backdrop blur of whatever's behind, tinted per Liquid variant and given a
 * touch of noise so it reads as glass rather than a flat translucent panel.
 *
 * [HazeStyle.backgroundColor] is kept neutral (charcoal, not a saturated color) on purpose: it's
 * what devices without real-time blur support fall back to drawing as a flat scrim, so a
 * strongly-colored value here would make the "glass" look like a solid color card on those
 * devices instead of tinted glass. Only the tint (which sits on top of the real blur) carries
 * each variant's accent color. */
fun glassStyleFor(theme: AppTheme): HazeStyle = HazeStyle(
    backgroundColor = Color(0xFF1C1C1E),
    tint = HazeTint(theme.swatchColor().copy(alpha = 0.14f)),
    blurRadius = 22.dp,
    noiseFactor = 0.1f,
    fallbackTint = HazeTint(Color.White.copy(alpha = 0.22f))
)
