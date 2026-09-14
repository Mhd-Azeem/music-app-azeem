from pathlib import Path

root = Path('.')
np = root / 'app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt'
about = root / 'app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt'

text = np.read_text()

# Add imports used by the custom arc timeline.
if 'import androidx.compose.foundation.gestures.detectTapGestures' not in text:
    text = text.replace(
        'import androidx.compose.foundation.gestures.detectVerticalDragGestures\n',
        'import androidx.compose.foundation.gestures.detectTapGestures\nimport androidx.compose.foundation.gestures.detectVerticalDragGestures\n'
    )
if 'import androidx.compose.ui.graphics.drawscope.Stroke' not in text:
    text = text.replace(
        'import androidx.compose.ui.graphics.graphicsLayer\n',
        'import androidx.compose.ui.graphics.graphicsLayer\nimport androidx.compose.ui.graphics.drawscope.Stroke\n'
    )

# Make the full-screen artwork much closer to the reference: recognizable artwork with soft glass blur.
text = text.replace('.blur(34.dp)\n                    .alpha(0.78f)', '.blur(10.dp)\n                    .alpha(0.92f)')
text = text.replace('.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))', '.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))')
text = text.replace('.background(Color.White.copy(alpha = 0.06f))', '.background(Color.White.copy(alpha = 0.10f))')

# In glass mode the reference uses the artwork as the large visual surface rather than a square card.
text = text.replace(
    'AnimatedVisibility(visible = !isUpNextExpanded) {\n                    Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {',
    'AnimatedVisibility(visible = !isUpNextExpanded && !glassmorphismNowPlaying) {\n                    Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {'
)

start = text.index('            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {\n                Slider(')
end = text.index('            var volumeRowModifier = Modifier.fillMaxWidth().padding(top = 16.dp)', start)

