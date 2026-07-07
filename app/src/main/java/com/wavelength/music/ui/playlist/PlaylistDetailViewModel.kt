package com.wavelength.music.ui.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.repository.MusicRepository
import com.wavelength.music.playback.PlayerController
import com.wavelength.music.ui.components.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MusicRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val playlistId: Long = checkNotNull(savedStateHandle["playlistId"])

    private val _name = MutableStateFlow("Playlist")
    val name: StateFlow<String> = _name.asStateFlow()

    val tracks: StateFlow<ScreenState<List<Track>>> = repository.observePlaylistTracks(playlistId)
        .map { list -> if (list.isEmpty()) ScreenState.Empty else ScreenState.Success(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScreenState.Loading)

    init {
        viewModelScope.launch {
            repository.getPlaylistName(playlistId)?.let { _name.value = it }
        }
    }

    fun playAll() {
        val current = tracks.value
        if (current is ScreenState.Success) {
            playerController.playQueue(current.data, 0)
        }
    }

    fun playTrack(index: Int) {
        val current = tracks.value
        if (current is ScreenState.Success) {
            playerController.playQueue(current.data, index)
        }
    }

    fun removeTrack(track: Track) {
        viewModelScope.launch { repository.removeTrackFromPlaylist(playlistId, track.id) }
    }

    fun moveTrack(from: Int, to: Int) {
        val current = tracks.value
        if (current !is ScreenState.Success) return
        val list = current.data
        if (from !in list.indices || to !in list.indices || from == to) return
        val reordered = list.toMutableList().apply { add(to, removeAt(from)) }
        viewModelScope.launch { repository.reorderPlaylistTracks(playlistId, reordered.map { it.id }) }
    }
}
