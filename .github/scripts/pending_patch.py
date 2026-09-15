from pathlib import Path

p = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = p.read_text()

# Remove the old white decorative separator line drawn across the Glass background.
old_line = '''            Canvas(modifier = Modifier.fillMaxSize()) {\n                val y = size.height * 0.585f\n                val inset = 18.dp.toPx()\n                val path = Path().apply {\n                    moveTo(inset, y + 8.dp.toPx())\n                    quadraticBezierTo(size.width / 2f, y - 22.dp.toPx(), size.width - inset, y + 8.dp.toPx())\n                }\n                drawPath(\n                    path = path,\n                    color = Color.White.copy(alpha = 0.58f),\n                    style = Stroke(width = 1.35.dp.toPx())\n                )\n            }\n'''
assert old_line in s, 'glass decorative separator line not found'
s = s.replace(old_line, '', 1)

# Make Up Next cards a bit longer and give the row enough height for the circular motion.
s = s.replace('modifier = Modifier.fillMaxWidth().height(196.dp)', 'modifier = Modifier.fillMaxWidth().height(214.dp)', 1)
s = s.replace('.height(158.dp)', '.height(176.dp)', 1)
s = s.replace('.height(54.dp)', '.height(60.dp)', 1)
s = s.replace('modifier = Modifier.fillMaxWidth().height(78.dp)', 'modifier = Modifier.fillMaxWidth().height(94.dp)', 1)

# Add a clear gap so Up Next cards never touch the light-blue player panel.
needle = '''                    }\n                }\n\n\n                val progressFraction = if (state.durationMs > 0L) {'''
replacement = '''                    }\n                }\n\n                Spacer(modifier = Modifier.height(22.dp))\n\n                val progressFraction = if (state.durationMs > 0L) {'''
assert needle in s, 'Up Next to player boundary not found'
s = s.replace(needle, replacement, 1)

p.write_text(s)

about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a = about.read_text()
anchor = 'private val latestUpdates = listOf(\n'
entry = '    "Removed the white Glass separator line, lengthened Up Next cards, and added a clear gap above the player panel",\n'
assert anchor in a
if entry not in a:
    a = a.replace(anchor, anchor + entry, 1)
about.write_text(a)
