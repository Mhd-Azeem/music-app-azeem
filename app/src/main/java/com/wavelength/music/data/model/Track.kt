package com.wavelength.music.data.model

enum class TrackSource {
    JIOSAAVN, LOCAL
}

data class Track(
    val id: String,
    val name: String,
    val artistId: String,
    val artistName: String,
    val albumId: String,
    val albumName: String,
    val albumArtUrl: String,
    val audioUrl: String,
    val durationSeconds: Int,
    val source: TrackSource = TrackSource.JIOSAAVN
)
