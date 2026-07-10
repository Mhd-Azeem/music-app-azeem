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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerController: PlayerController,
    private val pendingSearchQuery: PendingSearchQuery
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<ScreenState<List<Track>>>(ScreenState.Empty)
    val results: StateFlow<ScreenState<List<Track>>> = _results.asStateFlow()

    val searchHistory: StateFlow<List<String>> = repository.observeSearchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null

    init {
        pendingSearchQuery.consume()?.let {
            onQueryChange(it)
            commitSearch()
        }
    }

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
                // distinctBy guards against the unofficial JioSaavn API occasionally returning
                // overlapping/duplicate ids within one result set — the list below is keyed by
                // track.id in Compose, which would crash on a duplicate.
                val deduped = tracks.distinctBy { it.id }
                _results.value = if (deduped.isEmpty()) ScreenState.Empty else ScreenState.Success(deduped)
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

    fun commitSearch() {
        val q = _query.value.trim()
        if (q.isNotEmpty()) {
            viewModelScope.launch { repository.recordSearch(q) }
        }
    }

    fun onHistoryItemClick(historyQuery: String) {
        onQueryChange(historyQuery)
        commitSearch()
    }

    fun removeHistoryEntry(historyQuery: String) {
        viewModelScope.launch { repository.removeSearchHistoryEntry(historyQuery) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearSearchHistory() }
    }
}
