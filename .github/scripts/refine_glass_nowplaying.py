from pathlib import Path

np = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = np.read_text()

def replace_once(old: str, new: str, label: str):
    global s
    count = s.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly 1 match, found {count}')
    s = s.replace(old, new, 1)

# Keep the album-art backdrop readable instead of heavily zoomed/washed out.
replace_once(
'''                    .graphicsLayer {
                        scaleX = 1.16f
                        scaleY = 1.16f
                    }
                    .blur(10.dp)
                    .alpha(0.92f),''',
'''                    .graphicsLayer {
                        scaleX = 1.03f
                        scaleY = 1.03f
                    }
                    .blur(14.dp)
                    .alpha(0.86f),''',
'glass background scale/blur'
)
replace_once(
'''                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))''',
'''                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f))''',
'glass background tint'
)
replace_once(
'''                    .background(Color.White.copy(alpha = 0.10f))''',
'''                    .background(Color.White.copy(alpha = 0.14f))''',
'glass background veil'
)
replace_once(
'''                        glassmorphismNowPlaying -> Color.White.copy(alpha = 0.11f)''',
'''                        glassmorphismNowPlaying -> Color.White.copy(alpha = 0.16f)''',
'glass panel opacity'
)
replace_once(
'''                    color = if (glassmorphismNowPlaying) Color.White.copy(alpha = 0.30f) else Color.Transparent,''',
'''                    color = if (glassmorphismNowPlaying) Color.White.copy(alpha = 0.38f) else Color.Transparent,''',
'glass panel border'
)

# Add the centered Music label from the reference header.
replace_once(
'''                    IconButton(onClick = onCollapse) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse")
                    }
                    IconButton(''',
'''                    IconButton(onClick = onCollapse) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse")
                    }
                    Text(
                        text = "Music",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                    IconButton(''',
'glass header title'
)

# Bring metadata closer to the header in glass mode and center it.
replace_once(
'''            Column(modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {''',
'''            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (glassmorphismNowPlaying) 10.dp else 32.dp)
            ) {''',
'track info spacing'
)
replace_once(
'''                        Column {
                            Text(
                                text = t?.name.orEmpty(),''',
'''                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (glassmorphismNowPlaying) {
                                Alignment.CenterHorizontally
                            } else {
                                Alignment.Start
                            }
                        ) {
                            Text(
                                text = t?.name.orEmpty(),''',
'track info alignment'
)

# Hide the crowded secondary action row only in glass mode. All actions remain in normal mode.
icon_start = s.index('                    var topIconRowModifier: Modifier = Modifier')
icon_end_marker = '                }\n            }\n\n            if (glassmorphismNowPlaying) {'
icon_end = s.index(icon_end_marker, icon_start)
icon_block = s[icon_start:icon_end]
s = s[:icon_start] + '                    if (!glassmorphismNowPlaying) {\n' + ''.join('    ' + line if line.strip() else line for line in icon_block.splitlines(True)) + '                    }\n' + s[icon_end:]

