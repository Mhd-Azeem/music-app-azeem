package com.wavelength.music.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

@HiltViewModel
class GenreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MusicRepository,
    private val playerController: PlayerController
) : ViewModel() {

    val tag: String = checkNotNull(savedStateHandle["tag"])
    val label: String = checkNotNull(savedStateHandle["label"])

    private val _tracks = MutableStateFlow<ScreenState<List<Track>>>(ScreenState.Loading)
    val tracks: StateFlow<ScreenState<List<Track>>> = _tracks.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _tracks.value = ScreenState.Loading
            repository.getTracksByTag(tag).fold(
                onSuccess = { list ->
                    _tracks.value = if (list.isEmpty()) ScreenState.Empty else ScreenState.Success(list)
                },
                onFailure = { e -> _tracks.value = ScreenState.Error(e.message ?: "Something went wrong") }
            )
        }
    }

    fun playAll() {
        val current = _tracks.value
        if (current is ScreenState.Success) {
            playerController.playQueue(current.data, 0)
        }
    }

    fun playTrack(index: Int) {
        val current = _tracks.value
        if (current is ScreenState.Success) {
            playerController.playQueue(current.data, index)
        }
    }
}
