package com.wavelength.music.data.repository

import com.wavelength.music.data.local.FavoriteDao
import com.wavelength.music.data.local.FavoriteTrackEntity
import com.wavelength.music.data.local.RecentlyPlayedDao
import com.wavelength.music.data.local.RecentlyPlayedEntity
import com.wavelength.music.data.model.Album
import com.wavelength.music.data.model.Artist
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.model.TrackSource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single facade the UI layer talks to, regardless of where a [Track] actually comes from.
 * It delegates browsing/search to [JamendoRepository] and [JioSaavnRepository], device files to
 * [LocalSongRepository], and owns the source-agnostic Room-backed favorites/recently-played
 * tables itself (a favorite or recently-played entry can point at a track from any source).
 *
 * All pre-existing method signatures used by ViewModels are preserved unchanged; everything here
 * is additive.
 */
@Singleton
class MusicRepository @Inject constructor(
    private val jamendoRepository: JamendoRepository,
    private val jioSaavnRepository: JioSaavnRepository,
    private val localSongRepository: LocalSongRepository,
    private val favoriteDao: FavoriteDao,
    private val recentlyPlayedDao: RecentlyPlayedDao
) {

    // --- Jamendo (unchanged behavior) ---------------------------------------------------------

    suspend fun getFeaturedTracks(limit: Int = 20): Result<List<Track>> =
        jamendoRepository.getFeaturedTracks(limit)

    suspend fun getTracksByTag(tag: String, limit: Int = 20): Result<List<Track>> =
        jamendoRepository.getTracksByTag(tag, limit)

    suspend fun searchTracks(query: String, limit: Int = 30): Result<List<Track>> =
        jamendoRepository.searchTracks(query, limit)

    suspend fun searchArtists(query: String, limit: Int = 15): Result<List<Artist>> =
        jamendoRepository.searchArtists(query, limit)

    suspend fun searchAlbums(query: String, limit: Int = 15): Result<List<Album>> =
        jamendoRepository.searchAlbums(query, limit)

    suspend fun getArtistTracks(artistId: String): Result<Pair<Artist, List<Track>>> =
        jamendoRepository.getArtistTracks(artistId)

    suspend fun getAlbumTracks(albumId: String): Result<Pair<Album, List<Track>>> =
        jamendoRepository.getAlbumTracks(albumId)

    // --- JioSaavn (second online source) ------------------------------------------------------

    suspend fun searchJioSaavn(query: String, limit: Int = 20): Result<List<Track>> =
        jioSaavnRepository.searchSongs(query, limit)

    /**
     * Searches Jamendo and JioSaavn concurrently and merges the results, tagging each track with
     * its source. A JioSaavn failure (it's an unofficial, self-hosted API — expect instability)
     * never fails the whole search; [MultiSourceSearchResult.jioSaavnFailed] just flips true so
     * the UI can show a small "unavailable" hint instead of losing the Jamendo results too.
     */
    suspend fun searchAllSources(query: String, limit: Int = 20): MultiSourceSearchResult =
        coroutineScope {
            val jamendoDeferred = async { jamendoRepository.searchTracks(query, limit) }
            val jioSaavnDeferred = async { jioSaavnRepository.searchSongs(query, limit) }

            val jamendoResult = jamendoDeferred.await()
            val jioSaavnResult = jioSaavnDeferred.await()

            MultiSourceSearchResult(
                tracks = jamendoResult.getOrDefault(emptyList()) + jioSaavnResult.getOrDefault(emptyList()),
                jamendoFailed = jamendoResult.isFailure,
                jioSaavnFailed = jioSaavnResult.isFailure,
                error = jamendoResult.exceptionOrNull()
            )
        }

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
}

data class MultiSourceSearchResult(
    val tracks: List<Track>,
    val jamendoFailed: Boolean,
    val jioSaavnFailed: Boolean,
    val error: Throwable?
)

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
    .getOrDefault(TrackSource.JAMENDO)
