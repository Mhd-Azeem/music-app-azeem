package com.wavelength.music.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query(
        "SELECT playlists.id AS id, playlists.name AS name, playlists.createdAt AS createdAt, " +
            "COUNT(playlist_tracks.entryId) AS trackCount " +
            "FROM playlists LEFT JOIN playlist_tracks ON playlists.id = playlist_tracks.playlistId " +
            "GROUP BY playlists.id ORDER BY playlists.createdAt DESC"
    )
    fun observePlaylistsWithCount(): Flow<List<PlaylistWithCount>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylist(playlistId: Long): PlaylistEntity?

    @Insert
    suspend fun addTrack(track: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrack(playlistId: Long, trackId: String)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun observePlaylistTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>>
}