replacement = r'''            if (glassmorphismNowPlaying) {
                // Reference-style arched timeline: a shallow upward arc, draggable white thumb,
                // favorite centered beneath it, and elapsed/total time on one centered line.
                val progressFraction = if (state.durationMs > 0L) {
                    (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val seekFromX: (Float, Float) -> Unit = { x, width ->
                    if (state.durationMs > 0L && width > 0f) {
                        val p = (x / width).coerceIn(0f, 1f)
                        viewModel.seekTo((state.durationMs * p).toLong())
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(108.dp)
                            .pointerInput(state.durationMs) {
                                detectTapGestures { offset -> seekFromX(offset.x, size.width.toFloat()) }
                            }
                            .pointerInput(state.durationMs) {
                                detectDragGestures(
                                    onDragStart = { offset -> seekFromX(offset.x, size.width.toFloat()) }
                                ) { change, _ ->
                                    change.consume()
                                    seekFromX(change.position.x, size.width.toFloat())
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 3.2.dp.toPx()
                            val arcLeft = 10.dp.toPx()
                            val arcRight = size.width - 10.dp.toPx()
                            val arcWidth = arcRight - arcLeft
                            val arcHeight = 104.dp.toPx()
                            val top = 28.dp.toPx()
                            val arcSize = Size(arcWidth, arcHeight)

                            drawArc(
                                color = Color.White.copy(alpha = 0.38f),
                                startAngle = 200f,
                                sweepAngle = 140f,
                                useCenter = false,
                                topLeft = Offset(arcLeft, top),
                                size = arcSize,
                                style = Stroke(width = stroke)
                            )
                            drawArc(
                                color = Color.White.copy(alpha = 0.96f),
                                startAngle = 200f,
                                sweepAngle = 140f * progressFraction,
                                useCenter = false,
                                topLeft = Offset(arcLeft, top),
                                size = arcSize,
                                style = Stroke(width = stroke)
                            )

                            val angle = Math.toRadians((200f + 140f * progressFraction).toDouble())
                            val cx = arcLeft + arcWidth / 2f
                            val cy = top + arcHeight / 2f
                            val rx = arcWidth / 2f
                            val ry = arcHeight / 2f
                            val thumb = Offset(
                                x = cx + (rx * kotlin.math.cos(angle)).toFloat(),
                                y = cy + (ry * kotlin.math.sin(angle)).toFloat()
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.30f),
                                radius = 9.dp.toPx(),
                                center = thumb
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 5.dp.toPx(),
                                center = thumb
                            )
                        }
                    }

                    IconButton(
                        onClick = viewModel::toggleFavorite,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) accentColor else Color.White.copy(alpha = 0.88f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "${formatMillis(state.positionMs)} / ${formatMillis(state.durationMs)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.86f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Small floating shuffle/repeat circles above the connected liquid transport body.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape)
                            .clickable(onClick = viewModel::toggleShuffle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (state.shuffleEnabled) accentColor else Color.White.copy(alpha = 0.90f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape)
                            .clickable(onClick = viewModel::cycleRepeatMode),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            contentDescription = "Repeat",
                            tint = if (state.repeatMode != RepeatMode.OFF) accentColor else Color.White.copy(alpha = 0.90f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Connected liquid-glass transport, shaped like the reference: two rounded side
                // lobes flow into a raised circular center well, with a bright floating play/pause.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(142.dp)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .clip(RoundedCornerShape(42.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(1.dp, Color.White.copy(alpha = 0.38f), RoundedCornerShape(42.dp))
                    )
                    Box(
                        modifier = Modifier
                            .size(118.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(1.dp, Color.White.copy(alpha = 0.36f), CircleShape)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = viewModel::skipPrevious,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                Icons.Filled.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Box(modifier = Modifier.size(100.dp))

                        IconButton(
                            onClick = viewModel::skipNext,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.97f))
                            .border(8.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                            .clickable(enabled = !state.isBuffering, onClick = viewModel::playPause),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isBuffering) {
                            CircularProgressIndicator(
                                strokeWidth = 3.dp,
                                color = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Slider(
                        value = state.positionMs.toFloat().coerceIn(0f, state.durationMs.toFloat().coerceAtLeast(1f)),
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatMillis(state.positionMs), style = MaterialTheme.typography.labelSmall)
                        Text(formatMillis(state.durationMs), style = MaterialTheme.typography.labelSmall)
                    }
                }

                var transportRowModifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                if (isLiquid) {
                    transportRowModifier = transportRowModifier
                        .clip(pillShape)
                        .hazeChild(state = hazeState, style = glassStyle) { inputScale = HazeInputScale.Auto }
                        .border(1.dp, Color.White.copy(alpha = 0.25f), pillShape)
                }
                Row(
                    modifier = transportRowModifier,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = viewModel::toggleShuffle) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (state.shuffleEnabled) accentColor else Color.White
                        )
                    }
                    IconButton(onClick = viewModel::skipPrevious) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.padding(4.dp))
                    }
                    if (state.isBuffering) {
                        Box(modifier = Modifier.padding(8.dp).padding(4.dp).size(24.dp)) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.fillMaxSize())
                        }
                    } else {
                        IconButton(onClick = viewModel::playPause, modifier = Modifier.padding(8.dp)) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                    IconButton(onClick = viewModel::skipNext) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                    }
                    IconButton(onClick = viewModel::cycleRepeatMode) {
                        Icon(
                            imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            contentDescription = "Repeat",
                            tint = if (state.repeatMode != RepeatMode.OFF) accentColor else Color.White
                        )
                    }
                }
            }

'''

text = text[:start] + replacement + text[end:]
np.write_text(text)

about_text = about.read_text()
entry = '    "Glass Now Playing now matches the reference with an arched seek timeline and connected liquid playback controls",\n'
marker = 'private val latestUpdates = listOf(\n'
if entry.strip() not in about_text:
    about_text = about_text.replace(marker, marker + entry)
about.write_text(about_text)
