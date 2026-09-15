from pathlib import Path

p = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = p.read_text()

marker = '                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {'
start = s.index(marker)
brace_start = s.index('{', start)
depth = 0
end = None
for i in range(brace_start, len(s)):
    c = s[i]
    if c == '{':
        depth += 1
    elif c == '}':
        depth -= 1
        if depth == 0:
            end = i + 1
            break
assert end is not None, 'transport block end not found'

new_block = '''                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            // Reference-matched liquid-glass transport: one organic four-lobed body,
                            // separate upper Shuffle/Repeat glass orbs, and a luminous center disc.
                            val transportShape = remember {
                                GenericShape { size, _ ->
                                    val w = size.width
                                    val h = size.height
                                    moveTo(w * 0.50f, 0f)
                                    cubicTo(w * 0.61f, 0f, w * 0.65f, h * 0.14f, w * 0.69f, h * 0.28f)
                                    cubicTo(w * 0.73f, h * 0.40f, w * 0.79f, h * 0.40f, w * 0.84f, h * 0.35f)
                                    cubicTo(w * 0.92f, h * 0.28f, w * 0.99f, h * 0.40f, w, h * 0.52f)
                                    cubicTo(w * 1.01f, h * 0.65f, w * 0.94f, h * 0.73f, w * 0.86f, h * 0.70f)
                                    cubicTo(w * 0.77f, h * 0.67f, w * 0.72f, h * 0.72f, w * 0.67f, h * 0.83f)
                                    cubicTo(w * 0.62f, h * 0.95f, w * 0.58f, h, w * 0.50f, h)
                                    cubicTo(w * 0.42f, h, w * 0.38f, h * 0.95f, w * 0.33f, h * 0.83f)
                                    cubicTo(w * 0.28f, h * 0.72f, w * 0.23f, h * 0.67f, w * 0.14f, h * 0.70f)
                                    cubicTo(w * 0.06f, h * 0.73f, -w * 0.01f, h * 0.65f, 0f, h * 0.52f)
                                    cubicTo(w * 0.01f, h * 0.40f, w * 0.08f, h * 0.28f, w * 0.16f, h * 0.35f)
                                    cubicTo(w * 0.21f, h * 0.40f, w * 0.27f, h * 0.40f, w * 0.31f, h * 0.28f)
                                    cubicTo(w * 0.35f, h * 0.14f, w * 0.39f, 0f, w * 0.50f, 0f)
                                    close()
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(322.dp)
                                    .height(184.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .width(306.dp)
                                        .height(150.dp)
                                        .clip(transportShape)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFFF3FAFF).copy(alpha = 0.42f),
                                                    Color(0xFFBBD9F3).copy(alpha = 0.38f),
                                                    Color(0xFF7FAEDB).copy(alpha = 0.30f)
                                                )
                                            )
                                        )
                                        .border(1.6.dp, Color.White.copy(alpha = 0.42f), transportShape)
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 29.dp, top = 4.dp)
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.28f),
                                                    Color(0xFFA7C9EA).copy(alpha = 0.30f)
                                                )
                                            )
                                        )
                                        .border(1.2.dp, Color.White.copy(alpha = 0.32f), CircleShape)
                                        .clickable(onClick = viewModel::toggleShuffle),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = if (state.shuffleEnabled) Color.White else Color.White.copy(alpha = 0.78f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(end = 29.dp, top = 4.dp)
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.28f),
                                                    Color(0xFFA7C9EA).copy(alpha = 0.30f)
                                                )
                                            )
                                        )
                                        .border(1.2.dp, Color.White.copy(alpha = 0.32f), CircleShape)
                                        .clickable(onClick = viewModel::cycleRepeatMode),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                        contentDescription = "Repeat",
                                        tint = if (state.repeatMode != RepeatMode.OFF) Color.White else Color.White.copy(alpha = 0.78f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 26.dp)
                                        .offset(y = 23.dp)
                                        .size(72.dp)
                                        .clickable(onClick = viewModel::skipPrevious),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.SkipPrevious,
                                        contentDescription = "Previous",
                                        tint = Color.White.copy(alpha = 0.94f),
                                        modifier = Modifier.size(34.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 26.dp)
                                        .offset(y = 23.dp)
                                        .size(72.dp)
                                        .clickable(onClick = viewModel::skipNext),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.SkipNext,
                                        contentDescription = "Next",
                                        tint = Color.White.copy(alpha = 0.94f),
                                        modifier = Modifier.size(34.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 5.dp)
                                        .size(128.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.sweepGradient(
                                                colors = listOf(
                                                    Color(0xFF69D8FF).copy(alpha = 0.78f),
                                                    Color(0xFFB695FF).copy(alpha = 0.72f),
                                                    Color(0xFFFFA9D5).copy(alpha = 0.56f),
                                                    Color(0xFF66E7E0).copy(alpha = 0.68f),
                                                    Color(0xFF69D8FF).copy(alpha = 0.78f)
                                                )
                                            )
                                        )
                                        .border(1.5.dp, Color.White.copy(alpha = 0.62f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(99.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.96f))
                                            .clickable(enabled = !state.isBuffering, onClick = viewModel::playPause),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (state.isBuffering) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(34.dp),
                                                color = Color.Black.copy(alpha = 0.84f),
                                                strokeWidth = 3.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                                tint = Color.Black.copy(alpha = 0.90f),
                                                modifier = Modifier.size(48.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }'''

s = s[:start] + new_block + s[end:]
p.write_text(s)

about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a = about.read_text()
needle = 'private val latestUpdates = listOf(\n'
entry = '    "Rebuilt Glass playback controls to match the supplied liquid-glass reference: organic four-lobed body, upper Shuffle/Repeat orbs, white side transport icons, and luminous center Play/Pause disc",\n'
assert needle in a
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
