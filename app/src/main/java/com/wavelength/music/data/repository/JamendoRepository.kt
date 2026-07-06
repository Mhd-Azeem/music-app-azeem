package com.wavelength.music.data.repository

import com.wavelength.music.data.model.Album
import com.wavelength.music.data.model.Artist
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.remote.JamendoApiService
import com.wavelength.music.data.remote.toDomain
import javax.inject.Inject
import javax.inject.Singleton

/** Everything that talks to the Jamendo API. Unchanged in behavior from before the multi-source facade. */
@Singleton
class JamendoRepository @Inject constructor(
    private val api: JamendoApiService
) {

    suspend fun getFeaturedTracks(limit: Int = 20): Result<List<Track>> = runCatching {
        api.getTracks(order = "popularity_total", limit = limit).results.map { it.toDomain() }
    }

    suspend fun getTracksByTag(tag: String, limit: Int = 20): Result<List<Track>> = runCatching {
        api.getTracks(order = "popularity_total", limit = limit, tags = tag).results
            .map { it.toDomain() }
    }

    suspend fun searchTracks(query: String, limit: Int = 30): Result<List<Track>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        api.getTracks(order = "relevance", limit = limit, search = query).results
            .map { it.toDomain() }
    }

    suspend fun searchArtists(query: String, limit: Int = 15): Result<List<Artist>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        api.getArtists(search = query, limit = limit).results.map { it.toDomain() }
    }

    suspend fun searchAlbums(query: String, limit: Int = 15): Result<List<Album>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        api.getAlbums(search = query, limit = limit).results.map { it.toDomain() }
    }

    suspend fun getArtistTracks(artistId: String): Result<Pair<Artist, List<Track>>> = runCatching {
        val result = api.getArtistTracks(artistId).results.firstOrNull()
            ?: throw NoSuchElementException("Artist not found")
        val artist = Artist(id = result.id, name = result.name, imageUrl = result.image)
        val tracks = result.tracks.map {
            it.toDomain(fallbackArtistId = result.id, fallbackArtistName = result.name)
        }
        artist to tracks
    }

    suspend fun getAlbumTracks(albumId: String): Result<Pair<Album, List<Track>>> = runCatching {
        val result = api.getAlbumTracks(albumId).results.firstOrNull()
            ?: throw NoSuchElementException("Album not found")
        val album = Album(
            id = result.id,
            name = result.name,
            artistId = result.artistId,
            artistName = result.artistName,
            imageUrl = result.image
        )
        val tracks = result.tracks.map {
            it.toDomain(
                fallbackAlbumArt = result.image,
                fallbackAlbumId = result.id,
                fallbackAlbumName = result.name,
                fallbackArtistId = result.artistId,
                fallbackArtistName = result.artistName
            )
        }
        album to tracks
    }
}
