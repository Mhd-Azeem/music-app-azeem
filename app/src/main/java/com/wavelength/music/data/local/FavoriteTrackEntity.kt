package com.wavelength.music.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artist: String,
    val albumArtUrl: String,
    val audioUrl: String,
    val source: String = "JIOSAAVN",
    val addedAt: Long = System.currentTimeMillis()
)
