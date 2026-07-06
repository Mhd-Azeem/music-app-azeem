package com.wavelength.music.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_songs")
data class LocalSongEntity(
    @PrimaryKey val mediaStoreId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: String,
    val albumArtUri: String?
)
