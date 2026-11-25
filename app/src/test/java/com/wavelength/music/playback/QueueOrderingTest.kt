package com.wavelength.music.playback

import com.wavelength.music.data.model.Track
import com.wavelength.music.data.model.TrackSource
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueOrderingTest {
    private fun track(id: String) = Track(id, id, "", "", "", "", "", "https://example.test/$id", 30)

    @Test
    fun selectingADuplicateKeepsTheRequestedPlayableSource() {
        val remote = track("a")
        val local = remote.copy(source = TrackSource.DOWNLOADED, audioUrl = "file:///a")
        assertEquals(listOf(local, track("b")), distinctTracksForPlayback(listOf(remote, track("b"), local), 2))
    }

    @Test
    fun appendingFiltersExistingSongsAndDuplicatesWithinTheBatch() {
        assertEquals(
            listOf(track("c"), track("b")),
            newUpcomingTracks(listOf(track("a"), track("c"), track("c"), track("b")), setOf("a"))
        )
    }

    @Test
    fun smartShuffleKeepsEveryEntryAndFavorsFrequentlyPlayedSongs() {
        val entries = listOf("favorite", "other", "third")
        val random = Random(15)
        var favoriteFirst = 0
        repeat(100) {
            val shuffled = weightedShuffle(entries, random) { if (it == "favorite") 50 else 0 }
            assertEquals(entries.toSet(), shuffled.toSet())
            assertEquals(entries.size, shuffled.size)
            if (shuffled.first() == "favorite") favoriteFirst++
        }
        assertTrue(favoriteFirst > 85)
    }
}
