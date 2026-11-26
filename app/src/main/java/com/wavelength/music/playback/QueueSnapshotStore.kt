package com.wavelength.music.playback

import android.content.Context
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.wavelength.music.data.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
internal data class QueueSnapshot(
    val entries: List<QueueEntry>,
    val currentEntryId: Long?,
    val positionMs: Long,
    val shuffled: Boolean,
    val repeatMode: Int,
    val playlistId: Long? = null,
    val autoQueueSuppressed: Boolean = false,
    val radioSeed: Track? = null
)

@Singleton
class QueueSnapshotStore @Inject constructor(
    @ApplicationContext context: Context,
    moshi: Moshi
) {
    private val timeline = context.getSharedPreferences("azmusic_playback_queue", Context.MODE_PRIVATE)
    private val cursor = context.getSharedPreferences("azmusic_playback_cursor", Context.MODE_PRIVATE)
    private val adapter = moshi.adapter(QueueSnapshot::class.java)
    private val pending = Channel<QueueSnapshot>(Channel.CONFLATED)
    @Volatile private var latest: QueueSnapshot? = null

    init {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            var lastEntries: List<QueueEntry>? = null
            var lastRadioSeed: Track? = null
            var timelineVersion = ""
            for (snapshot in pending) {
                runCatching {
                    if (lastEntries !== snapshot.entries || lastRadioSeed != snapshot.radioSeed) {
                        timelineVersion = UUID.randomUUID().toString()
                        timeline.edit()
                            .putString("version", timelineVersion)
                            .putString("queue", adapter.toJson(snapshot))
                            .apply()
                        lastEntries = snapshot.entries
                        lastRadioSeed = snapshot.radioSeed
                    }
                    // Position checkpoints write only this small file. A version mismatch
                    // after process death falls back to the cursor embedded in the timeline.
                    cursor.edit().putString("version", timelineVersion)
                        .putLong("entry", snapshot.currentEntryId ?: -1L)
                        .putLong("position", snapshot.positionMs)
                        .putBoolean("shuffle", snapshot.shuffled)
                        .putInt("repeat", snapshot.repeatMode)
                        .putBoolean("auto_queue_suppressed", snapshot.autoQueueSuppressed)
                        .apply()
                }
            }
        }
    }

    internal fun save(snapshot: QueueSnapshot) {
        latest = snapshot
        pending.trySend(snapshot)
    }

    internal fun load(): QueueSnapshot? = latest ?: runCatching {
        val snapshot = timeline.getString("queue", null)?.let(adapter::fromJson) ?: return null
        if (timeline.getString("version", "") != cursor.getString("version", null)) {
            return snapshot
        }
        snapshot.copy(
            currentEntryId = cursor.getLong("entry", -1L).takeIf { it >= 0L },
            positionMs = cursor.getLong("position", 0L).coerceAtLeast(0L),
            shuffled = cursor.getBoolean("shuffle", false),
            repeatMode = cursor.getInt("repeat", 0).coerceIn(0, 2),
            autoQueueSuppressed = cursor.getBoolean("auto_queue_suppressed", false)
        )
    }.getOrNull()
}
