from pathlib import Path
p=Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s=p.read_text()
old='''                    if (glassUpcoming.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(92.dp),
                            contentPadding = PaddingValues(horizontal = 54.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(glassUpcoming, key = { _, entry -> entry.instanceId }) { index, entry ->
                                // Keep the queue horizontally scrollable while arranging each
                                // vertical title along one shallow, smooth visual curve.
                                val curveStep = index % 7
                                val arcOffset = when (curveStep) {
                                    0, 6 -> 12.dp
                                    1, 5 -> 7.dp
                                    2, 4 -> 3.dp
                                    else -> 0.dp
                                }
                                Box(
                                    modifier = Modifier
                                        .width(46.dp)
                                        .height(82.dp)
                                        .offset(y = arcOffset)
                                        .clickable { viewModel.playQueueItem(state.currentIndex + 1 + index) }
                                        .padding(vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = entry.track.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.94f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .width(78.dp)
                                            .graphicsLayer { rotationZ = -90f },
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }'''
new='''                    if (glassUpcoming.isNotEmpty()) {
                        // Up Next behaves like labels travelling around the upper part of a wheel:
                        // horizontal swiping moves the queue, while offsets + tangential rotation
                        // give the visible songs a circular/fan arrangement without card outlines.
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(106.dp),
                            contentPadding = PaddingValues(horizontal = 38.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            itemsIndexed(glassUpcoming, key = { _, entry -> entry.instanceId }) { index, entry ->
                                val wheel = index % 9
                                val arcOffset = when (wheel) {
                                    0, 8 -> 26.dp
                                    1, 7 -> 16.dp
                                    2, 6 -> 8.dp
                                    3, 5 -> 3.dp
                                    else -> 0.dp
                                }
                                val tangent = when (wheel) {
                                    0 -> -24f
                                    1 -> -18f
                                    2 -> -12f
                                    3 -> -6f
                                    4 -> 0f
                                    5 -> 6f
                                    6 -> 12f
                                    7 -> 18f
                                    else -> 24f
                                }
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(88.dp)
                                        .offset(y = arcOffset)
                                        .clickable { viewModel.playQueueItem(state.currentIndex + 1 + index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = entry.track.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.94f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .width(82.dp)
                                            .graphicsLayer { rotationZ = -90f + tangent },
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        // Curved separator requested between Up Next and the active-player zone.
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .align(Alignment.BottomCenter)
                        ) {
                            drawArc(
                                color = Color.White.copy(alpha = 0.62f),
                                startAngle = 198f,
                                sweepAngle = 144f,
                                useCenter = false,
                                topLeft = Offset(-18.dp.toPx(), -82.dp.toPx()),
                                size = Size(size.width + 36.dp.toPx(), 150.dp.toPx()),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }'''
if old not in s: raise SystemExit('target Up Next block not found')
s=s.replace(old,new,1)
# Add a stronger translucent veil to the lower player zone, starting after Up Next.
needle='''            if (glassmorphismNowPlaying) {
                // Compact reference-style arched seek timeline.'''
repl='''            if (glassmorphismNowPlaying) {
                // The active-player zone below the separator is intentionally softer than Up Next.
                // This translucent veil increases the perceived blur/separation while preserving
                // album-art colour and keeping controls legible.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color.White.copy(alpha = 0.045f))
                )

                // Compact reference-style arched seek timeline.'''
if needle not in s: raise SystemExit('seek marker not found')
s=s.replace(needle,repl,1)
p.write_text(s)

a=Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
t=a.read_text(); needle2='private val latestUpdates = listOf(\n'
entry='    "Glass Now Playing now separates Up Next with a curved glass line and uses a circular fan-style horizontally scrollable queue",\n'
if entry not in t: t=t.replace(needle2,needle2+entry,1)
a.write_text(t)
