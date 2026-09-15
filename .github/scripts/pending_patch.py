from pathlib import Path

np = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = np.read_text()
old_shape = '''                                GenericShape { size, _ ->
                                    val w = size.width
                                    val h = size.height
                                    moveTo(w * 0.50f, 0f)
                                    cubicTo(w * 0.61f, 0f, w * 0.65f, h * 0.14f, w * 0.69f, h * 0.28f)
                                    cubicTo(w * 0.73f, h * 0.40f, w * 0.79f, h * 0.40f, w * 0.84f, h * 0.35f)
                                    cubicTo(w * 0.92f, h * 0.28f, w * 0.99f, h * 0.40f, w, h * 0.52f)
                                    cubicTo(w * 1.01f, h * 0.65f, w * 0.94f, h * 0.73f, w * 0.86f, h * 0.70f)
                                    cubicTo(w * 0.77f, h * 0.67f, w * 0.72f, h * 0.72f, w * 0.67f, h * 0.83f)
                                    cubicTo(w * 0.62f, h * 0.95f, w * 0.58f, h, w * 0.50f, h)
                                    cubicTo(w * 0.42f, h, w * 0.38f, h * 0.95f, w * 0.33f, h * 0.83f)
                                    cubicTo(w * 0.28f, h * 0.72f, w * 0.23f, h * 0.67f, w * 0.14f, h * 0.70f)
                                    cubicTo(w * 0.06f, h * 0.73f, -w * 0.01f, h * 0.65f, 0f, h * 0.52f)
                                    cubicTo(w * 0.01f, h * 0.40f, w * 0.08f, h * 0.28f, w * 0.16f, h * 0.35f)
                                    cubicTo(w * 0.21f, h * 0.40f, w * 0.27f, h * 0.40f, w * 0.31f, h * 0.28f)
                                    cubicTo(w * 0.35f, h * 0.14f, w * 0.39f, 0f, w * 0.50f, 0f)
                                    close()
                                }'''
new_shape = '''                                GenericShape { size, _ ->
                                    val w = size.width
                                    val h = size.height
                                    // Tighter reference-style liquid outline: a large circular crown around
                                    // Play/Pause, narrow necks, rounded side pods, then a smooth lower bowl.
                                    moveTo(w * 0.50f, 0f)
                                    cubicTo(w * 0.60f, 0f, w * 0.64f, h * 0.08f, w * 0.67f, h * 0.21f)
                                    cubicTo(w * 0.70f, h * 0.32f, w * 0.75f, h * 0.35f, w * 0.81f, h * 0.31f)
                                    cubicTo(w * 0.90f, h * 0.25f, w * 0.98f, h * 0.32f, w, h * 0.46f)
                                    cubicTo(w * 1.01f, h * 0.59f, w * 0.94f, h * 0.68f, w * 0.84f, h * 0.65f)
                                    cubicTo(w * 0.76f, h * 0.62f, w * 0.71f, h * 0.68f, w * 0.67f, h * 0.79f)
                                    cubicTo(w * 0.63f, h * 0.92f, w * 0.58f, h, w * 0.50f, h)
                                    cubicTo(w * 0.42f, h, w * 0.37f, h * 0.92f, w * 0.33f, h * 0.79f)
                                    cubicTo(w * 0.29f, h * 0.68f, w * 0.24f, h * 0.62f, w * 0.16f, h * 0.65f)
                                    cubicTo(w * 0.06f, h * 0.68f, -w * 0.01f, h * 0.59f, 0f, h * 0.46f)
                                    cubicTo(w * 0.02f, h * 0.32f, w * 0.10f, h * 0.25f, w * 0.19f, h * 0.31f)
                                    cubicTo(w * 0.25f, h * 0.35f, w * 0.30f, h * 0.32f, w * 0.33f, h * 0.21f)
                                    cubicTo(w * 0.36f, h * 0.08f, w * 0.40f, 0f, w * 0.50f, 0f)
                                    close()
                                }'''
assert old_shape in s, 'transport shape block not found'
s = s.replace(old_shape, new_shape, 1)
old_border = '.border(1.6.dp, Color.White.copy(alpha = 0.42f), transportShape)'
new_border = '.border(2.2.dp, Color.White.copy(alpha = 0.56f), transportShape)'
assert old_border in s, 'transport outer border not found'
s = s.replace(old_border, new_border, 1)
np.write_text(s)

about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a = about.read_text()
needle = 'private val latestUpdates = listOf(\n'
entry = '    "Refined the Glass transport outer contour and luminous rim to more closely match the supplied liquid-glass reference",\n'
assert needle in a
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
