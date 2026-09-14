from pathlib import Path
p=Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s=p.read_text()
old='''                                val curveStep = index % 9
                                val arcOffset = when (curveStep) {
                                    0, 8 -> 18.dp
                                    1, 7 -> 12.dp
                                    2, 6 -> 7.dp
                                    3, 5 -> 3.dp
                                    else -> 0.dp
                                }
                                val spokeRotation = when (curveStep) {
                                    0 -> -30f
                                    1 -> -22f
                                    2 -> -14f
                                    3 -> -7f
                                    4 -> 0f
                                    5 -> 7f
                                    6 -> 14f
                                    7 -> 22f
                                    else -> 30f
                                }'''
new='''                                val curveStep = index % 9
                                // Reference fan: the centre upcoming song is lowest/straight,
                                // while songs toward either side climb and rotate outward.
                                val arcOffset = when (curveStep) {
                                    0, 8 -> 0.dp
                                    1, 7 -> 8.dp
                                    2, 6 -> 15.dp
                                    3, 5 -> 21.dp
                                    else -> 24.dp
                                }
                                val spokeRotation = when (curveStep) {
                                    0 -> -38f
                                    1 -> -29f
                                    2 -> -20f
                                    3 -> -10f
                                    4 -> 0f
                                    5 -> 10f
                                    6 -> 20f
                                    7 -> 29f
                                    else -> 38f
                                }'''
assert old in s, 'queue fan block not found'
s=s.replace(old,new,1)
# Make fan area taller so rotated edge labels are not clipped.
s=s.replace('.height(92.dp),\n                            contentPadding = PaddingValues(horizontal = 54.dp)', '.height(116.dp),\n                            contentPadding = PaddingValues(horizontal = 38.dp)',1)
s=s.replace('horizontalArrangement = Arrangement.spacedBy(4.dp)', 'horizontalArrangement = Arrangement.spacedBy(1.dp)',1)
s=s.replace('.width(46.dp)\n                                        .height(82.dp)', '.width(50.dp)\n                                        .height(104.dp)',1)
s=s.replace('.width(78.dp)\n                                            .graphicsLayer', '.width(88.dp)\n                                            .graphicsLayer',1)
# Match reference transport proportions: compact side lobes and dominant round centre.
old2='''.fillMaxWidth()
                        .height(132.dp)
                        .padding(horizontal = 18.dp),'''
new2='''.fillMaxWidth()
                        .height(154.dp)
                        .padding(horizontal = 42.dp),'''
assert old2 in s, 'transport outer block not found'
s=s.replace(old2,new2,1)
s=s.replace('.fillMaxWidth()\n                            .height(116.dp)\n                            .clip(saturnTransportShape)', '.fillMaxWidth()\n                            .height(126.dp)\n                            .clip(saturnTransportShape)',1)
s=s.replace('.background(Color.White.copy(alpha = 0.13f))\n                            .border(1.6.dp, Color.White.copy(alpha = 0.68f)', '.background(Color.White.copy(alpha = 0.10f))\n                            .border(1.35.dp, Color.White.copy(alpha = 0.58f)',1)
s=s.replace('Box(modifier = Modifier.width(112.dp).fillMaxHeight())', 'Box(modifier = Modifier.width(126.dp).fillMaxHeight())',1)
s=s.replace('.size(108.dp)\n                            .clip(CircleShape)', '.size(126.dp)\n                            .clip(CircleShape)',1)
s=s.replace('.size(88.dp)\n                            .clip(CircleShape)', '.size(94.dp)\n                            .clip(CircleShape)',1)
s=s.replace('modifier = Modifier.size(42.dp)', 'modifier = Modifier.size(44.dp)',1)
p.write_text(s)

about=Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a=about.read_text()
needle='private val latestUpdates = listOf(\n'
entry='    "Refined Glass Now Playing to match the supplied reference: true circular-fan Up Next geometry and corrected compact Saturn transport proportions",\n'
assert needle in a
a=a.replace(needle,needle+entry,1)
about.write_text(a)
