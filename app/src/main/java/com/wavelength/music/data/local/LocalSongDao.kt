package com.wavelength.music.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalSongDao {

    @Query("SELECT * FROM local_songs ORDER BY title ASC")
    fun observeAll(): Flow<List<LocalSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<LocalSongEntity>)

    @Query("DELETE FROM local_songs")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(songs: List<LocalSongEntity>) {
        clearAll()
        insertAll(songs)
    }
}
