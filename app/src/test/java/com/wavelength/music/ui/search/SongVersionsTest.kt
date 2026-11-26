package com.wavelength.music.ui.search

import com.wavelength.music.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class SongVersionsTest {
    @Test
    fun keepsRankAndSeparatesSameTitleInAnotherLanguage() {
        val remix = track("1", "Moonlight (Remix)", "DJ", "tamil")
        val original = track("2", "Moonlight (From Film)", "Singer", "tamil")
        val hindi = track("3", "Moonlight", "Singer", "hindi")
        assertEquals(listOf(listOf(remix, original), listOf(hindi)), groupSongVersions(listOf(remix, original, hindi)))
    }

    @Test
    fun keepsUnrelatedSameTitleRecordingsSeparate() {
        val first = track("1", "Moonlight", "First singer", "tamil")
        val second = track("2", "Moonlight", "Second singer", "tamil")
        val lofi = track("3", "Moonlight - Lofi", "Second singer", "tamil")
        assertEquals(listOf(listOf(first), listOf(second, lofi)), groupSongVersions(listOf(first, second, lofi)))
    }

    private fun track(id: String, title: String, artist: String, language: String): Track =
        Track(id, title, "", artist, "", "", "", "", 120, language = language)
}
