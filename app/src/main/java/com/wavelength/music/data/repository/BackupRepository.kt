package com.wavelength.music.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.squareup.moshi.Moshi
import com.wavelength.music.data.backup.BackupData
import com.wavelength.music.data.backup.BackupPlaylist
import com.wavelength.music.data.backup.BackupSettings
import com.wavelength.music.data.backup.BackupTrack
import com.wavelength.music.data.backup.ImportSummary
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.model.TrackSource
import com.wavelength.music.ui.settings.IconPreset
import com.wavelength.music.ui.theme.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** UTF-8 byte-order mark some apps/clouds prepend to text files; stripped before parsing since
 * Moshi's JSON reader treats it as a syntax error rather than whitespace. */
private const val BOM = "\uFEFF"

/** Exports/imports favorites, playlists (not folders — see [BackupData]), every user-configurable
 * setting, the current background, and every favorite wallpaper to a single JSON file the user
 * picks via the system file picker, so it can be backed up anywhere (Drive, email, a computer)
 * and restored after a reinstall or on a fresh install of any future app version. */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    moshi: Moshi,
    private val repository: MusicRepository,
    private val settingsRepository: SettingsRepository
) {
    /** Lenient because backup files often get moved through clouds/chat apps before a restore,
     * which can prepend a UTF-8 BOM or otherwise lightly mangle the JSON without corrupting the
     * data itself — strict parsing rejects those files outright with a cryptic Moshi error. */
    private val adapter = moshi.adapter(BackupData::class.java).indent("  ").lenient()

    suspend fun exportTo(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val favorites = repository.observeFavorites().first().map { it.toBackupTrack() }
            val playlistSummaries = repository.observePlaylists().first().filter { !it.isFolder }
            val playlists = playlistSummaries.map { summary ->
                val tracks = repository.observePlaylistTracks(summary.id).first().map { it.toBackupTrack() }
                BackupPlaylist(name = summary.name, tracks = tracks)
            }

            val settingsState = settingsRepository.state.value
            val settings = BackupSettings(
                iconPreset = settingsState.iconPreset.name,
                theme = settingsState.theme.name,
                backgroundOpacity = settingsState.backgroundOpacity,
                expandUpNextOnScroll = settingsState.expandUpNextOnScroll,
                dynamicThemeFromAlbumArt = settingsState.dynamicThemeFromAlbumArt,
                vinylStyleAlbumArt = settingsState.vinylStyleAlbumArt,
                aiDjEnabled = settingsState.aiDjEnabled,
                crossfadeDurationMs = settingsState.crossfadeDurationMs,
                audioVisualizerEnabled = settingsState.audioVisualizerEnabled,
                trackTransitionEnabled = settingsState.trackTransitionEnabled,
                trackTransitionDurationMs = settingsState.trackTransitionDurationMs
            )
            val customBackgroundBase64 = settingsRepository.customBackgroundFile
                .takeIf { it.exists() }
                ?.let { Base64.encodeToString(it.readBytes(), Base64.NO_WRAP) }
            val favoriteWallpapersBase64 = settingsRepository.favoriteWallpapers.value
                .map { Base64.encodeToString(it.readBytes(), Base64.NO_WRAP) }

            val json = adapter.toJson(
                BackupData(
                    favorites = favorites,
                    playlists = playlists,
                    settings = settings,
                    customBackgroundBase64 = customBackgroundBase64,
                    favoriteWallpapersBase64 = favoriteWallpapersBase64
                )
            )
            val stream = context.contentResolver.openOutputStream(uri)
                ?: error("Couldn't open the selected file for writing")
            stream.use { it.write(json.toByteArray()) }
        }
    }

    suspend fun importFrom(uri: Uri): Result<ImportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = context.contentResolver.openInputStream(uri)
                ?: error("Couldn't open the selected file for reading")
            val json = stream.use { it.reader().readText() }.removePrefix(BOM).trim()
            val data = adapter.fromJson(json) ?: error("This doesn't look like a valid backup file")

            data.favorites.forEach { repository.addFavorite(it.toTrack()) }
            data.playlists.forEach { backupPlaylist ->
                val playlistId = repository.createPlaylist(backupPlaylist.name)
                backupPlaylist.tracks.forEach { repository.addTrackToPlaylist(playlistId, it.toTrack()) }
            }

            val settingsRestored = data.settings?.also { restoreSettings(it) } != null

            data.customBackgroundBase64?.let { base64 ->
                runCatching { Base64.decode(base64, Base64.NO_WRAP) }.getOrNull()?.let { bytes ->
                    settingsRepository.restoreCustomBackground(bytes)
                }
            }

            var wallpaperCount = 0
            data.favoriteWallpapersBase64.forEach { base64 ->
                runCatching { Base64.decode(base64, Base64.NO_WRAP) }.getOrNull()?.let { bytes ->
                    if (settingsRepository.restoreFavoriteWallpaper(bytes).isSuccess) wallpaperCount++
                }
            }

            ImportSummary(
                favoriteCount = data.favorites.size,
                playlistCount = data.playlists.size,
                settingsRestored = settingsRestored,
                wallpaperCount = wallpaperCount
            )
        }
    }

    private fun restoreSettings(settings: BackupSettings) {
        runCatching { IconPreset.valueOf(settings.iconPreset) }.getOrNull()
            ?.let { settingsRepository.setIconPreset(it) }
        runCatching { AppTheme.valueOf(settings.theme) }.getOrNull()
            ?.let { settingsRepository.setTheme(it) }
        settingsRepository.setBackgroundOpacity(settings.backgroundOpacity)
        settingsRepository.setExpandUpNextOnScroll(settings.expandUpNextOnScroll)
        settingsRepository.setDynamicThemeFromAlbumArt(settings.dynamicThemeFromAlbumArt)
        settingsRepository.setVinylStyleAlbumArt(settings.vinylStyleAlbumArt)
        settingsRepository.setAiDjEnabled(settings.aiDjEnabled)
        settingsRepository.setCrossfadeDurationMs(settings.crossfadeDurationMs)
        settingsRepository.setAudioVisualizerEnabled(settings.audioVisualizerEnabled)
        settingsRepository.setTrackTransitionEnabled(settings.trackTransitionEnabled)
        settingsRepository.setTrackTransitionDurationMs(settings.trackTransitionDurationMs)
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
