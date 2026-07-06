package com.wavelength.music.data.repository

import com.wavelength.music.data.local.FavoriteDao
import com.wavelength.music.data.local.FavoriteTrackEntity
import com.wavelength.music.data.local.RecentlyPlayedDao
import com.wavelength.music.data.local.RecentlyPlayedEntity
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.model.TrackSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single facade the UI layer talks to. JioSaavn (see [JioSaavnRepository]) is the only online
 * source; [LocalSongRepository] covers files already on the device. It also owns the
 * source-agnostic Room-backed favorites/recently-played tables (a favorite or recently-played
 * entry can point at a JioSaavn or local track alike).
 */
@Singleton
class MusicRepository @Inject constructor(
    private val jioSaavnRepository: JioSaavnRepository,
    private val localSongRepository: LocalSongRepository,
    private val favoriteDao: FavoriteDao,
    private val recentlyPlayedDao: RecentlyPlayedDao
) {

    // --- JioSaavn (the only online source) ------------------------------------------------------

    suspend fun getFeaturedTracks(limit: Int = 20): Result<List<Track>> =
        jioSaavnRepository.searchSongs(FEATURED_SEED_QUERY, limit)

    /** [tag] here is a language/mood term (e.g. "tamil", "hindi") rather than a fixed taxonomy —
     * JioSaavn search already returns language-relevant results for those terms. */
    suspend fun getTracksByTag(tag: String, limit: Int = 20): Result<List<Track>> =
        jioSaavnRepository.searchSongs(tag, limit)

    suspend fun searchTracks(query: String, limit: Int = 30): Result<List<Track>> =
        jioSaavnRepository.searchSongs(query, limit)

    // --- Local device songs --------------------------------------------------------------------

    fun observeLocalSongs(): Flow<List<Track>> = localSongRepository.observeLocalSongs()

    suspend fun rescanLocalLibrary(): Result<Int> = localSongRepository.rescanLibrary()

    // --- Favorites / recently played (source-agnostic, works for any Track) --------------------

    fun observeFavorites(): Flow<List<Track>> = favoriteDao.observeFavorites().map { list ->
        list.map { it.toTrack() }
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
                    audioUrl = track.audioUrl,
                    source = track.source.name
                )
            )
        }
    }

    fun observeRecentlyPlayed(limit: Int = 50): Flow<List<Track>> =
        recentlyPlayedDao.observeRecent(limit).map { list ->
            list.map { it.toTrack() }
        }

    suspend fun recordPlayed(track: Track) {
        recentlyPlayedDao.recordPlay(
            RecentlyPlayedEntity(
                trackId = track.id,
                name = track.name,
                artist = track.artistName,
                albumArtUrl = track.albumArtUrl,
                audioUrl = track.audioUrl,
                source = track.source.name
            )
        )
    }

    private companion object {
        const val FEATURED_SEED_QUERY = "top hits"
    }
}

private fun FavoriteTrackEntity.toTrack(): Track = Track(
    id = id,
    name = name,
    artistId = "",
    artistName = artist,
    albumId = "",
    albumName = "",
    albumArtUrl = albumArtUrl,
    audioUrl = audioUrl,
    durationSeconds = 0,
    source = source.toTrackSource()
)

private fun RecentlyPlayedEntity.toTrack(): Track = Track(
    id = trackId,
    name = name,
    artistId = "",
    artistName = artist,
    albumId = "",
    albumName = "",
    albumArtUrl = albumArtUrl,
    audioUrl = audioUrl,
    durationSeconds = 0,
    source = source.toTrackSource()
)

private fun String.toTrackSource(): TrackSource = runCatching { TrackSource.valueOf(this) }
    .getOrDefault(TrackSource.JIOSAAVN)
