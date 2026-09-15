from pathlib import Path

now = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = now.read_text()
old = '''                Spacer(modifier = Modifier.height(22.dp))\n\n                val progressFraction = if (state.durationMs > 0L) {'''
new = '''                val progressFraction = if (state.durationMs > 0L) {'''
assert old in s, 'Expected Glass separator spacer not found'
s = s.replace(old, new, 1)
now.write_text(s)

about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a = about.read_text()
needle = 'private val latestUpdates = listOf(\n'
entry = '    "Removed the remaining invisible gap between the Glass Up Next fan and the frosted player curve",\n'
assert needle in a
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
