package com.wavelength.music.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wavelength.music.activation.ActivationScreen
import com.wavelength.music.activation.ActivationStatus
import com.wavelength.music.activation.ActivationViewModel
import com.wavelength.music.activation.AdminActivationScreen
import com.wavelength.music.ui.components.AppBackground
import com.wavelength.music.ui.components.MiniPlayerBar
import com.wavelength.music.ui.home.GenreScreen
import com.wavelength.music.ui.home.HomeScreen
import com.wavelength.music.ui.library.LibraryScreen
import com.wavelength.music.ui.nowplaying.NowPlayingScreen
import com.wavelength.music.ui.nowplaying.PlayerViewModel
import com.wavelength.music.ui.playlist.PlaylistDetailScreen
import com.wavelength.music.ui.search.SearchScreen
import com.wavelength.music.ui.settings.AppSettingsViewModel
import com.wavelength.music.ui.settings.SettingsScreen
import com.wavelength.music.ui.statistics.StatisticsScreen
import com.wavelength.music.ui.theme.glassStyleFor
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

@OptIn(ExperimentalHazeApi::class)
@Composable
fun WavelengthNavHost() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playbackState by playerViewModel.state.collectAsStateWithLifecycle()
    val settingsViewModel: AppSettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.state.collectAsStateWithLifecycle()
    val activationViewModel: ActivationViewModel = hiltViewModel()
    val activation by activationViewModel.activation.collectAsStateWithLifecycle()
    val isLiquid = settings.theme.isGlass
    val glassStyle = glassStyleFor(settings.theme)
    val hazeState = remember { HazeState() }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showChrome = currentRoute != Screen.NowPlaying.route &&
        currentRoute != Screen.Settings.route &&
        currentRoute != Screen.Activation.route &&
        currentRoute != Screen.AdminActivation.route
    val activationRequiredSequence by playerViewModel.activationRequiredSequence.collectAsStateWithLifecycle()
    var showActivationPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(activationRequiredSequence) {
        if (activationRequiredSequence > 0L && activation.status != ActivationStatus.ACTIVE) {
            showActivationPrompt = true
        }
    }

    if (showActivationPrompt) {
        AlertDialog(
            onDismissRequest = { showActivationPrompt = false },
            title = { Text("Email activation required") },
            text = { Text("Your email has not been activated.") },
            dismissButton = {
                TextButton(onClick = { showActivationPrompt = false }) { Text("Later") }
            },
            confirmButton = {
                Button(onClick = {
                    showActivationPrompt = false
                    navController.navigate(Screen.Activation.route) { launchSingleTop = true }
                }) { Text("Activate Email") }
            }
        )
    }

    Scaffold(
        topBar = {
            if (showChrome && activation.status != ActivationStatus.ACTIVE) {
                Button(
                    onClick = { navController.navigate(Screen.Activation.route) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Email Activation")
                }
            }
        },
        bottomBar = {
            if (showChrome) {
                Column {
                        MiniPlayerBar(
                            state = playbackState,
                            onClick = { navController.navigate(Screen.NowPlaying.route) },
                            onPlayPause = playerViewModel::playPause,
                            onSkipNext = playerViewModel::skipNext,
                            onSkipPrevious = playerViewModel::skipPrevious,
                            hazeState = if (isLiquid) hazeState else null,
                            glassStyle = glassStyle,
                            trackTransitionEnabled = settings.trackTransitionEnabled,
                            trackTransitionDurationMs = settings.trackTransitionDurationMs
                        )
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        val navSpacing = 10.dp
                        val itemWidth = (maxWidth - navSpacing * 2) / 3
                        val selectedIndex = bottomNavScreens.indexOfFirst { currentRoute == it.route }
                            .coerceAtLeast(0)
                        val density = LocalDensity.current

                        var isDraggingNav by remember { mutableStateOf(false) }
                        var dragIndex by remember { mutableFloatStateOf(selectedIndex.toFloat()) }

                        LaunchedEffect(selectedIndex, isDraggingNav) {
                            if (!isDraggingNav) {
                                dragIndex = selectedIndex.toFloat()
                            }
                        }

                        val overlayIndex by animateFloatAsState(
                            targetValue = dragIndex,
                            animationSpec = if (isDraggingNav) {
                                snap()
                            } else {
                                spring(
                                    dampingRatio = 0.72f,
                                    stiffness = Spring.StiffnessLow
                                )
                            },
                            label = "bottomNavTileOverlayIndex"
                        )

                        val stepPx = with(density) { (itemWidth + navSpacing).toPx() }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(stepPx, selectedIndex) {
                                    detectHorizontalDragGestures(
                                        onDragStart = {
                                            isDraggingNav = true
                                            dragIndex = selectedIndex.toFloat()
                                        },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragIndex = (dragIndex + dragAmount / stepPx)
                                                .coerceIn(0f, (bottomNavScreens.size - 1).toFloat())
                                        },
                                        onDragCancel = {
                                            isDraggingNav = false
                                            dragIndex = selectedIndex.toFloat()
                                        },
                                        onDragEnd = {
                                            val targetIndex = kotlin.math.round(dragIndex)
                                                .toInt()
                                                .coerceIn(0, bottomNavScreens.lastIndex)
                                            isDraggingNav = false
                                            dragIndex = targetIndex.toFloat()

                                            val target = bottomNavScreens[targetIndex]
                                            if (currentRoute != target.route) {
                                                navController.navigate(target.route) {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(navSpacing),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val purpleGlassBrush = if (settings.neomorphismEnabled) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF313947),
                                            Color(0xFF252C37),
                                            Color(0xFF1D232C)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xCC332060),
                                            Color(0xCC56367F),
                                            Color(0xCC764595)
                                        )
                                    )
                                }

                                bottomNavScreens.forEachIndexed { index, screen ->
                                    val selected = selectedIndex == index
                                    val tileShape = RoundedCornerShape(24.dp)
                                    val itemAlpha by animateFloatAsState(
                                        targetValue = if (selected) 1f else 0.82f,
                                        animationSpec = tween(180),
                                        label = "navItemAlpha$index"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .width(itemWidth)
                                            .graphicsLayer { alpha = itemAlpha }
                                            .shadow(
                                                if (selected) 16.dp else 10.dp,
                                                tileShape,
                                                ambientColor = if (settings.neomorphismEnabled) Color.Black.copy(alpha = 0.55f) else Color.Black,
                                                spotColor = if (settings.neomorphismEnabled) Color.Black.copy(alpha = 0.70f) else Color.Black
                                            )
                                            .clip(tileShape)
                                            .background(purpleGlassBrush)
                                            .border(
                                                if (selected) 1.4.dp else 1.1.dp,
                                                Color.White.copy(alpha = if (selected) 0.56f else 0.34f),
                                                tileShape
                                            )
                                            .clickable {
                                                if (!selected) {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.startDestinationId) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 11.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                iconFor(screen),
                                                contentDescription = navLabelFor(screen),
                                                tint = Color.White
                                            )
                                            Text(
                                                navLabelFor(screen),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Color.White,
                                                modifier = Modifier.padding(start = 7.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Same pill/rounded-rectangle silhouette as the actual nav tile.
                            // It is only a few dp larger so the liquid overlay remains visible
                            // around the Home/Search/Library tile while following the finger.
                            val overlayExtra = 4.dp
                            val overlayShape = RoundedCornerShape(26.dp)
                            val overlayX = (itemWidth + navSpacing) * overlayIndex - overlayExtra / 2

                            Box(
                                modifier = Modifier
                                    .offset(x = overlayX, y = -overlayExtra / 2)
                                    .width(itemWidth + overlayExtra)
                                    .height(50.dp)
                                    .shadow(
                                        elevation = 18.dp,
                                        shape = overlayShape,
                                        ambientColor = Color(0xFFB86BFF),
                                        spotColor = Color(0xFF72E8FF)
                                    )
                                    .clip(overlayShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.16f),
                                                Color(0xFFB56BFF).copy(alpha = 0.17f),
                                                Color(0xFF60E9FF).copy(alpha = 0.10f)
                                            )
                                        )
                                    )
                                    .border(
                                        1.45.dp,
                                        Brush.linearGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.76f),
                                                Color(0xFFB96DFF).copy(alpha = 0.46f),
                                                Color(0xFF6DEBFF).copy(alpha = 0.36f)
                                            )
                                        ),
                                        overlayShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        var contentModifier: Modifier = Modifier.fillMaxSize()
        if (isLiquid) {
            contentModifier = contentModifier.haze(state = hazeState)
        }
        Box(modifier = contentModifier) {
            if (settings.glassmorphismNowPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF071A2B),
                                    Color(settings.customAccentArgb).copy(alpha = 0.42f),
                                    Color(0xFF10243D),
                                    Color(0xFF241B3A)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.13f),
                                    Color.Transparent
                                ),
                                radius = 900f
                            )
                        )
                )
            } else {
                AppBackground(
                    hasCustomBackground = settings.hasCustomBackground,
                    customBackgroundFile = settingsViewModel.customBackgroundFile,
                    opacity = settings.backgroundOpacity,
                    builtInWallpaper = settings.builtInWallpaper
                )
            }
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onTrackClick = { navController.navigate(Screen.NowPlaying.route) },
                        onGenreClick = { tag, label ->
                            navController.navigate(Screen.Genre.createRoute(tag, label))
                        },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) },
                        onStatisticsClick = { navController.navigate(Screen.Statistics.route) },
                        onPlaylistClick = { id ->
                            navController.navigate(Screen.PlaylistDetail.createRoute(id))
                        },
                        onSearchClick = {
                            navController.navigate(Screen.Search.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onSwipeToSearch = {
                            navController.navigate(Screen.Search.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable(Screen.Search.route) {
                    SearchScreen(
                        onTrackClick = { navController.navigate(Screen.NowPlaying.route) },
                        onSwipeToLibrary = {
                            navController.navigate(Screen.Library.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onSwipeToHome = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                composable(Screen.Library.route) {
                    LibraryScreen(
                        onTrackClick = { navController.navigate(Screen.NowPlaying.route) },
                        onPlaylistClick = { id ->
                            navController.navigate(Screen.PlaylistDetail.createRoute(id))
                        },
                        onSwipeToSearch = {
                            navController.navigate(Screen.Search.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

composable(
                    route = Screen.NowPlaying.route,
                    enterTransition = {
                        slideInVertically(initialOffsetY = { it }, animationSpec = tween(320))
                    },
                    exitTransition = {
                        slideOutVertically(targetOffsetY = { it }, animationSpec = tween(280))
                    },
                    popEnterTransition = {
                        slideInVertically(initialOffsetY = { it }, animationSpec = tween(320))
                    },
                    popExitTransition = {
                        slideOutVertically(targetOffsetY = { it }, animationSpec = tween(280))
                    }
                ) {
                    NowPlayingScreen(
                        onCollapse = { navController.popBackStack() },
                        onBrowseArtist = { artist ->
                            navController.navigate(Screen.Genre.createRoute(artist, artist))
                        },
                        onBrowseAlbum = { album ->
                            navController.navigate(Screen.Genre.createRoute(album, album))
                        },
                        viewModel = playerViewModel,
                        isLiquid = isLiquid,
                        glassStyle = glassStyle,
                        expandUpNextOnScroll = settings.expandUpNextOnScroll,
                        dynamicThemeFromAlbumArt = settings.dynamicThemeFromAlbumArt,
                        albumArtStyle = settings.albumArtStyle,
                        audioVisualizerEnabled = settings.audioVisualizerEnabled,
                        trackTransitionEnabled = settings.trackTransitionEnabled,
                        trackTransitionDurationMs = settings.trackTransitionDurationMs,
                        syncVolumeWithSystem = settings.syncVolumeWithSystem,
                        glassmorphismNowPlaying = settings.glassmorphismNowPlaying,
                        glassTimelineArgb = settings.glassTimelineArgb,
                        glassPlayedGlowArgb = settings.glassPlayedGlowArgb,
                        liquidAlbumArtBackground = false
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onStatisticsClick = { navController.navigate(Screen.Statistics.route) },
                        onEmailActivationClick = { navController.navigate(Screen.Activation.route) }
                    )
                }
                composable(Screen.Activation.route) {
                    ActivationScreen(
                        onBack = { navController.popBackStack() },
                        onAdminClick = { navController.navigate(Screen.AdminActivation.route) }
                    )
                }
                composable(Screen.AdminActivation.route) {
                    AdminActivationScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.Statistics.route) {
                    StatisticsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAdmin = { navController.navigate(Screen.AdminActivation.route) }
                    )
                }
                composable(
                    route = Screen.Genre.route,
                    arguments = listOf(
                        navArgument("tag") { type = NavType.StringType },
                        navArgument("label") { type = NavType.StringType }
                    )
                ) {
                    GenreScreen(
                        onBack = { navController.popBackStack() },
                        onTrackClick = { navController.navigate(Screen.NowPlaying.route) }
                    )
                }
                composable(
                    route = Screen.PlaylistDetail.route,
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                ) {
                    PlaylistDetailScreen(
                        onBack = { navController.popBackStack() },
                        onTrackClick = { navController.navigate(Screen.NowPlaying.route) }
                    )
                }
            }
        }
    }
}

private fun iconFor(screen: Screen) = when (screen) {
    Screen.Home -> Icons.Filled.Home
    Screen.Search -> Icons.Filled.Search
    Screen.Library -> Icons.Filled.LibraryMusic
    else -> Icons.Filled.Home
}


private fun navLabelFor(screen: Screen) = when (screen) {
    Screen.Home -> "Home"
    Screen.Search -> "Search"
    Screen.Library -> "Library"
    else -> ""
}
