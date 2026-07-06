package com.wavelength.music.data.model

/** Domain model for a JioSaavn search/album/playlist result, independent of the DTO shape. */
data class JioSaavnSong(
    val id: String,
    val name: String,
    val album: String,
    val artist: String,
    val durationSeconds: Int,
    val imageUrl: String,
    val streamUrl: String
)

fun JioSaavnSong.toTrack(): Track = Track(
    id = "jiosaavn_$id",
    name = name,
    artistId = "",
    artistName = artist,
    albumId = "",
    albumName = album,
    albumArtUrl = imageUrl,
    audioUrl = streamUrl,
    durationSeconds = durationSeconds,
    source = TrackSource.JIOSAAVN
)
