package com.wavelength.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** A knob-styled control: drag vertically to adjust, rendered as a circular gauge rather than a
 * linear slider. Used for the equalizer's Simple mode (Bass / Vocals / Treble). */
@Composable
fun CircularKnob(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val rangeSize = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val sweepFraction = ((value - valueRange.start) / rangeSize).coerceIn(0f, 1f)
    val currentValue = rememberUpdatedState(value)
    val currentOnValueChange = rememberUpdatedState(onValueChange)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Canvas(
            modifier = Modifier
                .size(64.dp)
                .pointerInput(enabled, valueRange) {
                    if (!enabled) return@pointerInput
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount.y / 200f * rangeSize
                        val newValue = (currentValue.value + delta)
                            .coerceIn(valueRange.start, valueRange.endInclusive)
                        currentOnValueChange.value(newValue)
                    }
                }
        ) {
            val strokeWidth = 6.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            drawArc(
                color = activeColor,
                startAngle = 135f,
                sweepAngle = 270f * sweepFraction,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(valueLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
