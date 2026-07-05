package com.wavelength.music.ui.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavelength.music.data.model.Artist
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

data class ArtistDetailData(val artist: Artist, val tracks: List<Track>)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MusicRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val artistId: String = checkNotNull(savedStateHandle["artistId"])

    private val _state = MutableStateFlow<ScreenState<ArtistDetailData>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<ArtistDetailData>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = ScreenState.Loading
            repository.getArtistTracks(artistId).fold(
                onSuccess = { (artist, tracks) ->
                    _state.value = if (tracks.isEmpty()) {
                        ScreenState.Empty
                    } else {
                        ScreenState.Success(ArtistDetailData(artist, tracks))
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
