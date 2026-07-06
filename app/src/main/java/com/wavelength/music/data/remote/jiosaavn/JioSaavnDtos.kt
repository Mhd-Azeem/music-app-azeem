package com.wavelength.music.data.remote.jiosaavn

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTOs for a self-hosted https://github.com/sumitkolhe/jiosaavn-api deployment. This is an
 * unofficial, community-run API with no stability guarantees, so several fields are nullable
 * with fallbacks (e.g. "link" vs "url") to tolerate small differences across deployments/versions.
 */
@JsonClass(generateAdapter = true)
data class JioSaavnImageDto(
    @Json(name = "quality") val quality: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "link") val link: String? = null
) {
    val resolvedUrl: String? get() = url ?: link
}

@JsonClass(generateAdapter = true)
data class JioSaavnSongDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "song") val song: String? = null,
    @Json(name = "album") val album: String? = null,
    @Json(name = "primary_artists") val primaryArtists: String? = null,
    @Json(name = "singers") val singers: String? = null,
    @Json(name = "duration") val duration: String? = null,
    @Json(name = "image") val image: List<JioSaavnImageDto> = emptyList(),
    @Json(name = "downloadUrl") val downloadUrl: List<JioSaavnImageDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JioSaavnSearchResponse(
    @Json(name = "data") val data: JioSaavnSearchData? = null
)

@JsonClass(generateAdapter = true)
data class JioSaavnSearchData(
    @Json(name = "results") val results: List<JioSaavnSongDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JioSaavnSongResponse(
    @Json(name = "data") val data: List<JioSaavnSongDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JioSaavnAlbumResponse(
    @Json(name = "data") val data: JioSaavnCollectionData? = null
)

@JsonClass(generateAdapter = true)
data class JioSaavnPlaylistResponse(
    @Json(name = "data") val data: JioSaavnCollectionData? = null
)

@JsonClass(generateAdapter = true)
data class JioSaavnCollectionData(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "songs") val songs: List<JioSaavnSongDto> = emptyList()
)
