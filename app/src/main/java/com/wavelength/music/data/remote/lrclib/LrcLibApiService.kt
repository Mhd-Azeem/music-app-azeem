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
        @Query("duration") durationSeconds: Int
    ): LrcLibResponseDto
}

@JsonClass(generateAdapter = true)
data class LrcLibResponseDto(
    @Json(name = "syncedLyrics") val syncedLyrics: String? = null,
    @Json(name = "plainLyrics") val plainLyrics: String? = null
)
