package com.wavelength.music.ui.settings

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavelength.music.data.backup.ImportSummary
import com.wavelength.music.data.model.DownloadsSummary
import com.wavelength.music.data.repository.AppSettingsState
import com.wavelength.music.data.repository.BackupRepository
import com.wavelength.music.data.repository.MusicRepository
import com.wavelength.music.data.repository.SettingsRepository
import com.wavelength.music.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class UsageUiState(
    val isLoading: Boolean = true,
    val used: Int? = null,
    val limit: Int? = null,
    val date: String? = null,
    val error: String? = null
)

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val repository: MusicRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    val state: StateFlow<AppSettingsState> = settingsRepository.state
    val customBackgroundFile: File get() = settingsRepository.customBackgroundFile
    val favoriteWallpapers: StateFlow<List<File>> = settingsRepository.favoriteWallpapers

    val downloadsSummary: StateFlow<DownloadsSummary> = repository.observeDownloadsSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DownloadsSummary(0, 0L))

    private val _usageState = MutableStateFlow(UsageUiState())
    val usageState: StateFlow<UsageUiState> = _usageState.asStateFlow()

    init {
        refreshUsage()
    }

    fun refreshUsage() {
        viewModelScope.launch {
            _usageState.value = UsageUiState(isLoading = true)
            repository.getApiUsage()
                .onSuccess { dto ->
                    _usageState.value = UsageUiState(
                        isLoading = false,
                        used = dto.used,
                        limit = dto.limit,
                        date = dto.date
                    )
                }
                .onFailure {
                    _usageState.value = UsageUiState(
                        isLoading = false,
                        error = "Usage tracking isn't set up on this deployment yet."
                    )
                }
        }
    }

    fun selectIcon(preset: IconPreset) = settingsRepository.setIconPreset(preset)

    fun selectTheme(theme: AppTheme) = settingsRepository.setTheme(theme)

    fun pickBackground(bitmap: Bitmap) {
        viewModelScope.launch { settingsRepository.setCustomBackground(bitmap) }
    }

    fun resetBackground() = settingsRepository.resetBackground()

    fun addFavoriteWallpaper(bitmap: Bitmap) {
        viewModelScope.launch { settingsRepository.addFavoriteWallpaper(bitmap) }
    }

    suspend fun addFavoriteWallpapers(uris: List<Uri>): Int = settingsRepository.addFavoriteWallpapers(uris)

    fun removeFavoriteWallpaper(file: File) = settingsRepository.removeFavoriteWallpaper(file)

    fun applyFavoriteWallpaper(file: File) {
        viewModelScope.launch { settingsRepository.applyFavoriteWallpaper(file) }
    }

    fun setBackgroundOpacity(opacity: Float) = settingsRepository.setBackgroundOpacity(opacity)

    fun setExpandUpNextOnScroll(enabled: Boolean) = settingsRepository.setExpandUpNextOnScroll(enabled)

    fun setDynamicThemeFromAlbumArt(enabled: Boolean) = settingsRepository.setDynamicThemeFromAlbumArt(enabled)

    fun setVinylStyleAlbumArt(enabled: Boolean) = settingsRepository.setVinylStyleAlbumArt(enabled)

    fun setAiDjEnabled(enabled: Boolean) = settingsRepository.setAiDjEnabled(enabled)

    fun setCrossfadeDurationMs(durationMs: Int) = settingsRepository.setCrossfadeDurationMs(durationMs)

    fun setAudioVisualizerEnabled(enabled: Boolean) = settingsRepository.setAudioVisualizerEnabled(enabled)

    fun setTrackTransitionEnabled(enabled: Boolean) = settingsRepository.setTrackTransitionEnabled(enabled)

    fun setTrackTransitionDurationMs(durationMs: Int) = settingsRepository.setTrackTransitionDurationMs(durationMs)

    fun clearAllDownloads() {
        viewModelScope.launch { repository.clearAllDownloads() }
    }

    suspend fun exportBackup(uri: Uri): Result<Unit> = backupRepository.exportTo(uri)

    suspend fun importBackup(uri: Uri): Result<ImportSummary> = backupRepository.importFrom(uri)
}
