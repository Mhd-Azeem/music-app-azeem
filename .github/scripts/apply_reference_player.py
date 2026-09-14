from pathlib import Path
p=Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s=p.read_text()
start=s.index('            if (glassmorphismNowPlaying) {\n                val glassUpcoming = state.queue.drop(state.currentIndex + 1)')
end=s.index('            if (!glassmorphismNowPlaying) {', start)
new='''            if (glassmorphismNowPlaying) {
                val glassUpcoming = state.queue.drop(state.currentIndex + 1)
                val fanState = rememberLazyListState()

                // Up Next: real queue cards arranged as a horizontally scrollable circular fan.
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (glassUpcoming.isNotEmpty()) {
                        LazyRow(
                            state = fanState,
                            modifier = Modifier.fillMaxWidth().height(184.dp),
                            contentPadding = PaddingValues(horizontal = 54.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            itemsIndexed(glassUpcoming, key = { _, entry -> entry.instanceId }) { index, entry ->
                                val centerIndex = fanState.firstVisibleItemIndex + 3
                                val delta = (index - centerIndex).coerceIn(-4, 4)
                                val distance = kotlin.math.abs(delta)
                                val lift = when (distance) {
                                    0 -> 0.dp
                                    1 -> 7.dp
                                    2 -> 20.dp
                                    3 -> 39.dp
                                    else -> 62.dp
                                }
                                Box(
                                    modifier = Modifier
                                        .width(62.dp)
                                        .height(146.dp)
                                        .offset(y = lift)
                                        .graphicsLayer { rotationZ = delta * 10f }
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(Color(0xFFFFEA28))
                                        .border(1.dp, Color.White.copy(alpha = 0.82f), RoundedCornerShape(11.dp))
                                        .clickable { viewModel.playQueueItem(state.currentIndex + 1 + index) }
                                        .padding(5.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        AsyncImage(
                                            model = entry.track.albumArtUrl,
                                            contentDescription = entry.track.name,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp)
                                                .clip(RoundedCornerShape(7.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(78.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = entry.track.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF111111),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.width(72.dp).graphicsLayer { rotationZ = 90f },
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val progressFraction = if (state.durationMs > 0L) {
                    (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val lowerGlassShape = remember {
                    GenericShape { size, _ ->
                        moveTo(0f, size.height * 0.23f)
                        cubicTo(size.width * 0.20f, size.height * 0.02f, size.width * 0.34f, 0f, size.width * 0.50f, 0f)
                        cubicTo(size.width * 0.66f, 0f, size.width * 0.80f, size.height * 0.02f, size.width, size.height * 0.23f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                }

                // One large frosted lower player panel with the same circular-cap silhouette.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(lowerGlassShape)
                        .background(Color(0xFFD7ECFF).copy(alpha = 0.82f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.78f), lowerGlassShape)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(top = 52.dp, start = 28.dp, end = 28.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Single curved seek line inside the panel.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(92.dp)
                                .pointerInput(state.durationMs) {
                                    detectTapGestures { offset ->
                                        if (state.durationMs > 0L) {
                                            val p = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                            viewModel.seekTo((state.durationMs * p).toLong())
                                        }
                                    }
                                }
                        ) {
                            Canvas(Modifier.fillMaxSize()) {
                                val startAngle = 202f
                                val sweepAngle = 136f
                                val arcSize = Size(size.width, 176.dp.toPx())
                                val top = -5.dp.toPx()
                                drawArc(
                                    color = Color(0xFF087EAE),
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = Offset(0f, top),
                                    size = arcSize,
                                    style = Stroke(width = 2.4.dp.toPx())
                                )
                                val angle = Math.toRadians((startAngle + sweepAngle * progressFraction).toDouble())
                                val cx = size.width / 2f
                                val cy = top + arcSize.height / 2f
                                val thumb = Offset(
                                    cx + (arcSize.width / 2f * kotlin.math.cos(angle)).toFloat(),
                                    cy + (arcSize.height / 2f * kotlin.math.sin(angle)).toFloat()
                                )
                                drawCircle(Color(0xFF087EAE), 7.dp.toPx(), thumb)
                            }
                            Text(
                                text = formatMillis(state.positionMs),
                                color = Color(0xFF08283A),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.align(Alignment.BottomStart)
                            )
                            Text(
                                text = formatMillis(state.durationMs),
                                color = Color(0xFF08283A),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.align(Alignment.BottomEnd)
                            )
                        }

                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            // Compact connected Previous / Play-Pause / Next control.
                            Row(
                                modifier = Modifier
                                    .width(220.dp)
                                    .height(76.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFF9CCEF4).copy(alpha = 0.72f)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier.width(62.dp).fillMaxHeight().clickable(onClick = viewModel::skipPrevious),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.SkipPrevious, "Previous", tint = Color(0xFF075B83), modifier = Modifier.size(34.dp))
                                }
                                Box(modifier = Modifier.width(96.dp))
                                Box(
                                    modifier = Modifier.width(62.dp).fillMaxHeight().clickable(onClick = viewModel::skipNext),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.SkipNext, "Next", tint = Color(0xFF075B83), modifier = Modifier.size(34.dp))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(126.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF9CCEF4).copy(alpha = 0.92f))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.75f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(92.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD9EEFF))
                                        .clickable(enabled = !state.isBuffering, onClick = viewModel::playPause),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.isBuffering) {
                                        CircularProgressIndicator(modifier = Modifier.size(36.dp), color = Color(0xFF075B83), strokeWidth = 3.dp)
                                    } else {
                                        Icon(
                                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                                            tint = Color(0xFF075B83),
                                            modifier = Modifier.size(46.dp)
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier.align(Alignment.CenterStart).size(48.dp).clip(CircleShape)
                                    .background(Color(0xFFEAF6FF).copy(alpha = 0.82f)).clickable(onClick = viewModel::toggleShuffle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Shuffle, "Shuffle", tint = if (state.shuffleEnabled) accentColor else Color(0xFF08283A), modifier = Modifier.size(24.dp))
                            }
                            Box(
                                modifier = Modifier.align(Alignment.CenterEnd).size(48.dp).clip(CircleShape)
                                    .background(Color(0xFFEAF6FF).copy(alpha = 0.82f)).clickable(onClick = viewModel::cycleRepeatMode),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                    contentDescription = "Repeat",
                                    tint = if (state.repeatMode != RepeatMode.OFF) accentColor else Color(0xFF08283A),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

'''
s=s[:start]+new+s[end:]
p.write_text(s)

about=Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a=about.read_text()
needle='private val latestUpdates = listOf(\n'
entry='    "Rebuilt Glass Now Playing to the supplied reference: yellow album-art Up Next cards in a circular swipe fan, one large curved frosted player panel, arched seek line, and compact connected transport",\n'
assert needle in a
a=a.replace(needle,needle+entry,1)
about.write_text(a)
