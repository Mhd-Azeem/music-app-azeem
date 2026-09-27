from pathlib import Path

np = Path("app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt")
ns = np.read_text()

old_shuffle = '''                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 29.dp, top = 4.dp)
                                        .size(UiDesignConfig.GLASS_ORB_SIZE_DP.dp)
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
'''

new_shuffle = '''                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 29.dp, top = 4.dp)
                                        .size(UiDesignConfig.GLASS_ORB_SIZE_DP.dp)
                                        .shadow(
                                            elevation = if (state.shuffleEnabled) 14.dp else 3.dp,
                                            shape = CircleShape,
                                            ambientColor = if (state.shuffleEnabled) glassPlayedGlowColor.copy(alpha = 0.70f) else Color.Transparent,
                                            spotColor = if (state.shuffleEnabled) glassPlayedGlowColor.copy(alpha = 0.85f) else Color.Transparent
                                        )
                                        .clip(CircleShape)
                                        .background(
                                            if (state.shuffleEnabled) {
                                                Brush.radialGradient(
                                                    listOf(
                                                        glassPlayedGlowColor.copy(alpha = 0.95f),
                                                        accentColor.copy(alpha = 0.82f),
                                                        Color(0xFF6D5BFF).copy(alpha = 0.78f)
                                                    )
                                                )
                                            } else {
                                                Brush.radialGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = 0.14f),
                                                        Color(0xFFA7C9EA).copy(alpha = 0.16f)
                                                    )
                                                )
                                            }
                                        )
                                        .border(
                                            if (state.shuffleEnabled) 2.dp else 1.1.dp,
                                            if (state.shuffleEnabled) Color.White.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.24f),
                                            CircleShape
                                        )
                                        .clickable(onClick = viewModel::toggleShuffle),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Shuffle,
                                        contentDescription = if (state.shuffleEnabled) "Shuffle on" else "Shuffle off",
                                        tint = if (state.shuffleEnabled) Color.White else Color.White.copy(alpha = 0.48f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 5.dp)
                                            .size(if (state.shuffleEnabled) 6.dp else 4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (state.shuffleEnabled) Color.White
                                                else Color.White.copy(alpha = 0.26f)
                                            )
                                    )
                                }
'''
assert old_shuffle in ns, "Glass Shuffle control block not found"
ns = ns.replace(old_shuffle, new_shuffle, 1)

old_repeat = '''                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(end = 29.dp, top = 4.dp)
                                        .size(UiDesignConfig.GLASS_ORB_SIZE_DP.dp)
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
'''

new_repeat = '''                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(end = 29.dp, top = 4.dp)
                                        .size(UiDesignConfig.GLASS_ORB_SIZE_DP.dp)
                                        .shadow(
                                            elevation = if (state.repeatMode != RepeatMode.OFF) 14.dp else 3.dp,
                                            shape = CircleShape,
                                            ambientColor = if (state.repeatMode != RepeatMode.OFF) glassPlayedGlowColor.copy(alpha = 0.70f) else Color.Transparent,
                                            spotColor = if (state.repeatMode != RepeatMode.OFF) glassPlayedGlowColor.copy(alpha = 0.85f) else Color.Transparent
                                        )
                                        .clip(CircleShape)
                                        .background(
                                            if (state.repeatMode != RepeatMode.OFF) {
                                                Brush.radialGradient(
                                                    listOf(
                                                        glassPlayedGlowColor.copy(alpha = 0.95f),
                                                        accentColor.copy(alpha = 0.82f),
                                                        Color(0xFF6D5BFF).copy(alpha = 0.78f)
                                                    )
                                                )
                                            } else {
                                                Brush.radialGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = 0.14f),
                                                        Color(0xFFA7C9EA).copy(alpha = 0.16f)
                                                    )
                                                )
                                            }
                                        )
                                        .border(
                                            if (state.repeatMode != RepeatMode.OFF) 2.dp else 1.1.dp,
                                            if (state.repeatMode != RepeatMode.OFF) Color.White.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.24f),
                                            CircleShape
                                        )
                                        .clickable(onClick = viewModel::cycleRepeatMode),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                        contentDescription = when (state.repeatMode) {
                                            RepeatMode.ONE -> "Repeat one"
                                            RepeatMode.ALL -> "Repeat all"
                                            else -> "Repeat off"
                                        },
                                        tint = if (state.repeatMode != RepeatMode.OFF) Color.White else Color.White.copy(alpha = 0.48f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 5.dp)
                                            .size(if (state.repeatMode != RepeatMode.OFF) 6.dp else 4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (state.repeatMode != RepeatMode.OFF) Color.White
                                                else Color.White.copy(alpha = 0.26f)
                                            )
                                    )
                                }
'''
assert old_repeat in ns, "Glass Repeat control block not found"
ns = ns.replace(old_repeat, new_repeat, 1)
np.write_text(ns)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Made Glass Shuffle and Repeat states clearly visible: active controls now glow cyan-purple with a bright rim and status dot, while inactive controls are strongly dimmed",\n'
assert needle in a, "About changelog anchor not found"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
