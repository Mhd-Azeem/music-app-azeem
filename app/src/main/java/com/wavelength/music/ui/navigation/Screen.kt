package com.wavelength.music.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Library : Screen("library")
    data object NowPlaying : Screen("now_playing")
    data object Settings : Screen("settings")

    data object Genre : Screen("genre/{tag}/{label}") {
        fun createRoute(tag: String, label: String) = "genre/$tag/$label"
    }

    data object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
}

val bottomNavScreens = listOf(Screen.Home, Screen.Search, Screen.Library)
