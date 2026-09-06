package com.wavelength.music.data.remote.lrclib

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

/** https://lrclib.net — a free, keyless, no-rate-limit-hassle synced-lyrics API. Best-effort:
 * most tracks (especially non-English ones from JioSaavn) simply won't have a match. */
interface LrcLibApiService {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String? = null,
        @Query("duration") durationSeconds: Int? = null
    ): LrcLibResponseDto

    @GET("api/search")
    suspend fun searchLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String? = null
    ): List<LrcLibResponseDto>
}

@JsonClass(generateAdapter = true)
data class LrcLibResponseDto(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "trackName") val trackName: String? = null,
    @Json(name = "artistName") val artistName: String? = null,
    @Json(name = "albumName") val albumName: String? = null,
    @Json(name = "duration") val duration: Double? = null,
    @Json(name = "instrumental") val instrumental: Boolean = false,
    @Json(name = "syncedLyrics") val syncedLyrics: String? = null,
    @Json(name = "plainLyrics") val plainLyrics: String? = null
)
