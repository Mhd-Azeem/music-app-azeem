package com.wavelength.music.ui.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavelength.music.data.model.Album
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.repository.MusicRepository
import com.wavelength.music.playback.PlayerController
import com.wavelength.music.ui.components.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailData(val album: Album, val tracks: List<Track>)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MusicRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val albumId: String = checkNotNull(savedStateHandle["albumId"])

    private val _state = MutableStateFlow<ScreenState<AlbumDetailData>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<AlbumDetailData>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = ScreenState.Loading
            repository.getAlbumTracks(albumId).fold(
                onSuccess = { (album, tracks) ->
                    _state.value = if (tracks.isEmpty()) {
                        ScreenState.Empty
                    } else {
                        ScreenState.Success(AlbumDetailData(album, tracks))
                    }
                },
                onFailure = { e -> _state.value = ScreenState.Error(e.message ?: "Something went wrong") }
            )
        }
    }

    fun playAll() {
        val current = _state.value
        if (current is ScreenState.Success) {
            playerController.playQueue(current.data.tracks, 0)
        }
    }

    fun playTrack(index: Int) {
        val current = _state.value
        if (current is ScreenState.Success) {
            playerController.playQueue(current.data.tracks, index)
        }
    }
}
