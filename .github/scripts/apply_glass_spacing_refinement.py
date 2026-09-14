from pathlib import Path
p=Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s=p.read_text()
# Visible gaps between Up Next cards while preserving live circular transforms.
s=s.replace('horizontalArrangement = Arrangement.spacedBy((-22).dp)', 'horizontalArrangement = Arrangement.spacedBy(8.dp)', 1)
# Create a deliberate vertical separation between Up Next and the light-blue controls panel.
s=s.replace('''                val progressFraction = if (state.durationMs > 0L) {''','''                Spacer(modifier = Modifier.height(18.dp))\n\n                val progressFraction = if (state.durationMs > 0L) {''',1)
# Keep seek/timeline and its time labels away from physical screen edges.
s=s.replace('''.fillMaxWidth()\n                                .padding(horizontal = 0.dp)\n                                .height(92.dp)''','''.fillMaxWidth()\n                                .padding(horizontal = 18.dp)\n                                .height(92.dp)''',1)
# Keep shuffle/repeat away from edges without narrowing the central transport area.
s=s.replace('''modifier = Modifier.align(Alignment.CenterStart).size(48.dp).clip(CircleShape)''','''modifier = Modifier.align(Alignment.CenterStart).padding(start = 18.dp).size(48.dp).clip(CircleShape)''',1)
s=s.replace('''modifier = Modifier.align(Alignment.CenterEnd).size(48.dp).clip(CircleShape)''','''modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp).size(48.dp).clip(CircleShape)''',1)
p.write_text(s)

about=Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a=about.read_text()
needle='private val latestUpdates = listOf(\n'
entry='    "Refined Glass Now Playing spacing: separated Up Next cards and controls, plus safe margins for timeline, time, shuffle and repeat",\n'
assert needle in a
if entry not in a:
    a=a.replace(needle,needle+entry,1)
about.write_text(a)
