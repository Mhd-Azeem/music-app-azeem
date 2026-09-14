from pathlib import Path
p=Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s=p.read_text()
# Show the whole remaining queue; LazyRow keeps it efficient.
s=s.replace('val glassUpcoming = state.queue.drop(state.currentIndex + 1).take(12)', 'val glassUpcoming = state.queue.drop(state.currentIndex + 1)', 1)
old='''                                val curveStep = index % 7
                                val arcOffset = when (curveStep) {
                                    0, 6 -> 12.dp
                                    1, 5 -> 7.dp
                                    2, 4 -> 3.dp
                                    else -> 0.dp
                                }'''
new='''                                // Fan the upcoming titles around a shallow circular arc. The
                                // LazyRow still scrolls horizontally, while each visible item is
                                // angled like a spoke instead of staying parallel.
                                val curveStep = index % 9
                                val arcOffset = when (curveStep) {
                                    0, 8 -> 18.dp
                                    1, 7 -> 12.dp
                                    2, 6 -> 7.dp
                                    3, 5 -> 3.dp
                                    else -> 0.dp
                                }
                                val spokeRotation = when (curveStep) {
                                    0 -> -30f
                                    1 -> -22f
                                    2 -> -14f
                                    3 -> -7f
                                    4 -> 0f
                                    5 -> 7f
                                    6 -> 14f
                                    7 -> 22f
                                    else -> 30f
                                }'''
if old not in s: raise SystemExit('curve block not found')
s=s.replace(old,new,1)
s=s.replace('.graphicsLayer { rotationZ = -90f },', '.graphicsLayer { rotationZ = -90f + spokeRotation },',1)
# Add stronger lower-panel blur and a clean curved separator before foreground controls.
needle='''        if (isLiquid && liquidAlbumArtBackground && !track?.albumArtUrl.isNullOrBlank()) {'''
insert='''        if (glassmorphismNowPlaying && !track?.albumArtUrl.isNullOrBlank()) {
            // The lower playback zone is deliberately more frosted than the artwork/queue zone.
            // Clip a second copy of the same artwork to the lower 43% so the split feels like
            // one continuous image rather than a different background.
            AsyncImage(
                model = track?.albumArtUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        clip = true
                        shape = GenericShape { size, _ ->
                            moveTo(0f, size.height * 0.57f)
                            lineTo(size.width, size.height * 0.57f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                    }
                    .blur(17.dp)
                    .alpha(0.88f),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Transparent,
                                0.56f to Color.Transparent,
                                0.59f to Color.Black.copy(alpha = 0.04f),
                                1.00f to Color.Black.copy(alpha = 0.14f)
                            )
                        )
                    )
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val y = size.height * 0.585f
                val inset = 18.dp.toPx()
                val path = Path().apply {
                    moveTo(inset, y + 8.dp.toPx())
                    quadraticBezierTo(size.width / 2f, y - 22.dp.toPx(), size.width - inset, y + 8.dp.toPx())
                }
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.58f),
                    style = Stroke(width = 1.35.dp.toPx())
                )
            }
        }

'''+needle
if needle not in s: raise SystemExit('background insertion point not found')
s=s.replace(needle,insert,1)
p.write_text(s)

a=Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
t=a.read_text()
needle2='private val latestUpdates = listOf(\n'
entry='    "Glass Now Playing now separates Up Next from playback with a curved divider, stronger lower-panel frost, and a horizontally scrollable circular fan queue",\n'
if entry not in t: t=t.replace(needle2,needle2+entry,1)
a.write_text(t)
