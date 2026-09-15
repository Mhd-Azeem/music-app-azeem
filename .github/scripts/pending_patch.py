from pathlib import Path
p = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = p.read_text()

# More visible spacing between Up Next cards while preserving the circular motion.
s = s.replace('horizontalArrangement = Arrangement.spacedBy(8.dp)', 'horizontalArrangement = Arrangement.spacedBy(22.dp)', 1)
s = s.replace('horizontalArrangement = Arrangement.spacedBy(16.dp)', 'horizontalArrangement = Arrangement.spacedBy(22.dp)', 1)

# Replace the yellow Up Next cards with the same frosted light-blue glass family as the player.
s = s.replace('.background(Color(0xFFFFEA28))', '.background(Color(0xFFD7ECFF).copy(alpha = 0.52f))', 1)
s = s.replace('.background(Color(0xFFD7ECFF).copy(alpha = 0.62f))', '.background(Color(0xFFD7ECFF).copy(alpha = 0.52f))', 1)
s = s.replace('color = Color(0xFF111111),', 'color = Color(0xFF08283A),', 1)

# The light-blue panel border itself is the separator. Remove any extra vertical gap.
s = s.replace('\n                Spacer(modifier = Modifier.height(18.dp))\n', '\n', 1)

# Make the top border a single clean symmetric arc matching the timeline's crown direction.
old1 = '''                        moveTo(0f, size.height * 0.12f)\n                        cubicTo(size.width * 0.16f, 0f, size.width * 0.34f, 0f, size.width * 0.50f, 0f)\n                        cubicTo(size.width * 0.66f, 0f, size.width * 0.84f, 0f, size.width, size.height * 0.12f)'''
old2 = '''                        moveTo(0f, size.height * 0.12f)\n                        cubicTo(size.width * 0.20f, size.height * 0.015f, size.width * 0.36f, 0f, size.width * 0.50f, 0f)\n                        cubicTo(size.width * 0.64f, 0f, size.width * 0.80f, size.height * 0.015f, size.width, size.height * 0.12f)'''
new = '''                        moveTo(0f, size.height * 0.12f)\n                        quadraticBezierTo(\n                            size.width * 0.50f,\n                            -size.height * 0.12f,\n                            size.width,\n                            size.height * 0.12f\n                        )'''
if old1 in s:
    s = s.replace(old1, new, 1)
elif old2 in s:
    s = s.replace(old2, new, 1)
else:
    assert new in s, 'lower glass curve geometry not found'

assert 'Arrangement.spacedBy(22.dp)' in s
assert 'Color(0xFFD7ECFF).copy(alpha = 0.52f)' in s
assert 'Spacer(modifier = Modifier.height(18.dp))' not in s
assert 'quadraticBezierTo(' in s
p.write_text(s)

about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a = about.read_text()
needle = 'private val latestUpdates = listOf(\n'
entry = '    "Removed the extra Up Next/player separation, matched the Glass panel crown to the timeline arc, switched Up Next to frosted glass, and increased card spacing",\n'
assert needle in a
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
