package com.wavelength.music.ui.nowplaying

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil.Coil
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.wavelength.music.data.model.LyricLine
import com.wavelength.music.playback.RepeatMode
import com.wavelength.music.ui.components.EmptyView
import com.wavelength.music.ui.components.LoadingView
import com.wavelength.music.ui.components.ScreenState
import com.wavelength.music.ui.components.TrackOptionsSheet
import com.wavelength.music.ui.components.TrackRow
import com.wavelength.music.ui.components.dragDropItemOffset
import com.wavelength.music.ui.components.dragToReorder
import com.wavelength.music.ui.components.rememberDragDropListState
import com.wavelength.music.ui.playlist.AddToPlaylistDialog
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalHazeApi::class)
@Composable
fun NowPlayingScreen(
    onCollapse: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
    isLiquid: Boolean = false,
    glassStyle: HazeStyle = HazeStyle.Unspecified,
    expandUpNextOnScroll: Boolean = false,
    dynamicThemeFromAlbumArt: Boolean = false,
    vinylStyleAlbumArt: Boolean = false,
    audioVisualizerEnabled: Boolean = false
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
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var menuQueueIndex by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current
    var dynamicAccent by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(track?.albumArtUrl, dynamicThemeFromAlbumArt) {
        dynamicAccent = if (dynamicThemeFromAlbumArt) {
            loadDominantColor(context, track?.albumArtUrl)
        } else {
            null
        }
    }
    val accentColor = dynamicAccent ?: MaterialTheme.colorScheme.primary

    // Driven manually (not rememberInfiniteTransition) so pausing genuinely stops the clock rather
    // than just freezing what's displayed: cancelling this coroutine leaves the Animatable sitting
    // at its exact current value, so resuming continues smoothly from there with no jump. Gated
    // behind vinylStyleAlbumArt so it costs nothing for the (default) users who don't enable it,
    // and .value is only read inside the graphicsLayer block below rather than into a composition-
    // level val, so a spinning disc redraws its own layer each frame instead of recomposing the
    // whole screen.
    val vinylAngle = remember { Animatable(0f) }
    LaunchedEffect(vinylStyleAlbumArt, state.isPlaying) {
        if (vinylStyleAlbumArt && state.isPlaying) {
            while (true) {
                vinylAngle.animateTo(
                    targetValue = vinylAngle.value + 360f,
                    animationSpec = tween(durationMillis = 6000, easing = LinearEasing)
                )
            }
        }
    }

    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasRecordAudioPermission = granted }
    LaunchedEffect(audioVisualizerEnabled, hasRecordAudioPermission) {
        if (audioVisualizerEnabled && !hasRecordAudioPermission) {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val visualizerWaveform by viewModel.visualizerWaveform.collectAsStateWithLifecycle()
    DisposableEffect(audioVisualizerEnabled, hasRecordAudioPermission) {
        viewModel.setVisualizerCaptureEnabled(audioVisualizerEnabled && hasRecordAudioPermission)
        onDispose { viewModel.setVisualizerCaptureEnabled(false) }
    }

    val lyricsState by viewModel.lyrics.collectAsStateWithLifecycle()
    LaunchedEffect(track?.id, showLyrics) {
        if (showLyrics) viewModel.loadLyrics()
    }

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

    val menuTrack = menuQueueIndex?.let { state.queue.getOrNull(it)?.track }
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
    // Once the queue is short enough to fully fit at its expanded height, growing it snaps the
    // scroll offset straight back to 0 on its own (LazyColumn's remeasure, not a real scroll) —
    // which would immediately flip upNextScrolled back to false and shrink it again, causing an
    // expand/collapse flicker. Latching it keeps the queue expanded once triggered. To still let
    // the user collapse it by hand, a NestedScrollConnection watches for the list settling at the
    // top as a result of an *actual* scroll delta passing through it — nested scroll callbacks
    // never fire for the layout-driven remeasure reset, only for real drag/fling consumption.
    // But that alone isn't enough: once the queue is pinned at the top with nothing left to
    // scroll, every further delta shows up here too, including a drag continuing in the same
    // "reveal more of the queue" direction that caused the expansion in the first place — so the
    // delta's direction is checked too, and only a delta trying to move back *toward* the top
    // counts as the user asking to collapse it.
    var upNextExpandedLatch by remember { mutableStateOf(false) }
    val upNextNestedScrollConnection = remember(upNextListState) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val towardTop = consumed.y + available.y > 0f
                if (towardTop &&
                    upNextListState.firstVisibleItemIndex == 0 &&
                    upNextListState.firstVisibleItemScrollOffset == 0
                ) {
                    upNextExpandedLatch = false
                }
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(upNextScrolled) {
        if (upNextScrolled) upNextExpandedLatch = true
    }
    LaunchedEffect(track?.id) {
        upNextExpandedLatch = false
    }
    // Album art hides to free up room for the queue to actually grow into — without this, "half
    // the screen" has nowhere to expand into since art + controls already fill most of the
    // screen, so the box would just get clipped off the bottom with no visible size change.
    val isUpNextExpanded = expandUpNextOnScroll && upNextExpandedLatch

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
                        if (vinylStyleAlbumArt) {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(20.dp)) {
                                AsyncImage(
                                    model = track?.albumArtUrl,
                                    contentDescription = track?.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { rotationZ = vinylAngle.value }
                                        .clip(CircleShape)
                                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.background)
                                )
                            }
                        } else {
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
                    Row(modifier = topIconRowModifier, verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            Text(
                                text = speedLabel(state.playbackSpeed),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (state.playbackSpeed != 1f) accentColor else Color.White,
                                modifier = Modifier
                                    .clickable { showSpeedMenu = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                            DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                                PLAYBACK_SPEEDS.forEach { speed ->
                                    DropdownMenuItem(
                                        text = { Text(speedLabel(speed)) },
                                        onClick = {
                                            viewModel.setPlaybackSpeed(speed)
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { showSleepTimerDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = "Sleep timer",
                                tint = if (sleepTimerRemaining != null || sleepTimerEndOfTrack) {
                                    accentColor
                                } else {
                                    Color.White
                                }
                            )
                        }
                        IconButton(onClick = { showLyrics = true }) {
                            Icon(
                                imageVector = Icons.Filled.Subject,
                                contentDescription = "Lyrics",
                                tint = Color.White
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
                                tint = if (isFavorite) accentColor else Color.White
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
                        thumbColor = accentColor,
                        activeTrackColor = accentColor
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
                        tint = if (state.shuffleEnabled) accentColor else Color.White
                    )
                }
                IconButton(onClick = viewModel::skipPrevious) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.padding(4.dp))
                }
                if (state.isBuffering) {
                    Box(modifier = Modifier.padding(8.dp).padding(4.dp).size(24.dp)) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.fillMaxSize())
                    }
                } else {
                    IconButton(onClick = viewModel::playPause, modifier = Modifier.padding(8.dp)) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
                IconButton(onClick = viewModel::skipNext) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                }
                IconButton(onClick = viewModel::cycleRepeatMode) {
                    Icon(
                        imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != RepeatMode.OFF) accentColor else Color.White
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
                        thumbColor = accentColor,
                        activeTrackColor = accentColor
                    )
                )
            }

            if (audioVisualizerEnabled && hasRecordAudioPermission) {
                AudioVisualizer(
                    waveform = visualizerWaveform,
                    color = accentColor,
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 12.dp)
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
                    Modifier.height(upNextHeight).nestedScroll(upNextNestedScrollConnection)
                } else {
                    Modifier.weight(1f)
                }
                val dragDropState = rememberDragDropListState(upNextListState) { from, to ->
                    viewModel.moveQueueItem(state.currentIndex + 1 + from, state.currentIndex + 1 + to)
                }
                LazyColumn(
                    modifier = upNextModifier.dragToReorder(dragDropState),
                    state = upNextListState
                ) {
                    itemsIndexed(upcoming, key = { _, entry -> entry.instanceId }) { offset, entry ->
                        val queueIndex = state.currentIndex + 1 + offset
                        val isDragging = dragDropState.draggingItemIndex == offset
                        TrackRow(
                            track = entry.track,
                            onClick = { viewModel.playQueueItem(queueIndex) },
                            onMoreClick = { menuQueueIndex = queueIndex },
                            modifier = Modifier
                                .dragDropItemOffset(dragDropState, offset)
                                .zIndex(if (isDragging) 1f else 0f)
                        )
                    }
                }
            }
        }

        if (showLyrics) {
            LyricsOverlay(
                lyricsState = lyricsState,
                positionMs = state.positionMs,
                albumArtUrl = track?.albumArtUrl,
                onClose = { showLyrics = false }
            )
        }
    }
}

