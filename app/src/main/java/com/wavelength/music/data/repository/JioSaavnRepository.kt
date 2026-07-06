package com.wavelength.music.data.repository

import com.wavelength.music.data.model.Track
import com.wavelength.music.data.model.toTrack
import com.wavelength.music.data.remote.jiosaavn.JioSaavnApiService
import com.wavelength.music.data.remote.jiosaavn.toDomain
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JioSaavn is an unofficial, self-hosted API (see JioSaavnApiService) — treat every call as
 * likely to fail (deployment asleep, endpoint shape changed, rate limited, etc.) and let callers
 * degrade gracefully via Result rather than crashing the rest of the app.
 */
@Singleton
class JioSaavnRepository @Inject constructor(
    private val api: JioSaavnApiService
) {

    suspend fun searchSongs(query: String, limit: Int = 20): Result<List<Track>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        api.searchSongs(query).data?.results.orEmpty()
            .take(limit)
            .map { it.toDomain().toTrack() }
    }

    suspend fun getSong(id: String): Result<Track?> = runCatching {
        api.getSong(id).data.firstOrNull()?.toDomain()?.toTrack()
    }

    suspend fun getAlbumTracks(albumLink: String): Result<List<Track>> = runCatching {
        api.getAlbum(albumLink).data?.songs.orEmpty().map { it.toDomain().toTrack() }
    }

    suspend fun getPlaylistTracks(playlistLink: String): Result<List<Track>> = runCatching {
        api.getPlaylist(playlistLink).data?.songs.orEmpty().map { it.toDomain().toTrack() }
    }
}
