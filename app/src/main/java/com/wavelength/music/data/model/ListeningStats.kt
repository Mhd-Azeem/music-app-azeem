package com.wavelength.music.data.model

data class ListeningStats(
    val totalPlays: Int,
    val uniqueTracks: Int,
    val uniqueArtists: Int
)

data class ArtistStat(
    val artistName: String,
    val playCount: Int
)
