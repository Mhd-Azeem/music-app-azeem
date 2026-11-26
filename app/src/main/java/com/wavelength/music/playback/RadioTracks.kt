package com.wavelength.music.playback

import com.wavelength.music.data.model.Track
import com.wavelength.music.data.model.baseTitleQuery
import com.wavelength.music.data.model.canonicalSongTitle
import com.wavelength.music.data.model.normalizeForMatch
import com.wavelength.music.data.model.variantSignature

internal fun distinctRadioTracks(candidates: List<Track>, queued: List<Track>): List<Track> {
    val ids = queued.mapTo(hashSetOf()) { it.id }
    val recordings = queued.mapNotNull(::radioRecording).groupBy { it.title }
        .mapValuesTo(mutableMapOf()) { (_, tracks) -> tracks.toMutableList() }

    return candidates.filter { candidate ->
        if (!ids.add(candidate.id)) return@filter false
        val recording = radioRecording(candidate) ?: return@filter true
        val sameTitle = recordings.getOrPut(recording.title) { mutableListOf() }
        if (sameTitle.any { it.matches(recording) }) return@filter false
        sameTitle.add(recording)
        true
    }
}

private data class RadioRecording(
    val title: String,
    val version: String,
    val language: String,
    val artists: Set<String>
) {
    fun matches(other: RadioRecording): Boolean =
        version == other.version &&
            (language.isEmpty() || other.language.isEmpty() || language == other.language) &&
            artists.any { it in other.artists }
}

private fun radioRecording(track: Track): RadioRecording? {
    val version = variantSignature(track.name)
    val title = canonicalSongTitle(track.name).let { if (version.isEmpty()) it else baseTitleQuery(it) }
    val artists = track.artistName.split(',').map(::normalizeForMatch).filter { it.isNotEmpty() }.toSet()
    if (title.isEmpty() || artists.isEmpty()) return null
    return RadioRecording(title, version, track.language.trim().lowercase(), artists)
}
