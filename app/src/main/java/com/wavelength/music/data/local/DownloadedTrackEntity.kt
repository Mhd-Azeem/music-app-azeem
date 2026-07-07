package com.wavelength.music.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_tracks")
data class DownloadedTrackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artist: String,
    val albumArtUrl: String,
    val filePath: String,
    val source: String = "JIOSAAVN",
    val downloadedAt: Long = System.currentTimeMillis()
)