# Replace the entire glass timeline + transport area with a tighter reference-faithful layout.
glass_start = s.index('            if (glassmorphismNowPlaying) {\n                // Reference-style arched timeline')
normal_marker = '            } else {\n                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp))'
glass_end = s.index(normal_marker, glass_start)
new_glass = r'''            if (glassmorphismNowPlaying) {
                // Compact reference-style arched seek timeline.
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
                        .padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(86.dp)
                            .padding(horizontal = 10.dp)
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
                            val stroke = 2.6.dp.toPx()
                            val arcLeft = 16.dp.toPx()
                            val arcRight = size.width - 16.dp.toPx()
                            val arcWidth = arcRight - arcLeft
                            val arcHeight = 90.dp.toPx()
                            val top = 18.dp.toPx()
                            val arcSize = Size(arcWidth, arcHeight)
                            val startAngle = 205f
                            val sweepAngle = 130f

                            drawArc(
                                color = Color.White.copy(alpha = 0.34f),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(arcLeft, top),
                                size = arcSize,
                                style = Stroke(width = stroke)
                            )
                            drawArc(
                                color = Color.White.copy(alpha = 0.95f),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle * progressFraction,
                                useCenter = false,
                                topLeft = Offset(arcLeft, top),
                                size = arcSize,
                                style = Stroke(width = stroke)
                            )

                            val angle = Math.toRadians((startAngle + sweepAngle * progressFraction).toDouble())
                            val cx = arcLeft + arcWidth / 2f
                            val cy = top + arcHeight / 2f
                            val rx = arcWidth / 2f
                            val ry = arcHeight / 2f
                            val thumb = Offset(
                                x = cx + (rx * kotlin.math.cos(angle)).toFloat(),
                                y = cy + (ry * kotlin.math.sin(angle)).toFloat()
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.24f),
                                radius = 8.dp.toPx(),
                                center = thumb
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 4.5.dp.toPx(),
                                center = thumb
                            )
                        }
                    }

                    IconButton(
                        onClick = viewModel::toggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) accentColor else Color.White.copy(alpha = 0.88f),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Text(
                        text = "${formatMillis(state.positionMs)} / ${formatMillis(state.durationMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.84f),
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }

                // Keep shuffle and repeat close to the transport assembly like the reference.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, start = 50.dp, end = 50.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                            .clickable(onClick = viewModel::toggleShuffle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (state.shuffleEnabled) accentColor else Color.White.copy(alpha = 0.90f),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                            .clickable(onClick = viewModel::cycleRepeatMode),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            contentDescription = "Repeat",
                            tint = if (state.repeatMode != RepeatMode.OFF) accentColor else Color.White.copy(alpha = 0.90f),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                // Organic three-part liquid transport: left and right lobes curve into the raised
                // center well instead of looking like a normal rectangular Material pill.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp)
                        .padding(horizontal = 26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(70.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 36.dp,
                                        topEnd = 22.dp,
                                        bottomEnd = 22.dp,
                                        bottomStart = 36.dp
                                    )
                                )
                                .background(Color.White.copy(alpha = 0.16f))
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.34f),
                                    RoundedCornerShape(
                                        topStart = 36.dp,
                                        topEnd = 22.dp,
                                        bottomEnd = 22.dp,
                                        bottomStart = 36.dp
                                    )
                                )
                                .clickable(onClick = viewModel::skipPrevious),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(31.dp)
                            )
                        }

                        Box(modifier = Modifier.size(94.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(70.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 22.dp,
                                        topEnd = 36.dp,
                                        bottomEnd = 36.dp,
                                        bottomStart = 22.dp
                                    )
                                )
                                .background(Color.White.copy(alpha = 0.16f))
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.34f),
                                    RoundedCornerShape(
                                        topStart = 22.dp,
                                        topEnd = 36.dp,
                                        bottomEnd = 36.dp,
                                        bottomStart = 22.dp
                                    )
                                )
                                .clickable(onClick = viewModel::skipNext),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(31.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(114.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.13f))
                            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(91.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.96f))
                            .border(7.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                            .clickable(enabled = !state.isBuffering, onClick = viewModel::playPause),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isBuffering) {
                            CircularProgressIndicator(
                                strokeWidth = 3.dp,
                                color = Color.Black,
                                modifier = Modifier.size(30.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                }
'''
s = s[:glass_start] + new_glass + s[glass_end:]

# Hide the large volume row in glass mode; it remains unchanged in standard mode.
vol_start = s.index('            var volumeRowModifier = Modifier.fillMaxWidth().padding(top = 16.dp)')
vol_end = s.index('            if (audioVisualizerEnabled && hasRecordAudioPermission)', vol_start)
vol_block = s[vol_start:vol_end]
vol_block = ''.join('    ' + line if line.strip() else line for line in vol_block.splitlines(True))
s = s[:vol_start] + '            if (!glassmorphismNowPlaying) {\n' + vol_block + '            }\n\n' + s[vol_end:]

# Keep the glass reference screen minimal; visualizer and queue stay in normal mode.
s = s.replace(
    '            if (audioVisualizerEnabled && hasRecordAudioPermission) {',
    '            if (!glassmorphismNowPlaying && audioVisualizerEnabled && hasRecordAudioPermission) {',
    1
)
s = s.replace(
    '            if (upcoming.isNotEmpty()) {',
    '            if (upcoming.isNotEmpty() && !glassmorphismNowPlaying) {',
    1
)

np.write_text(s)

about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a = about.read_text()
needle = 'private val latestUpdates = listOf(\n'
if needle not in a:
    raise SystemExit('About latestUpdates marker missing')
entry = '    "Refined Glass Now Playing to closely match the reference: cleaner artwork, centered metadata, compact arc timeline, organic liquid transport, and minimal controls",\n'
if entry.strip() not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)

print('Refined Glass Now Playing patch applied successfully.')
