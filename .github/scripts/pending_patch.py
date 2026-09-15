from pathlib import Path

p = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = p.read_text()

old_shape = '''                                    // Tighter reference-style liquid outline: a large circular crown around
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
'''
new_shape = '''                                    // Reference contour: tighter center crown, pronounced inward shoulders,
                                    // compact round side lobes and a restrained lower bowl around Play/Pause.
                                    moveTo(w * 0.50f, 0f)
                                    cubicTo(w * 0.585f, 0f, w * 0.625f, h * 0.07f, w * 0.650f, h * 0.19f)
                                    cubicTo(w * 0.670f, h * 0.285f, w * 0.705f, h * 0.345f, w * 0.755f, h * 0.345f)
                                    cubicTo(w * 0.805f, h * 0.345f, w * 0.830f, h * 0.285f, w * 0.875f, h * 0.285f)
                                    cubicTo(w * 0.955f, h * 0.285f, w, h * 0.365f, w, h * 0.485f)
                                    cubicTo(w, h * 0.605f, w * 0.955f, h * 0.685f, w * 0.875f, h * 0.685f)
                                    cubicTo(w * 0.815f, h * 0.685f, w * 0.790f, h * 0.630f, w * 0.745f, h * 0.630f)
                                    cubicTo(w * 0.695f, h * 0.630f, w * 0.670f, h * 0.705f, w * 0.645f, h * 0.815f)
                                    cubicTo(w * 0.615f, h * 0.935f, w * 0.570f, h, w * 0.50f, h)
                                    cubicTo(w * 0.430f, h, w * 0.385f, h * 0.935f, w * 0.355f, h * 0.815f)
                                    cubicTo(w * 0.330f, h * 0.705f, w * 0.305f, h * 0.630f, w * 0.255f, h * 0.630f)
                                    cubicTo(w * 0.210f, h * 0.630f, w * 0.185f, h * 0.685f, w * 0.125f, h * 0.685f)
                                    cubicTo(w * 0.045f, h * 0.685f, 0f, h * 0.605f, 0f, h * 0.485f)
                                    cubicTo(0f, h * 0.365f, w * 0.045f, h * 0.285f, w * 0.125f, h * 0.285f)
                                    cubicTo(w * 0.170f, h * 0.285f, w * 0.195f, h * 0.345f, w * 0.245f, h * 0.345f)
                                    cubicTo(w * 0.295f, h * 0.345f, w * 0.330f, h * 0.285f, w * 0.350f, h * 0.19f)
                                    cubicTo(w * 0.375f, h * 0.07f, w * 0.415f, 0f, w * 0.50f, 0f)
'''
assert old_shape in s, 'current transport shape block not found'
s = s.replace(old_shape, new_shape, 1)

s = s.replace('.width(306.dp)\n                                        .height(150.dp)', '.width(292.dp)\n                                        .height(142.dp)', 1)
s = s.replace('.border(2.2.dp, Color.White.copy(alpha = 0.56f), transportShape)', '.border(1.5.dp, Color.White.copy(alpha = 0.46f), transportShape)', 1)
s = s.replace('.padding(start = 26.dp)\n                                        .offset(y = 23.dp)\n                                        .size(72.dp)', '.padding(start = 31.dp)\n                                        .offset(y = 18.dp)\n                                        .size(68.dp)', 1)
s = s.replace('.padding(end = 26.dp)\n                                        .offset(y = 23.dp)\n                                        .size(72.dp)', '.padding(end = 31.dp)\n                                        .offset(y = 18.dp)\n                                        .size(68.dp)', 1)
s = s.replace('.padding(bottom = 5.dp)\n                                        .size(128.dp)', '.padding(bottom = 11.dp)\n                                        .size(120.dp)', 1)
s = s.replace('.size(99.dp)\n                                            .clip(CircleShape)', '.size(94.dp)\n                                            .clip(CircleShape)', 1)

p.write_text(s)

about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a = about.read_text()
needle = 'private val latestUpdates = listOf(\n'
entry = '    "Refined the Glass transport silhouette to the reference with tighter shoulders, compact side lobes, a higher smaller center disc, and a softer outer rim",\n'
assert needle in a
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
