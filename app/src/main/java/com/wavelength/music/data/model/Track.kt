package com.wavelength.music.data.model

import com.squareup.moshi.JsonClass

// Moshi reads these enum fields by name when restoring a saved playback queue.
@JsonClass(generateAdapter = false)
enum class TrackSource {
    JIOSAAVN, LOCAL, DOWNLOADED
}

@JsonClass(generateAdapter = true)
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
    val source: TrackSource = TrackSource.JIOSAAVN,
    val language: String = "",
    val fileSizeBytes: Long = 0L,
    val modifiedAtSeconds: Long = 0L
)
