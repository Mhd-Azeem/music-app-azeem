package com.wavelength.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wavelength.music.data.repository.BuiltInWallpaper
import java.io.File

@Composable
fun AppBackground(
    hasCustomBackground: Boolean,
    customBackgroundFile: File,
    opacity: Float = 0.25f,
    builtInWallpaper: BuiltInWallpaper = BuiltInWallpaper.DEFAULT
) {
    if (hasCustomBackground && customBackgroundFile.exists()) {
        val context = LocalContext.current
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(customBackgroundFile)
                .memoryCacheKey("${customBackgroundFile.absolutePath}_${customBackgroundFile.lastModified()}")
                .diskCacheKey("${customBackgroundFile.absolutePath}_${customBackgroundFile.lastModified()}")
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(opacity),
            contentScale = ContentScale.Crop
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(opacity)
            .background(wallpaperBrush(builtInWallpaper))
    )

    if (builtInWallpaper == BuiltInWallpaper.DEFAULT) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(opacity * 0.72f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x6638BDF8),
                            Color(0x3322D3EE),
                            Color.Transparent
                        ),
                        radius = 920f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(opacity * 0.52f)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x334F46E5),
                            Color.Transparent,
                            Color(0x3322D3EE),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

fun wallpaperBrush(wallpaper: BuiltInWallpaper): Brush = when (wallpaper) {
    BuiltInWallpaper.DEFAULT -> Brush.linearGradient(
        listOf(
            Color(0xFF020617),
            Color(0xFF061329),
            Color(0xFF0A1F44),
            Color(0xFF052B3F),
            Color(0xFF020617)
        )
    )
    BuiltInWallpaper.AURORA -> Brush.linearGradient(
        listOf(Color(0xFF001B2E), Color(0xFF00C853), Color(0xFF00B8D4), Color(0xFF304FFE))
    )
    BuiltInWallpaper.NEON -> Brush.linearGradient(
        listOf(Color(0xFF050008), Color(0xFFFF00CC), Color(0xFF3333FF), Color(0xFF00F5D4))
    )
    BuiltInWallpaper.SUNSET_GLOW -> Brush.linearGradient(
        listOf(Color(0xFF2B1055), Color(0xFF7B2CBF), Color(0xFFFF4D6D), Color(0xFFFFB703))
    )
    BuiltInWallpaper.OCEAN_SHINE -> Brush.linearGradient(
        listOf(Color(0xFF001219), Color(0xFF005F73), Color(0xFF0A9396), Color(0xFF94D2BD))
    )
    BuiltInWallpaper.PURPLE_SHINE -> Brush.linearGradient(
        listOf(Color(0xFF10002B), Color(0xFF5A189A), Color(0xFF9D4EDD), Color(0xFFE0AAFF))
    )
}
