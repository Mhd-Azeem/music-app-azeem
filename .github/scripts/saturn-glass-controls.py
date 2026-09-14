from pathlib import Path

p = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = p.read_text()

# Deeper, longer reference-like curved timeline.
s = s.replace('.height(94.dp)', '.height(112.dp)', 1)
s = s.replace('val stroke = 2.6.dp.toPx()', 'val stroke = 3.0.dp.toPx()', 1)
s = s.replace('val arcLeft = 16.dp.toPx()', 'val arcLeft = 6.dp.toPx()', 1)
s = s.replace('val arcRight = size.width - 16.dp.toPx()', 'val arcRight = size.width - 6.dp.toPx()', 1)
s = s.replace('val arcHeight = 104.dp.toPx()', 'val arcHeight = 138.dp.toPx()', 1)
s = s.replace('val top = 12.dp.toPx()', 'val top = 8.dp.toPx()', 1)
s = s.replace('val startAngle = 202f', 'val startAngle = 198f', 1)
s = s.replace('val sweepAngle = 136f', 'val sweepAngle = 144f', 1)
s = s.replace('Color.White.copy(alpha = 0.34f)', 'Color.White.copy(alpha = 0.28f)', 1)
s = s.replace('radius = 8.dp.toPx()', 'radius = 10.dp.toPx()', 1)
s = s.replace('radius = 4.5.dp.toPx()', 'radius = 5.dp.toPx()', 1)

# Slightly lower, tighter satellites around center.
s = s.replace('.padding(top = 6.dp, start = 46.dp, end = 46.dp)', '.padding(top = 2.dp, start = 54.dp, end = 54.dp)', 1)
s = s.replace('.size(48.dp)', '.size(50.dp)', 2)
s = s.replace('Color.White.copy(alpha = 0.30f), CircleShape', 'Color.White.copy(alpha = 0.36f), CircleShape', 2)

# Make side lobes overlap deeper into Saturn ring.
s = s.replace('Box(modifier = Modifier.size(18.dp))', 'Box(modifier = Modifier.size(0.dp))', 1)
s = s.replace('.height(74.dp),', '.height(78.dp),', 1)
s = s.replace('.height(74.dp)\n                                .clip(', '.height(78.dp)\n                                .clip(', 2)
s = s.replace('.background(Color.White.copy(alpha = 0.16f))', '.background(Color.White.copy(alpha = 0.14f))', 2)
s = s.replace('Color.White.copy(alpha = 0.34f),', 'Color.White.copy(alpha = 0.42f),', 2)

# Replace single center halo with layered Saturn-orbit rings.
old = '''                    Box(\n                        modifier = Modifier\n                            .size(120.dp)\n                            .clip(CircleShape)\n                            .background(Color.White.copy(alpha = 0.13f))\n                            .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)\n                    )\n                    Box(\n                        modifier = Modifier\n                            .size(88.dp)\n                            .clip(CircleShape)\n                            .background(Color.White.copy(alpha = 0.96f))\n                            .border(6.dp, Color.White.copy(alpha = 0.23f), CircleShape)'''
new = '''                    Box(\n                        modifier = Modifier\n                            .size(136.dp)\n                            .clip(CircleShape)\n                            .background(Color.White.copy(alpha = 0.055f))\n                            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)\n                    )\n                    Box(\n                        modifier = Modifier\n                            .size(122.dp)\n                            .clip(CircleShape)\n                            .background(Color.White.copy(alpha = 0.09f))\n                            .border(2.dp, Color.White.copy(alpha = 0.42f), CircleShape)\n                    )\n                    Box(\n                        modifier = Modifier\n                            .size(104.dp)\n                            .clip(CircleShape)\n                            .background(Color.White.copy(alpha = 0.12f))\n                            .border(1.dp, Color.White.copy(alpha = 0.34f), CircleShape)\n                    )\n                    Box(\n                        modifier = Modifier\n                            .size(88.dp)\n                            .clip(CircleShape)\n                            .background(Color.White.copy(alpha = 0.98f))\n                            .border(5.dp, Color.White.copy(alpha = 0.34f), CircleShape)'''
if old not in s:
    raise SystemExit('center halo block not found')
s = s.replace(old, new, 1)

p.write_text(s)

about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a = about.read_text()
marker = 'private val latestUpdates = listOf(\n'
entry = '    "Glass playback controls now use Saturn-style orbital rings and a deeper curved song timeline",\n'
if entry.strip() not in a:
    a = a.replace(marker, marker + entry, 1)
about.write_text(a)
