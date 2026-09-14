from pathlib import Path
p=Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s=p.read_text()
# Replace index-modulo fan with viewport-relative geometry. This makes the fan rotate continuously as LazyRow scrolls.
s=s.replace('''                                val curveStep = index % 9
                                val arcOffset = when (curveStep) {
                                    0, 8 -> 18.dp
                                    1, 7 -> 11.dp
                                    2, 6 -> 6.dp
                                    3, 5 -> 2.dp
                                    else -> 0.dp
                                }
                                val spokeRotation = when (curveStep) {
                                    0 -> -32f
                                    1 -> -24f
                                    2 -> -16f
                                    3 -> -8f
                                    4 -> 0f
                                    5 -> 8f
                                    6 -> 16f
                                    7 -> 24f
                                    else -> 32f
                                }''','''                                val firstVisible = glassQueueListState.firstVisibleItemIndex
                                val scrollFraction = glassQueueListState.firstVisibleItemScrollOffset / 50f
                                val visualSlot = index - firstVisible - scrollFraction - 3.5f
                                val normalized = (visualSlot / 3.5f).coerceIn(-1.15f, 1.15f)
                                val arcOffset = (18f * normalized * normalized).dp
                                val spokeRotation = 32f * normalized''',1)
# attach list state
s=s.replace('''                        LazyRow(
                            modifier = Modifier''','''                        LazyRow(
                            state = glassQueueListState,
                            modifier = Modifier''',1)
# add state next to queue declaration
s=s.replace('''                val glassUpcoming = state.queue.drop(state.currentIndex + 1)
                Box(''','''                val glassUpcoming = state.queue.drop(state.currentIndex + 1)
                val glassQueueListState = rememberLazyListState()
                Box(''',1)
# Align separator to the actual fan baseline: same horizontal inset, centered immediately below queue.
s=s.replace('''                        .fillMaxWidth()
                        .height(28.dp)
                ) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.46f),
                        startAngle = 198f,
                        sweepAngle = 144f,
                        useCenter = false,
                        topLeft = Offset(10.dp.toPx(), -88.dp.toPx()),
                        size = Size(size.width - 20.dp.toPx(), 180.dp.toPx()),''','''                        .fillMaxWidth()
                        .height(24.dp)
                        .padding(horizontal = 28.dp)
                ) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.46f),
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(0f, -92.dp.toPx()),
                        size = Size(size.width, 188.dp.toPx()),''',1)
p.write_text(s)

about=Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a=about.read_text(); needle='private val latestUpdates = listOf(\n'
entry='    "Fixed Glass Up Next motion so songs continuously rotate around the circular fan while scrolling, with the separator realigned to the fan baseline",\n'
a=a.replace(needle,needle+entry,1); about.write_text(a)
