package com.wavelength.music.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.edit
import com.wavelength.music.ui.settings.IconPreset
import com.wavelength.music.ui.theme.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class BuiltInWallpaper(val label: String) {
    DEFAULT("AZ Music"),
    AURORA("Aurora"),
    NEON("Neon Glow"),
    SUNSET_GLOW("Sunset Glow"),
    OCEAN_SHINE("Ocean Shine"),
    PURPLE_SHINE("Purple Shine")
}


enum class AlbumArtStyle(val label: String, val description: String) {
    OFF("Off", "Static album cover"),
    VINYL("Vinyl", "Spinning record-style artwork"),
    PARALLAX("Parallax", "Follows the phone's tilt"),
    DEPTH_FLOAT("Depth Float", "Slow floating depth movement"),
    BASS_ZOOM("Bass Zoom", "Smooth zoom driven by real bass strength"),
    SPATIAL_FLOAT("Spatial Float", "Strong gyro depth with inertial movement")
}

data class AppSettingsState(
    val iconPreset: IconPreset = IconPreset.CLASSIC,
    val theme: AppTheme = AppTheme.CLASSIC,
    val hasCustomBackground: Boolean = false,
    /** Bumped on every write to the background file, even when [hasCustomBackground] itself stays
     * `true` (picking a new photo while one's already set). `MutableStateFlow` skips emitting when
     * an update produces a structurally-equal value, so without this a straight `true` -> `true`
     * background change would silently never reach collectors — the UI would only pick up the new
     * photo after some other field also changed (e.g. resetting first, which flips this same
     * boolean to `false`). */
    val backgroundVersion: Long = 0L,
    val backgroundOpacity: Float = DEFAULT_BACKGROUND_OPACITY,
    val builtInWallpaper: BuiltInWallpaper = BuiltInWallpaper.DEFAULT,
    val customAccentArgb: Int = DEFAULT_CUSTOM_ACCENT_ARGB,
    val expandUpNextOnScroll: Boolean = true,
    val dynamicThemeFromAlbumArt: Boolean = false,
    val albumArtStyle: AlbumArtStyle = AlbumArtStyle.OFF,
    val aiDjEnabled: Boolean = false,
    /** Native seamless playlist transitions when crossfade is off. */
    val gaplessPlaybackEnabled: Boolean = true,
    /** Milliseconds to fade out the ending track and fade in the next one; 0 disables it. */
    val crossfadeDurationMs: Int = 0,
    val audioVisualizerEnabled: Boolean = false,
    /** Whether album art/title crossfade when the track changes (swipe, transport buttons, or
     * auto-advance) — separate from [crossfadeDurationMs], which fades the *audio* between songs. */
    val trackTransitionEnabled: Boolean = true,
    val trackTransitionDurationMs: Int = DEFAULT_TRACK_TRANSITION_DURATION_MS,
    /** Whether the Now Playing volume slider controls the device's actual media volume (the same
     * one the hardware rocker controls) instead of an app-only software gain. */
    val syncVolumeWithSystem: Boolean = true,
    /** When enabled in Liquid themes, the current track artwork fills the Now Playing backdrop. */
    val liquidAlbumArtBackground: Boolean = false
)

