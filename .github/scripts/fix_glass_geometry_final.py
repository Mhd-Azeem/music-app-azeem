from pathlib import Path
p=Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s=p.read_text()
# Queue: compact inward fan, keep edge titles within viewport.
s=s.replace('.height(116.dp),\n                            contentPadding = PaddingValues(horizontal = 38.dp),\n                            horizontalArrangement = Arrangement.spacedBy(1.dp)', '.height(106.dp),\n                            contentPadding = PaddingValues(horizontal = 58.dp),\n                            horizontalArrangement = Arrangement.spacedBy(2.dp)',1)
old='''                                val arcOffset = when (curveStep) {
                                    0, 8 -> 0.dp
                                    1, 7 -> 8.dp
                                    2, 6 -> 15.dp
                                    3, 5 -> 21.dp
                                    else -> 24.dp
                                }
                                val spokeRotation = when (curveStep) {
                                    0 -> -38f
                                    1 -> -29f
                                    2 -> -20f
                                    3 -> -10f
                                    4 -> 0f
                                    5 -> 10f
                                    6 -> 20f
                                    7 -> 29f
                                    else -> 38f
                                }'''
new='''                                val arcOffset = when (curveStep) {
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
                                }'''
assert old in s
s=s.replace(old,new,1)
s=s.replace('.width(50.dp)\n                                        .height(104.dp)', '.width(48.dp)\n                                        .height(94.dp)',1)
s=s.replace('.width(88.dp)\n                                            .graphicsLayer', '.width(80.dp)\n                                            .graphicsLayer',1)
# Separator: one thin clean curve only; remove the rectangular frost band entirely.
start=s.index('            if (glassmorphismNowPlaying) {\n                // Curved translucent separator:')
end=s.index('            if (glassmorphismNowPlaying) {\n                // Compact reference-style arched seek timeline.', start)
separator='''            if (glassmorphismNowPlaying) {
                // Single thin curved boundary between Up Next and the active player.
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                ) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.46f),
                        startAngle = 198f,
                        sweepAngle = 144f,
                        useCenter = false,
                        topLeft = Offset(10.dp.toPx(), -88.dp.toPx()),
                        size = Size(size.width - 20.dp.toPx(), 180.dp.toPx()),
                        style = Stroke(width = 1.4.dp.toPx())
                    )
                }
            }

'''
s=s[:start]+separator+s[end:]
# Seek arc shorter/lower so it cannot visually merge with separator.
s=s.replace('.height(132.dp)\n                            .padding(horizontal = 2.dp)', '.height(112.dp)\n                            .padding(horizontal = 24.dp)',1)
s=s.replace('val arcHeight = 270.dp.toPx()\n                            val top = -10.dp.toPx()', 'val arcHeight = 238.dp.toPx()\n                            val top = -4.dp.toPx()',1)
# Saturn: compact reference proportions and eliminate redundant outer centre circle.
s=s.replace('.height(160.dp)\n                        .padding(horizontal = 50.dp)', '.height(142.dp)\n                        .padding(horizontal = 78.dp)',1)
s=s.replace('.height(132.dp)\n                            .clip(saturnTransportShape)', '.height(108.dp)\n                            .clip(saturnTransportShape)',1)
s=s.replace('Box(modifier = Modifier.width(104.dp).fillMaxHeight())', 'Box(modifier = Modifier.width(92.dp).fillMaxHeight())',1)
outer='''                    Box(
                        modifier = Modifier
                            .size(138.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(1.6.dp, Color.White.copy(alpha = 0.50f), CircleShape)
                    )
'''
assert outer in s
s=s.replace(outer,'',1)
s=s.replace('.size(98.dp)\n                            .clip(CircleShape)', '.size(94.dp)\n                            .clip(CircleShape)',1)
s=s.replace('modifier = Modifier.size(44.dp)', 'modifier = Modifier.size(42.dp)',1)
p.write_text(s)

about=Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a=about.read_text()
needle='private val latestUpdates = listOf(\n'
entry='    "Corrected Glass Now Playing geometry: clean single separator, unclipped circular Up Next fan, separated seek arc, and compact single-outline Saturn controls",\n'
assert needle in a
a=a.replace(needle,needle+entry,1)
about.write_text(a)
