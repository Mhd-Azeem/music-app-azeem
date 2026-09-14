from pathlib import Path
p=Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s=p.read_text()
repls=[
('modifier = Modifier.fillMaxWidth().height(184.dp),','modifier = Modifier.fillMaxWidth().height(196.dp),'),
('horizontalArrangement = Arrangement.spacedBy((-7).dp),','horizontalArrangement = Arrangement.spacedBy((-22).dp),'),
('.width(68.dp)\n                                        .height(150.dp)','.width(82.dp)\n                                        .height(158.dp)'),
('0 -> 10.dp\n                                    1 -> 16.dp\n                                    2 -> 28.dp\n                                    3 -> 45.dp\n                                    else -> 64.dp','0 -> 0.dp\n                                    1 -> 5.dp\n                                    2 -> 14.dp\n                                    3 -> 28.dp\n                                    else -> 46.dp'),
('modifier = Modifier.fillMaxSize().padding(top = 52.dp, start = 28.dp, end = 28.dp, bottom = 20.dp)','modifier = Modifier.fillMaxSize().padding(top = 40.dp, start = 0.dp, end = 0.dp, bottom = 0.dp)'),
('.fillMaxWidth()\n                                .height(92.dp)','.fillMaxWidth()\n                                .padding(horizontal = 0.dp)\n                                .height(92.dp)'),
]
for old,new in repls:
    if old not in s: raise SystemExit('missing pattern: '+old[:80])
    s=s.replace(old,new,1)
# Make separator crown meet the queue cards more aggressively at the top edge.
old='''moveTo(0f, size.height * 0.17f)
                        cubicTo(size.width * 0.18f, size.height * 0.01f, size.width * 0.34f, 0f, size.width * 0.50f, 0f)
                        cubicTo(size.width * 0.66f, 0f, size.width * 0.82f, size.height * 0.01f, size.width, size.height * 0.17f)'''
new='''moveTo(0f, size.height * 0.12f)
                        cubicTo(size.width * 0.16f, 0f, size.width * 0.34f, 0f, size.width * 0.50f, 0f)
                        cubicTo(size.width * 0.66f, 0f, size.width * 0.84f, 0f, size.width, size.height * 0.12f)'''
if old not in s: raise SystemExit('missing crown')
s=s.replace(old,new,1)
p.write_text(s)

q=Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a=q.read_text(); needle='private val latestUpdates = listOf(\n'
a=a.replace(needle, needle+'    "Made the Glass player zero-gap layout visibly edge-to-edge, with tightly overlapping Up Next cards touching the separator crown",\n',1)
q.write_text(a)
