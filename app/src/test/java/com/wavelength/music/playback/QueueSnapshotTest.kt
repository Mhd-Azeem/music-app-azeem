package com.wavelength.music.playback

import com.squareup.moshi.Moshi
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.model.TrackSource
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueSnapshotTest {
    @Test
    fun persistedSnapshotRetainsOrderCurrentPositionModesAndTrackSources() {
        val local = Track("local", "Song", "", "", "", "", "", "content://media/21", 41, TrackSource.LOCAL)
        val remote = local.copy(id = "online", audioUrl = "https://example.test/audio", source = TrackSource.JIOSAAVN)
        val original = QueueSnapshot(
            entries = listOf(QueueEntry(14, local), QueueEntry(3, remote)),
            currentEntryId = 14,
            positionMs = 23_900L,
            shuffled = true,
            repeatMode = 2,
            playlistId = 51,
            autoQueueSuppressed = true
        )
        val adapter = Moshi.Builder().build().adapter(QueueSnapshot::class.java)
        assertEquals(original, adapter.fromJson(adapter.toJson(original)))
    }
}
