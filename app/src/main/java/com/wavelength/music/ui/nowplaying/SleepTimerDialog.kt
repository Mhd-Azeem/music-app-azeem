package com.wavelength.music.ui.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
    var showCustomInput by remember { mutableStateOf(false) }
    var customMinutesText by remember { mutableStateOf("") }
    val customMinutes = customMinutesText.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column {
                when {
                    remainingMs != null || isEndOfTrack -> {
                        Text(
                            text = if (isEndOfTrack) {
                                "Playback will pause at the end of this track."
                            } else {
                                "Pausing in ${formatRemaining(remainingMs ?: 0L)}"
                            },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        TimerOptionRow(label = "Cancel timer", onClick = onCancelTimer)
                    }
                    showCustomInput -> {
                        OutlinedTextField(
                            value = customMinutesText,
                            onValueChange = { customMinutesText = it.filter(Char::isDigit).take(4) },
                            label = { Text("Minutes") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showCustomInput = false }) { Text("Back") }
                            TextButton(
                                onClick = { customMinutes?.let(onSelectMinutes) },
                                enabled = customMinutes != null && customMinutes > 0
                            ) { Text("Start") }
                        }
                    }
                    else -> {
                        presetMinutes.forEach { minutes ->
                            TimerOptionRow(label = "$minutes minutes", onClick = { onSelectMinutes(minutes) })
                        }
                        TimerOptionRow(label = "Custom", onClick = { showCustomInput = true })
                        TimerOptionRow(label = "End of track", onClick = onSelectEndOfTrack)
                    }
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
