from pathlib import Path
p=Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s=p.read_text()
old='''                                val arcOffset = when (index % 5) {
                                    0, 4 -> 28.dp
                                    1, 3 -> 12.dp
                                    else -> 0.dp
                                }
                                Box(
                                    modifier = Modifier
                                        .width(68.dp)
                                        .height(104.dp)
                                        .offset(y = arcOffset)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(Color.White.copy(alpha = 0.075f))
                                        .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(22.dp))
                                        .clickable { viewModel.playQueueItem(state.currentIndex + 1 + index) }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "%02d   ${entry.track.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.92f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .width(92.dp)
                                            .graphicsLayer { rotationZ = -90f },
                                        textAlign = TextAlign.Center
                                    )
                                }'''
new='''                                // Keep the queue horizontally scrollable while arranging each
                                // vertical title along one shallow, smooth visual curve.
                                val curveStep = index % 7
                                val arcOffset = when (curveStep) {
                                    0, 6 -> 18.dp
                                    1, 5 -> 10.dp
                                    2, 4 -> 4.dp
                                    else -> 0.dp
                                }
                                Box(
                                    modifier = Modifier
                                        .width(62.dp)
                                        .height(100.dp)
                                        .offset(y = arcOffset)
                                        .clickable { viewModel.playQueueItem(state.currentIndex + 1 + index) }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "%02d   ${entry.track.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.94f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .width(94.dp)
                                            .graphicsLayer { rotationZ = -90f },
                                        textAlign = TextAlign.Center
                                    )
                                }'''
if old not in s: raise SystemExit('Up Next block not found')
p.write_text(s.replace(old,new))

a=Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
t=a.read_text()
needle='private val latestUpdates = listOf(\n'
entry='    "Refined Glass Up Next: borderless vertical song names in a smoother horizontally scrollable curved layout",\n'
if entry not in t:
    t=t.replace(needle, needle+entry,1)
a.write_text(t)
