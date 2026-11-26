package com.wavelength.music.ui.search

import com.wavelength.music.data.model.Track
import com.wavelength.music.data.model.baseTitleQuery
import com.wavelength.music.data.model.canonicalSongTitle
import com.wavelength.music.data.model.normalizeForMatch
import com.wavelength.music.data.model.variantSignature

internal fun groupSongVersions(tracks: List<Track>): List<List<Track>> {
    val variants = tracks.associate { it.id to variantSignature(it.name) }
    val buckets = tracks.groupBy { track ->
        val title = canonicalSongTitle(track.name)
        val base = if (variants.getValue(track.id).isEmpty()) title else baseTitleQuery(title)
        base.ifBlank { track.id } to track.language.trim().lowercase()
    }
    val rank = tracks.mapIndexed { index, track -> track.id to index }.toMap()
    return buckets.values.flatMap { bucket ->
        val groups = mutableListOf<MutableList<Track>>()
        val (originals, versions) = bucket.partition { variants.getValue(it.id).isEmpty() }
        for (track in originals + versions) {
            val matching = groups.filter { group -> group.any { sharesRecordingCredit(it, track) } }
            val group = when {
                matching.size == 1 -> matching.single()
                matching.isEmpty() && groups.size == 1 && originals.size == 1 && track in versions -> groups.single()
                else -> mutableListOf<Track>().also(groups::add)
            }
            group.add(track)
        }
        groups.map { group -> group.sortedBy { rank.getValue(it.id) } }
    }.sortedBy { rank.getValue(it.first().id) }
}

private fun sharesRecordingCredit(first: Track, second: Track): Boolean {
    if (first.albumId.isNotBlank() && first.albumId == second.albumId) return true
    val names = first.artistName.split(',').map(::normalizeForMatch).filter { it.isNotEmpty() }
    return second.artistName.split(',').map(::normalizeForMatch).any { it in names }
}
