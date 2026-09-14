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

    private val _selectedLanguage = MutableStateFlow("All")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    val searchHistory: StateFlow<List<String>> = repository.observeSearchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null

    private var pagedQuery: String = ""
    private var currentPage = 0
    private var canLoadMore = true

    fun consumePendingSearch() {
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
        val selected = _selectedLanguage.value
        val languageSuffix = selected.takeUnless { it == "All" }?.let { " $it" }.orEmpty()
        val effectiveQuery = "$q$languageSuffix"
        pagedQuery = effectiveQuery
        currentPage = 0
        canLoadMore = true

        repository.searchTracks(effectiveQuery, page = 0, limit = SEARCH_PAGE_SIZE).fold(
            onSuccess = { primaryTracks ->
                // JioSaavn can perform poorly when a version keyword such as "slowed" or
                // "reverb" is appended to an otherwise exact song title. In that case, also
                // search the base title and merge both result sets before ranking. This keeps
                // the intended song family in the candidate pool instead of returning unrelated
                // tracks that merely contain "slowed".
                val baseQuery = stripVersionIntent(q)
                val fallbackTracks = if (
                    baseQuery.isNotBlank() &&
                    normalizeForMatch(baseQuery) != normalizeForMatch(q)
                ) {
                    repository.searchTracks(
                        "$baseQuery$languageSuffix",
                        page = 0,
                        limit = SEARCH_PAGE_SIZE
                    ).getOrDefault(emptyList())
                } else {
                    emptyList()
                }

                val tracks = primaryTracks + fallbackTracks
                val ranked = rankTracks(dedupeTracks(tracks), q)
                canLoadMore = primaryTracks.isNotEmpty()
                _results.value = if (ranked.isEmpty()) ScreenState.Empty else ScreenState.Success(ranked)

                if (shouldTryLyricsSearch(q)) {
                    val language = selected.takeUnless { it == "All" }
                    val lyricMatches = repository.searchTracksByLyrics(q, language = language)
                    if (pagedQuery == effectiveQuery && lyricMatches.isNotEmpty()) {
                        val latest = (_results.value as? ScreenState.Success)?.data.orEmpty()
                        val merged = rankTracks(dedupeTracks(lyricMatches + latest), q)
                        _results.value = ScreenState.Success(merged)
                    }
                }
            },
            onFailure = { e ->
                if (shouldTryLyricsSearch(q)) {
                    val language = selected.takeUnless { it == "All" }
                    val lyricMatches = repository.searchTracksByLyrics(q, language = language)
                    if (lyricMatches.isNotEmpty()) {
                        _results.value = ScreenState.Success(rankTracks(dedupeTracks(lyricMatches), q))
                    } else {
                        _results.value = ScreenState.Error(e.message ?: "Something went wrong")
                    }
                } else {
                    _results.value = ScreenState.Error(e.message ?: "Something went wrong")
                }
            }
        )
    }

    fun loadMore() {
        val first = _results.value
        if (first !is ScreenState.Success || _isLoadingMore.value || !canLoadMore) return
        val q = pagedQuery
        val rankingQuery = _query.value.trim()
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                var attempts = 0
                while (attempts < 3 && canLoadMore && pagedQuery == q) {
                    val nextPage = currentPage + 1
                    val result = repository.searchTracks(q, page = nextPage, limit = SEARCH_PAGE_SIZE)
                    val newTracks = result.getOrElse {
                        return@launch
                    }

                    if (pagedQuery != q) return@launch
                    currentPage = nextPage

                    if (newTracks.isEmpty()) {
                        canLoadMore = false
                        break
                    }

                    canLoadMore = true
                    val latest = (_results.value as? ScreenState.Success)?.data ?: first.data
                    val merged = rankTracks(dedupeTracks(latest + newTracks), rankingQuery)
                    if (merged.size > latest.size) {
                        _results.value = ScreenState.Success(merged)
                        break
                    }
                    attempts++
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun selectLanguage(language: String) {
        if (_selectedLanguage.value == language) return
        _selectedLanguage.value = language
        val baseQuery = _query.value.trim()
        if (baseQuery.isBlank()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _results.value = ScreenState.Loading
            runSearch(baseQuery)
        }
    }

    fun availableLanguages(tracks: List<Track>): List<String> {
        val common = listOf("Tamil", "Hindi", "English", "Telugu", "Malayalam", "Kannada", "Punjabi", "Bengali")
        val detected = tracks.mapNotNull { track ->
            track.language.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() }
        }.distinct().sorted()
        return listOf("All") + (common + detected).distinct()
    }

    fun filteredTracks(tracks: List<Track>): List<Track> = tracks

    private fun dedupeTracks(tracks: List<Track>): List<Track> {
        val seen = linkedSetOf<String>()
        return tracks.filter { track ->
            val canonicalTitle = canonicalSongTitle(track.name)
            val canonicalLanguage = track.language.lowercase().trim()
            val version = versionSignature(track.name)

            // Preserve meaningful alternate versions. Previously, "slowed", "reverb", "lofi",
            // etc. were stripped before deduplication, which could collapse the exact requested
            // version into the original song (or another variant) before ranking even ran.
            val key = "$canonicalLanguage|$canonicalTitle|$version"
            key.isNotBlank() && seen.add(key)
        }
    }

    private fun shouldTryLyricsSearch(query: String): Boolean {
        val words = normalizeForMatch(query).split(' ').filter { it.isNotBlank() }
        return words.size >= 3 && query.trim().length >= 10
    }

    private fun rankTracks(tracks: List<Track>, query: String): List<Track> {
        val q = normalizeForMatch(query)
        if (q.isBlank()) return tracks

        val baseQuery = normalizeForMatch(stripVersionIntent(query))
        val queryWords = q.split(' ').filter { it.isNotBlank() }
        val baseWords = baseQuery.split(' ').filter { it.isNotBlank() }
        val requestedVersions = requestedVersionTerms(query)

        fun score(track: Track): Int {
            val title = normalizeForMatch(track.name)
            val artist = normalizeForMatch(track.artistName)
            val album = normalizeForMatch(track.albumName)
            val trackVersions = requestedVersionTerms(track.name)
            var score = 0

            if (title == q) score += 1400
            if (artist == q) score += 700
            if (title.startsWith(q)) score += 550
            if (artist.startsWith(q)) score += 350
            if (title.contains(q)) score += 350
            if (artist.contains(q)) score += 200
            if (album.contains(q)) score += 100

            // Strongly prefer the requested song title even when the API returned it from the
            // base-title fallback search.
            if (baseQuery.isNotBlank()) {
                if (title == baseQuery) score += 900
                if (title.startsWith(baseQuery)) score += 650
                if (title.contains(baseQuery)) score += 500
                score += baseWords.count { it in title } * 90
            }

            score += queryWords.count { it in title } * 60
            score += queryWords.count { it in artist } * 40

            // Version-aware ranking: "ennai kolladhey slowed" should rank an Ennai Kolladhey
            // slowed/reverb entry above unrelated songs that happen to contain "slowed".
            if (requestedVersions.isNotEmpty()) {
                val matched = requestedVersions.intersect(trackVersions).size
                score += matched * 500
                if (matched == requestedVersions.size) score += 450
                if (trackVersions.isEmpty()) score -= 180
            }

            if (track.source != com.wavelength.music.data.model.TrackSource.JIOSAAVN) score += 80
            return score
        }

        return tracks.withIndex()
            .sortedWith(compareByDescending<IndexedValue<Track>> { score(it.value) }.thenBy { it.index })
            .map { it.value }
    }

    private fun normalizeForMatch(value: String): String = value
        .lowercase()
        .replace("&", " and ")
        .replace(Regex("[^a-z0-9\\p{L}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun stripVersionIntent(raw: String): String {
        val versionWords = VERSION_TERMS.joinToString("|") { Regex.escape(it) }
        return raw
            .replace(Regex("\\b(?:$versionWords)\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-', '–', '—', '(', ')', '[', ']', ':')
    }

    private fun requestedVersionTerms(raw: String): Set<String> {
        val normalized = normalizeForMatch(raw)
        return VERSION_TERMS.filterTo(linkedSetOf()) { term ->
            Regex("(^|\\s)${Regex.escape(term)}(\\s|$)").containsMatchIn(normalized)
        }
    }

    private fun versionSignature(raw: String): String = requestedVersionTerms(raw)
        .sorted()
        .joinToString("+")
        .ifBlank { "original" }

    private fun canonicalSongTitle(raw: String): String {
        return stripVersionIntent(
            raw
                .lowercase()
                .replace(Regex("\\([^)]*(from|movie|film|soundtrack|single|theme|ost|original motion picture)[^)]*\\)", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("\\[[^]]*(from|movie|film|soundtrack|single|theme|ost|original motion picture)[^]]*]", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("\\s*[-–—:]\\s*(from|original motion picture soundtrack|motion picture soundtrack|soundtrack|ost|single|song|theme).*", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("\\s+from\\s+.+$", RegexOption.IGNORE_CASE), " ")
        )
            .replace("&", " and ")
            .replace(Regex("[^a-z0-9\\p{L}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
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

    private companion object {
        const val SEARCH_PAGE_SIZE = 20
        val VERSION_TERMS = listOf(
            "slowed",
            "reverb",
            "lofi",
            "remix",
            "acoustic",
            "instrumental",
            "karaoke",
            "sped",
            "speed",
            "nightcore"
        )
    }
}
