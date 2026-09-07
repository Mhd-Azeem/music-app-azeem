package com.wavelength.music.data.repository

import com.wavelength.music.data.cache.TtlCache
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.model.toTrack
import com.wavelength.music.data.remote.jiosaavn.JioSaavnApiService
import com.wavelength.music.data.remote.jiosaavn.JioSaavnUsageDataDto
import com.wavelength.music.data.remote.jiosaavn.toDomain
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JioSaavn is an unofficial, self-hosted API (see JioSaavnApiService) — treat every call as
 * likely to fail (deployment asleep, endpoint shape changed, rate limited, etc.) and let callers
 * degrade gracefully via Result rather than crashing the rest of the app.
 *
 * Search/detail results are also cached in-memory (see [TtlCache]) with the same fresh/stale
 * tiers as the backend's Cache API/KV layer, so repeat calls for the same query/id within a
 * session skip the network (and JSON parsing) entirely, or return instantly with the last good
 * value while a background refresh brings the cache up to date.
 */
@Singleton
class JioSaavnRepository @Inject constructor(
    private val api: JioSaavnApiService
) {
    private val searchCache = TtlCache<String, List<Track>>()
    private val detailCache = TtlCache<String, List<Track>>()
    private val songCache = TtlCache<String, Track?>()

    suspend fun searchSongs(
        query: String,
        page: Int = 0,
        limit: Int = 20,
        forceRefresh: Boolean = false
    ): Result<List<Track>> = runCatching {
        val normalizedQuery = query.trim().replace(Regex("\\s+"), " ")
        if (normalizedQuery.isBlank()) return@runCatching emptyList()
        val cacheKey = normalizedQuery.lowercase()
        searchCache.getOrPut("search:$cacheKey:$page:$limit", SEARCH_FRESH_MS, SEARCH_STALE_MS, forceRefresh) {
            api.searchSongs(normalizedQuery, page, limit).data?.results.orEmpty()
                .take(limit)
                .map { it.toDomain().toTrack() }
        }
    }

    suspend fun getSong(id: String): Result<Track?> = runCatching {
        songCache.getOrPut("song:$id", DETAIL_FRESH_MS, DETAIL_STALE_MS) {
            api.getSong(id).data.firstOrNull()?.toDomain()?.toTrack()
        }
    }

    suspend fun getAlbumTracks(albumLink: String): Result<List<Track>> = runCatching {
        detailCache.getOrPut("album:$albumLink", DETAIL_FRESH_MS, DETAIL_STALE_MS) {
            api.getAlbum(albumLink).data?.songs.orEmpty().map { it.toDomain().toTrack() }
        }
    }

    suspend fun getPlaylistTracks(playlistLink: String): Result<List<Track>> = runCatching {
        detailCache.getOrPut("playlist:$playlistLink", DETAIL_FRESH_MS, DETAIL_STALE_MS) {
            api.getPlaylist(playlistLink).data?.songs.orEmpty().map { it.toDomain().toTrack() }
        }
    }

    /** Requires the deployment to have CF_API_TOKEN/CF_ACCOUNT_ID configured (see the worker's
     * GET /usage endpoint) — fails on deployments that haven't set that up yet. Deliberately
     * uncached: usage numbers change continuously and are only ever checked on demand. */
    suspend fun getUsage(): Result<JioSaavnUsageDataDto> = runCatching {
        api.getUsage().data ?: error("Usage data missing from response")
    }

    private companion object {
        val SEARCH_FRESH_MS = TimeUnit.MINUTES.toMillis(10)
        val SEARCH_STALE_MS = TimeUnit.MINUTES.toMillis(5)
        val DETAIL_FRESH_MS = TimeUnit.HOURS.toMillis(24)
        val DETAIL_STALE_MS = TimeUnit.HOURS.toMillis(1)
    }
}
