package com.wavelength.music.data.remote.jiosaavn

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTOs for a self-hosted https://github.com/sumitkolhe/jiosaavn-api deployment (the modern
 * Hono/TypeScript rewrite — album/artists are nested objects, not flat strings like the older
 * JioSaavn API schema). This is an unofficial, community-run API with no stability guarantees,
 * so fields are nullable with fallbacks where reasonable.
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
data class JioSaavnAlbumRefDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null
)

@JsonClass(generateAdapter = true)
data class JioSaavnArtistRefDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null
)

@JsonClass(generateAdapter = true)
data class JioSaavnArtistsDto(
    @Json(name = "primary") val primary: List<JioSaavnArtistRefDto> = emptyList(),
    @Json(name = "featured") val featured: List<JioSaavnArtistRefDto> = emptyList(),
    @Json(name = "all") val all: List<JioSaavnArtistRefDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JioSaavnSongDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "album") val album: JioSaavnAlbumRefDto? = null,
    @Json(name = "artists") val artists: JioSaavnArtistsDto? = null,
    @Json(name = "duration") val duration: Int? = null,
    @Json(name = "language") val language: String? = null,
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

@JsonClass(generateAdapter = true)
data class JioSaavnUsageResponse(
    @Json(name = "data") val data: JioSaavnUsageDataDto? = null
)

@JsonClass(generateAdapter = true)
data class JioSaavnUsageDataDto(
    @Json(name = "used") val used: Int? = null,
    @Json(name = "limit") val limit: Int? = null,
    @Json(name = "date") val date: String? = null
)
