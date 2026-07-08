package com.wavelength.music.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentlyPlayedDao {

    @Query("SELECT * FROM recently_played ORDER BY timestamp DESC")
    fun observeRecentlyPlayed(): Flow<List<RecentlyPlayedEntity>>

    @Query("SELECT * FROM recently_played ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RecentlyPlayedEntity>>

    // Replays of a track already in the table overwrite its row (same trackId, via the unique
    // index) rather than adding a duplicate entry, so a repeatedly-played song still only takes
    // one slot in "Recently Played" — just with its timestamp bumped to the most recent play.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: RecentlyPlayedEntity)

    @Query("DELETE FROM recently_played WHERE trackId = :trackId")
    suspend fun deleteById(trackId: String)

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
