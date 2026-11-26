package com.wavelength.music.playback

import com.wavelength.music.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class RadioTracksTest {
    @Test
    fun skipsCompilationEntriesForTheSeedAcrossDifferentCreditsAndIds() {
        val seed = track("seed", "Kala Chashma (From Baar Baar Dekho)", "Amar Arshi, Badshah, Neha Kakkar")
        val copy = track("copy", "Kala Chashma", "Kumaar, Badshah, Prem & Hardeep").copy(albumName = "Dance Hits")
        val other = track("other", "Kar Gayi Chull", "Badshah")
        assertEquals(listOf(other), distinctRadioTracks(listOf(copy, other), listOf(seed)))
    }

    @Test
    fun keepsOneCopyPerRecordingAndRetainsDistinctVersionsAndSongs() {
        val original = track("1", "Moonlight", "Singer")
        val copy = original.copy(id = "2", albumName = "Hits")
        val remix = original.copy(id = "3", name = "Moonlight (Remix)")
        val unrelated = original.copy(id = "4", artistName = "Different artist")
        val otherLanguage = original.copy(id = "5", language = "tamil")
        val candidates = listOf(original, copy, remix, unrelated, otherLanguage)
        assertEquals(listOf(original, remix, unrelated, otherLanguage), distinctRadioTracks(candidates, emptyList()))
        assertEquals(listOf(remix, unrelated, otherLanguage), distinctRadioTracks(candidates, listOf(original)))
    }

    private fun track(id: String, title: String, artist: String): Track =
        Track(id, title, "", artist, "", "", "", "https://example.test/audio", 180, language = "hindi")
}
