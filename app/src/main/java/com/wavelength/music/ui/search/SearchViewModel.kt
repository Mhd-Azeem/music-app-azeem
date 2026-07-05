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
    val albums: List<Album> = emptyList()
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
        val tracksDeferred = viewModelScope.async { repository.searchTracks(q) }
        val artistsDeferred = viewModelScope.async { repository.searchArtists(q) }
        val albumsDeferred = viewModelScope.async { repository.searchAlbums(q) }

        val tracksResult = tracksDeferred.await()
        val artistsResult = artistsDeferred.await()
        val albumsResult = albumsDeferred.await()

        val failure = tracksResult.exceptionOrNull() ?: artistsResult.exceptionOrNull() ?: albumsResult.exceptionOrNull()
        val tracks = tracksResult.getOrNull().orEmpty()
        val artists = artistsResult.getOrNull().orEmpty()
        val albums = albumsResult.getOrNull().orEmpty()

        _results.value = when {
            tracks.isEmpty() && artists.isEmpty() && albums.isEmpty() && failure != null ->
                ScreenState.Error(failure.message ?: "Something went wrong")
            tracks.isEmpty() && artists.isEmpty() && albums.isEmpty() -> ScreenState.Empty
            else -> ScreenState.Success(SearchResults(tracks, artists, albums))
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
