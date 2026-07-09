package com.wavelength.music.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
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

data class AppSettingsState(
    val iconPreset: IconPreset = IconPreset.CLASSIC,
    val theme: AppTheme = AppTheme.CLASSIC,
    val hasCustomBackground: Boolean = false,
    val backgroundOpacity: Float = DEFAULT_BACKGROUND_OPACITY,
    val expandUpNextOnScroll: Boolean = false,
    val dynamicThemeFromAlbumArt: Boolean = false,
    val vinylStyleAlbumArt: Boolean = false,
    val aiDjEnabled: Boolean = false,
    /** Milliseconds to fade out the ending track and fade in the next one; 0 disables it. */
    val crossfadeDurationMs: Int = 0,
    val audioVisualizerEnabled: Boolean = false
)

const val DEFAULT_BACKGROUND_OPACITY = 0.25f

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("wavelength_settings", Context.MODE_PRIVATE)

    val customBackgroundFile: File = File(context.filesDir, "custom_background.jpg")

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<AppSettingsState> = _state.asStateFlow()

    private fun loadState(): AppSettingsState = AppSettingsState(
        iconPreset = runCatching {
            IconPreset.valueOf(prefs.getString(KEY_ICON, null) ?: IconPreset.CLASSIC.name)
        }.getOrDefault(IconPreset.CLASSIC),
        theme = runCatching {
            AppTheme.valueOf(prefs.getString(KEY_THEME, null) ?: AppTheme.CLASSIC.name)
        }.getOrDefault(AppTheme.CLASSIC),
        hasCustomBackground = customBackgroundFile.exists(),
        backgroundOpacity = prefs.getFloat(KEY_BACKGROUND_OPACITY, DEFAULT_BACKGROUND_OPACITY),
        expandUpNextOnScroll = prefs.getBoolean(KEY_EXPAND_UP_NEXT, false),
        dynamicThemeFromAlbumArt = prefs.getBoolean(KEY_DYNAMIC_THEME, false),
        vinylStyleAlbumArt = prefs.getBoolean(KEY_VINYL_STYLE, false),
        aiDjEnabled = prefs.getBoolean(KEY_AI_DJ, false),
        crossfadeDurationMs = prefs.getInt(KEY_CROSSFADE, 0),
        audioVisualizerEnabled = prefs.getBoolean(KEY_VISUALIZER, false)
    )

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
            _state.update { it.copy(hasCustomBackground = true) }
        }
    }

    fun resetBackground() {
        if (customBackgroundFile.exists()) customBackgroundFile.delete()
        _state.update { it.copy(hasCustomBackground = false) }
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

    fun setVinylStyleAlbumArt(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_VINYL_STYLE, enabled) }
        _state.update { it.copy(vinylStyleAlbumArt = enabled) }
    }

    fun setAiDjEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AI_DJ, enabled) }
        _state.update { it.copy(aiDjEnabled = enabled) }
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
        const val KEY_EXPAND_UP_NEXT = "expand_up_next_on_scroll"
        const val KEY_DYNAMIC_THEME = "dynamic_theme_from_album_art"
        const val KEY_VINYL_STYLE = "vinyl_style_album_art"
        const val KEY_AI_DJ = "ai_dj_enabled"
        const val KEY_CROSSFADE = "crossfade_duration_ms"
        const val KEY_VISUALIZER = "audio_visualizer_enabled"
    }
}
