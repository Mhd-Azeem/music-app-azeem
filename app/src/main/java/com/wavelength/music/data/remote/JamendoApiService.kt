package com.wavelength.music.data.remote

import com.wavelength.music.data.remote.dto.AlbumDto
import com.wavelength.music.data.remote.dto.AlbumTracksDto
import com.wavelength.music.data.remote.dto.ArtistDto
import com.wavelength.music.data.remote.dto.ArtistTracksDto
import com.wavelength.music.data.remote.dto.JamendoResponse
import com.wavelength.music.data.remote.dto.PlaylistDto
import com.wavelength.music.data.remote.dto.TrackDto
import retrofit2.http.GET
import retrofit2.http.Query

interface JamendoApiService {

    @GET("tracks/")
    suspend fun getTracks(
        @Query("order") order: String = "popularity_total",
        @Query("limit") limit: Int = 20,
        @Query("tags") tags: String? = null,
        @Query("search") search: String? = null,
        @Query("include") include: String = "musicinfo"
    ): JamendoResponse<TrackDto>

    @GET("artists/")
    suspend fun getArtists(
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 20
    ): JamendoResponse<ArtistDto>

    @GET("artists/tracks/")
    suspend fun getArtistTracks(
        @Query("id") artistId: String,
        @Query("limit") limit: Int = 50
    ): JamendoResponse<ArtistTracksDto>

    @GET("albums/")
    suspend fun getAlbums(
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 20
    ): JamendoResponse<AlbumDto>

    @GET("albums/tracks/")
    suspend fun getAlbumTracks(
        @Query("id") albumId: String,
        @Query("limit") limit: Int = 50
    ): JamendoResponse<AlbumTracksDto>

    @GET("playlists/")
    suspend fun getPlaylists(
        @Query("order") order: String = "popularity_total",
        @Query("limit") limit: Int = 20
    ): JamendoResponse<PlaylistDto>
}
