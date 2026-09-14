from pathlib import Path

np = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
text = np.read_text()
old = '''            var transportRowModifier = Modifier.fillMaxWidth().padding(top = 16.dp)
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
'''
new = '''            if (glassmorphismNowPlaying) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(1.dp, Color.White.copy(alpha = 0.34f), CircleShape)
                            .clickable(onClick = viewModel::toggleShuffle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (state.shuffleEnabled) accentColor else Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                                .clip(RoundedCornerShape(40.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(1.dp, Color.White.copy(alpha = 0.38f), RoundedCornerShape(40.dp))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = viewModel::skipPrevious,
                                modifier = Modifier.size(58.dp)
                            ) {
                                Icon(
                                    Icons.Filled.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.94f))
                                    .border(7.dp, Color.White.copy(alpha = 0.22f), CircleShape)
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
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = viewModel::skipNext,
                                modifier = Modifier.size(58.dp)
                            ) {
                                Icon(
                                    Icons.Filled.SkipNext,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(1.dp, Color.White.copy(alpha = 0.34f), CircleShape)
                            .clickable(onClick = viewModel::cycleRepeatMode),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            contentDescription = "Repeat",
                            tint = if (state.repeatMode != RepeatMode.OFF) accentColor else Color.White
                        )
                    }
                }
            } else {
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
if old not in text:
    raise SystemExit('transport block not found')
text = text.replace(old, new, 1)
np.write_text(text)

about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a = about.read_text()
needle = 'private val latestUpdates = listOf(\n'
entry = '    "Glassmorphism Now Playing now includes reference-style liquid-glass Previous, Play/Pause, Next, Shuffle and Repeat controls",\n'
if entry not in a:
    if needle not in a:
        raise SystemExit('about list not found')
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
