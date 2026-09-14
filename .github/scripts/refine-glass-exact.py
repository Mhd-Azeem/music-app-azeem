from pathlib import Path

p = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = p.read_text()

# Make album art fully own the glass background; no app wallpaper bleed-through.
s = s.replace('''                    .graphicsLayer {\n                        scaleX = 1.03f\n                        scaleY = 1.03f\n                    }\n                    .blur(14.dp)\n                    .alpha(0.86f),''', '''                    .graphicsLayer {\n                        scaleX = 1.00f\n                        scaleY = 1.00f\n                    }\n                    .blur(5.dp)\n                    .alpha(1.00f),''')
s = s.replace('MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)', 'MaterialTheme.colorScheme.primary.copy(alpha = 0.035f)')
s = s.replace('Color.White.copy(alpha = 0.14f)', 'Color.White.copy(alpha = 0.055f)', 1)
s = s.replace('glassmorphismNowPlaying -> Color.White.copy(alpha = 0.16f)', 'glassmorphismNowPlaying -> Color.White.copy(alpha = 0.075f)')
s = s.replace('Color.White.copy(alpha = 0.38f) else Color.Transparent', 'Color.White.copy(alpha = 0.28f) else Color.Transparent', 1)

# Hide ordinary title/artist/actions completely in glass mode; the reference only keeps the Music header.
old = '''            Column(\n                modifier = Modifier\n                    .fillMaxWidth()\n                    .padding(top = if (glassmorphismNowPlaying) 10.dp else 32.dp)\n            ) {\n                Row(verticalAlignment = Alignment.CenterVertically) {'''
new = '''            if (!glassmorphismNowPlaying) {\n                Column(\n                    modifier = Modifier\n                        .fillMaxWidth()\n                        .padding(top = 32.dp)\n                ) {\n                    Row(verticalAlignment = Alignment.CenterVertically) {'''
if old not in s:
    raise SystemExit('title block start not found')
s = s.replace(old, new, 1)

# Close the new guard after the existing title/actions section.
needle = '''                    }\n                }\n            }\n\n            if (glassmorphismNowPlaying) {\n                // Compact reference-style arched seek timeline.'''
replacement = '''                    }\n                }\n            }\n            }\n\n            if (glassmorphismNowPlaying) {\n                Box(modifier = Modifier.weight(1f))\n            }\n\n            if (glassmorphismNowPlaying) {\n                // Compact reference-style arched seek timeline.'''
if needle not in s:
    raise SystemExit('title block end not found')
s = s.replace(needle, replacement, 1)

# Bring the arc closer to the reference proportions.
s = s.replace('.padding(top = 12.dp),', '.padding(top = 2.dp),', 1)
s = s.replace('.height(86.dp)', '.height(94.dp)', 1)
s = s.replace('val arcHeight = 90.dp.toPx()', 'val arcHeight = 104.dp.toPx()', 1)
s = s.replace('val top = 18.dp.toPx()', 'val top = 12.dp.toPx()', 1)
s = s.replace('val startAngle = 205f', 'val startAngle = 202f', 1)
s = s.replace('val sweepAngle = 130f', 'val sweepAngle = 136f', 1)

# Tighten shuffle/repeat around the connected transport.
s = s.replace('.padding(top = 10.dp, start = 50.dp, end = 50.dp)', '.padding(top = 6.dp, start = 46.dp, end = 46.dp)', 1)

# Make side lobes flow underneath the center ring instead of leaving a visible gap.
s = s.replace('Box(modifier = Modifier.size(94.dp))', 'Box(modifier = Modifier.size(18.dp))', 1)
s = s.replace('.height(126.dp)', '.height(132.dp)', 1)
s = s.replace('.padding(horizontal = 26.dp)', '.padding(horizontal = 24.dp)', 1)
s = s.replace('.height(70.dp),', '.height(74.dp),', 1)
s = s.replace('.height(70.dp)\n                                .clip(', '.height(74.dp)\n                                .clip(', 2)
s = s.replace('topEnd = 22.dp,\n                                        bottomEnd = 22.dp,', 'topEnd = 38.dp,\n                                        bottomEnd = 38.dp,', 2)
s = s.replace('topStart = 22.dp,\n                                        topEnd = 36.dp,\n                                        bottomEnd = 36.dp,\n                                        bottomStart = 22.dp', 'topStart = 38.dp,\n                                        topEnd = 36.dp,\n                                        bottomEnd = 36.dp,\n                                        bottomStart = 38.dp', 2)
s = s.replace('.size(114.dp)', '.size(120.dp)', 1)
s = s.replace('.size(91.dp)', '.size(88.dp)', 1)
s = s.replace('.border(7.dp, Color.White.copy(alpha = 0.20f), CircleShape)', '.border(6.dp, Color.White.copy(alpha = 0.23f), CircleShape)', 1)

p.write_text(s)

about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a = about.read_text()
marker = 'private val latestUpdates = listOf(\n'
entry = '    "Glass Now Playing refined again for full-screen artwork, lower reference layout, and truly connected liquid transport controls",\n'
if entry.strip() not in a:
    a = a.replace(marker, marker + entry, 1)
about.write_text(a)
