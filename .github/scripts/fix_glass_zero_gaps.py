from pathlib import Path
p=Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s=p.read_text()
# Remove side margins and inter-card gaps; overlap slightly so rotated cards visually touch.
s=s.replace('contentPadding = PaddingValues(horizontal = 54.dp),\n                            horizontalArrangement = Arrangement.spacedBy(2.dp)', 'contentPadding = PaddingValues(horizontal = 0.dp),\n                            horizontalArrangement = Arrangement.spacedBy((-7).dp)',1)
s=s.replace('.width(62.dp)\n                                        .height(146.dp)', '.width(68.dp)\n                                        .height(150.dp)',1)
# Bring cards down into direct contact with the separator/panel crown.
s=s.replace('0 -> 0.dp\n                                    1 -> 7.dp\n                                    2 -> 20.dp\n                                    3 -> 39.dp\n                                    else -> 62.dp', '0 -> 10.dp\n                                    1 -> 16.dp\n                                    2 -> 28.dp\n                                    3 -> 45.dp\n                                    else -> 64.dp',1)
# Lower panel fills edge-to-edge and reaches the bottom; no external horizontal/bottom margin.
s=s.replace('.fillMaxWidth()\n                        .height(340.dp)\n                        .clip(lowerGlassShape)', '.fillMaxWidth()\n                        .height(356.dp)\n                        .offset(y = (-2).dp)\n                        .clip(lowerGlassShape)',1)
# Make the crown/separator rise directly behind the card bottoms instead of leaving a visible band.
old='''                        moveTo(0f, size.height * 0.23f)
                        cubicTo(size.width * 0.20f, size.height * 0.02f, size.width * 0.34f, 0f, size.width * 0.50f, 0f)
                        cubicTo(size.width * 0.66f, 0f, size.width * 0.80f, size.height * 0.02f, size.width, size.height * 0.23f)'''
new='''                        moveTo(0f, size.height * 0.17f)
                        cubicTo(size.width * 0.18f, size.height * 0.01f, size.width * 0.34f, 0f, size.width * 0.50f, 0f)
                        cubicTo(size.width * 0.66f, 0f, size.width * 0.82f, size.height * 0.01f, size.width, size.height * 0.17f)'''
assert old in s
s=s.replace(old,new,1)
p.write_text(s)

about=Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a=about.read_text()
needle='private val latestUpdates = listOf(\n'
entry='    "Removed Glass Now Playing outer margins and closed the gaps between Up Next cards and the curved separator",\n'
assert needle in a
a=a.replace(needle,needle+entry,1)
about.write_text(a)
