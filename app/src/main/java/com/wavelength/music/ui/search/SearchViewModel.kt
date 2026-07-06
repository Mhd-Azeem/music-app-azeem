package com.wavelength.music.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavelength.music.data.model.Album
import com.wavelength.music.data.model.Artist
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.repository.MusicRepository
import com.wavelength.music.playback.PlayerController
import com.wavelength.music.ui.components.ScreenState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchResults(
    val tracks: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val jioSaavnUnavailable: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<ScreenState<SearchResults>>(ScreenState.Empty)
    val results: StateFlow<ScreenState<SearchResults>> = _results.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _results.value = ScreenState.Empty
            return
        }
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(350)
            _results.value = ScreenState.Loading
            runSearch(newQuery)
        }
    }

    private suspend fun runSearch(q: String) {
        val artistsDeferred = viewModelScope.async { repository.searchArtists(q) }
        val albumsDeferred = viewModelScope.async { repository.searchAlbums(q) }
        // Tracks come from Jamendo + JioSaavn together, each tagged with its Track.source so
        // playback and the UI know where a given result came from.
        val multiSourceDeferred = viewModelScope.async { repository.searchAllSources(q) }

        val artistsResult = artistsDeferred.await()
        val albumsResult = albumsDeferred.await()
        val multiSource = multiSourceDeferred.await()

        val tracks = multiSource.tracks
        val artists = artistsResult.getOrNull().orEmpty()
        val albums = albumsResult.getOrNull().orEmpty()

        // A JioSaavn-only failure never counts as a hard failure — it's an unofficial API that's
        // expected to be flaky, so the search just quietly falls back to whatever else succeeded.
        val hardFailure = artistsResult.exceptionOrNull()
            ?: albumsResult.exceptionOrNull()
            ?: multiSource.error.takeIf { multiSource.jamendoFailed }

        _results.value = when {
            tracks.isEmpty() && artists.isEmpty() && albums.isEmpty() && hardFailure != null ->
                ScreenState.Error(hardFailure.message ?: "Something went wrong")
            tracks.isEmpty() && artists.isEmpty() && albums.isEmpty() -> ScreenState.Empty
            else -> ScreenState.Success(
                SearchResults(tracks, artists, albums, jioSaavnUnavailable = multiSource.jioSaavnFailed)
            )
        }
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
