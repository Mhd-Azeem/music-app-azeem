package com.wavelength.music.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentlyPlayedDao {

    @Query("SELECT * FROM recently_played ORDER BY timestamp DESC")
    fun observeRecentlyPlayed(): Flow<List<RecentlyPlayedEntity>>

    @Query("SELECT * FROM recently_played ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RecentlyPlayedEntity>>

    @Insert
    suspend fun insert(entry: RecentlyPlayedEntity)

    @Query(
        "DELETE FROM recently_played WHERE entryId NOT IN " +
            "(SELECT entryId FROM recently_played ORDER BY timestamp DESC LIMIT :maxEntries)"
    )
    suspend fun trimTo(maxEntries: Int)

    @Transaction
    suspend fun recordPlay(entry: RecentlyPlayedEntity, maxEntries: Int = 50) {
        insert(entry)
        trimTo(maxEntries)
    }
}
