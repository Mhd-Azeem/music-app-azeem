package com.wavelength.music.ui.nowplaying

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.wavelength.music.data.repository.AlbumArtStyle
import com.wavelength.music.playback.RepeatMode
import com.wavelength.music.ui.components.EmptyView
import com.wavelength.music.ui.components.LoadingView
import com.wavelength.music.ui.components.QuickAddToPlaylistDialog
import com.wavelength.music.ui.components.ScreenState
import com.wavelength.music.ui.components.TrackOptionsSheet
import com.wavelength.music.ui.components.TrackRow
import com.wavelength.music.ui.components.dragDropItemOffset
import com.wavelength.music.ui.components.dragHandle
import com.wavelength.music.ui.components.rememberDragDropListState
import com.wavelength.music.ui.components.swipeHorizontal
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
    onBrowseArtist: (String) -> Unit = {},
    onBrowseAlbum: (String) -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
    isLiquid: Boolean = false,
    glassStyle: HazeStyle = HazeStyle.Unspecified,
    expandUpNextOnScroll: Boolean = false,
    dynamicThemeFromAlbumArt: Boolean = false,
    albumArtStyle: AlbumArtStyle = AlbumArtStyle.OFF,
    audioVisualizerEnabled: Boolean = false,
    trackTransitionEnabled: Boolean = true,
    trackTransitionDurationMs: Int = 300,
    syncVolumeWithSystem: Boolean = true,
    glassmorphismNowPlaying: Boolean = false,
    liquidAlbumArtBackground: Boolean = false
) {
    val trackTransitionSpec: FiniteAnimationSpec<Float> = if (trackTransitionEnabled) {
        tween(trackTransitionDurationMs)
    } else {
        snap()
    }
    val vinylStyleAlbumArt = albumArtStyle == AlbumArtStyle.VINYL
    val parallaxAlbumArt = albumArtStyle == AlbumArtStyle.PARALLAX
    val depthFloatAlbumArt = albumArtStyle == AlbumArtStyle.DEPTH_FLOAT
    val bassZoomAlbumArt = albumArtStyle == AlbumArtStyle.BASS_ZOOM
    val spatialFloatAlbumArt = albumArtStyle == AlbumArtStyle.SPATIAL_FLOAT
    val sensorMotionEnabled = parallaxAlbumArt || spatialFloatAlbumArt
    val hazeState = remember { HazeState() }
    val pillShape = RoundedCornerShape(28.dp)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val systemVolume by viewModel.systemVolume.collectAsStateWithLifecycle()
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
    var showCurrentTrackMenu by remember { mutableStateOf(false) }
    var quickAddQueueIndex by remember { mutableStateOf<Int?>(null) }

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

    // Spatial parallax: calibrated physical orientation plus gyroscope angular velocity.
    // The album art is deliberately rendered smaller while active so the extra movement has
    // breathing room, similar to the spatial/depth motion used by iPhone artwork effects.
    var sensorTiltX by remember { mutableFloatStateOf(0f) }
    var sensorTiltY by remember { mutableFloatStateOf(0f) }
    var sensorGyroX by remember { mutableFloatStateOf(0f) }
    var sensorGyroY by remember { mutableFloatStateOf(0f) }
    DisposableEffect(sensorMotionEnabled, spatialFloatAlbumArt, context) {
        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val gravityFallback = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val motionSensor = rotationSensor ?: gravityFallback
        val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val listener = object : SensorEventListener {
            private var baselinePitch: Float? = null
            private var baselineRoll: Float? = null
            private var baselineGravityX: Float? = null
            private var baselineGravityY: Float? = null
            private var filteredX = 0f
            private var filteredY = 0f
            private var filteredGyroX = 0f
            private var filteredGyroY = 0f
            private val rotationMatrix = FloatArray(9)
            private val orientation = FloatArray(3)

            private fun angularDelta(value: Float, baseline: Float): Float {
                var delta = value - baseline
                val pi = Math.PI.toFloat()
                val twoPi = (Math.PI * 2.0).toFloat()
                while (delta > pi) delta -= twoPi
                while (delta < -pi) delta += twoPi
                return delta
            }

            override fun onSensorChanged(event: SensorEvent) {
                if (!sensorMotionEnabled) return

                if (event.sensor.type == Sensor.TYPE_GYROSCOPE && event.values.size >= 2) {
                    // Angular velocity gives the artwork a responsive inertial nudge while the
                    // phone is being moved. When motion stops the sensor itself reports ~0, so
                    // this is still entirely physical input rather than an artificial animation.
                    val gx = (event.values[1] / 2.2f).coerceIn(-1.35f, 1.35f)
                    val gy = (-event.values[0] / 2.2f).coerceIn(-1.35f, 1.35f)
                    filteredGyroX += (gx - filteredGyroX) * 0.46f
                    filteredGyroY += (gy - filteredGyroY) * 0.46f
                    sensorGyroX = filteredGyroX
                    sensorGyroY = filteredGyroY
                    return
                }

                val targetX: Float
                val targetY: Float
                if (event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR ||
                    event.sensor.type == Sensor.TYPE_ROTATION_VECTOR
                ) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val pitch = orientation[1]
                    val roll = orientation[2]
                    if (baselinePitch == null || baselineRoll == null) {
                        baselinePitch = pitch
                        baselineRoll = roll
                        return
                    }
                    // Reach full depth movement with about 8 degrees of phone tilt. This is
                    // intentionally much more sensitive than the previous gentle 14-degree range.
                    val maxTiltRad = Math.toRadians(8.0).toFloat()
                    targetX = (angularDelta(roll, baselineRoll!!) / maxTiltRad).coerceIn(-1.2f, 1.2f)
                    targetY = (angularDelta(pitch, baselinePitch!!) / maxTiltRad).coerceIn(-1.2f, 1.2f)
                } else {
                    if (event.values.size < 2) return
                    val gx = (event.values[0] / 9.81f).coerceIn(-1f, 1f)
                    val gy = (event.values[1] / 9.81f).coerceIn(-1f, 1f)
                    if (baselineGravityX == null || baselineGravityY == null) {
                        baselineGravityX = gx
                        baselineGravityY = gy
                        return
                    }
                    targetX = ((gx - baselineGravityX!!) * 3.8f).coerceIn(-1.2f, 1.2f)
                    targetY = ((gy - baselineGravityY!!) * 3.8f).coerceIn(-1.2f, 1.2f)
                }

                filteredX += (targetX - filteredX) * 0.40f
                filteredY += (targetY - filteredY) * 0.40f
                sensorTiltX = filteredX
                sensorTiltY = filteredY
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (sensorMotionEnabled && motionSensor != null) {
            sensorManager.registerListener(listener, motionSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        if (spatialFloatAlbumArt && gyroSensor != null) {
            sensorManager.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose {
            sensorManager.unregisterListener(listener)
            sensorTiltX = 0f
            sensorTiltY = 0f
            sensorGyroX = 0f
            sensorGyroY = 0f
        }
    }

    // Depth Float is intentionally ambient rather than beat-driven: a slow forward/backward
    // movement gives the artwork a calm floating-card feel.
    val depthFloatPhase = remember { Animatable(0f) }
    LaunchedEffect(depthFloatAlbumArt, state.isPlaying) {
        if (depthFloatAlbumArt && state.isPlaying) {
            while (true) {
                depthFloatPhase.animateTo(1f, animationSpec = tween(2400, easing = LinearEasing))
                depthFloatPhase.animateTo(-1f, animationSpec = tween(2400, easing = LinearEasing))
            }
        } else {
            depthFloatPhase.animateTo(0f, animationSpec = tween(280))
        }
    }

    // Bass Zoom uses the continuous decoded-PCM bass level, not the old Beat Bounce onset event.
    // Quiet/low-bass sections therefore stay almost still while strong bass smoothly zooms in.
    val pcmBassLevel by viewModel.pcmBassLevel.collectAsStateWithLifecycle()
    val bassZoomScale by animateFloatAsState(
        targetValue = if (bassZoomAlbumArt && state.isPlaying) {
            1f + pcmBassLevel.coerceIn(0f, 1f) * 0.115f
        } else {
            1f
        },
        animationSpec = spring(
            dampingRatio = 0.74f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bassZoomScale"
    )

    // RECORD_AUDIO is now needed only for the optional waveform visualizer UI. Beat Bounce no
    // longer depends on Android's Visualizer API or any runtime audio-recording permission.
    var hasRecordAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasRecordAudioPermission = granted }
    val needsAudioCapture = audioVisualizerEnabled
    LaunchedEffect(needsAudioCapture, hasRecordAudioPermission) {
        if (needsAudioCapture && !hasRecordAudioPermission) {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val visualizerWaveform by viewModel.visualizerWaveform.collectAsStateWithLifecycle()
    DisposableEffect(needsAudioCapture, hasRecordAudioPermission) {
        viewModel.setVisualizerCaptureEnabled(needsAudioCapture && hasRecordAudioPermission)
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

    if (showCurrentTrackMenu && track != null) {
        TrackOptionsSheet(
            track = track,
            onDismiss = { showCurrentTrackMenu = false },
            onBrowseArtist = { onBrowseArtist(track.artistName) },
            onBrowseAlbum = { onBrowseAlbum(track.albumName) }
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

    val quickAddTrack = quickAddQueueIndex?.let { state.queue.getOrNull(it)?.track }
    if (quickAddTrack != null) {
        QuickAddToPlaylistDialog(track = quickAddTrack, onDismiss = { quickAddQueueIndex = null })
    }

    val density = LocalDensity.current
    val collapseThresholdPx = with(density) { 80.dp.toPx() }
    val skipThresholdPx = with(density) { 56.dp.toPx() }

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
    //
    // Collapsing is two-stage rather than firing the instant the list reaches the top: the first
    // scroll-to-top just lands the list at its top (so you can see the start of the queue) without
    // shrinking the panel out from under you mid-gesture. Only a *second*, separate pull — one
    // that starts already pinned at the top — actually collapses it. upNextTopArmed tracks whether
    // the list was already sitting at the top when the CURRENT scroll/fling gesture began; it's
    // (re)computed only once a gesture fully settles, so reaching the top partway through one
    // continuous drag never arms it for that same drag.
    var upNextExpandedLatch by remember { mutableStateOf(false) }
    var upNextTopArmed by remember { mutableStateOf(false) }
    LaunchedEffect(upNextListState.isScrollInProgress) {
        if (!upNextListState.isScrollInProgress) {
            upNextTopArmed = upNextListState.firstVisibleItemIndex == 0 &&
                upNextListState.firstVisibleItemScrollOffset == 0
        }
    }
    val upNextNestedScrollConnection = remember(upNextListState) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val towardTop = consumed.y + available.y > 0f
                if (towardTop &&
                    upNextTopArmed &&
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
        upNextTopArmed = false
    }
    // Album art hides to free up room for the queue to actually grow into — without this, "half
    // the screen" has nowhere to expand into since art + controls already fill most of the
    // screen, so the box would just get clipped off the bottom with no visible size change.
    val isUpNextExpanded = expandUpNextOnScroll && upNextExpandedLatch

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                // translationY only — no alpha fade here. This Box (and its opaque background
                // Column below) sits directly above the shared AppBackground that's rendered
                // underneath the whole NavHost (see NavGraph.kt), which can be the user's own
                // custom wallpaper. Fading this screen's alpha during the drag let that image
                // bleed through as an ugly, disconnected flash instead of a clean slide-away.
                translationY = dragOffset
            }
    ) {
        if (glassmorphismNowPlaying && !track?.albumArtUrl.isNullOrBlank()) {
            AsyncImage(
                model = track?.albumArtUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.00f
                        scaleY = 1.00f
                    }
                    .blur(5.dp)
                    .alpha(1.00f),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.035f))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.055f))
            )
        }
        if (glassmorphismNowPlaying && !track?.albumArtUrl.isNullOrBlank()) {
            // The lower playback zone is deliberately more frosted than the artwork/queue zone.
            // Clip a second copy of the same artwork to the lower 43% so the split feels like
            // one continuous image rather than a different background.
            AsyncImage(
                model = track?.albumArtUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        clip = true
                        shape = GenericShape { size, _ ->
                            moveTo(0f, size.height * 0.57f)
                            lineTo(size.width, size.height * 0.57f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                    }
                    .blur(17.dp)
                    .alpha(0.88f),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.56f to Color.Transparent,
                                0.59f to Color.Black.copy(alpha = 0.04f),
                                1.00f to Color.Black.copy(alpha = 0.14f)
                            )
                        )
                    )
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val y = size.height * 0.585f
                val inset = 18.dp.toPx()
                val path = Path().apply {
                    moveTo(inset, y + 8.dp.toPx())
                    quadraticBezierTo(size.width / 2f, y - 22.dp.toPx(), size.width - inset, y + 8.dp.toPx())
                }
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.58f),
                    style = Stroke(width = 1.35.dp.toPx())
                )
            }
        }

        if (isLiquid && liquidAlbumArtBackground && !track?.albumArtUrl.isNullOrBlank()) {
            AsyncImage(
                model = track?.albumArtUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(1f)
                    .haze(state = hazeState),
                contentScale = ContentScale.Crop
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(
                    if (glassmorphismNowPlaying) RoundedCornerShape(34.dp)
                    else RoundedCornerShape(0.dp)
                )
                .background(
                    when {
                        glassmorphismNowPlaying -> Color.White.copy(alpha = 0.075f)
                        isLiquid -> Color.Transparent
                        else -> MaterialTheme.colorScheme.background
                    }
                )
                .border(
                    width = if (glassmorphismNowPlaying) 1.dp else 0.dp,
                    color = if (glassmorphismNowPlaying) Color.White.copy(alpha = 0.28f) else Color.Transparent,
                    shape = if (glassmorphismNowPlaying) RoundedCornerShape(34.dp) else RoundedCornerShape(0.dp)
                )
                // Glass mode must remain edge-to-edge. The previous 20.dp parent padding
                // constrained every child, so fillMaxWidth() could never reach screen edges.
                .padding(if (glassmorphismNowPlaying) 0.dp else 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragOffset > collapseThresholdPx) {
                                    // Leave dragOffset where the drag ended rather than snapping it
                                    // to 0 first — this composable is about to be popped off the
                                    // back stack anyway, and resetting the offset here made the
                                    // screen visibly jump back to its start position for a frame
                                    // before the nav pop's own slide-out transition took over,
                                    // producing a jarring double-motion glitch on release.
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
                    .swipeHorizontal(
                        thresholdPx = skipThresholdPx,
                        onSwipeLeft = { viewModel.skipNext() },
                        onSwipeRight = { viewModel.skipPrevious() }
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (glassmorphismNowPlaying) 20.dp else 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCollapse) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse")
                    }
                    Text(
                        text = "Music",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                    IconButton(
                        onClick = { showCurrentTrackMenu = true },
                        enabled = track != null
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Song options")
                    }
                }

                AnimatedVisibility(visible = !isUpNextExpanded && !glassmorphismNowPlaying) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                        if (vinylStyleAlbumArt) {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(20.dp)) {
                                Crossfade(targetState = track, animationSpec = trackTransitionSpec, label = "vinylArt") { t ->
                                    AsyncImage(
                                        model = t?.albumArtUrl,
                                        contentDescription = t?.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                rotationZ = if (vinylStyleAlbumArt) vinylAngle.value else 0f
                                                val spatialX = (sensorTiltX + sensorGyroX * 0.58f).coerceIn(-1.4f, 1.4f)
                                                val spatialY = (sensorTiltY + sensorGyroY * 0.58f).coerceIn(-1.4f, 1.4f)
                                                when {
                                                    spatialFloatAlbumArt -> {
                                                        rotationX = spatialY * 12f
                                                        rotationY = -spatialX * 15f
                                                        translationX = spatialX * 30f
                                                        translationY = spatialY * 24f
                                                    }
                                                    parallaxAlbumArt -> {
                                                        rotationX = sensorTiltY * 7.5f
                                                        rotationY = -sensorTiltX * 9.5f
                                                        translationX = sensorTiltX * 18f
                                                        translationY = sensorTiltY * 14f
                                                    }
                                                    depthFloatAlbumArt -> {
                                                        rotationX = depthFloatPhase.value * 1.4f
                                                        translationY = -depthFloatPhase.value * 9f
                                                        shadowElevation = 14f + depthFloatPhase.value * 4f
                                                    }
                                                }
                                                val motionScale = when {
                                                    bassZoomAlbumArt -> bassZoomScale
                                                    depthFloatAlbumArt -> 1f + depthFloatPhase.value * 0.018f
                                                    spatialFloatAlbumArt -> 0.88f
                                                    parallaxAlbumArt -> 0.93f
                                                    else -> 1f
                                                }
                                                scaleX = motionScale
                                                scaleY = motionScale
                                                cameraDistance = if (spatialFloatAlbumArt) 18f else 24f
                                            }
                                            .clip(if (vinylStyleAlbumArt) CircleShape else RoundedCornerShape(22.dp))
                                            .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.background)
                                )
                            }
                        } else {
                            Crossfade(targetState = track, animationSpec = trackTransitionSpec, label = "albumArt") { t ->
                                AsyncImage(
                                    model = t?.albumArtUrl,
                                    contentDescription = t?.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .graphicsLayer {
                                            val spatialX = (sensorTiltX + sensorGyroX * 0.58f).coerceIn(-1.4f, 1.4f)
                                            val spatialY = (sensorTiltY + sensorGyroY * 0.58f).coerceIn(-1.4f, 1.4f)
                                            when {
                                                spatialFloatAlbumArt -> {
                                                    rotationX = spatialY * 12f
                                                    rotationY = -spatialX * 15f
                                                    translationX = spatialX * 30f
                                                    translationY = spatialY * 24f
                                                }
                                                parallaxAlbumArt -> {
                                                    rotationX = sensorTiltY * 7.5f
                                                    rotationY = -sensorTiltX * 9.5f
                                                    translationX = sensorTiltX * 18f
                                                    translationY = sensorTiltY * 14f
                                                }
                                                depthFloatAlbumArt -> {
                                                    rotationX = depthFloatPhase.value * 1.4f
                                                    translationY = -depthFloatPhase.value * 9f
                                                    shadowElevation = 14f + depthFloatPhase.value * 4f
                                                }
                                            }
                                            val motionScale = when {
                                                bassZoomAlbumArt -> bassZoomScale
                                                depthFloatAlbumArt -> 1f + depthFloatPhase.value * 0.018f
                                                spatialFloatAlbumArt -> 0.88f
                                                parallaxAlbumArt -> 0.93f
                                                else -> 1f
                                            }
                                            scaleX = motionScale
                                            scaleY = motionScale
                                            cameraDistance = if (spatialFloatAlbumArt) 18f else 24f
                                        }
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }
                        }
                    }
                }
            }

            if (!glassmorphismNowPlaying) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                    Crossfade(
                        targetState = track,
                        animationSpec = trackTransitionSpec,
                        label = "trackInfo",
                        modifier = Modifier.weight(1f)
                    ) { t ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (glassmorphismNowPlaying) {
                                Alignment.CenterHorizontally
                            } else {
                                Alignment.Start
                            }
                        ) {
                            Text(
                                text = t?.name.orEmpty(),
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = t?.artistName.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (!glassmorphismNowPlaying) {
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
            }
            }

            if (glassmorphismNowPlaying) {
                val glassUpcoming = state.queue.drop(state.currentIndex + 1)
                val fanState = rememberLazyListState()

                // Up Next: real queue cards arranged as a horizontally scrollable circular fan.
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (glassUpcoming.isNotEmpty()) {
                        LazyRow(
                            state = fanState,
                            modifier = Modifier.fillMaxWidth().height(196.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            horizontalArrangement = Arrangement.spacedBy(22.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            itemsIndexed(glassUpcoming, key = { _, entry -> entry.instanceId }) { index, entry ->
                                // Derive the card transform from its live pixel position in the viewport.
                                // This keeps the cards on one continuous circular arc while dragging,
                                // instead of jumping only when firstVisibleItemIndex changes.
                                val layoutInfo = fanState.layoutInfo
                                val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                                val itemCenter = visibleItem?.let { it.offset + it.size / 2f } ?: viewportCenter
                                val halfViewport = ((layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2f).coerceAtLeast(1f)
                                val x = ((itemCenter - viewportCenter) / halfViewport).coerceIn(-1f, 1f)
                                // Same shallow crown geometry as the lower timeline/player curve:
                                // center is highest; cards descend smoothly toward both screen edges.
                                val circleY = 1f - kotlin.math.sqrt((1f - x * x).coerceAtLeast(0f))
                                val lift = with(LocalDensity.current) { (circleY * 58f).dp }
                                val tangentAngle = Math.toDegrees(kotlin.math.asin(x.toDouble())).toFloat() * 0.42f
                                Box(
                                    modifier = Modifier
                                        .width(82.dp)
                                        .height(158.dp)
                                        .offset(y = lift)
                                        .graphicsLayer { rotationZ = tangentAngle }
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(Color(0xFFD7ECFF).copy(alpha = 0.52f))
                                        .border(1.dp, Color.White.copy(alpha = 0.82f), RoundedCornerShape(11.dp))
                                        .clickable { viewModel.playQueueItem(state.currentIndex + 1 + index) }
                                        .padding(5.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        AsyncImage(
                                            model = entry.track.albumArtUrl,
                                            contentDescription = entry.track.name,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp)
                                                .clip(RoundedCornerShape(7.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(78.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = entry.track.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF08283A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.width(72.dp).graphicsLayer { rotationZ = 90f },
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                val progressFraction = if (state.durationMs > 0L) {
                    (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val lowerGlassShape = remember {
                    GenericShape { size, _ ->
                        moveTo(0f, size.height * 0.12f)
                        quadraticBezierTo(
                            size.width * 0.50f,
                            -size.height * 0.12f,
                            size.width,
                            size.height * 0.12f
                        )
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                }

                // One large frosted lower player panel with the same circular-cap silhouette.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(356.dp)
                        .offset(y = (-2).dp)
                        .clip(lowerGlassShape)
                        .background(Color(0xFFD7ECFF).copy(alpha = 0.82f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.78f), lowerGlassShape)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(top = 40.dp, start = 0.dp, end = 0.dp, bottom = 0.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Single curved seek line inside the panel.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp)
                                .height(92.dp)
                                .pointerInput(state.durationMs) {
                                    detectTapGestures { offset ->
                                        if (state.durationMs > 0L) {
                                            val p = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                            viewModel.seekTo((state.durationMs * p).toLong())
                                        }
                                    }
                                }
                        ) {
                            Canvas(Modifier.fillMaxSize()) {
                                val startAngle = 202f
                                val sweepAngle = 136f
                                val arcSize = Size(size.width, 176.dp.toPx())
                                val top = -5.dp.toPx()
                                drawArc(
                                    color = Color(0xFF087EAE),
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = Offset(0f, top),
                                    size = arcSize,
                                    style = Stroke(width = 2.4.dp.toPx())
                                )
                                val angle = Math.toRadians((startAngle + sweepAngle * progressFraction).toDouble())
                                val cx = size.width / 2f
                                val cy = top + arcSize.height / 2f
                                val thumb = Offset(
                                    cx + (arcSize.width / 2f * kotlin.math.cos(angle)).toFloat(),
                                    cy + (arcSize.height / 2f * kotlin.math.sin(angle)).toFloat()
                                )
                                drawCircle(Color(0xFF087EAE), 7.dp.toPx(), thumb)
                            }
                            Text(
                                text = formatMillis(state.positionMs),
                                color = Color(0xFF08283A),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.align(Alignment.BottomStart)
                            )
                            Text(
                                text = formatMillis(state.durationMs),
                                color = Color(0xFF08283A),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }

                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            // Compact connected Previous / Play-Pause / Next control.
                            Row(
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(76.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF9CCEF4).copy(alpha = 0.72f)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier.width(62.dp).fillMaxHeight().clickable(onClick = viewModel::skipPrevious),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.SkipPrevious, "Previous", tint = Color(0xFF075B83), modifier = Modifier.size(34.dp))
                                }
                                Box(modifier = Modifier.width(96.dp))
                                Box(
                                    modifier = Modifier.width(62.dp).fillMaxHeight().clickable(onClick = viewModel::skipNext),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.SkipNext, "Next", tint = Color(0xFF075B83), modifier = Modifier.size(34.dp))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(126.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF9CCEF4).copy(alpha = 0.92f))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.75f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(92.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD9EEFF))
                                        .clickable(enabled = !state.isBuffering, onClick = viewModel::playPause),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.isBuffering) {
                                        CircularProgressIndicator(modifier = Modifier.size(36.dp), color = Color(0xFF075B83), strokeWidth = 3.dp)
                                    } else {
                                        Icon(
                                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                                            tint = Color(0xFF075B83),
                                            modifier = Modifier.size(46.dp)
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp).size(48.dp).clip(CircleShape)
                                    .background(Color(0xFFEAF6FF).copy(alpha = 0.82f)).clickable(onClick = viewModel::toggleShuffle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Shuffle, "Shuffle", tint = if (state.shuffleEnabled) accentColor else Color(0xFF08283A), modifier = Modifier.size(24.dp))
                            }
                            Box(
                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp).size(48.dp).clip(CircleShape)
                                    .background(Color(0xFFEAF6FF).copy(alpha = 0.82f)).clickable(onClick = viewModel::cycleRepeatMode),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                    contentDescription = "Repeat",
                                    tint = if (state.repeatMode != RepeatMode.OFF) accentColor else Color(0xFF08283A),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!glassmorphismNowPlaying) {
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
                    val displayedVolume = if (syncVolumeWithSystem) systemVolume else state.volume
                    Icon(
                        imageVector = when {
                            displayedVolume <= 0f -> Icons.Filled.VolumeOff
                            displayedVolume < 0.5f -> Icons.Filled.VolumeDown
                            else -> Icons.Filled.VolumeUp
                        },
                        contentDescription = "Volume",
                        tint = Color.White
                    )
                    Slider(
                        value = displayedVolume,
                        onValueChange = {
                            if (syncVolumeWithSystem) viewModel.setSystemVolume(it) else viewModel.setVolume(it)
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor
                        )
                    )
                }

            }

            if (!glassmorphismNowPlaying && audioVisualizerEnabled && hasRecordAudioPermission) {
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
            if (upcoming.isNotEmpty() && !glassmorphismNowPlaying) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Up next", style = MaterialTheme.typography.titleSmall)
                    if (upcoming.size > 1) {
                        IconButton(onClick = { viewModel.smartShuffle() }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Smart shuffle (favors your most-played tracks)",
                                tint = accentColor
                            )
                        }
                    }
                }
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
                    modifier = upNextModifier,
                    state = upNextListState
                ) {
                    itemsIndexed(upcoming, key = { _, entry -> entry.instanceId }) { offset, entry ->
                        val queueIndex = state.currentIndex + 1 + offset
                        val isDragging = dragDropState.draggingItemIndex == offset
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .dragDropItemOffset(dragDropState, offset)
                                .zIndex(if (isDragging) 1f else 0f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrackRow(
                                track = entry.track,
                                onClick = { viewModel.playQueueItem(queueIndex) },
                                onAddToPlaylistClick = { quickAddQueueIndex = queueIndex },
                                onMoreClick = { menuQueueIndex = queueIndex },
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .dragHandle(dragDropState, offset),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DragHandle,
                                    contentDescription = "Hold and drag to reorder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
