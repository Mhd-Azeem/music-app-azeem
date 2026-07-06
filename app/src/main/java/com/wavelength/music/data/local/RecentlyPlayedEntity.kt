package com.wavelength.music.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
    val trackId: String,
    val name: String,
    val artist: String,
    val albumArtUrl: String,
    val audioUrl: String,
    val source: String = "JIOSAAVN",
    val timestamp: Long = System.currentTimeMillis()
)
