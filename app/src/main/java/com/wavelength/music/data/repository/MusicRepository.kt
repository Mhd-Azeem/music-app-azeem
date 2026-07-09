package com.wavelength.music.data.repository

import com.wavelength.music.data.local.DownloadedTrackEntity
import com.wavelength.music.data.local.FavoriteDao
import com.wavelength.music.data.local.FavoriteTrackEntity
import com.wavelength.music.data.local.PlayEventDao
import com.wavelength.music.data.local.PlayEventEntity
import com.wavelength.music.data.local.PlaylistDao
import com.wavelength.music.data.local.PlaylistEntity
import com.wavelength.music.data.local.PlaylistTrackEntity
import com.wavelength.music.data.local.RecentlyPlayedDao
import com.wavelength.music.data.local.RecentlyPlayedEntity
import com.wavelength.music.data.local.SearchHistoryDao
import com.wavelength.music.data.local.SearchHistoryEntity
import com.wavelength.music.data.local.TrackPlayCount
import com.wavelength.music.data.model.ArtistStat
import com.wavelength.music.data.model.ListeningStats
import com.wavelength.music.data.model.PlaylistSummary
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.model.TrackSource
import android.net.Uri
import com.wavelength.music.data.model.DownloadsSummary
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val playlistDao: PlaylistDao,
    private val downloadRepository: DownloadRepository,
    private val searchHistoryDao: SearchHistoryDao,
    private val playEventDao: PlayEventDao
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

    suspend fun getApiUsage() = jioSaavnRepository.getUsage()

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

    /** For quick actions (like swipe-to-favorite) that don't already know the current state. */
    suspend fun toggleFavoriteAuto(track: Track) {
        toggleFavorite(track, isCurrentlyFavorite = favoriteDao.isFavorite(track.id).first())
    }

    fun observeRecentlyPlayed(limit: Int = 50): Flow<List<Track>> =
        recentlyPlayedDao.observeRecent(limit).map { list ->
            list.map { it.toTrack() }
        }

    suspend fun removeFromRecentlyPlayed(trackId: String) = recentlyPlayedDao.deleteById(trackId)

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
        playEventDao.recordEvent(
            PlayEventEntity(
                trackId = track.id,
                name = track.name,
                artist = track.artistName,
                albumArtUrl = track.albumArtUrl,
                audioUrl = track.audioUrl,
                source = track.source.name
            )
        )
    }

    // --- Listening statistics / smart playlists (derived from the play_events log) --------------

    fun observeListeningStats(): Flow<ListeningStats> = combine(
        playEventDao.observeTotalPlays(),
        playEventDao.observeUniqueTrackCount(),
        playEventDao.observeUniqueArtistCount()
    ) { total, tracks, artists -> ListeningStats(total, tracks, artists) }

    fun observeTopTracks(limit: Int = 20): Flow<List<Track>> =
        playEventDao.observeTopTracks(limit).map { list -> list.map { it.toTrack() } }

    fun observeTopArtists(limit: Int = 10): Flow<List<ArtistStat>> =
        playEventDao.observeTopArtists(limit).map { list ->
            list.map { ArtistStat(it.artist, it.playCount) }
        }

    // --- User-created playlists -----------------------------------------------------------------

    fun observePlaylists(): Flow<List<PlaylistSummary>> = playlistDao.observePlaylistsWithCount()
        .map { list ->
            list.map { PlaylistSummary(it.id, it.name, it.trackCount, it.isFolder, it.parentFolderId) }
        }

    suspend fun createPlaylist(name: String, parentFolderId: Long? = null): Long =
        playlistDao.insertPlaylist(PlaylistEntity(name = name, parentFolderId = parentFolderId))

    suspend fun createFolder(name: String): Long =
        playlistDao.insertPlaylist(PlaylistEntity(name = name, isFolder = true))

    suspend fun movePlaylistToFolder(playlistId: Long, folderId: Long?) =
        playlistDao.movePlaylistToFolder(playlistId, folderId)

    // Deleting a folder moves any playlists inside it back to the top level rather than leaving
    // them pointing at a now-nonexistent parentFolderId (which would make them permanently
    // unreachable in the UI). A no-op for a non-folder playlist, so it's always safe to call.
    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.clearFolderReferences(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun getPlaylistName(playlistId: Long): String? = playlistDao.getPlaylist(playlistId)?.name

    fun observePlaylistTracks(playlistId: Long): Flow<List<Track>> =
        playlistDao.observePlaylistTracks(playlistId).map { list -> list.map { it.toTrack() } }

    suspend fun addTrackToPlaylist(playlistId: Long, track: Track) {
        val nextPosition = playlistDao.maxPosition(playlistId) + 1
        playlistDao.addTrack(
            PlaylistTrackEntity(
                playlistId = playlistId,
                trackId = track.id,
                name = track.name,
                artist = track.artistName,
                albumArtUrl = track.albumArtUrl,
                audioUrl = track.audioUrl,
                source = track.source.name,
                position = nextPosition
            )
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) =
        playlistDao.removeTrack(playlistId, trackId)

    /** Persists a new track order after a drag/move-up/move-down reorder in the UI. */
    suspend fun reorderPlaylistTracks(playlistId: Long, orderedTrackIds: List<String>) {
        orderedTrackIds.forEachIndexed { index, trackId ->
            playlistDao.updatePosition(playlistId, trackId, index)
        }
    }

    // --- Offline downloads -----------------------------------------------------------------------

    fun observeDownloadedTracks(): Flow<List<Track>> = downloadRepository.observeDownloads()
        .map { list -> list.map { it.toTrack() } }

    fun observeDownloadsSummary(): Flow<DownloadsSummary> = downloadRepository.observeDownloads()
        .map { list -> DownloadsSummary(count = list.size, totalSizeBytes = list.sumOf { it.sizeBytes }) }

    fun isDownloaded(trackId: String): Flow<Boolean> = downloadRepository.isDownloaded(trackId)

    suspend fun downloadTrack(track: Track): Result<Unit> = downloadRepository.download(track)

    suspend fun removeDownload(trackId: String) = downloadRepository.removeDownload(trackId)

    suspend fun clearAllDownloads() = downloadRepository.clearAll()

    // --- Search history ----------------------------------------------------------------------------

    fun observeSearchHistory(limit: Int = 15): Flow<List<String>> =
        searchHistoryDao.observeRecent(limit).map { list -> list.map { it.query } }

    suspend fun recordSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        searchHistoryDao.insert(SearchHistoryEntity(query = trimmed))
    }

    suspend fun removeSearchHistoryEntry(query: String) = searchHistoryDao.delete(query)

    suspend fun clearSearchHistory() = searchHistoryDao.clearAll()

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

private fun TrackPlayCount.toTrack(): Track = Track(
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

private fun DownloadedTrackEntity.toTrack(): Track = Track(
    id = id,
    name = name,
    artistId = "",
    artistName = artist,
    albumId = "",
    albumName = "",
    albumArtUrl = albumArtUrl,
    audioUrl = Uri.fromFile(File(filePath)).toString(),
    durationSeconds = 0,
    source = TrackSource.DOWNLOADED
)

private fun String.toTrackSource(): TrackSource = runCatching { TrackSource.valueOf(this) }
    .getOrDefault(TrackSource.JIOSAAVN)
