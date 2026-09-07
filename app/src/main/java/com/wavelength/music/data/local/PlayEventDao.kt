package com.wavelength.music.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class TrackPlayCount(
    val trackId: String,
    val name: String,
    val artist: String,
    val albumArtUrl: String,
    val audioUrl: String,
    val source: String,
    val playCount: Int
)

data class ArtistPlayCount(
    val artist: String,
    val playCount: Int
)

data class TrackIdPlayCount(
    val trackId: String,
    val playCount: Int
)

@Dao
interface PlayEventDao {

    @Insert
    suspend fun insert(event: PlayEventEntity)

    // Caps the log at maxEntries so it doesn't grow unbounded over months of daily use, while
    // staying large enough (default 5000) for meaningful "most played"/statistics aggregation.
    @Query(
        "DELETE FROM play_events WHERE id NOT IN " +
            "(SELECT id FROM play_events ORDER BY playedAt DESC LIMIT :maxEntries)"
    )
    suspend fun trimTo(maxEntries: Int)

    @Transaction
    suspend fun recordEvent(event: PlayEventEntity, maxEntries: Int = 5000) {
        insert(event)
        trimTo(maxEntries)
    }

    @Query("SELECT COUNT(*) FROM play_events")
    fun observeTotalPlays(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT trackId) FROM play_events")
    fun observeUniqueTrackCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT artist) FROM play_events")
    fun observeUniqueArtistCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM play_events WHERE playedAt >= :since")
    fun observePlaysSince(since: Long): Flow<Int>

    @Query(
        "SELECT trackId, name, artist, albumArtUrl, audioUrl, source, COUNT(*) AS playCount " +
            "FROM play_events GROUP BY trackId ORDER BY playCount DESC, MAX(playedAt) DESC LIMIT :limit"
    )
    fun observeTopTracks(limit: Int): Flow<List<TrackPlayCount>>

    @Query(
        "SELECT artist, COUNT(*) AS playCount FROM play_events " +
            "GROUP BY artist ORDER BY playCount DESC, MAX(playedAt) DESC LIMIT :limit"
    )
    fun observeTopArtists(limit: Int): Flow<List<ArtistPlayCount>>

    /** One-shot (not observed) counts for a specific set of tracks — used by smart shuffle to
     * weight a queue reorder by how often each track has actually been played. Tracks with no
     * play history simply don't appear in the result (treat as a 0 count). */
    @Query("SELECT trackId, COUNT(*) AS playCount FROM play_events WHERE trackId IN (:trackIds) GROUP BY trackId")
    suspend fun getPlayCounts(trackIds: List<String>): List<TrackIdPlayCount>
}