const val DEFAULT_BACKGROUND_OPACITY = 0.25f
const val DEFAULT_TRACK_TRANSITION_DURATION_MS = 300
const val DEFAULT_CUSTOM_ACCENT_ARGB: Int = 0xFF22D3EE.toInt()

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("wavelength_settings", Context.MODE_PRIVATE)

    val customBackgroundFile: File = File(context.filesDir, "custom_background.jpg")

    private val favoriteWallpapersDir: File = File(context.filesDir, "favorite_wallpapers").apply { mkdirs() }

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<AppSettingsState> = _state.asStateFlow()

    private val _favoriteWallpapers = MutableStateFlow(listFavoriteWallpapersFromDisk())
    val favoriteWallpapers: StateFlow<List<File>> = _favoriteWallpapers.asStateFlow()

    private fun listFavoriteWallpapersFromDisk(): List<File> =
        favoriteWallpapersDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    private fun loadState(): AppSettingsState = AppSettingsState(
        iconPreset = runCatching {
            IconPreset.valueOf(prefs.getString(KEY_ICON, null) ?: IconPreset.CLASSIC.name)
        }.getOrDefault(IconPreset.CLASSIC),
        theme = runCatching {
            AppTheme.valueOf(prefs.getString(KEY_THEME, null) ?: AppTheme.CLASSIC.name)
        }.getOrDefault(AppTheme.CLASSIC),
        hasCustomBackground = customBackgroundFile.exists(),
        backgroundOpacity = prefs.getFloat(KEY_BACKGROUND_OPACITY, DEFAULT_BACKGROUND_OPACITY),
        builtInWallpaper = runCatching {
            BuiltInWallpaper.valueOf(prefs.getString(KEY_BUILT_IN_WALLPAPER, null) ?: BuiltInWallpaper.DEFAULT.name)
        }.getOrDefault(BuiltInWallpaper.DEFAULT),
        customAccentArgb = prefs.getInt(KEY_CUSTOM_ACCENT_ARGB, DEFAULT_CUSTOM_ACCENT_ARGB),
        expandUpNextOnScroll = prefs.getBoolean(KEY_EXPAND_UP_NEXT, true),
        dynamicThemeFromAlbumArt = prefs.getBoolean(KEY_DYNAMIC_THEME, false),
        albumArtStyle = loadAlbumArtStyle(),
        aiDjEnabled = prefs.getBoolean(KEY_AI_DJ, false),
        gaplessPlaybackEnabled = prefs.getBoolean(KEY_GAPLESS_PLAYBACK, true),
        crossfadeDurationMs = prefs.getInt(KEY_CROSSFADE, 0),
        audioVisualizerEnabled = prefs.getBoolean(KEY_VISUALIZER, false),
        trackTransitionEnabled = prefs.getBoolean(KEY_TRACK_TRANSITION_ENABLED, true),
        trackTransitionDurationMs = prefs.getInt(KEY_TRACK_TRANSITION_DURATION, DEFAULT_TRACK_TRANSITION_DURATION_MS),
        syncVolumeWithSystem = prefs.getBoolean(KEY_SYNC_VOLUME_WITH_SYSTEM, true),
        liquidAlbumArtBackground = false
    )

    private fun loadAlbumArtStyle(): AlbumArtStyle {
        val stored = prefs.getString(KEY_ALBUM_ART_STYLE, null)
        if (!stored.isNullOrBlank()) {
            return runCatching { AlbumArtStyle.valueOf(stored) }.getOrDefault(AlbumArtStyle.OFF)
        }

        // One-time migration from the old independent switches. Beat Bounce is intentionally
        // removed; users who had it enabled move to the new continuous real-bass Bass Zoom.
        return when {
            prefs.getBoolean(KEY_BEAT_BOUNCE_ALBUM_ART, false) -> AlbumArtStyle.BASS_ZOOM
            prefs.getBoolean(KEY_PARALLAX_ALBUM_ART, false) -> AlbumArtStyle.PARALLAX
            prefs.getBoolean(KEY_VINYL_STYLE, false) -> AlbumArtStyle.VINYL
            else -> AlbumArtStyle.OFF
        }
    }

    fun setIconPreset(preset: IconPreset) {
        prefs.edit { putString(KEY_ICON, preset.name) }
        applyIconPreset(preset)
        _state.update { it.copy(iconPreset = preset) }
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit { putString(KEY_THEME, theme.name) }
        _state.update { it.copy(theme = theme) }
    }

    suspend fun setCustomBackground(bitmap: Bitmap): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            customBackgroundFile.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
            }
            Unit
        }.onSuccess {
            _state.update { it.copy(hasCustomBackground = true, backgroundVersion = System.nanoTime()) }
        }
    }

    fun resetBackground() {
        if (customBackgroundFile.exists()) customBackgroundFile.delete()
        _state.update { it.copy(hasCustomBackground = false) }
    }

    /** Used by [com.wavelength.music.data.repository.BackupRepository] to restore the background
     * from raw already-decoded JPEG bytes (a backup's base64 payload), rather than a freshly
     * picked [Bitmap] that would need re-compressing. */
    suspend fun restoreCustomBackground(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            customBackgroundFile.writeBytes(bytes)
        }.onSuccess {
            _state.update { it.copy(hasCustomBackground = true, backgroundVersion = System.nanoTime()) }
        }
    }

    /** Saves [bitmap] as a new favorite wallpaper, separate from the currently-applied
     * background — lets the user build up a small gallery of pictures to switch between later
     * without re-picking/re-cropping from their device photos each time. */
    suspend fun addFavoriteWallpaper(bitmap: Bitmap): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(favoriteWallpapersDir, "wallpaper_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
            }
            Unit
        }.onSuccess {
            _favoriteWallpapers.value = listFavoriteWallpapersFromDisk()
        }
    }

    /** Adds every picked image directly as a new favorite wallpaper, skipping the crop step and
     * bitmap re-encode that [addFavoriteWallpaper] does — lets the user build up a gallery from
     * several gallery picks at once instead of cropping and confirming one at a time. Returns how
     * many of [uris] were saved successfully. */
    suspend fun addFavoriteWallpapers(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var count = 0
        uris.forEach { uri ->
            runCatching {
                val file = File(favoriteWallpapersDir, "wallpaper_${System.nanoTime()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Couldn't open picked image")
            }.onSuccess { count++ }
        }
        _favoriteWallpapers.value = listFavoriteWallpapersFromDisk()
        count
    }

    fun removeFavoriteWallpaper(file: File) {
        if (file.exists()) file.delete()
        _favoriteWallpapers.value = listFavoriteWallpapersFromDisk()
    }

    /** Restores a favorite wallpaper from raw already-decoded JPEG bytes (a backup's base64
     * payload) as a new file — mirrors [addFavoriteWallpaper] but skips the Bitmap round-trip
     * since the bytes are already a valid JPEG. [System.nanoTime] (rather than
     * [System.currentTimeMillis]) avoids filename collisions when restoring many wallpapers in
     * quick succession. */
    suspend fun restoreFavoriteWallpaper(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(favoriteWallpapersDir, "wallpaper_${System.nanoTime()}.jpg")
            file.writeBytes(bytes)
        }.onSuccess {
            _favoriteWallpapers.value = listFavoriteWallpapersFromDisk()
        }
    }

    /** Applies a saved favorite as the current background immediately, reusing
     * [customBackgroundFile]'s existing cache-busted-by-lastModified() display path — no separate
     * "current wallpaper" state needed. */
    suspend fun applyFavoriteWallpaper(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            file.copyTo(customBackgroundFile, overwrite = true)
            Unit
        }.onSuccess {
            _state.update { it.copy(hasCustomBackground = true, backgroundVersion = System.nanoTime()) }
        }
    }

    fun setBuiltInWallpaper(wallpaper: BuiltInWallpaper) {
        prefs.edit { putString(KEY_BUILT_IN_WALLPAPER, wallpaper.name) }
        if (customBackgroundFile.exists()) customBackgroundFile.delete()
        _state.update {
            it.copy(
                builtInWallpaper = wallpaper,
                hasCustomBackground = false,
                backgroundVersion = System.nanoTime()
            )
        }
    }

    fun setCustomAccentArgb(argb: Int) {
        prefs.edit { putInt(KEY_CUSTOM_ACCENT_ARGB, argb) }
        _state.update { it.copy(customAccentArgb = argb) }
    }

    fun setBackgroundOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0f, 1f)
        prefs.edit { putFloat(KEY_BACKGROUND_OPACITY, clamped) }
        _state.update { it.copy(backgroundOpacity = clamped) }
    }

    fun setExpandUpNextOnScroll(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_EXPAND_UP_NEXT, enabled) }
        _state.update { it.copy(expandUpNextOnScroll = enabled) }
    }

    fun setDynamicThemeFromAlbumArt(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DYNAMIC_THEME, enabled) }
        _state.update { it.copy(dynamicThemeFromAlbumArt = enabled) }
    }

    fun setAlbumArtStyle(style: AlbumArtStyle) {
        prefs.edit {
            putString(KEY_ALBUM_ART_STYLE, style.name)
            // Clear legacy switches after migration so they can never fight the enum again.
            putBoolean(KEY_VINYL_STYLE, false)
            putBoolean(KEY_PARALLAX_ALBUM_ART, false)
            putBoolean(KEY_BEAT_BOUNCE_ALBUM_ART, false)
        }
        _state.update { it.copy(albumArtStyle = style) }
    }

    fun setAiDjEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AI_DJ, enabled) }
        _state.update { it.copy(aiDjEnabled = enabled) }
    }

    fun setGaplessPlaybackEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_GAPLESS_PLAYBACK, enabled) }
        _state.update { it.copy(gaplessPlaybackEnabled = enabled) }
    }

    fun setCrossfadeDurationMs(durationMs: Int) {
        val clamped = durationMs.coerceIn(0, 8000)
        prefs.edit { putInt(KEY_CROSSFADE, clamped) }
        _state.update { it.copy(crossfadeDurationMs = clamped) }
    }

    fun setAudioVisualizerEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_VISUALIZER, enabled) }
        _state.update { it.copy(audioVisualizerEnabled = enabled) }
    }

    fun setTrackTransitionEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_TRACK_TRANSITION_ENABLED, enabled) }
        _state.update { it.copy(trackTransitionEnabled = enabled) }
    }

    fun setTrackTransitionDurationMs(durationMs: Int) {
        val clamped = durationMs.coerceIn(100, 1000)
        prefs.edit { putInt(KEY_TRACK_TRANSITION_DURATION, clamped) }
        _state.update { it.copy(trackTransitionDurationMs = clamped) }
    }

    fun setSyncVolumeWithSystem(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SYNC_VOLUME_WITH_SYSTEM, enabled) }
        _state.update { it.copy(syncVolumeWithSystem = enabled) }
    }

    fun setLiquidAlbumArtBackground(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_LIQUID_ALBUM_ART_BACKGROUND, enabled) }
        _state.update { it.copy(liquidAlbumArtBackground = enabled) }
    }

    /** Enables the alias matching [preset] and disables the others, so exactly one launcher
     * icon is ever active at a time. */
    private fun applyIconPreset(preset: IconPreset) {
        val packageManager = context.packageManager
        IconPreset.entries.forEach { candidate ->
            val newState = if (candidate == preset) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            packageManager.setComponentEnabledSetting(
                ComponentName(context.packageName, context.packageName + candidate.aliasSuffix),
                newState,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    private companion object {
        const val KEY_ICON = "icon_preset"
        const val KEY_THEME = "theme"
        const val KEY_BACKGROUND_OPACITY = "background_opacity"
        const val KEY_BUILT_IN_WALLPAPER = "built_in_wallpaper"
        const val KEY_CUSTOM_ACCENT_ARGB = "custom_accent_argb"
        const val KEY_EXPAND_UP_NEXT = "expand_up_next_on_scroll"
        const val KEY_DYNAMIC_THEME = "dynamic_theme_from_album_art"
        const val KEY_ALBUM_ART_STYLE = "album_art_style"
        const val KEY_VINYL_STYLE = "vinyl_style_album_art"
        const val KEY_PARALLAX_ALBUM_ART = "parallax_album_art"
        const val KEY_BEAT_BOUNCE_ALBUM_ART = "beat_bounce_album_art"
        const val KEY_AI_DJ = "ai_dj_enabled"
        const val KEY_GAPLESS_PLAYBACK = "gapless_playback_enabled"
        const val KEY_CROSSFADE = "crossfade_duration_ms"
        const val KEY_VISUALIZER = "audio_visualizer_enabled"
        const val KEY_TRACK_TRANSITION_ENABLED = "track_transition_enabled"
        const val KEY_TRACK_TRANSITION_DURATION = "track_transition_duration_ms"
        const val KEY_SYNC_VOLUME_WITH_SYSTEM = "sync_volume_with_system"
        const val KEY_LIQUID_ALBUM_ART_BACKGROUND = "liquid_album_art_background"
    }
}
