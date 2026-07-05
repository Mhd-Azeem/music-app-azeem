package com.wavelength.music.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AlbumDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "artist_id") val artistId: String = "",
    @Json(name = "artist_name") val artistName: String = "",
    @Json(name = "image") val image: String = ""
)
