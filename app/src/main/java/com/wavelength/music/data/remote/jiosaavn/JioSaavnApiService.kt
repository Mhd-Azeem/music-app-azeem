package com.wavelength.music.data.remote.jiosaavn

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for a self-hosted https://github.com/sumitkolhe/jiosaavn-api deployment.
 * Point BuildConfig.JIOSAAVN_BASE_URL (via local.properties' JIOSAAVN_BASE_URL) at your own
 * deployment, e.g. https://your-deployment.vercel.app/api/
 */
interface JioSaavnApiService {

    @GET("search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 20
    ): JioSaavnSearchResponse

    @GET("songs/{id}")
    suspend fun getSong(@Path("id") id: String): JioSaavnSongResponse

    @GET("albums")
    suspend fun getAlbum(@Query("link") link: String): JioSaavnAlbumResponse

    @GET("playlists")
    suspend fun getPlaylist(@Query("link") link: String): JioSaavnPlaylistResponse

    @GET("usage")
    suspend fun getUsage(): JioSaavnUsageResponse
}
