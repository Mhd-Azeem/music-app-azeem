package com.wavelength.music.data.model

data class Track(
    val id: String,
    val name: String,
    val artistId: String,
    val artistName: String,
    val albumId: String,
    val albumName: String,
    val albumArtUrl: String,
    val audioUrl: String,
    val durationSeconds: Int
)
