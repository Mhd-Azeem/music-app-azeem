from pathlib import Path
p=Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s=p.read_text()
old='''                            itemsIndexed(glassUpcoming, key = { _, entry -> entry.instanceId }) { index, entry ->
                                val centerIndex = fanState.firstVisibleItemIndex + 3
                                val delta = (index - centerIndex).coerceIn(-4, 4)
                                val distance = kotlin.math.abs(delta)
                                val lift = when (distance) {
                                    0 -> 0.dp
                                    1 -> 5.dp
                                    2 -> 14.dp
                                    3 -> 28.dp
                                    else -> 46.dp
                                }
                                Box(
                                    modifier = Modifier
                                        .width(82.dp)
                                        .height(158.dp)
                                        .offset(y = lift)
                                        .graphicsLayer { rotationZ = delta * 10f }'''
new='''                            itemsIndexed(glassUpcoming, key = { _, entry -> entry.instanceId }) { index, entry ->
                                // Derive the card transform from its live pixel position in the viewport.
                                // This keeps the cards on one continuous circular arc while dragging,
                                // instead of jumping only when firstVisibleItemIndex changes.
                                val layoutInfo = fanState.layoutInfo
                                val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                                val itemCenter = visibleItem?.let { it.offset + it.size / 2f } ?: viewportCenter
                                val halfViewport = ((layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2f).coerceAtLeast(1f)
                                val x = ((itemCenter - viewportCenter) / halfViewport).coerceIn(-1f, 1f)
                                // Same shallow crown geometry as the lower timeline/player curve:
                                // center is highest; cards descend smoothly toward both screen edges.
                                val circleY = 1f - kotlin.math.sqrt((1f - x * x).coerceAtLeast(0f))
                                val lift = with(LocalDensity.current) { (circleY * 58f).dp }
                                val tangentAngle = Math.toDegrees(kotlin.math.asin(x.toDouble())).toFloat() * 0.42f
                                Box(
                                    modifier = Modifier
                                        .width(82.dp)
                                        .height(158.dp)
                                        .offset(y = lift)
                                        .graphicsLayer { rotationZ = tangentAngle }'''
assert old in s, 'old discrete fan transform not found'
s=s.replace(old,new,1)
p.write_text(s)

about=Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a=about.read_text()
needle='private val latestUpdates = listOf(\n'
entry='    "Up Next now scrolls continuously along the curved player timeline with live circular position and tangent rotation",\n'
assert needle in a
if entry not in a:
    a=a.replace(needle,needle+entry,1)
about.write_text(a)
