package com.wavelength.music.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Library : Screen("library")
    data object NowPlaying : Screen("now_playing")
    data object Settings : Screen("settings")
    data object Activation : Screen("activation")
    data object AdminActivation : Screen("activation_admin")
    data object Statistics : Screen("statistics")

    data object Genre : Screen("genre/{tag}/{label}") {
        fun createRoute(tag: String, label: String) = "genre/${Uri.encode(tag)}/${Uri.encode(label)}"
    }

    data object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
}

val bottomNavScreens = listOf(Screen.Home, Screen.Search, Screen.Library)
