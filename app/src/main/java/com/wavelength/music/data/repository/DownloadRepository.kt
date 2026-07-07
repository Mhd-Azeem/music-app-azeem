package com.wavelength.music.data.repository

import android.content.Context
import com.wavelength.music.data.local.DownloadDao
import com.wavelength.music.data.local.DownloadedTrackEntity
import com.wavelength.music.data.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** Downloads a track's audio to app-internal storage so it can play back without a network
 * connection; tracked in Room so Library's Downloads tab survives process death. */
@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val downloadsDir: File = File(context.filesDir, "downloads").apply { mkdirs() }

    fun observeDownloads(): Flow<List<DownloadedTrackEntity>> = downloadDao.observeAll()

    fun isDownloaded(trackId: String): Flow<Boolean> = downloadDao.isDownloaded(trackId)

    suspend fun download(track: Track): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val destination = File(downloadsDir, "${track.id}.audio")
            URL(track.audioUrl).openStream().use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            downloadDao.insert(
                DownloadedTrackEntity(
                    id = track.id,
                    name = track.name,
                    artist = track.artistName,
                    albumArtUrl = track.albumArtUrl,
                    filePath = destination.absolutePath,
                    source = track.source.name
                )
            )
        }
    }

    suspend fun removeDownload(trackId: String) {
        withContext(Dispatchers.IO) {
            downloadDao.get(trackId)?.let { File(it.filePath).delete() }
            downloadDao.deleteById(trackId)
        }
    }
}
