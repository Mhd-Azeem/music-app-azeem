package com.wavelength.music.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteTrackEntity::class,
        RecentlyPlayedEntity::class,
        LocalSongEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        DownloadedTrackEntity::class,
        SearchHistoryEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
    abstract fun localSongDao(): LocalSongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun downloadDao(): DownloadDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}
