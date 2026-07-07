package com.wavelength.music.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

/** Frosted-glass style used by every glass surface in the Liquid theme (mini player, bottom nav,
 * Now Playing control clusters) — a real backdrop blur of whatever's behind, tinted and given a
 * touch of noise so it reads as glass rather than a flat translucent panel. */
val LiquidGlassStyle = HazeStyle(
    backgroundColor = Color(0xFF0B1220),
    tint = HazeTint(Color.White.copy(alpha = 0.10f)),
    blurRadius = 24.dp,
    noiseFactor = 0.12f
)
