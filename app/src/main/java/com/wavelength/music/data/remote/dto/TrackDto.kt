package com.wavelength.music.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TrackDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "duration") val duration: Int = 0,
    @Json(name = "artist_id") val artistId: String = "",
    @Json(name = "artist_name") val artistName: String = "",
    @Json(name = "album_id") val albumId: String = "",
    @Json(name = "album_name") val albumName: String = "",
    @Json(name = "album_image") val albumImage: String = "",
    @Json(name = "image") val image: String = "",
    @Json(name = "audio") val audio: String = ""
)
