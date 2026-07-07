package com.wavelength.music.ui.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.concurrent.TimeUnit

private val presetMinutes = listOf(5, 10, 15, 30, 45, 60)

@Composable
fun SleepTimerDialog(
    remainingMs: Long?,
    isEndOfTrack: Boolean,
    onDismiss: () -> Unit,
    onSelectMinutes: (Int) -> Unit,
    onSelectEndOfTrack: () -> Unit,
    onCancelTimer: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column {
                if (remainingMs != null || isEndOfTrack) {
                    Text(
                        text = if (isEndOfTrack) {
                            "Playback will pause at the end of this track."
                        } else {
                            "Pausing in ${formatRemaining(remainingMs ?: 0L)}"
                        },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    TimerOptionRow(label = "Cancel timer", onClick = onCancelTimer)
                } else {
                    presetMinutes.forEach { minutes ->
                        TimerOptionRow(label = "$minutes minutes", onClick = { onSelectMinutes(minutes) })
                    }
                    TimerOptionRow(label = "End of track", onClick = onSelectEndOfTrack)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun TimerOptionRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    )
}

private fun formatRemaining(millis: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis.coerceAtLeast(0))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
