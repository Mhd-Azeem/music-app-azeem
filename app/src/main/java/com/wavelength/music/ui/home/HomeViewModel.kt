package com.wavelength.music.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavelength.music.data.model.PlaylistSummary
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.repository.MusicRepository
import com.wavelength.music.playback.PlayerController
import com.wavelength.music.ui.components.ScreenState
import com.wavelength.music.ui.search.PendingSearchQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// JioSaavn search already returns language-relevant results for these terms, so "genre" here
// really means "language" — the thing that actually matters for this catalog.
val genreShortcuts = listOf(
    "tamil", "hindi", "telugu", "english", "punjabi", "malayalam", "kannada", "bengali"
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerController: PlayerController,
    private val pendingSearchQuery: PendingSearchQuery
) : ViewModel() {

    private val _featured = MutableStateFlow<ScreenState<List<Track>>>(ScreenState.Loading)
    val featured: StateFlow<ScreenState<List<Track>>> = _featured.asStateFlow()

    val recentlyPlayed: StateFlow<List<Track>> = repository.observeRecentlyPlayed(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistSummary>> = repository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<String>> = repository.observeSearchHistory(8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadFeatured()
    }

    fun loadFeatured() {
        viewModelScope.launch {
            _featured.value = ScreenState.Loading
            repository.getFeaturedTracks().fold(
                onSuccess = { tracks ->
                    _featured.value = if (tracks.isEmpty()) ScreenState.Empty else ScreenState.Success(tracks)
                },
                onFailure = { e ->
                    _featured.value = ScreenState.Error(e.message ?: "Something went wrong")
                }
            )
        }
    }

    fun playTrack(queue: List<Track>, index: Int) {
        playerController.playQueue(queue, index)
    }

    fun prepareSearch(query: String) {
        pendingSearchQuery.set(query)
    }
}
