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
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()
    private var hasMore = true
    private val pageSize = 30

    init {
        load()
    }

    fun load() {
        currentPage = 0
        hasMore = true
        _isLoadingMore.value = false
        viewModelScope.launch {
            _tracks.value = ScreenState.Loading
            repository.getTracksByTag(tag, page = 0, limit = pageSize).fold(
                onSuccess = { list ->
                    val unique = list.distinctBy { it.id }
                    _tracks.value = if (unique.isEmpty()) ScreenState.Empty else ScreenState.Success(unique)
                    hasMore = list.isNotEmpty()
                },
                onFailure = { e -> _tracks.value = ScreenState.Error(e.message ?: "Something went wrong") }
            )
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value || !hasMore) return
        _isLoadingMore.value = true
        viewModelScope.launch {
            try {
                var attempts = 0
                while (attempts < 3 && hasMore) {
                    val current = (_tracks.value as? ScreenState.Success)?.data ?: break
                    val nextPage = currentPage + 1
                    val result = repository.getTracksByTag(tag, page = nextPage, limit = pageSize)
                    val incoming = result.getOrElse {
                        // Keep what is already visible and allow a later scroll to retry.
                        return@launch
                    }

                    currentPage = nextPage
                    if (incoming.isEmpty()) {
                        hasMore = false
                        break
                    }

                    // Some artist searches return short/overlapping pages even though later pages
                    // still exist. Keep paging until the API itself returns an empty page.
                    hasMore = true
                    val existingIds = current.map { it.id }.toHashSet()
                    val newTracks = incoming.filterNot { it.id in existingIds }
                    if (newTracks.isNotEmpty()) {
                        _tracks.value = ScreenState.Success(current + newTracks)
                        break
                    }

                    attempts++
                }
            } finally {
                _isLoadingMore.value = false
            }
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
