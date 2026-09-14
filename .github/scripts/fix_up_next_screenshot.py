from pathlib import Path
p=Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s=p.read_text()
s=s.replace('.height(118.dp),\n                            contentPadding = PaddingValues(horizontal = 96.dp),\n                            horizontalArrangement = Arrangement.spacedBy(14.dp),', '.height(92.dp),\n                            contentPadding = PaddingValues(horizontal = 54.dp),\n                            horizontalArrangement = Arrangement.spacedBy(4.dp),',1)
s=s.replace('''                                val curveStep = index % 7
                                val arcOffset = when (curveStep) {
                                    0, 6 -> 18.dp
                                    1, 5 -> 10.dp
                                    2, 4 -> 4.dp
                                    else -> 0.dp
                                }''','''                                val curveStep = index % 7
                                val arcOffset = when (curveStep) {
                                    0, 6 -> 12.dp
                                    1, 5 -> 7.dp
                                    2, 4 -> 3.dp
                                    else -> 0.dp
                                }''',1)
s=s.replace('.width(62.dp)\n                                        .height(100.dp)', '.width(46.dp)\n                                        .height(82.dp)',1)
s=s.replace('.padding(vertical = 5.dp),', '.padding(vertical = 2.dp),',1)
s=s.replace('text = "%02d   ${entry.track.name}",','text = entry.track.name,',1)
s=s.replace('.width(94.dp)\n                                            .graphicsLayer { rotationZ = -90f },', '.width(78.dp)\n                                            .graphicsLayer { rotationZ = -90f },',1)
p.write_text(s)

a=Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
t=a.read_text()
needle='private val latestUpdates = listOf(\n'
entry='    "Fixed Glass Up Next screenshot issue: removed numbering/encoded-looking prefixes, tightened spacing, and softened the horizontal arc",\n'
if entry not in t: t=t.replace(needle,needle+entry,1)
a.write_text(t)
