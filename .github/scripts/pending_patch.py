from pathlib import Path
p = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
s = p.read_text()
needle = 'private val latestUpdates = listOf(\n'
entry = '    "Fine-polished the Glass transport edges with smoother symmetric shoulders, cleaner side pods, and a tighter Saturn-style contour",\n'
assert needle in s
if entry not in s:
    s = s.replace(needle, needle + entry, 1)
p.write_text(s)
