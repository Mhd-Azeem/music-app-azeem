from pathlib import Path
p=Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s=p.read_text()
# Insert a timeline-matched curved translucent separator directly between Up Next and playback.
anchor='''            if (glassmorphismNowPlaying) {
                // Compact reference-style arched seek timeline.'''
separator='''            if (glassmorphismNowPlaying) {
                // Curved translucent separator: deliberately follows the same visual language
                // as the seek timeline, separating Up Next from the active playback controls.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val separatorStroke = 5.6.dp.toPx()
                        val separatorHeight = 132.dp.toPx()
                        drawArc(
                            color = Color.White.copy(alpha = 0.42f),
                            startAngle = 202f,
                            sweepAngle = 136f,
                            useCenter = false,
                            topLeft = Offset(0f, -78.dp.toPx()),
                            size = Size(size.width, separatorHeight),
                            style = Stroke(width = separatorStroke)
                        )
                    }
                    // Extra soft frost immediately below the divider. This intentionally stays
                    // subtle so the lower playback zone is only a little blurrier than Up Next.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(30.dp)
                            .blur(10.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.035f),
                                        Color.Black.copy(alpha = 0.14f)
                                    )
                                )
                            )
                    )
                }
            }

            if (glassmorphismNowPlaying) {
                // Compact reference-style arched seek timeline.'''
assert anchor in s, 'timeline anchor missing'
s=s.replace(anchor,separator,1)
# Transport: larger centre orbit, side controls tucked closer to it.
s=s.replace('.height(154.dp)\n                        .padding(horizontal = 42.dp)', '.height(160.dp)\n                        .padding(horizontal = 50.dp)',1)
s=s.replace('.height(126.dp)\n                            .clip(saturnTransportShape)', '.height(132.dp)\n                            .clip(saturnTransportShape)',1)
s=s.replace('Box(modifier = Modifier.width(126.dp).fillMaxHeight())', 'Box(modifier = Modifier.width(104.dp).fillMaxHeight())',1)
s=s.replace('.size(126.dp)\n                            .clip(CircleShape)\n                            .background(Color.White.copy(alpha = 0.10f))', '.size(138.dp)\n                            .clip(CircleShape)\n                            .background(Color.White.copy(alpha = 0.10f))\n                            .border(1.6.dp, Color.White.copy(alpha = 0.50f), CircleShape)',1)
s=s.replace('.size(94.dp)\n                            .clip(CircleShape)', '.size(98.dp)\n                            .clip(CircleShape)',1)
p.write_text(s)

about=Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a=about.read_text()
needle='private val latestUpdates = listOf(\n'
entry='    "Now Playing refined with a thicker translucent timeline-shaped separator, softer lower playback frost, larger Play/Pause orbit, and Previous/Next moved closer to center",\n'
assert needle in a
a=a.replace(needle,needle+entry,1)
about.write_text(a)
