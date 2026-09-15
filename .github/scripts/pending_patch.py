from pathlib import Path

now = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = now.read_text()

marker = '''            if (!glassmorphismNowPlaying) {
                var volumeRowModifier = Modifier.fillMaxWidth().padding(top = 16.dp)'''

restore = '''            if (!glassmorphismNowPlaying) {
                // Restore the standard seek/timeline controls when Glass mode is disabled.
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

            if (!glassmorphismNowPlaying) {
                var volumeRowModifier = Modifier.fillMaxWidth().padding(top = 16.dp)'''

if 'Restore the standard seek/timeline controls when Glass mode is disabled.' not in s:
    assert marker in s, 'non-glass volume block marker not found'
    s = s.replace(marker, restore, 1)
now.write_text(s)

about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a = about.read_text()
needle = 'private val latestUpdates = listOf(\n'
entry = '    "Restored the normal seek bar, time labels, Shuffle, Previous, Play/Pause, Next and Repeat controls when Glass mode is off",\n'
assert needle in a, 'Latest updates list not found'
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
