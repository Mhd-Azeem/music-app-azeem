package com.wavelength.music.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wavelength.music.playback.PlaybackUiState
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

@OptIn(ExperimentalHazeApi::class)
@Composable
fun MiniPlayerBar(
    state: PlaybackUiState,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    glassStyle: HazeStyle = HazeStyle.Unspecified,
    trackTransitionEnabled: Boolean = true,
    trackTransitionDurationMs: Int = 300
) {
    val track = state.currentTrack ?: return
    val trackTransitionSpec: FiniteAnimationSpec<Float> = if (trackTransitionEnabled) {
        tween(trackTransitionDurationMs)
    } else {
        snap()
    }
    val density = LocalDensity.current
    val expandThresholdPx = with(density) { 40.dp.toPx() }
    val skipThresholdPx = with(density) { 56.dp.toPx() }
    val isLiquid = hazeState != null
    val shape = RoundedCornerShape(28.dp)

    var barModifier = modifier.fillMaxWidth()
    if (isLiquid) {
        barModifier = barModifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(64.dp)
            .clip(shape)
            .hazeChild(state = hazeState!!, style = glassStyle) { inputScale = HazeInputScale.Auto }
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
    } else {
        barModifier = barModifier.height(64.dp)
    }
    barModifier = barModifier
        .clickable(onClick = onClick)
        .swipeVertical(thresholdPx = expandThresholdPx, onSwipeUp = onClick)
        .swipeHorizontal(thresholdPx = skipThresholdPx, onSwipeLeft = onSkipNext, onSwipeRight = onSkipPrevious)

    Surface(
        modifier = barModifier,
        color = if (isLiquid) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (isLiquid) 0.dp else 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Crossfade(targetState = track, animationSpec = trackTransitionSpec, label = "miniPlayerArt") { t ->
                AsyncImage(
                    model = t.albumArtUrl,
                    contentDescription = t.name,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
            Crossfade(
                targetState = track,
                animationSpec = trackTransitionSpec,
                label = "miniPlayerInfo",
                modifier = Modifier.weight(1f)
            ) { t ->
                Column {
                    Text(
                        text = t.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = t.artistName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (state.isBuffering) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                }
            }
            IconButton(onClick = onSkipNext) {
                Icon(imageVector = Icons.Filled.SkipNext, contentDescription = null)
            }
        }
    }
}
