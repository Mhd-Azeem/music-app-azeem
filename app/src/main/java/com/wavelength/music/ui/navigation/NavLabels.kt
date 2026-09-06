package com.wavelength.music.ui.navigation

fun labelFor(screen: Screen): String = when (screen) {
    Screen.Home -> "Home"
    Screen.Search -> "Search"
    Screen.Library -> "Library"
    else -> ""
}
