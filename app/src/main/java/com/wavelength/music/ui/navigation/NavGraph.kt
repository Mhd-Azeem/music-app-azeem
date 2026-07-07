package com.wavelength.music.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wavelength.music.ui.components.MiniPlayerBar
import com.wavelength.music.ui.home.GenreScreen
import com.wavelength.music.ui.home.HomeScreen
import com.wavelength.music.ui.library.LibraryScreen
import com.wavelength.music.ui.nowplaying.NowPlayingScreen
import com.wavelength.music.ui.nowplaying.PlayerViewModel
import com.wavelength.music.ui.playlist.PlaylistDetailScreen
import com.wavelength.music.ui.search.SearchScreen
import com.wavelength.music.ui.settings.SettingsScreen

@Composable
fun WavelengthNavHost() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playbackState by playerViewModel.state.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showChrome = currentRoute != Screen.NowPlaying.route && currentRoute != Screen.Settings.route

    Scaffold(
        bottomBar = {
            if (showChrome) {
                Column {
                    MiniPlayerBar(
                        state = playbackState,
                        onClick = { navController.navigate(Screen.NowPlaying.route) },
                        onPlayPause = playerViewModel::playPause,
                        onSkipNext = playerViewModel::skipNext
                    )
                    NavigationBar {
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
                    onPlaylistClick = { id ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(id))
                    }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onTrackClick = { navController.navigate(Screen.NowPlaying.route) }
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    onTrackClick = { navController.navigate(Screen.NowPlaying.route) },
                    onPlaylistClick = { id ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(id))
                    }
                )
            }
            composable(Screen.NowPlaying.route) {
                NowPlayingScreen(
                    onCollapse = { navController.popBackStack() },
                    viewModel = playerViewModel
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
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

private fun iconFor(screen: Screen) = when (screen) {
    Screen.Home -> Icons.Filled.Home
    Screen.Search -> Icons.Filled.Search
    Screen.Library -> Icons.Filled.LibraryMusic
    else -> Icons.Filled.Home
}

private fun labelFor(screen: Screen) = when (screen) {
    Screen.Home -> "Home"
    Screen.Search -> "Search"
    Screen.Library -> "Library"
    else -> ""
}
