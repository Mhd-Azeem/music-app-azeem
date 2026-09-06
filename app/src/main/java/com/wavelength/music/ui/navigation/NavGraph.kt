package com.wavelength.music.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.wavelength.music.ui.youtube.YouTubeScreen
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
    val showActivationBanner = showChrome && activation.status != ActivationStatus.ACTIVE

    Scaffold(
        topBar = {
            if (showActivationBanner) {
                Button(
                    onClick = { navController.navigate(Screen.Activation.route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val label = if (activation.email.isBlank()) {
                        "Email Activation"
                    } else {
                        "Email Activation • ${activation.status.name.replace('_', ' ')}"
                    }
                    Text(label)
                }
            }
        },
        bottomBar = {
            if (showChrome) {
                var navBarModifier: Modifier = Modifier
                if (isLiquid) {
                    navBarModifier = navBarModifier.hazeChild(state = hazeState, style = glassStyle) {
                        inputScale = HazeInputScale.Auto
                    }
                }
                Column {
                    if (currentRoute != Screen.YouTube.route) {
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
                    }
                    NavigationBar(
                        modifier = navBarModifier,
                        containerColor = if (isLiquid) Color.Transparent else NavigationBarDefaults.containerColor
                    ) {
                        bottomNavScreens.forEach { screen ->
                            NavigationBarItem(
                                selected = currentRoute == screen.route,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(iconFor(screen), contentDescription = null) },
                                label = { Text(labelFor(screen)) }
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
            AppBackground(
                hasCustomBackground = settings.hasCustomBackground,
                customBackgroundFile = settingsViewModel.customBackgroundFile,
                opacity = settings.backgroundOpacity
            )
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
                composable(Screen.YouTube.route) {
                    LaunchedEffect(Unit) {
                        playerViewModel.pause()
                    }
                    YouTubeScreen()
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
                        viewModel = playerViewModel,
                        isLiquid = isLiquid,
                        glassStyle = glassStyle,
                        expandUpNextOnScroll = settings.expandUpNextOnScroll,
                        dynamicThemeFromAlbumArt = settings.dynamicThemeFromAlbumArt,
                        vinylStyleAlbumArt = settings.vinylStyleAlbumArt,
                        audioVisualizerEnabled = settings.audioVisualizerEnabled,
                        trackTransitionEnabled = settings.trackTransitionEnabled,
                        trackTransitionDurationMs = settings.trackTransitionDurationMs,
                        syncVolumeWithSystem = settings.syncVolumeWithSystem
                    )
                }
                composable(Screen.Settings.route) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onStatisticsClick = { navController.navigate(Screen.Statistics.route) }
                        )
                        Button(
                            onClick = { navController.navigate(Screen.Activation.route) },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                        ) {
                            Text("Email Activation")
                        }
                    }
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
                    StatisticsScreen(onBack = { navController.popBackStack() })
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
    Screen.YouTube -> Icons.Filled.SmartDisplay
    else -> Icons.Filled.Home
}


private fun labelFor(screen: Screen) = when (screen) {
    Screen.Home -> "Home"
    Screen.Search -> "Search"
    Screen.Library -> "Library"
    Screen.YouTube -> "YouTube"
    else -> ""
}
