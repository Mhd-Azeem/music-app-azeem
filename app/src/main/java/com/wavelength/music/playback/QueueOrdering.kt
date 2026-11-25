package com.wavelength.music.playback

import com.wavelength.music.data.model.Track
import kotlin.math.ln
import kotlin.random.Random

internal fun distinctTracksForPlayback(tracks: List<Track>, startIndex: Int): List<Track> {
    val selected = tracks[startIndex]
    return tracks.distinctBy { it.id }.map { if (it.id == selected.id) selected else it }
}

internal fun newUpcomingTracks(tracks: List<Track>, existingIds: Set<String>): List<Track> {
    val seen = existingIds.toMutableSet()
    return tracks.filter { seen.add(it.id) }
}

internal fun <T> weightedShuffle(
    items: List<T>,
    random: Random = Random.Default,
    weightOf: (T) -> Int
): List<T> = items.map { item ->
    // Independent exponential draws give a weighted permutation with one sort.
    item to -ln(1.0 - random.nextDouble()) / (weightOf(item).coerceAtLeast(0) + 1.0)
}.sortedBy { it.second }.map { it.first }
