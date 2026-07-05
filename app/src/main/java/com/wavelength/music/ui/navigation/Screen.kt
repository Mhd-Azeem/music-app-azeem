package com.wavelength.music.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Library : Screen("library")
    data object NowPlaying : Screen("now_playing")

    data object ArtistDetail : Screen("artist/{artistId}") {
        fun createRoute(artistId: String) = "artist/$artistId"
    }

    data object AlbumDetail : Screen("album/{albumId}") {
        fun createRoute(albumId: String) = "album/$albumId"
    }

    data object Genre : Screen("genre/{tag}/{label}") {
        fun createRoute(tag: String, label: String) = "genre/$tag/$label"
    }
}

val bottomNavScreens = listOf(Screen.Home, Screen.Search, Screen.Library)
