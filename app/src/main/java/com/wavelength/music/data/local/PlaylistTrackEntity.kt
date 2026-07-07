package com.wavelength.music.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Denormalized (like FavoriteTrackEntity/RecentlyPlayedEntity) so a playlist can list its
 * tracks without needing a network round-trip back to whichever source they came from. */
@Entity(tableName = "playlist_tracks")
data class PlaylistTrackEntity(
    @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
    val playlistId: Long,
    val trackId: String,
    val name: String,
    val artist: String,
    val albumArtUrl: String,
    val audioUrl: String,
    val source: String = "JIOSAAVN",
    val addedAt: Long = System.currentTimeMillis()
)
