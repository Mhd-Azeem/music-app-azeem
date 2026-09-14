from pathlib import Path

p = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = p.read_text()

old = '''                                Column(
                                    modifier = Modifier
                                        .width(92.dp)
                                        .offset(y = arcOffset)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                                        .clickable { viewModel.playQueueItem(state.currentIndex + 1 + index) }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "%02d".format(index + 1),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.70f)
                                    )
                                    Text(
                                        text = entry.track.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.92f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }'''
new = '''                                Box(
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
if old not in s:
    raise SystemExit('up next block not found')
s = s.replace(old, new, 1)

# Make the arc visibly deeper/more bowed while preserving the seek interaction.
s = s.replace('val arcHeight = 214.dp.toPx()', 'val arcHeight = 270.dp.toPx()', 1)
s = s.replace('val top = 10.dp.toPx()', 'val top = -10.dp.toPx()', 1)
s = s.replace('val startAngle = 202f\n                            val sweepAngle = 136f', 'val startAngle = 206f\n                            val sweepAngle = 128f', 1)

# Refine the Saturn silhouette: narrower side pods and a larger smooth circular waist around play/pause.
start = s.index('                val saturnTransportShape = remember {')
end = s.index('                Box(\n                    modifier = Modifier\n                        .fillMaxWidth()\n                        .height(132.dp)', start)
shape = '''                val saturnTransportShape = remember {
                    GenericShape { size, _ ->
                        val w = size.width
                        val h = size.height
                        moveTo(0f, h * 0.50f)
                        cubicTo(0f, h * 0.36f, w * 0.035f, h * 0.31f, w * 0.095f, h * 0.31f)
                        lineTo(w * 0.29f, h * 0.31f)
                        cubicTo(w * 0.345f, h * 0.31f, w * 0.355f, h * 0.10f, w * 0.425f, h * 0.035f)
                        cubicTo(w * 0.465f, -0.005f, w * 0.535f, -0.005f, w * 0.575f, h * 0.035f)
                        cubicTo(w * 0.645f, h * 0.10f, w * 0.655f, h * 0.31f, w * 0.71f, h * 0.31f)
                        lineTo(w * 0.905f, h * 0.31f)
                        cubicTo(w * 0.965f, h * 0.31f, w, h * 0.36f, w, h * 0.50f)
                        cubicTo(w, h * 0.64f, w * 0.965f, h * 0.69f, w * 0.905f, h * 0.69f)
                        lineTo(w * 0.71f, h * 0.69f)
                        cubicTo(w * 0.655f, h * 0.69f, w * 0.645f, h * 0.90f, w * 0.575f, h * 0.965f)
                        cubicTo(w * 0.535f, h * 1.005f, w * 0.465f, h * 1.005f, w * 0.425f, h * 0.965f)
                        cubicTo(w * 0.355f, h * 0.90f, w * 0.345f, h * 0.69f, w * 0.29f, h * 0.69f)
                        lineTo(w * 0.095f, h * 0.69f)
                        cubicTo(w * 0.035f, h * 0.69f, 0f, h * 0.64f, 0f, h * 0.50f)
                        close()
                    }
                }
'''
s = s[:start] + shape + s[end:]

p.write_text(s)

# Full-app glass: don't paint the user's wallpaper over the liquid background.
nav = Path('app/src/main/java/com/wavelength/music/ui/navigation/NavGraph.kt')
n = nav.read_text()
n = n.replace('import androidx.compose.foundation.layout.Box\n', 'import androidx.compose.foundation.background\nimport androidx.compose.foundation.layout.Box\n')
n = n.replace('import androidx.compose.ui.graphics.Color\n', 'import androidx.compose.ui.graphics.Brush\nimport androidx.compose.ui.graphics.Color\n')
old_bg = '''            AppBackground(
                hasCustomBackground = settings.hasCustomBackground,
                customBackgroundFile = settingsViewModel.customBackgroundFile,
                opacity = settings.backgroundOpacity,
                builtInWallpaper = settings.builtInWallpaper
            )'''
new_bg = '''            if (settings.glassmorphismNowPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF071A2B),
                                    Color(settings.customAccentArgb).copy(alpha = 0.42f),
                                    Color(0xFF10243D),
                                    Color(0xFF241B3A)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.13f),
                                    Color.Transparent
                                ),
                                radius = 900f
                            )
                        )
                )
            } else {
                AppBackground(
                    hasCustomBackground = settings.hasCustomBackground,
                    customBackgroundFile = settingsViewModel.customBackgroundFile,
                    opacity = settings.backgroundOpacity,
                    builtInWallpaper = settings.builtInWallpaper
                )
            }'''
if old_bg not in n:
    raise SystemExit('NavGraph AppBackground block not found')
n = n.replace(old_bg, new_bg, 1)
nav.write_text(n)

# Prevent pale/white cards in full glass mode; use dark translucent glass surfaces instead.
theme = Path('app/src/main/java/com/wavelength/music/ui/theme/Theme.kt')
t = theme.read_text()
t = t.replace('surface = Color.White.copy(alpha = 0.10f),', 'surface = Color(0xFF102238).copy(alpha = 0.72f),', 1)
t = t.replace('surfaceVariant = Color.White.copy(alpha = 0.16f),', 'surfaceVariant = Color(0xFF17324D).copy(alpha = 0.66f),', 1)
t = t.replace('surfaceContainer = Color.White.copy(alpha = 0.09f),', 'surfaceContainer = Color(0xFF102A42).copy(alpha = 0.64f),', 1)
t = t.replace('surfaceContainerLow = Color.White.copy(alpha = 0.07f),', 'surfaceContainerLow = Color(0xFF0C2035).copy(alpha = 0.58f),', 1)
t = t.replace('surfaceContainerHigh = Color.White.copy(alpha = 0.14f),', 'surfaceContainerHigh = Color(0xFF1B3853).copy(alpha = 0.70f),', 1)
theme.write_text(t)

about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a = about.read_text()
needle = 'private val latestUpdates = listOf(\n'
entry = '    "Refined full-app Glassmorphism with a liquid gradient background, darker readable glass cards, vertical scrollable Up Next labels, deeper seek arc, and a smoother Saturn transport outline",\n'
if entry.strip() not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
