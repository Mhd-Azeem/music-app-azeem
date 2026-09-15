from pathlib import Path
p=Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s=p.read_text()
# More visible spacing between Up Next cards.
s=s.replace('horizontalArrangement = Arrangement.spacedBy(8.dp)', 'horizontalArrangement = Arrangement.spacedBy(16.dp)', 1)
# Replace yellow cards with frosted glass styling.
s=s.replace('.background(Color(0xFFFFEA28))', '.background(Color(0xFFD7ECFF).copy(alpha = 0.62f))', 1)
s=s.replace('color = Color(0xFF111111),', 'color = Color(0xFF08283A),', 1)
# The panel border itself is the separator: remove the extra vertical spacer.
s=s.replace('\n                Spacer(modifier = Modifier.height(18.dp))\n', '\n', 1)
# Match the light-blue panel crown more closely to the timeline circular arc.
old='''                        moveTo(0f, size.height * 0.12f)\n                        cubicTo(size.width * 0.16f, 0f, size.width * 0.34f, 0f, size.width * 0.50f, 0f)\n                        cubicTo(size.width * 0.66f, 0f, size.width * 0.84f, 0f, size.width, size.height * 0.12f)'''
new='''                        moveTo(0f, size.height * 0.12f)\n                        cubicTo(size.width * 0.20f, size.height * 0.015f, size.width * 0.36f, 0f, size.width * 0.50f, 0f)\n                        cubicTo(size.width * 0.64f, 0f, size.width * 0.80f, size.height * 0.015f, size.width, size.height * 0.12f)'''
assert old in s, 'lower glass crown geometry not found'
s=s.replace(old,new,1)
p.write_text(s)

about=Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a=about.read_text()
needle='private val latestUpdates = listOf(\n'
entry='    "Matched the Glass player border curve to the timeline, removed the extra separator gap, and restyled Up Next as spaced frosted-glass cards",\n'
assert needle in a
if entry not in a:
    a=a.replace(needle,needle+entry,1)
about.write_text(a)
