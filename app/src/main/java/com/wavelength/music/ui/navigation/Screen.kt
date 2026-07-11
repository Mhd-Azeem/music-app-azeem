package com.wavelength.music.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Library : Screen("library")
    data object NowPlaying : Screen("now_playing")
    data object Settings : Screen("settings")
    data object Statistics : Screen("statistics")

    data object Genre : Screen("genre/{tag}/{label}") {
        // Encoded since callers may now pass free-text values containing spaces (e.g. artist
        // names) rather than just the single-word language tags this route originally carried.
        fun createRoute(tag: String, label: String) = "genre/${Uri.encode(tag)}/${Uri.encode(label)}"
    }

    data object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
}

val bottomNavScreens = listOf(Screen.Home, Screen.Search, Screen.Library)
