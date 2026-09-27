from pathlib import Path

p = Path("app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt")
s = p.read_text()

old = '''                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (glassUpcoming.isNotEmpty()) {
                        LazyRow(
'''
new = '''                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (glassUpcoming.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 14.dp, top = 4.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.26f),
                                    RoundedCornerShape(22.dp)
                                )
                                .clickable { viewModel.smartShuffle() }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Shuffle Up Next",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Shuffle",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    if (glassUpcoming.isNotEmpty()) {
                        LazyRow(
'''
assert old in s, "Glass Up Next container not found"
s = s.replace(old, new, 1)
p.write_text(s)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Added a visible Shuffle pill to the Glassmorphism Up Next area so the upcoming queue can be reshuffled directly from the Glass player",\n'
assert needle in a, "About changelog anchor not found"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
