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
            entries = listOf(QueueEntry(14, local), QueueEntry(3, remote, recommended = true)),
            currentEntryId = 14,
            positionMs = 23_900L,
            shuffled = true,
            repeatMode = 2,
            playlistId = 51,
            autoQueueSuppressed = true,
            radioSeed = remote
        )
        val adapter = Moshi.Builder().build().adapter(QueueSnapshot::class.java)
        assertEquals(original, adapter.fromJson(adapter.toJson(original)))
    }

    @Test
    fun olderSnapshotKeepsItsQueue() {
        val track = Track("local", "Song", "", "", "", "", "", "content://media/21", 41, TrackSource.LOCAL)
        val snapshot = QueueSnapshot(listOf(QueueEntry(14, track)), 14, 300L, false, 0)
        val adapter = Moshi.Builder().build().adapter(QueueSnapshot::class.java)
        val olderJson = adapter.toJson(snapshot).replace(",\"recommended\":false", "")
        assertEquals(snapshot, adapter.fromJson(olderJson))
    }
}
