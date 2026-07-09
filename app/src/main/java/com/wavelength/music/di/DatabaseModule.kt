package com.wavelength.music.di

import android.content.Context
import androidx.room.Room
import com.wavelength.music.data.local.AppDatabase
import com.wavelength.music.data.local.DownloadDao
import com.wavelength.music.data.local.FavoriteDao
import com.wavelength.music.data.local.LocalSongDao
import com.wavelength.music.data.local.PlayEventDao
import com.wavelength.music.data.local.PlaylistDao
import com.wavelength.music.data.local.RecentlyPlayedDao
import com.wavelength.music.data.local.SearchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "wavelength.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun provideRecentlyPlayedDao(database: AppDatabase): RecentlyPlayedDao =
        database.recentlyPlayedDao()

    @Provides
    fun provideLocalSongDao(database: AppDatabase): LocalSongDao = database.localSongDao()

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideDownloadDao(database: AppDatabase): DownloadDao = database.downloadDao()

    @Provides
    fun provideSearchHistoryDao(database: AppDatabase): SearchHistoryDao = database.searchHistoryDao()

    @Provides
    fun providePlayEventDao(database: AppDatabase): PlayEventDao = database.playEventDao()
}
