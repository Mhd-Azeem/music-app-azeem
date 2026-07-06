package com.wavelength.music.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.wavelength.music.data.local.LocalSongDao
import com.wavelength.music.data.local.LocalSongEntity
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.model.TrackSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Songs already stored on the device, scanned via MediaStore and cached in Room so the
 * (potentially slow) content resolver query only has to run again on an explicit rescan.
 */
@Singleton
class LocalSongRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localSongDao: LocalSongDao
) {

    fun observeLocalSongs(): Flow<List<Track>> =
        localSongDao.observeAll().map { entities -> entities.map { it.toTrack() } }

    suspend fun rescanLibrary(): Result<Int> = runCatching {
        withContext(Dispatchers.IO) {
            val songs = queryMediaStore()
            localSongDao.replaceAll(songs)
            songs.size
        }
    }

    private fun queryMediaStore(): List<LocalSongEntity> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val songs = mutableListOf<LocalSongEntity>()
        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                val contentUri = ContentUris.withAppendedId(collection, id)
                val albumArtUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId)

                songs += LocalSongEntity(
                    mediaStoreId = id,
                    title = cursor.getString(titleCol) ?: "Unknown title",
                    artist = cursor.getString(artistCol) ?: "Unknown artist",
                    album = cursor.getString(albumCol).orEmpty(),
                    durationMs = cursor.getLong(durationCol),
                    contentUri = contentUri.toString(),
                    albumArtUri = albumArtUri.toString()
                )
            }
        }
        return songs
    }

    private companion object {
        val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}

private fun LocalSongEntity.toTrack(): Track = Track(
    id = "local_$mediaStoreId",
    name = title,
    artistId = "",
    artistName = artist,
    albumId = "",
    albumName = album,
    albumArtUrl = albumArtUri.orEmpty(),
    audioUrl = contentUri,
    durationSeconds = (durationMs / 1000).toInt(),
    source = TrackSource.LOCAL
)
