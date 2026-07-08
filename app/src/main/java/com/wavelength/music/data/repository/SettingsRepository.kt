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
    val expandUpNextOnScroll: Boolean = false
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
        expandUpNextOnScroll = prefs.getBoolean(KEY_EXPAND_UP_NEXT, false)
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
    }
}
