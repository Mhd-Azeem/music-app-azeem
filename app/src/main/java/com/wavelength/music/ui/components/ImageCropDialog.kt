package com.wavelength.music.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/** Full-screen pan/pinch-zoom crop UI shown after picking a gallery photo, before it's saved as
 * a background or pinned as a home-screen shortcut icon. [aspectRatio] is width/height of the
 * crop frame — 1f for a square icon, a tall ratio for a phone background. */
@Composable
fun ImageCropDialog(
    imageUri: Uri,
    aspectRatio: Float,
    onDismiss: () -> Unit,
    onCropped: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var sourceBitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(imageUri) { mutableStateOf(true) }

    LaunchedEffect(imageUri) {
        isLoading = true
        sourceBitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(imageUri)?.use(BitmapFactory::decodeStream)
            }.getOrNull()
        }
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            val bitmap = sourceBitmap
            when {
                isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                bitmap == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Couldn't load that photo", color = Color.White)
                        TextButton(onClick = onDismiss) { Text("Close") }
                    }
                }
                else -> CropCanvas(
                    bitmap = bitmap,
                    aspectRatio = aspectRatio,
                    onDismiss = onDismiss,
                    onCropped = onCropped
                )
            }
        }
    }
}

@Composable
private fun CropCanvas(
    bitmap: Bitmap,
    aspectRatio: Float,
    onDismiss: () -> Unit,
    onCropped: (Bitmap) -> Unit
) {
    var userScale by remember { mutableStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var frameSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    val frameWidthPx = frameSize.width.toFloat()
    val frameHeightPx = frameSize.height.toFloat()
    val baseFitScale = if (frameWidthPx > 0f && frameHeightPx > 0f) {
        max(frameWidthPx / bitmap.width, frameHeightPx / bitmap.height)
    } else {
        1f
    }
    val effectiveScale = baseFitScale * userScale
    val displayedWidthPx = bitmap.width * effectiveScale
    val displayedHeightPx = bitmap.height * effectiveScale
    val maxOffsetX = max(0f, (displayedWidthPx - frameWidthPx) / 2f)
    val maxOffsetY = max(0f, (displayedHeightPx - frameHeightPx) / 2f)
    val clampedOffsetX = panOffset.x.coerceIn(-maxOffsetX, maxOffsetX)
    val clampedOffsetY = panOffset.y.coerceIn(-maxOffsetY, maxOffsetY)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = Color.White)
            }
            Text("Adjust photo", color = Color.White, style = MaterialTheme.typography.titleMedium)
            TextButton(
                onClick = {
                    onCropped(
                        cropBitmap(
                            source = bitmap,
                            frameWidthPx = frameWidthPx,
                            frameHeightPx = frameHeightPx,
                            effectiveScale = effectiveScale,
                            offsetX = clampedOffsetX,
                            offsetY = clampedOffsetY
                        )
                    )
                },
                enabled = frameWidthPx > 0f
            ) {
                Text("Use photo")
            }
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .then(if (aspectRatio >= 1f) Modifier.fillMaxWidth() else Modifier.fillMaxHeight(0.85f))
                    .aspectRatio(aspectRatio)
                    .onSizeChanged { frameSize = it }
                    .clipToBounds()
                    .background(Color(0xFF1A1A1A))
                    .border(2.dp, Color.White.copy(alpha = 0.8f))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            userScale = (userScale * zoom).coerceIn(1f, 5f)
                            panOffset += pan
                        }
                    }
            ) {
                if (frameWidthPx > 0f) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(
                                width = with(density) { displayedWidthPx.toDp() },
                                height = with(density) { displayedHeightPx.toDp() }
                            )
                            .offset(
                                x = with(density) { clampedOffsetX.toDp() },
                                y = with(density) { clampedOffsetY.toDp() }
                            )
                    )
                }
            }
        }

        Text(
            text = "Pinch to zoom, drag to reposition",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
        )
    }
}

private fun cropBitmap(
    source: Bitmap,
    frameWidthPx: Float,
    frameHeightPx: Float,
    effectiveScale: Float,
    offsetX: Float,
    offsetY: Float
): Bitmap {
    val displayedWidthPx = source.width * effectiveScale
    val displayedHeightPx = source.height * effectiveScale
    val localX = displayedWidthPx / 2f - frameWidthPx / 2f - offsetX
    val localY = displayedHeightPx / 2f - frameHeightPx / 2f - offsetY
    val srcX = localX / effectiveScale
    val srcY = localY / effectiveScale
    val srcWidth = frameWidthPx / effectiveScale
    val srcHeight = frameHeightPx / effectiveScale

    val clampedWidth = srcWidth.coerceIn(1f, source.width.toFloat())
    val clampedHeight = srcHeight.coerceIn(1f, source.height.toFloat())
    val clampedX = srcX.coerceIn(0f, source.width - clampedWidth)
    val clampedY = srcY.coerceIn(0f, source.height - clampedHeight)

    // Round width/height first, then re-derive x/y against the rounded size — rounding x/width
    // independently can push x + width 1px past source bounds and crash createBitmap.
    val width = clampedWidth.roundToInt().coerceIn(1, source.width)
    val height = clampedHeight.roundToInt().coerceIn(1, source.height)
    val x = clampedX.roundToInt().coerceIn(0, source.width - width)
    val y = clampedY.roundToInt().coerceIn(0, source.height - height)

    return Bitmap.createBitmap(source, x, y, width, height)
}
