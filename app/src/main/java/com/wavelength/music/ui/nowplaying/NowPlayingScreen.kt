package com.wavelength.music.ui.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.wavelength.music.playback.RepeatMode
import com.wavelength.music.ui.components.TrackOptionsSheet
import com.wavelength.music.ui.components.TrackRow
import com.wavelength.music.ui.playlist.AddToPlaylistDialog
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalHazeApi::class)
@Composable
fun NowPlayingScreen(
    onCollapse: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
    isLiquid: Boolean = false,
    glassStyle: HazeStyle = HazeStyle.Unspecified,
    expandUpNextOnScroll: Boolean = false
) {
    val hazeState = remember { HazeState() }
    val pillShape = RoundedCornerShape(28.dp)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isCurrentFavorite.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val sleepTimerRemaining by viewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()
    val sleepTimerEndOfTrack by viewModel.sleepTimerIsEndOfTrack.collectAsStateWithLifecycle()
    val track = state.currentTrack
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var menuQueueIndex by remember { mutableStateOf<Int?>(null) }

    if (showAddToPlaylist) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { showAddToPlaylist = false },
            onSelect = viewModel::addCurrentTrackToPlaylist,
            onCreateNew = viewModel::createPlaylistWithCurrentTrack
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            remainingMs = sleepTimerRemaining,
            isEndOfTrack = sleepTimerEndOfTrack,
            onDismiss = { showSleepTimerDialog = false },
            onSelectMinutes = { minutes ->
                viewModel.startSleepTimer(minutes)
                showSleepTimerDialog = false
            },
            onSelectEndOfTrack = {
                viewModel.startSleepTimerEndOfTrack()
                showSleepTimerDialog = false
            },
            onCancelTimer = {
                viewModel.cancelSleepTimer()
                showSleepTimerDialog = false
            }
        )
    }

    val menuTrack = menuQueueIndex?.let { state.queue.getOrNull(it) }
    if (menuTrack != null) {
        val queueIndex = menuQueueIndex!!
        TrackOptionsSheet(
            track = menuTrack,
            onDismiss = { menuQueueIndex = null },
            onMoveUp = if (queueIndex > state.currentIndex + 1) {
                { viewModel.moveQueueItem(queueIndex, queueIndex - 1) }
            } else null,
            onMoveDown = if (queueIndex < state.queue.lastIndex) {
                { viewModel.moveQueueItem(queueIndex, queueIndex + 1) }
            } else null
        )
    }

    val density = LocalDensity.current
    val collapseThresholdPx = with(density) { 80.dp.toPx() }

    var dragOffset by remember { mutableFloatStateOf(0f) }
    val settleAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val upNextListState = rememberLazyListState()
    val upNextScrolled by remember {
        derivedStateOf {
            upNextListState.firstVisibleItemIndex > 0 || upNextListState.firstVisibleItemScrollOffset > 0
        }
    }
    // Album art hides to free up room for the queue to actually grow into — without this, "half
    // the screen" has nowhere to expand into since art + controls already fill most of the
    // screen, so the box would just get clipped off the bottom with no visible size change.
    val isUpNextExpanded = expandUpNextOnScroll && upNextScrolled

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = dragOffset
                alpha = 1f - (dragOffset / 900f).coerceIn(0f, 0.5f)
            }
    ) {
        if (isLiquid) {
            AsyncImage(
                model = track?.albumArtUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.4f).haze(state = hazeState),
                contentScale = ContentScale.Crop
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragOffset > collapseThresholdPx) {
                                    dragOffset = 0f
                                    onCollapse()
                                } else {
                                    scope.launch {
                                        settleAnim.snapTo(dragOffset)
                                        settleAnim.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) { dragOffset = value }
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    settleAnim.snapTo(dragOffset)
                                    settleAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    ) { dragOffset = value }
                                }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                        }
                    }
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse")
                }

                AnimatedVisibility(visible = !isUpNextExpanded) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                        AsyncImage(
                            model = track?.albumArtUrl,
                            contentDescription = track?.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track?.name.orEmpty(),
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track?.artistName.orEmpty(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    var topIconRowModifier: Modifier = Modifier
                    if (isLiquid) {
                        topIconRowModifier = topIconRowModifier
                            .clip(pillShape)
                            .hazeChild(state = hazeState, style = glassStyle) { inputScale = HazeInputScale.Auto }
                            .border(1.dp, Color.White.copy(alpha = 0.25f), pillShape)
                    }
                    Row(modifier = topIconRowModifier) {
                        IconButton(onClick = { showSleepTimerDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = "Sleep timer",
                                tint = if (sleepTimerRemaining != null || sleepTimerEndOfTrack) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.White
                                }
                            )
                        }
                        IconButton(onClick = { showAddToPlaylist = true }) {
                            Icon(
                                imageVector = Icons.Filled.PlaylistAdd,
                                contentDescription = "Add to playlist",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Slider(
                    value = state.positionMs.toFloat().coerceIn(0f, state.durationMs.toFloat().coerceAtLeast(1f)),
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatMillis(state.positionMs), style = MaterialTheme.typography.labelSmall)
                    Text(formatMillis(state.durationMs), style = MaterialTheme.typography.labelSmall)
                }
            }

            var transportRowModifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            if (isLiquid) {
                transportRowModifier = transportRowModifier
                    .clip(pillShape)
                    .hazeChild(state = hazeState, style = glassStyle) { inputScale = HazeInputScale.Auto }
                    .border(1.dp, Color.White.copy(alpha = 0.25f), pillShape)
            }
            Row(
                modifier = transportRowModifier,
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::toggleShuffle) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
                IconButton(onClick = viewModel::skipPrevious) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.padding(4.dp))
                }
                IconButton(onClick = viewModel::playPause, modifier = Modifier.padding(8.dp)) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.padding(4.dp)
                    )
                }
                IconButton(onClick = viewModel::skipNext) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                }
                IconButton(onClick = viewModel::cycleRepeatMode) {
                    Icon(
                        imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            }

            var volumeRowModifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            if (isLiquid) {
                volumeRowModifier = volumeRowModifier
                    .clip(pillShape)
                    .hazeChild(state = hazeState, style = glassStyle) { inputScale = HazeInputScale.Auto }
                    .border(1.dp, Color.White.copy(alpha = 0.25f), pillShape)
                    .padding(horizontal = 8.dp)
            }
            Row(
                modifier = volumeRowModifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        state.volume <= 0f -> Icons.Filled.VolumeOff
                        state.volume < 0.5f -> Icons.Filled.VolumeDown
                        else -> Icons.Filled.VolumeUp
                    },
                    contentDescription = "Volume",
                    tint = Color.White
                )
                Slider(
                    value = state.volume,
                    onValueChange = { viewModel.setVolume(it) },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            if (track == null) {
                Text(
                    text = "Nothing playing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                )
            }

            val upcoming = state.queue.drop(state.currentIndex + 1)
            if (upcoming.isNotEmpty()) {
                Text(
                    text = "Up next",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                val upNextHeight by animateDpAsState(
                    targetValue = if (isUpNextExpanded) screenHeight / 2 else 220.dp,
                    label = "upNextHeight"
                )
                val upNextModifier = if (expandUpNextOnScroll) {
                    Modifier.height(upNextHeight)
                } else {
                    Modifier.weight(1f)
                }
                LazyColumn(modifier = upNextModifier, state = upNextListState) {
                    itemsIndexed(upcoming) { offset, upcomingTrack ->
                        val queueIndex = state.currentIndex + 1 + offset
                        TrackRow(
                            track = upcomingTrack,
                            onClick = { viewModel.playQueueItem(queueIndex) },
                            onMoreClick = { menuQueueIndex = queueIndex }
                        )
                    }
                }
            }
        }
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis.coerceAtLeast(0))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
