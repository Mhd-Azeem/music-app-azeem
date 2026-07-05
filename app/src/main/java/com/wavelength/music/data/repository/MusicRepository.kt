package com.wavelength.music.data.repository

import com.wavelength.music.data.local.FavoriteDao
import com.wavelength.music.data.local.FavoriteTrackEntity
import com.wavelength.music.data.local.RecentlyPlayedDao
import com.wavelength.music.data.local.RecentlyPlayedEntity
import com.wavelength.music.data.model.Album
import com.wavelength.music.data.model.Artist
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.remote.JamendoApiService
import com.wavelength.music.data.remote.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val api: JamendoApiService,
    private val favoriteDao: FavoriteDao,
    private val recentlyPlayedDao: RecentlyPlayedDao
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

    fun observeFavorites(): Flow<List<Track>> = favoriteDao.observeFavorites().map { list ->
        list.map { entity ->
            Track(
                id = entity.id,
                name = entity.name,
                artistId = "",
                artistName = entity.artist,
                albumId = "",
                albumName = "",
                albumArtUrl = entity.albumArtUrl,
                audioUrl = entity.audioUrl,
                durationSeconds = 0
            )
        }
    }

    fun isFavorite(trackId: String): Flow<Boolean> = favoriteDao.isFavorite(trackId)

    suspend fun toggleFavorite(track: Track, isCurrentlyFavorite: Boolean) {
        if (isCurrentlyFavorite) {
            favoriteDao.deleteById(track.id)
        } else {
            favoriteDao.insert(
                FavoriteTrackEntity(
                    id = track.id,
                    name = track.name,
                    artist = track.artistName,
                    albumArtUrl = track.albumArtUrl,
                    audioUrl = track.audioUrl
                )
            )
        }
    }

    fun observeRecentlyPlayed(limit: Int = 50): Flow<List<Track>> =
        recentlyPlayedDao.observeRecent(limit).map { list ->
            list.map { entity ->
                Track(
                    id = entity.trackId,
                    name = entity.name,
                    artistId = "",
                    artistName = entity.artist,
                    albumId = "",
                    albumName = "",
                    albumArtUrl = entity.albumArtUrl,
                    audioUrl = entity.audioUrl,
                    durationSeconds = 0
                )
            }
        }

    suspend fun recordPlayed(track: Track) {
        recentlyPlayedDao.recordPlay(
            RecentlyPlayedEntity(
                trackId = track.id,
                name = track.name,
                artist = track.artistName,
                albumArtUrl = track.albumArtUrl,
                audioUrl = track.audioUrl
            )
        )
    }
}
