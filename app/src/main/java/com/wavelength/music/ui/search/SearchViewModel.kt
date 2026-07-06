package com.wavelength.music.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.repository.MusicRepository
import com.wavelength.music.playback.PlayerController
import com.wavelength.music.ui.components.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<ScreenState<List<Track>>>(ScreenState.Empty)
    val results: StateFlow<ScreenState<List<Track>>> = _results.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _results.value = ScreenState.Empty
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            _results.value = ScreenState.Loading
            runSearch(newQuery)
        }
    }

    private suspend fun runSearch(q: String) {
        repository.searchTracks(q).fold(
            onSuccess = { tracks ->
                _results.value = if (tracks.isEmpty()) ScreenState.Empty else ScreenState.Success(tracks)
            },
            onFailure = { e ->
                _results.value = ScreenState.Error(e.message ?: "Something went wrong")
            }
        )
    }

    fun retry() {
        val q = _query.value
        if (q.isNotBlank()) {
            viewModelScope.launch {
                _results.value = ScreenState.Loading
                runSearch(q)
            }
        }
    }

    fun playTrack(queue: List<Track>, index: Int) {
        playerController.playQueue(queue, index)
    }
}