private val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

private fun speedLabel(speed: Float): String {
    val trimmed = if (speed == speed.toLong().toFloat()) speed.toLong().toString() else speed.toString()
    return "${trimmed}x"
}

/** Downloads the album art and picks a vibrant swatch via the Palette API, falling back to the
 * theme's primary color when disabled or the art can't be analyzed (missing art, network
 * failure, decode error). `allowHardware(false)` is required so Palette can read the bitmap's
 * pixels directly. */
private suspend fun loadDominantColor(context: android.content.Context, url: String?): Color? {
    if (url.isNullOrBlank()) return null
    return withContext(Dispatchers.IO) {
        runCatching {
            val loader = Coil.imageLoader(context)
            val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
            val bitmap = ((loader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
                ?: return@runCatching null
            val palette = Palette.from(bitmap).generate()
            val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
            swatch?.rgb?.let { Color(it) }
        }.getOrNull()
    }
}

/** Full-screen overlay showing synced lyrics (from lrclib.net, best-effort — many tracks won't
 * have a match), auto-scrolling to and highlighting whichever line's timestamp has most recently
 * passed. The backdrop is the current album art, heavily blurred and darkened, so the overlay
 * reads as a continuation of the player rather than a flat, disconnected screen. */
@Composable
private fun LyricsOverlay(
    lyricsState: ScreenState<List<LyricLine>>,
    positionMs: Long,
    albumArtUrl: String?,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = albumArtUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(40.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Lyrics", style = MaterialTheme.typography.titleMedium, color = Color.White)
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close lyrics", tint = Color.White)
                }
            }
            when (lyricsState) {
                is ScreenState.Loading -> LoadingView(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> EmptyView(modifier = Modifier.fillMaxSize(), message = lyricsState.message)
                is ScreenState.Empty -> EmptyView(
                    modifier = Modifier.fillMaxSize(),
                    message = "No synced lyrics found for this track"
                )
                is ScreenState.Success -> {
                    val lines = lyricsState.data
                    val listState = rememberLazyListState()
                    val activeIndex = lines.indexOfLast { it.timestampMs <= positionMs }.coerceAtLeast(0)
                    LaunchedEffect(activeIndex) {
                        listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
                    }
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(lines) { index, line ->
                            Text(
                                text = line.text,
                                style = if (index == activeIndex) {
                                    MaterialTheme.typography.titleMedium
                                } else {
                                    MaterialTheme.typography.bodyLarge
                                },
                                color = if (index == activeIndex) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.White.copy(alpha = 0.75f)
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Draws a simple bar visualizer from raw 8-bit PCM waveform samples (unsigned, centered at 128)
 * — samples a fixed number of evenly-spaced bars rather than plotting every byte, since the
 * capture buffer is much denser than needed for a readable bar chart. */
@Composable
private fun AudioVisualizer(waveform: ByteArray?, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val data = waveform
        if (data == null || data.isEmpty()) return@Canvas
        val barCount = 32
        val barWidth = size.width / barCount
        for (i in 0 until barCount) {
            val sampleIndex = (i * data.size / barCount).coerceIn(0, data.size - 1)
            // Visualizer's waveform bytes are unsigned (0-255); Byte.toInt() sign-extends, so the
            // upper half of the range (128-255) must be masked back to unsigned first or samples
            // above center come out as huge negative numbers instead of small positive ones.
            val unsigned = data[sampleIndex].toInt() and 0xFF
            val amplitude = kotlin.math.abs(unsigned - 128) / 128f
            val barHeight = (amplitude * size.height).coerceIn(2f, size.height)
            drawRect(
                color = color,
                topLeft = Offset(i * barWidth, size.height - barHeight),
                size = Size(barWidth * 0.6f, barHeight)
            )
        }
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis.coerceAtLeast(0))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
