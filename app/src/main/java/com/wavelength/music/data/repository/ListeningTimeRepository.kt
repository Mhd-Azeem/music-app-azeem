package com.wavelength.music.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ListeningTimeSummary(
    val todayMs: Long = 0L,
    val last7DaysMs: Long = 0L,
    val last30DaysMs: Long = 0L,
    val allTimeMs: Long = 0L
)

@Singleton
class ListeningTimeRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("listening_time_stats", Context.MODE_PRIVATE)
    private val _summary = MutableStateFlow(calculateSummary())
    val summary: StateFlow<ListeningTimeSummary> = _summary.asStateFlow()

    @Synchronized
    fun recordListening(deltaMs: Long) {
        if (deltaMs <= 0L) return
        val key = dayKey(System.currentTimeMillis())
        val current = prefs.getLong("day:$key", 0L)
        prefs.edit()
            .putLong("day:$key", current + deltaMs)
            .putLong("all_time_ms", prefs.getLong("all_time_ms", 0L) + deltaMs)
            .apply()
        pruneOldDays()
        _summary.value = calculateSummary()
    }

    private fun calculateSummary(): ListeningTimeSummary {
        val now = System.currentTimeMillis()
        val today = dayKey(now)
        var last7 = 0L
        var last30 = 0L
        for (offset in 0..29) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -offset)
            }
            val value = prefs.getLong("day:${dayKey(cal.timeInMillis)}", 0L)
            if (offset < 7) last7 += value
            last30 += value
        }
        return ListeningTimeSummary(
            todayMs = prefs.getLong("day:$today", 0L),
            last7DaysMs = last7,
            last30DaysMs = last30,
            allTimeMs = prefs.getLong("all_time_ms", 0L)
        )
    }

    private fun pruneOldDays() {
        val cutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -60) }.timeInMillis
        val editor = prefs.edit()
        prefs.all.keys
            .filter { it.startsWith("day:") }
            .forEach { key ->
                val raw = key.removePrefix("day:")
                val parsed = runCatching { DATE_FORMAT.parse(raw)?.time ?: Long.MAX_VALUE }.getOrDefault(Long.MAX_VALUE)
                if (parsed < cutoff) editor.remove(key)
            }
        editor.apply()
    }

    private fun dayKey(timeMs: Long): String = synchronized(DATE_FORMAT) {
        DATE_FORMAT.format(Date(timeMs))
    }

    private companion object {
        val DATE_FORMAT = SimpleDateFormat("yyyyMMdd", Locale.US)
    }
}
