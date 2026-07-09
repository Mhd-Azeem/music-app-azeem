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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    private val _suggested = MutableStateFlow<ScreenState<List<Track>>>(ScreenState.Loading)
    val suggested: StateFlow<ScreenState<List<Track>>> = _suggested.asStateFlow()

    val recentlyPlayed: StateFlow<List<Track>> = repository.observeRecentlyPlayed(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistSummary>> = repository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<String>> = repository.observeSearchHistory(8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadFeatured()
        viewModelScope.launch {
            repository.observeRecentlyPlayed(30).collectLatest { tracks -> loadSuggested(tracks) }
        }
    }

    /** Mixes general "top hits" with Tamil results so the featured carousel isn't purely
     * English/Hindi-leaning — JioSaavn search is per-language, so there's no single query that
     * covers both. */
    fun loadFeatured() {
        viewModelScope.launch {
            _featured.value = ScreenState.Loading
            coroutineScope {
                val topHitsDeferred = async { repository.getFeaturedTracks(20) }
                val tamilDeferred = async { repository.getTracksByTag("tamil", 10) }
                val topHits = topHitsDeferred.await()
                val tamilHits = tamilDeferred.await()
                val combined = (tamilHits.getOrDefault(emptyList()) + topHits.getOrDefault(emptyList()))
                    .distinctBy { it.id }
                _featured.value = when {
                    combined.isNotEmpty() -> ScreenState.Success(combined)
                    topHits.isFailure -> ScreenState.Error(topHits.exceptionOrNull()?.message ?: "Something went wrong")
                    else -> ScreenState.Empty
                }
            }
        }
    }

    /** Seeds "suggested for you" from whichever artist appears most often in recent plays, then
     * searches JioSaavn for more from that artist, excluding tracks already recently played. */
    private suspend fun loadSuggested(recentTracks: List<Track>) {
        val topArtist = recentTracks.groupingBy { it.artistName }.eachCount().maxByOrNull { it.value }?.key
        if (topArtist.isNullOrBlank()) {
            _suggested.value = ScreenState.Empty
            return
        }
        _suggested.value = ScreenState.Loading
        repository.searchTracks(topArtist, limit = 20).fold(
            onSuccess = { tracks ->
                val excludeIds = recentTracks.map { it.id }.toSet()
                val filtered = tracks.filterNot { it.id in excludeIds }
                _suggested.value = if (filtered.isEmpty()) ScreenState.Empty else ScreenState.Success(filtered)
            },
            onFailure = { e ->
                _suggested.value = ScreenState.Error(e.message ?: "Something went wrong")
            }
        )
    }

    fun playTrack(queue: List<Track>, index: Int) {
        playerController.playQueue(queue, index)
    }

    fun prepareSearch(query: String) {
        pendingSearchQuery.set(query)
    }
}
