package com.wavelength.music.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorite_tracks ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteTrackEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE id = :trackId)")
    fun isFavorite(trackId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteTrackEntity)

    @Delete
    suspend fun delete(favorite: FavoriteTrackEntity)

    @Query("DELETE FROM favorite_tracks WHERE id = :trackId")
    suspend fun deleteById(trackId: String)

    @Query("DELETE FROM favorite_tracks WHERE id = :trackId")
    suspend fun deleteIfPresent(trackId: String): Int

    @Transaction
    suspend fun toggle(favorite: FavoriteTrackEntity) {
        if (deleteIfPresent(favorite.id) == 0) insert(favorite)
    }
}
