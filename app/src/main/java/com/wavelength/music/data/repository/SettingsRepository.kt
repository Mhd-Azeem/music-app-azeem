package com.wavelength.music.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
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

data class AppSettingsState(
    val iconPreset: IconPreset = IconPreset.PHOTO_2,
    val theme: AppTheme = AppTheme.CLASSIC,
    val hasCustomBackground: Boolean = false
)

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
            IconPreset.valueOf(prefs.getString(KEY_ICON, null) ?: IconPreset.PHOTO_2.name)
        }.getOrDefault(IconPreset.PHOTO_2),
        theme = runCatching {
            AppTheme.valueOf(prefs.getString(KEY_THEME, null) ?: AppTheme.CLASSIC.name)
        }.getOrDefault(AppTheme.CLASSIC),
        hasCustomBackground = customBackgroundFile.exists()
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

    suspend fun setCustomBackground(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val input = context.contentResolver.openInputStream(uri)
                ?: error("Could not open the selected image")
            input.use { stream ->
                customBackgroundFile.outputStream().use { output -> stream.copyTo(output) }
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
    }
}
