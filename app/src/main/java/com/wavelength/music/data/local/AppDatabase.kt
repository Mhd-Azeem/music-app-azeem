package com.wavelength.music.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteTrackEntity::class, RecentlyPlayedEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
}
