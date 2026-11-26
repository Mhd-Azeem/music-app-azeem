package com.wavelength.music.ui.radio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wavelength.music.data.repository.VisualThemeMode

internal data class ExperienceHeaderStyle(
    val brush: Brush,
    val shape: RoundedCornerShape,
    val border: Color,
    val elevation: Dp
)

@Composable
internal fun experienceHeaderStyle(mode: VisualThemeMode): ExperienceHeaderStyle {
    val headerBrush = when (mode) {
        VisualThemeMode.SOLID -> Brush.linearGradient(
            listOf(Color(0xFF181818), Color(0xFF101010))
        )
        VisualThemeMode.LIQUID -> Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.58f),
                Color(0xCC29436B),
                Color(0xAA142033)
            )
        )
        VisualThemeMode.GLASSMORPHISM -> Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.20f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.26f),
                Color(0x66152D48)
            )
        )
        VisualThemeMode.NEOMORPHISM -> Brush.linearGradient(
            listOf(Color(0xFF38414F), Color(0xFF272E39), Color(0xFF1A1F27))
        )
        VisualThemeMode.AMOLED -> Brush.linearGradient(
            listOf(Color.Black, Color(0xFF050505))
        )
        VisualThemeMode.ALBUM_ADAPTIVE -> Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                Color(0xCC090C12)
            )
        )
        VisualThemeMode.AURORA -> Brush.linearGradient(
            listOf(
                Color(0xDD4338CA),
                Color(0xDD7E22CE),
                Color(0xBB0891B2)
            )
        )
    }

    val headerRadius = when (mode) {
        VisualThemeMode.SOLID -> 10.dp
        VisualThemeMode.LIQUID -> 28.dp
        VisualThemeMode.GLASSMORPHISM -> 32.dp
        VisualThemeMode.NEOMORPHISM -> 20.dp
        VisualThemeMode.AMOLED -> 3.dp
        VisualThemeMode.ALBUM_ADAPTIVE -> 22.dp
        VisualThemeMode.AURORA -> 34.dp
    }
    val headerShape = RoundedCornerShape(headerRadius)
    val headerBorder = when (mode) {
        VisualThemeMode.SOLID -> MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
        VisualThemeMode.LIQUID -> Color.White.copy(alpha = 0.34f)
        VisualThemeMode.GLASSMORPHISM -> Color.White.copy(alpha = 0.60f)
        VisualThemeMode.NEOMORPHISM -> Color.White.copy(alpha = 0.10f)
        VisualThemeMode.AMOLED -> Color.White.copy(alpha = 0.22f)
        VisualThemeMode.ALBUM_ADAPTIVE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.96f)
        VisualThemeMode.AURORA -> Color.White.copy(alpha = 0.72f)
    }
    val headerElevation = when (mode) {
        VisualThemeMode.AMOLED -> 0.dp
        VisualThemeMode.SOLID -> 4.dp
        VisualThemeMode.NEOMORPHISM -> 18.dp
        VisualThemeMode.AURORA -> 20.dp
        VisualThemeMode.ALBUM_ADAPTIVE -> 14.dp
        else -> 12.dp
    }
    return ExperienceHeaderStyle(headerBrush, headerShape, headerBorder, headerElevation)
}

@Composable
internal fun StyledExperienceSwitcher(
    mode: VisualThemeMode,
    onSwitch: () -> Unit,
    radioSelected: Boolean = false,
    style: ExperienceHeaderStyle = experienceHeaderStyle(mode)
) {
    Box(
        modifier = Modifier
            .shadow(
                style.elevation,
                style.shape,
                ambientColor = if (mode == VisualThemeMode.AURORA) Color(0xFF7C3AED) else Color.Black,
                spotColor = if (mode == VisualThemeMode.ALBUM_ADAPTIVE) MaterialTheme.colorScheme.primary else Color.Black
            )
            .clip(style.shape)
            .background(style.brush)
            .border(
                if (mode == VisualThemeMode.AMOLED) 1.dp else 1.3.dp,
                style.border,
                style.shape
            )
            .padding(
                horizontal = when (mode) {
                    VisualThemeMode.AMOLED, VisualThemeMode.SOLID -> 14.dp
                    VisualThemeMode.AURORA -> 20.dp
                    else -> 18.dp
                },
                vertical = when (mode) {
                    VisualThemeMode.AMOLED -> 9.dp
                    else -> 11.dp
                }
            )
    ) {
        ExperienceSwitcher(onSwitch = onSwitch, radioSelected = radioSelected, tint = Color.White)
    }

}
