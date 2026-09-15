from pathlib import Path

path = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = path.read_text(encoding='utf-8')


def replace_once(old: str, new: str) -> None:
    global s
    if old not in s:
        raise SystemExit(f'Expected source pattern not found:\n{old}')
    s = s.replace(old, new, 1)


if 'import com.wavelength.music.ui.design.normalizedSmoothClosedShape\n' not in s:
    replace_once(
        'import com.wavelength.music.ui.design.UiDesignConfig\n',
        'import com.wavelength.music.ui.design.UiDesignConfig\nimport com.wavelength.music.ui.design.normalizedSmoothClosedShape\n'
    )

replace_once(
'''                val lowerGlassShape = remember {
                    GenericShape { size, _ ->
                        moveTo(0f, size.height * 0.12f)
                        quadraticBezierTo(
                            size.width * 0.50f,
                            -size.height * 0.12f,
                            size.width,
                            size.height * 0.12f
                        )
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                }''',
'''                val lowerGlassShape = remember {
                    normalizedSmoothClosedShape(UiDesignConfig.GLASS_LOWER_PANEL_SHAPE_POINTS)
                }'''
)

start = '''                            val transportShape = remember {
                                GenericShape { size, _ ->
                                    val w = size.width
                                    val h = size.height
                                    // Reference contour: tighter center crown, pronounced inward shoulders,
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
                                    close()
                                }
                            }'''
replace_once(
    start,
'''                            val transportShape = remember {
                                normalizedSmoothClosedShape(UiDesignConfig.GLASS_TRANSPORT_SHAPE_POINTS)
                            }'''
)

path.write_text(s, encoding='utf-8')
print('Manual Glass shape points bound to Compose.')
