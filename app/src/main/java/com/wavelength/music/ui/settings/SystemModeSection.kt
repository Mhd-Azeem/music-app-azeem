package com.wavelength.music.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wavelength.music.data.repository.AppExperienceMode
import kotlin.math.roundToInt

@Composable
internal fun SystemModeSection(mode: AppExperienceMode, onMode: (AppExperienceMode) -> Unit) {
    val minimal = mode == AppExperienceMode.MINIMAL
    val action = if (minimal) "Exit Minimal Mode" else "Enter Minimal Mode"
    var confirm by remember(mode) { mutableStateOf(false) }
    var offset by remember(mode) { mutableFloatStateOf(0f) }
    var width by remember { mutableFloatStateOf(0f) }
    val thumbWidth = with(LocalDensity.current) { 56.dp.toPx() }
    val travel = (width - thumbWidth).coerceAtLeast(0f)
    val haptics = LocalHapticFeedback.current
    val request = { offset = 0f; confirm = true }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("System Mode", style = MaterialTheme.typography.titleLarge)
        Text(if (minimal) "Exit Minimal Mode" else "Use Minimal System")
        Text(if (minimal) "Return to the complete AzMusic visual experience." else
            "Switch to a lightweight red-and-black interface while keeping all music features and Live Island.")
        Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF350B10)).onSizeChanged { width = it.width.toFloat() }
            .semantics {
                contentDescription = "Swipe to $action"
                onClick(label = "Confirm with keyboard or accessibility") { request(); true }
            }
            .onKeyEvent {
                if (it.type == KeyEventType.KeyUp && (it.key == Key.Enter || it.key == Key.Spacebar)) {
                    request(); true
                } else false
            }.focusable(), contentAlignment = Alignment.CenterStart) {
            Text("Swipe to $action  →", Modifier.align(Alignment.Center), color = Color.White)
            Box(Modifier.offset { IntOffset(offset.roundToInt(), 0) }.size(56.dp)
                .clip(RoundedCornerShape(28.dp)).background(Color(0xFFE53935))
                .pointerInput(mode, travel) {
                    detectHorizontalDragGestures(
                        onDragCancel = { offset = 0f },
                        onDragEnd = {
                            if (travel > 0 && offset >= travel * 0.95f) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                request()
                            } else offset = 0f
                        }
                    ) { change, delta ->
                        change.consume()
                        offset = (offset + delta).coerceIn(0f, travel)
                    }
                }, contentAlignment = Alignment.Center) { Text("→", color = Color.White) }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("$action?") },
        text = { Text(if (minimal)
            "Your previous appearance settings and visual effects will be restored. Music playback will continue." else
            "AzMusic will disable advanced themes and visual effects to reduce resource usage. Music playback, playlists, downloads, Up Next, and Live Island will remain available.") },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
        confirmButton = { TextButton(onClick = {
            confirm = false
            onMode(if (minimal) AppExperienceMode.NORMAL else AppExperienceMode.MINIMAL)
        }) { Text(action) } }
    )
}
