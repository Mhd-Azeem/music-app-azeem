package com.wavelength.music.data.repository

import com.wavelength.music.data.local.FavoriteDao
import com.wavelength.music.data.local.FavoriteTrackEntity
import com.wavelength.music.data.local.PlaylistDao
import com.wavelength.music.data.local.PlaylistEntity
import com.wavelength.music.data.local.PlaylistTrackEntity
import com.wavelength.music.data.local.RecentlyPlayedDao
import com.wavelength.music.data.local.RecentlyPlayedEntity
import com.wavelength.music.data.model.PlaylistSummary
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
    private val recentlyPlayedDao: RecentlyPlayedDao,
    private val playlistDao: PlaylistDao
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

    // --- User-created playlists -----------------------------------------------------------------

    fun observePlaylists(): Flow<List<PlaylistSummary>> = playlistDao.observePlaylistsWithCount()
        .map { list -> list.map { PlaylistSummary(it.id, it.name, it.trackCount) } }

    suspend fun createPlaylist(name: String): Long =
        playlistDao.insertPlaylist(PlaylistEntity(name = name))

    suspend fun deletePlaylist(playlistId: Long) = playlistDao.deletePlaylist(playlistId)

    suspend fun getPlaylistName(playlistId: Long): String? = playlistDao.getPlaylist(playlistId)?.name

    fun observePlaylistTracks(playlistId: Long): Flow<List<Track>> =
        playlistDao.observePlaylistTracks(playlistId).map { list -> list.map { it.toTrack() } }

    suspend fun addTrackToPlaylist(playlistId: Long, track: Track) {
        playlistDao.addTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = track.id,
                name = track.name,
                artist = track.artistName,
                albumArtUrl = track.albumArtUrl,
                audioUrl = track.audioUrl,
                source = track.source.name
            )
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) =
        playlistDao.removeTrack(playlistId, trackId)

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

private fun PlaylistTrackEntity.toTrack(): Track = Track(
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
