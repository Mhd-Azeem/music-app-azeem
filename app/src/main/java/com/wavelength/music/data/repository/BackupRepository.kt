package com.wavelength.music.data.repository

import android.content.Context
import android.net.Uri
import com.squareup.moshi.Moshi
import com.wavelength.music.data.backup.BackupData
import com.wavelength.music.data.backup.BackupPlaylist
import com.wavelength.music.data.backup.BackupTrack
import com.wavelength.music.data.backup.ImportSummary
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.model.TrackSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Exports/imports favorites and playlists (not folders — see [BackupData]) to a single JSON
 * file the user picks via the system file picker, so it can be backed up anywhere (Drive, email,
 * a computer) and restored after a reinstall or on a fresh install of any future app version. */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    moshi: Moshi,
    private val repository: MusicRepository
) {
    private val adapter = moshi.adapter(BackupData::class.java).indent("  ")

    suspend fun exportTo(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val favorites = repository.observeFavorites().first().map { it.toBackupTrack() }
            val playlistSummaries = repository.observePlaylists().first().filter { !it.isFolder }
            val playlists = playlistSummaries.map { summary ->
                val tracks = repository.observePlaylistTracks(summary.id).first().map { it.toBackupTrack() }
                BackupPlaylist(name = summary.name, tracks = tracks)
            }
            val json = adapter.toJson(BackupData(favorites = favorites, playlists = playlists))
            val stream = context.contentResolver.openOutputStream(uri)
                ?: error("Couldn't open the selected file for writing")
            stream.use { it.write(json.toByteArray()) }
        }
    }

    suspend fun importFrom(uri: Uri): Result<ImportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = context.contentResolver.openInputStream(uri)
                ?: error("Couldn't open the selected file for reading")
            val json = stream.use { it.reader().readText() }
            val data = adapter.fromJson(json) ?: error("This doesn't look like a valid backup file")

            data.favorites.forEach { repository.addFavorite(it.toTrack()) }
            data.playlists.forEach { backupPlaylist ->
                val playlistId = repository.createPlaylist(backupPlaylist.name)
                backupPlaylist.tracks.forEach { repository.addTrackToPlaylist(playlistId, it.toTrack()) }
            }
            ImportSummary(favoriteCount = data.favorites.size, playlistCount = data.playlists.size)
        }
    }
}

private fun Track.toBackupTrack() = BackupTrack(
    id = id,
    name = name,
    artist = artistName,
    albumArtUrl = albumArtUrl,
    audioUrl = audioUrl,
    source = source.name
)

private fun BackupTrack.toTrack(): Track = Track(
    id = id,
    name = name,
    artistId = "",
    artistName = artist,
    albumId = "",
    albumName = "",
    albumArtUrl = albumArtUrl,
    audioUrl = audioUrl,
    durationSeconds = 0,
    source = runCatching { TrackSource.valueOf(source) }.getOrDefault(TrackSource.JIOSAAVN)
)
