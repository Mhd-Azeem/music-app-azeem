from pathlib import Path

now = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = now.read_text()
old = '''                                .height(92.dp)
                                .pointerInput(state.durationMs) {
                                    detectTapGestures { offset ->
                                        if (state.durationMs > 0L) {
                                            val p = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                            viewModel.seekTo((state.durationMs * p).toLong())
                                        }
                                    }
                                }
'''
new = '''                                .height(92.dp)
                                // Keep tap-to-seek, and also let the thumb follow a finger dragged
                                // continuously across the curved Glass timeline.
                                .pointerInput(state.durationMs) {
                                    detectTapGestures { offset ->
                                        if (state.durationMs > 0L) {
                                            val p = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                            viewModel.seekTo((state.durationMs * p).toLong())
                                        }
                                    }
                                }
                                .pointerInput(state.durationMs) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            if (state.durationMs > 0L) {
                                                val p = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                                viewModel.seekTo((state.durationMs * p).toLong())
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            if (state.durationMs > 0L) {
                                                change.consume()
                                                val p = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                                viewModel.seekTo((state.durationMs * p).toLong())
                                            }
                                        }
                                    )
                                }
'''
assert old in s, 'Glass timeline gesture block not found'
s = s.replace(old, new, 1)
now.write_text(s)

about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a = about.read_text()
needle = 'private val latestUpdates = listOf(\n'
entry = '    "Glass timeline now supports continuous swipe/drag seeking as well as tap-to-seek",\n'
assert needle in a, 'About latestUpdates list not found'
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
