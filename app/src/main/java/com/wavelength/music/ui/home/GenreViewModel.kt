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

    private var currentPage = 0
    private var isLoadingMore = false
    private var hasMore = true
    private val pageSize = 30

    init {
        load()
    }

    fun load() {
        currentPage = 0
        hasMore = true
        isLoadingMore = false
        viewModelScope.launch {
            _tracks.value = ScreenState.Loading
            repository.getTracksByTag(tag, page = 0, limit = pageSize).fold(
                onSuccess = { list ->
                    val unique = list.distinctBy { it.id }
                    _tracks.value = if (unique.isEmpty()) ScreenState.Empty else ScreenState.Success(unique)
                    hasMore = list.size >= pageSize
                },
                onFailure = { e -> _tracks.value = ScreenState.Error(e.message ?: "Something went wrong") }
            )
        }
    }

    fun loadMore() {
        if (isLoadingMore || !hasMore) return
        val current = (_tracks.value as? ScreenState.Success)?.data ?: return
        isLoadingMore = true
        val nextPage = currentPage + 1
        viewModelScope.launch {
            repository.getTracksByTag(tag, page = nextPage, limit = pageSize).fold(
                onSuccess = { incoming ->
                    val existingIds = current.map { it.id }.toHashSet()
                    val newTracks = incoming.filterNot { it.id in existingIds }
                    _tracks.value = ScreenState.Success(current + newTracks)
                    currentPage = nextPage
                    hasMore = incoming.size >= pageSize
                },
                onFailure = { /* Keep the already loaded songs visible; next scroll can retry. */ }
            )
            isLoadingMore = false
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
