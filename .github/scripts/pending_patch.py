from pathlib import Path

now = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = now.read_text()

card_border = '''                                        .background(Color(0xFFD7ECFF).copy(alpha = 0.52f))
                                        .border(1.dp, Color.White.copy(alpha = 0.82f), RoundedCornerShape(11.dp))
                                        .clickable { viewModel.playQueueItem(state.currentIndex + 1 + index) }'''
card_no_border = '''                                        .background(Color(0xFFD7ECFF).copy(alpha = 0.52f))
                                        .clickable { viewModel.playQueueItem(state.currentIndex + 1 + index) }'''
assert card_border in s, 'Up Next card border block not found'
s = s.replace(card_border, card_no_border, 1)

panel_border = '''                        .clip(lowerGlassShape)
                        .hazeChild(state = hazeState, style = glassStyle) { inputScale = HazeInputScale.Auto }
                        .border(1.5.dp, Color.White.copy(alpha = 0.82f), lowerGlassShape)'''
panel_no_border = '''                        .clip(lowerGlassShape)
                        .hazeChild(state = hazeState, style = glassStyle) { inputScale = HazeInputScale.Auto }'''
assert panel_border in s, 'Lower Glass panel border block not found'
s = s.replace(panel_border, panel_no_border, 1)

now.write_text(s)

about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a = about.read_text()
needle = 'private val latestUpdates = listOf(\n'
entry = '    "Removed the Glass lower-player outline and Up Next card outlines for a seamless borderless blur",\n'
assert needle in a, 'About latestUpdates list not found'
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
