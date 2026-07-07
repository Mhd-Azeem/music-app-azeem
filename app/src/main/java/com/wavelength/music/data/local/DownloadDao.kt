package com.wavelength.music.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloaded_tracks ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<DownloadedTrackEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_tracks WHERE id = :trackId)")
    fun isDownloaded(trackId: String): Flow<Boolean>

    @Query("SELECT * FROM downloaded_tracks WHERE id = :trackId")
    suspend fun get(trackId: String): DownloadedTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadedTrackEntity)

    @Query("DELETE FROM downloaded_tracks WHERE id = :trackId")
    suspend fun deleteById(trackId: String)

    @Query("DELETE FROM downloaded_tracks")
    suspend fun clearAll()
}
