from pathlib import Path
p=Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s=p.read_text()
old='''.border(\n                    width = if (glassmorphismNowPlaying) 1.dp else 0.dp,\n                    color = if (glassmorphismNowPlaying) Color.White.copy(alpha = 0.28f) else Color.Transparent,\n                    shape = if (glassmorphismNowPlaying) RoundedCornerShape(34.dp) else RoundedCornerShape(0.dp)\n                )\n                .padding(if (glassmorphismNowPlaying) 20.dp else 24.dp)'''
new='''.border(\n                    width = if (glassmorphismNowPlaying) 1.dp else 0.dp,\n                    color = if (glassmorphismNowPlaying) Color.White.copy(alpha = 0.28f) else Color.Transparent,\n                    shape = if (glassmorphismNowPlaying) RoundedCornerShape(34.dp) else RoundedCornerShape(0.dp)\n                )\n                // Glass mode must remain edge-to-edge. The previous 20.dp parent padding\n                // constrained every child, so fillMaxWidth() could never reach screen edges.\n                .padding(if (glassmorphismNowPlaying) 0.dp else 24.dp)'''
assert old in s, 'outer parent padding block not found'
s=s.replace(old,new,1)
# Preserve comfortable inset for only the top controls, not the main glass panel/carousel.
old2='''                Row(\n                    modifier = Modifier.fillMaxWidth(),\n                    horizontalArrangement = Arrangement.SpaceBetween,'''
new2='''                Row(\n                    modifier = Modifier\n                        .fillMaxWidth()\n                        .padding(horizontal = if (glassmorphismNowPlaying) 20.dp else 0.dp),\n                    horizontalArrangement = Arrangement.SpaceBetween,'''
assert old2 in s, 'top row block not found'
s=s.replace(old2,new2,1)
p.write_text(s)

about=Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a=about.read_text()
needle='private val latestUpdates = listOf(\n'
entry='    "Removed the inherited Glass Now Playing parent horizontal inset so the main player panel reaches both screen edges",\n'
assert needle in a
if entry not in a:
    a=a.replace(needle,needle+entry,1)
about.write_text(a)
