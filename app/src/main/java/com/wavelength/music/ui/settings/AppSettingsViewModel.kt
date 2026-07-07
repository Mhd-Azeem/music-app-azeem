package com.wavelength.music.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavelength.music.data.repository.AppSettingsState
import com.wavelength.music.data.repository.SettingsRepository
import com.wavelength.music.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val state: StateFlow<AppSettingsState> = settingsRepository.state
    val customBackgroundFile: File get() = settingsRepository.customBackgroundFile

    fun selectIcon(preset: IconPreset) = settingsRepository.setIconPreset(preset)

    fun selectTheme(theme: AppTheme) = settingsRepository.setTheme(theme)

    fun pickBackground(uri: Uri) {
        viewModelScope.launch { settingsRepository.setCustomBackground(uri) }
    }

    fun resetBackground() = settingsRepository.resetBackground()
}
