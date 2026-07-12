package com.wavelength.music.data.backup

import com.squareup.moshi.JsonClass

/** On-disk shape of a local backup file. Folders aren't preserved (only playlists and their
 * tracks, plus favorites) — restoring rebuilds flat playlists, which is enough to get your
 * library back after a reinstall or update without needing any server.
 *
 * `@JsonClass(generateAdapter = true)` gives this (and [BackupPlaylist]/[BackupTrack]) a
 * compile-time-generated Moshi adapter instead of falling back to Moshi's runtime-reflection
 * Kotlin adapter, which needs extra ProGuard/R8 keep rules to survive minification and is slower
 * to resolve at runtime besides. */
@JsonClass(generateAdapter = true)
data class BackupData(
    val version: Int = 2,
    val favorites: List<BackupTrack> = emptyList(),
    val playlists: List<BackupPlaylist> = emptyList(),
    val settings: BackupSettings? = null,
    /** Base64 JPEG of the currently-applied background, if any. Null on older backups (version 1)
     * and simply skipped on import — no image data existed in the file to restore. */
    val customBackgroundBase64: String? = null,
    /** Base64 JPEGs of every saved favorite wallpaper. */
    val favoriteWallpapersBase64: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BackupPlaylist(
    val name: String,
    val tracks: List<BackupTrack> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BackupTrack(
    val id: String,
    val name: String,
    val artist: String,
    val albumArtUrl: String,
    val audioUrl: String,
    val source: String
)

/** Snapshot of every user-configurable setting, so a restore puts the app back exactly how it
 * looked/behaved, not just the music library. Enums are stored by name and re-parsed defensively
 * on import, matching how [com.wavelength.music.data.repository.SettingsRepository] itself
 * already loads them from SharedPreferences. */
@JsonClass(generateAdapter = true)
data class BackupSettings(
    val iconPreset: String,
    val theme: String,
    val backgroundOpacity: Float,
    val expandUpNextOnScroll: Boolean,
    val dynamicThemeFromAlbumArt: Boolean,
    val vinylStyleAlbumArt: Boolean,
    val aiDjEnabled: Boolean,
    val crossfadeDurationMs: Int,
    val audioVisualizerEnabled: Boolean,
    val trackTransitionEnabled: Boolean,
    val trackTransitionDurationMs: Int,
    val syncVolumeWithSystem: Boolean = true
)

data class ImportSummary(
    val favoriteCount: Int,
    val playlistCount: Int,
    val settingsRestored: Boolean = false,
    val wallpaperCount: Int = 0
)
