package com.wavelength.music.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per track play, append-only (unlike [RecentlyPlayedEntity], which dedupes to one row
 * per track) — the history that listening statistics, "Most Played" smart playlists, and Daily
 * Mix are all derived from. */
@Entity(tableName = "play_events")
data class PlayEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val name: String,
    val artist: String,
    val albumArtUrl: String,
    val audioUrl: String,
    val source: String = "JIOSAAVN",
    val playedAt: Long = System.currentTimeMillis()
)
