package com.wavelength.music.ui.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavelength.music.data.model.LyricLine
import com.wavelength.music.data.model.PlaylistSummary
import com.wavelength.music.data.repository.LyricsRepository
import com.wavelength.music.data.repository.MusicRepository
import com.wavelength.music.playback.PlaybackUiState
import com.wavelength.music.playback.PlayerController
import com.wavelength.music.playback.SleepTimerController
import com.wavelength.music.playback.SystemVolumeController
import com.wavelength.music.playback.VisualizerController
import com.wavelength.music.ui.components.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val repository: MusicRepository,
    private val sleepTimerController: SleepTimerController,
    private val visualizerController: VisualizerController,
    private val systemVolumeController: SystemVolumeController,
    private val lyricsRepository: LyricsRepository
) : ViewModel() {

    val state: StateFlow<PlaybackUiState> = playerController.state
    val sleepTimerRemainingMs: StateFlow<Long?> = sleepTimerController.remainingMs
    val sleepTimerIsEndOfTrack: StateFlow<Boolean> = sleepTimerController.isEndOfTrack
    val visualizerWaveform: StateFlow<ByteArray?> = visualizerController.waveform
    val systemVolume: StateFlow<Float> = systemVolumeController.volume

    private val _lyrics = MutableStateFlow<ScreenState<List<LyricLine>>>(ScreenState.Empty)
    val lyrics: StateFlow<ScreenState<List<LyricLine>>> = _lyrics.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val isCurrentFavorite: StateFlow<Boolean> = state
        .map { it.currentTrack?.id }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(false) else repository.isFavorite(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val playlists: StateFlow<List<PlaylistSummary>> = repository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        playerController.connect()
    }

    fun playPause() = playerController.playPause()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun skipNext() = playerController.skipNext()
    fun skipPrevious() = playerController.skipPrevious()
    fun toggleShuffle() = playerController.toggleShuffle()
    fun smartShuffle() = playerController.smartShuffleQueue()
    fun cycleRepeatMode() = playerController.cycleRepeatMode()
    fun playQueueItem(index: Int) = playerController.playQueueItem(index)
    fun moveQueueItem(from: Int, to: Int) = playerController.moveQueueItem(from, to)
    fun setVolume(volume: Float) = playerController.setVolume(volume)
    fun setSystemVolume(volume: Float) = systemVolumeController.setVolume(volume)
    fun setPlaybackSpeed(speed: Float) = playerController.setPlaybackSpeed(speed)
    fun setVisualizerCaptureEnabled(enabled: Boolean) = visualizerController.setCaptureEnabled(enabled)

    fun loadLyrics() {
        val track = state.value.currentTrack ?: return
        viewModelScope.launch {
            _lyrics.value = ScreenState.Loading
            lyricsRepository.getSyncedLyrics(track.name, track.artistName, track.durationSeconds).fold(
                onSuccess = { lines ->
                    _lyrics.value = if (lines.isEmpty()) ScreenState.Empty else ScreenState.Success(lines)
                },
                onFailure = { e ->
                    _lyrics.value = ScreenState.Error(e.message ?: "No lyrics found")
                }
            )
        }
    }

    fun startSleepTimer(minutes: Int) = sleepTimerController.startCountdown(minutes * 60_000L)
    fun startSleepTimerEndOfTrack() = sleepTimerController.startEndOfTrack()
    fun cancelSleepTimer() = sleepTimerController.cancel()

    fun toggleFavorite() {
        val track = state.value.currentTrack ?: return
        viewModelScope.launch { repository.toggleFavorite(track, isCurrentFavorite.value) }
    }

    fun addCurrentTrackToPlaylist(playlistId: Long) {
        val track = state.value.currentTrack ?: return
        viewModelScope.launch { repository.addTrackToPlaylist(playlistId, track) }
    }

    fun createPlaylistWithCurrentTrack(name: String) {
        val track = state.value.currentTrack ?: return
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            repository.addTrackToPlaylist(id, track)
        }
    }
}
