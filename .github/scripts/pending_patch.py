from pathlib import Path

# 1) Give every visual theme its own Material shape language + surface treatment.
theme = Path("app/src/main/java/com/wavelength/music/ui/theme/Theme.kt")
t = theme.read_text()

if "import androidx.compose.foundation.shape.RoundedCornerShape\n" not in t:
    t = t.replace(
        "import androidx.compose.animation.core.tween\n",
        "import androidx.compose.animation.core.tween\nimport androidx.compose.foundation.shape.RoundedCornerShape\n",
        1
    )
if "import androidx.compose.material3.Shapes\n" not in t:
    t = t.replace(
        "import androidx.compose.material3.MaterialTheme\n",
        "import androidx.compose.material3.MaterialTheme\nimport androidx.compose.material3.Shapes\n",
        1
    )
if "import androidx.compose.ui.unit.dp\n" not in t:
    t = t.replace(
        "import androidx.compose.ui.graphics.Color\n",
        "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.unit.dp\n",
        1
    )
if "import com.wavelength.music.data.repository.VisualThemeMode\n" not in t:
    t = t.replace(
        "import androidx.compose.ui.unit.dp\n",
        "import androidx.compose.ui.unit.dp\nimport com.wavelength.music.data.repository.VisualThemeMode\n",
        1
    )

sig = '''    neomorphismEnabled: Boolean = false,
    amoledEnabled: Boolean = false,
    animateTransitions: Boolean = true,
    content: @Composable () -> Unit
) {
'''
sig_new = '''    neomorphismEnabled: Boolean = false,
    amoledEnabled: Boolean = false,
    visualThemeMode: VisualThemeMode = VisualThemeMode.SOLID,
    animateTransitions: Boolean = true,
    content: @Composable () -> Unit
) {
'''
assert sig in t, "Theme signature anchor not found"
t = t.replace(sig, sig_new, 1)

old_else = '''    } else {
        baseScheme
    }

    val duration = if (animateTransitions) 420 else 0
'''
new_else = '''    } else if (visualThemeMode == VisualThemeMode.LIQUID) {
        baseScheme.copy(
            background = Color.Transparent,
            surface = Color(0xFF101827).copy(alpha = 0.84f),
            surfaceVariant = Color(0xFF17243A).copy(alpha = 0.80f),
            surfaceContainer = Color(0xFF132037).copy(alpha = 0.82f),
            surfaceContainerHigh = Color(0xFF1D2D48).copy(alpha = 0.86f),
            outline = baseScheme.primary.copy(alpha = 0.42f),
            outlineVariant = Color.White.copy(alpha = 0.16f)
        )
    } else if (visualThemeMode == VisualThemeMode.ALBUM_ADAPTIVE) {
        baseScheme.copy(
            background = Color.Transparent,
            surface = baseScheme.primary.copy(alpha = 0.16f),
            surfaceVariant = baseScheme.primary.copy(alpha = 0.24f),
            surfaceContainer = baseScheme.primary.copy(alpha = 0.18f),
            surfaceContainerHigh = baseScheme.primary.copy(alpha = 0.28f),
            outline = baseScheme.primary.copy(alpha = 0.70f),
            outlineVariant = baseScheme.primary.copy(alpha = 0.34f)
        )
    } else if (visualThemeMode == VisualThemeMode.AURORA) {
        baseScheme.copy(
            background = Color.Transparent,
            surface = Color(0xFF12152D).copy(alpha = 0.88f),
            surfaceVariant = Color(0xFF242052).copy(alpha = 0.84f),
            surfaceContainer = Color(0xFF171B3A).copy(alpha = 0.86f),
            surfaceContainerHigh = Color(0xFF302665).copy(alpha = 0.86f),
            outline = Color(0xFF7DD3FC).copy(alpha = 0.54f),
            outlineVariant = Color(0xFFC084FC).copy(alpha = 0.30f)
        )
    } else {
        baseScheme
    }

    val visualShapes = when (visualThemeMode) {
        VisualThemeMode.SOLID -> Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(10.dp),
            large = RoundedCornerShape(14.dp),
            extraLarge = RoundedCornerShape(18.dp)
        )
        VisualThemeMode.LIQUID -> Shapes(
            extraSmall = RoundedCornerShape(14.dp),
            small = RoundedCornerShape(18.dp),
            medium = RoundedCornerShape(26.dp),
            large = RoundedCornerShape(34.dp),
            extraLarge = RoundedCornerShape(42.dp)
        )
        VisualThemeMode.GLASSMORPHISM -> Shapes(
            extraSmall = RoundedCornerShape(18.dp),
            small = RoundedCornerShape(22.dp),
            medium = RoundedCornerShape(30.dp),
            large = RoundedCornerShape(38.dp),
            extraLarge = RoundedCornerShape(48.dp)
        )
        VisualThemeMode.NEOMORPHISM -> Shapes(
            extraSmall = RoundedCornerShape(12.dp),
            small = RoundedCornerShape(18.dp),
            medium = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(30.dp),
            extraLarge = RoundedCornerShape(36.dp)
        )
        VisualThemeMode.AMOLED -> Shapes(
            extraSmall = RoundedCornerShape(0.dp),
            small = RoundedCornerShape(2.dp),
            medium = RoundedCornerShape(4.dp),
            large = RoundedCornerShape(6.dp),
            extraLarge = RoundedCornerShape(8.dp)
        )
        VisualThemeMode.ALBUM_ADAPTIVE -> Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(34.dp)
        )
        VisualThemeMode.AURORA -> Shapes(
            extraSmall = RoundedCornerShape(20.dp),
            small = RoundedCornerShape(26.dp),
            medium = RoundedCornerShape(34.dp),
            large = RoundedCornerShape(42.dp),
            extraLarge = RoundedCornerShape(52.dp)
        )
    }

    val duration = if (animateTransitions) 420 else 0
'''
assert old_else in t, "Theme appScheme tail anchor not found"
t = t.replace(old_else, new_else, 1)

mat = '''    MaterialTheme(
        colorScheme = animatedScheme,
        typography = WavelengthTypography,
        content = content
    )
'''
mat_new = '''    MaterialTheme(
        colorScheme = animatedScheme,
        typography = WavelengthTypography,
        shapes = visualShapes,
        content = content
    )
'''
assert mat in t, "MaterialTheme call not found"
t = t.replace(mat, mat_new, 1)
theme.write_text(t)

# 2) Pass the visual mode into MaterialTheme and add obvious app-wide background signatures.
main = Path("app/src/main/java/com/wavelength/music/MainActivity.kt")
m = main.read_text()
call = '''                amoledEnabled = settings.visualThemeMode == VisualThemeMode.AMOLED,
                animateTransitions = settings.animateThemeTransitions
'''
call_new = '''                amoledEnabled = settings.visualThemeMode == VisualThemeMode.AMOLED,
                visualThemeMode = settings.visualThemeMode,
                animateTransitions = settings.animateThemeTransitions
'''
assert call in m, "MainActivity theme args anchor not found"
m = m.replace(call, call_new, 1)

root = '''                    Box(modifier = Modifier.fillMaxWidth().weight(1f).then(appSurface)) {
                        WavelengthNavHost()
                    }
'''
root_new = '''                    Box(modifier = Modifier.fillMaxWidth().weight(1f).then(appSurface)) {
                        when (settings.visualThemeMode) {
                            VisualThemeMode.LIQUID -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    animatedAccent.copy(alpha = 0.22f),
                                                    Color.Transparent
                                                ),
                                                radius = 900f
                                            )
                                        )
                                )
                            }
                            VisualThemeMode.GLASSMORPHISM -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.08f),
                                                    animatedAccent.copy(alpha = 0.12f),
                                                    Color.Transparent
                                                ),
                                                radius = 760f
                                            )
                                        )
                                )
                            }
                            VisualThemeMode.NEOMORPHISM -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.035f),
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.18f)
                                                )
                                            )
                                        )
                                )
                            }
                            VisualThemeMode.ALBUM_ADAPTIVE -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    animatedAccent.copy(alpha = 0.38f),
                                                    animatedAccent.copy(alpha = 0.10f),
                                                    Color.Transparent
                                                ),
                                                radius = 1050f
                                            )
                                        )
                                )
                            }
                            VisualThemeMode.AURORA -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    auroraA.copy(alpha = 0.56f),
                                                    Color.Transparent,
                                                    auroraB.copy(alpha = 0.52f)
                                                )
                                            )
                                        )
                                )
                            }
                            VisualThemeMode.AMOLED, VisualThemeMode.SOLID -> Unit
                        }
                        WavelengthNavHost()
                    }
'''
assert root in m, "MainActivity root app surface not found"
m = m.replace(root, root_new, 1)
main.write_text(m)

# 3) Make bottom navigation visually different for each theme.
nav = Path("app/src/main/java/com/wavelength/music/ui/navigation/NavGraph.kt")
n = nav.read_text()
if "import com.wavelength.music.data.repository.VisualThemeMode\n" not in n:
    n = n.replace(
        "import com.wavelength.music.activation.AdminActivationScreen\n",
        "import com.wavelength.music.activation.AdminActivationScreen\nimport com.wavelength.music.data.repository.VisualThemeMode\n",
        1
    )

mini = '''                            trackTransitionEnabled = settings.trackTransitionEnabled,
                            trackTransitionDurationMs = settings.trackTransitionDurationMs
'''
mini_new = '''                            trackTransitionEnabled = settings.trackTransitionEnabled,
                            trackTransitionDurationMs = settings.trackTransitionDurationMs,
                            visualThemeMode = settings.visualThemeMode
'''
assert mini in n, "MiniPlayerBar args anchor not found"
n = n.replace(mini, mini_new, 1)

brush_old = '''                                val purpleGlassBrush = if (settings.neomorphismEnabled) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF313947),
                                            Color(0xFF252C37),
                                            Color(0xFF1D232C)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xCC332060),
                                            Color(0xCC56367F),
                                            Color(0xCC764595)
                                        )
                                    )
                                }
'''
brush_new = '''                                val navBrush = when (settings.visualThemeMode) {
                                    VisualThemeMode.SOLID -> Brush.linearGradient(
                                        listOf(Color(0xFF171717), Color(0xFF101010))
                                    )
                                    VisualThemeMode.LIQUID -> Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.56f),
                                            Color(0xCC263B63),
                                            Color(0xAA101A2C)
                                        )
                                    )
                                    VisualThemeMode.GLASSMORPHISM -> Brush.linearGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.20f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                            Color(0x66122A43)
                                        )
                                    )
                                    VisualThemeMode.NEOMORPHISM -> Brush.linearGradient(
                                        listOf(Color(0xFF37404E), Color(0xFF252C37), Color(0xFF1A1F27))
                                    )
                                    VisualThemeMode.AMOLED -> Brush.linearGradient(
                                        listOf(Color.Black, Color(0xFF050505))
                                    )
                                    VisualThemeMode.ALBUM_ADAPTIVE -> Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                                            Color(0xCC0A0D14)
                                        )
                                    )
                                    VisualThemeMode.AURORA -> Brush.linearGradient(
                                        listOf(
                                            Color(0xCC4338CA),
                                            Color(0xCC7E22CE),
                                            Color(0xAA0891B2)
                                        )
                                    )
                                }
'''
assert brush_old in n, "Nav brush block not found"
n = n.replace(brush_old, brush_new, 1)

shape_old = '''                                    val tileShape = RoundedCornerShape(if (compactBottomNav) 20.dp else 24.dp)
'''
shape_new = '''                                    val tileRadius = when (settings.visualThemeMode) {
                                        VisualThemeMode.SOLID -> if (compactBottomNav) 8.dp else 10.dp
                                        VisualThemeMode.LIQUID -> if (compactBottomNav) 22.dp else 28.dp
                                        VisualThemeMode.GLASSMORPHISM -> if (compactBottomNav) 24.dp else 30.dp
                                        VisualThemeMode.NEOMORPHISM -> if (compactBottomNav) 16.dp else 20.dp
                                        VisualThemeMode.AMOLED -> if (compactBottomNav) 2.dp else 4.dp
                                        VisualThemeMode.ALBUM_ADAPTIVE -> if (compactBottomNav) 16.dp else 22.dp
                                        VisualThemeMode.AURORA -> if (compactBottomNav) 26.dp else 32.dp
                                    }
                                    val tileShape = RoundedCornerShape(tileRadius)
'''
assert shape_old in n, "Nav tile shape anchor not found"
n = n.replace(shape_old, shape_new, 1)

shadow_old = '''                                            .shadow(
                                                if (selected) 16.dp else 10.dp,
                                                tileShape,
                                                ambientColor = if (settings.neomorphismEnabled) Color.Black.copy(alpha = 0.55f) else Color.Black,
                                                spotColor = if (settings.neomorphismEnabled) Color.Black.copy(alpha = 0.70f) else Color.Black
                                            )
                                            .clip(tileShape)
                                            .background(purpleGlassBrush)
                                            .border(
                                                if (selected) 1.4.dp else 1.1.dp,
                                                Color.White.copy(alpha = if (selected) 0.56f else 0.34f),
                                                tileShape
                                            )
'''
shadow_new = '''                                            .shadow(
                                                when (settings.visualThemeMode) {
                                                    VisualThemeMode.AMOLED -> if (selected) 2.dp else 0.dp
                                                    VisualThemeMode.SOLID -> if (selected) 8.dp else 3.dp
                                                    VisualThemeMode.NEOMORPHISM -> if (selected) 18.dp else 12.dp
                                                    VisualThemeMode.AURORA -> if (selected) 22.dp else 12.dp
                                                    else -> if (selected) 16.dp else 9.dp
                                                },
                                                tileShape,
                                                ambientColor = when (settings.visualThemeMode) {
                                                    VisualThemeMode.AURORA -> Color(0xFF7C3AED)
                                                    VisualThemeMode.ALBUM_ADAPTIVE -> MaterialTheme.colorScheme.primary
                                                    else -> Color.Black.copy(alpha = 0.62f)
                                                },
                                                spotColor = when (settings.visualThemeMode) {
                                                    VisualThemeMode.AURORA -> Color(0xFF22D3EE)
                                                    VisualThemeMode.ALBUM_ADAPTIVE -> MaterialTheme.colorScheme.primary
                                                    else -> Color.Black.copy(alpha = 0.76f)
                                                }
                                            )
                                            .clip(tileShape)
                                            .background(navBrush)
                                            .border(
                                                if (selected) {
                                                    if (settings.visualThemeMode == VisualThemeMode.AMOLED) 1.dp else 1.5.dp
                                                } else {
                                                    if (settings.visualThemeMode == VisualThemeMode.SOLID) 0.6.dp else 1.dp
                                                },
                                                when (settings.visualThemeMode) {
                                                    VisualThemeMode.SOLID -> MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.78f else 0.20f)
                                                    VisualThemeMode.AMOLED -> Color.White.copy(alpha = if (selected) 0.72f else 0.18f)
                                                    VisualThemeMode.NEOMORPHISM -> Color.White.copy(alpha = if (selected) 0.18f else 0.08f)
                                                    VisualThemeMode.ALBUM_ADAPTIVE -> MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.96f else 0.42f)
                                                    VisualThemeMode.AURORA -> Color.White.copy(alpha = if (selected) 0.82f else 0.36f)
                                                    else -> Color.White.copy(alpha = if (selected) 0.62f else 0.30f)
                                                },
                                                tileShape
                                            )
'''
assert shadow_old in n, "Nav shadow/background block not found"
n = n.replace(shadow_old, shadow_new, 1)

overlay_shape_old = '''                            val overlayExtra = if (compactBottomNav) 3.dp else 4.dp
                            val overlayShape = RoundedCornerShape(if (compactBottomNav) 22.dp else 26.dp)
'''
overlay_shape_new = '''                            val overlayExtra = when (settings.visualThemeMode) {
                                VisualThemeMode.AMOLED, VisualThemeMode.SOLID -> 2.dp
                                VisualThemeMode.AURORA -> if (compactBottomNav) 5.dp else 7.dp
                                else -> if (compactBottomNav) 3.dp else 4.dp
                            }
                            val overlayShape = RoundedCornerShape(
                                when (settings.visualThemeMode) {
                                    VisualThemeMode.SOLID -> if (compactBottomNav) 10.dp else 12.dp
                                    VisualThemeMode.LIQUID -> if (compactBottomNav) 24.dp else 30.dp
                                    VisualThemeMode.GLASSMORPHISM -> if (compactBottomNav) 26.dp else 32.dp
                                    VisualThemeMode.NEOMORPHISM -> if (compactBottomNav) 18.dp else 22.dp
                                    VisualThemeMode.AMOLED -> if (compactBottomNav) 3.dp else 5.dp
                                    VisualThemeMode.ALBUM_ADAPTIVE -> if (compactBottomNav) 18.dp else 24.dp
                                    VisualThemeMode.AURORA -> if (compactBottomNav) 28.dp else 35.dp
                                }
                            )
'''
assert overlay_shape_old in n, "Nav overlay shape anchor not found"
n = n.replace(overlay_shape_old, overlay_shape_new, 1)

overlay_bg_old = '''                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.16f),
                                                Color(0xFFB56BFF).copy(alpha = 0.17f),
                                                Color(0xFF60E9FF).copy(alpha = 0.10f)
                                            )
                                        )
                                    )
'''
overlay_bg_new = '''                                    .background(
                                        when (settings.visualThemeMode) {
                                            VisualThemeMode.SOLID -> Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                    Color.Transparent
                                                )
                                            )
                                            VisualThemeMode.AMOLED -> Brush.linearGradient(
                                                listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)
                                            )
                                            VisualThemeMode.NEOMORPHISM -> Brush.linearGradient(
                                                listOf(Color.White.copy(alpha = 0.07f), Color.Black.copy(alpha = 0.10f))
                                            )
                                            VisualThemeMode.ALBUM_ADAPTIVE -> Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                                )
                                            )
                                            VisualThemeMode.AURORA -> Brush.linearGradient(
                                                listOf(
                                                    Color(0xFF8B5CF6).copy(alpha = 0.34f),
                                                    Color(0xFF22D3EE).copy(alpha = 0.24f)
                                                )
                                            )
                                            else -> Brush.linearGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.16f),
                                                    Color(0xFFB56BFF).copy(alpha = 0.17f),
                                                    Color(0xFF60E9FF).copy(alpha = 0.10f)
                                                )
                                            )
                                        }
                                    )
'''
assert overlay_bg_old in n, "Nav overlay background block not found"
n = n.replace(overlay_bg_old, overlay_bg_new, 1)
nav.write_text(n)

# 4) Theme-specific mini-player silhouettes and materials.
mini_path = Path("app/src/main/java/com/wavelength/music/ui/components/MiniPlayerBar.kt")
mp = mini_path.read_text()
if "import com.wavelength.music.data.repository.VisualThemeMode\n" not in mp:
    # place near PlaybackUiState import
    marker = "import com.wavelength.music.ui.nowplaying.PlaybackUiState\n"
    if marker in mp:
        mp = mp.replace(marker, "import com.wavelength.music.data.repository.VisualThemeMode\n" + marker, 1)
    else:
        pkg = "package com.wavelength.music.ui.components\n"
        mp = mp.replace(pkg, pkg + "\nimport com.wavelength.music.data.repository.VisualThemeMode\n", 1)

sig = '''    glassStyle: HazeStyle = HazeStyle.Unspecified,
    trackTransitionEnabled: Boolean = true,
    trackTransitionDurationMs: Int = 300
) {
'''
sig_new = '''    glassStyle: HazeStyle = HazeStyle.Unspecified,
    trackTransitionEnabled: Boolean = true,
    trackTransitionDurationMs: Int = 300,
    visualThemeMode: VisualThemeMode = VisualThemeMode.SOLID
) {
'''
assert sig in mp, "MiniPlayer signature not found"
mp = mp.replace(sig, sig_new, 1)

shape = '''    val isLiquid = hazeState != null
    val shape = RoundedCornerShape(UiDesignConfig.MINI_PLAYER_RADIUS_DP.dp)

    var barModifier = modifier.fillMaxWidth()
    if (isLiquid) {
'''
shape_new = '''    val isLiquid = hazeState != null
    val shape = RoundedCornerShape(
        when (visualThemeMode) {
            VisualThemeMode.SOLID -> 8.dp
            VisualThemeMode.LIQUID -> 24.dp
            VisualThemeMode.GLASSMORPHISM -> 30.dp
            VisualThemeMode.NEOMORPHISM -> 20.dp
            VisualThemeMode.AMOLED -> 2.dp
            VisualThemeMode.ALBUM_ADAPTIVE -> 18.dp
            VisualThemeMode.AURORA -> 28.dp
        }
    )

    var barModifier = modifier.fillMaxWidth()
    if (isLiquid) {
'''
assert shape in mp, "MiniPlayer shape anchor not found"
mp = mp.replace(shape, shape_new, 1)

else_block = '''    } else {
        barModifier = barModifier.height(UiDesignConfig.MINI_PLAYER_HEIGHT_DP.dp)
    }
'''
else_new = '''    } else if (visualThemeMode == VisualThemeMode.SOLID) {
        barModifier = barModifier.height(UiDesignConfig.MINI_PLAYER_HEIGHT_DP.dp)
    } else {
        barModifier = barModifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(UiDesignConfig.MINI_PLAYER_HEIGHT_DP.dp)
            .clip(shape)
            .border(
                1.dp,
                when (visualThemeMode) {
                    VisualThemeMode.AMOLED -> Color.White.copy(alpha = 0.20f)
                    VisualThemeMode.NEOMORPHISM -> Color.White.copy(alpha = 0.08f)
                    VisualThemeMode.ALBUM_ADAPTIVE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                    VisualThemeMode.AURORA -> Color.White.copy(alpha = 0.32f)
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                shape
            )
    }
'''
assert else_block in mp, "MiniPlayer modifier else block not found"
mp = mp.replace(else_block, else_new, 1)

surf = '''    Surface(
        modifier = barModifier,
        color = if (isLiquid) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (isLiquid) 0.dp else 4.dp
    ) {
'''
surf_new = '''    Surface(
        modifier = barModifier,
        color = when {
            isLiquid -> Color.Transparent
            visualThemeMode == VisualThemeMode.AMOLED -> Color.Black
            visualThemeMode == VisualThemeMode.NEOMORPHISM -> MaterialTheme.colorScheme.surfaceVariant
            visualThemeMode == VisualThemeMode.ALBUM_ADAPTIVE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            visualThemeMode == VisualThemeMode.AURORA -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        tonalElevation = when (visualThemeMode) {
            VisualThemeMode.NEOMORPHISM -> 12.dp
            VisualThemeMode.AURORA -> 8.dp
            VisualThemeMode.AMOLED -> 0.dp
            else -> if (isLiquid) 0.dp else 4.dp
        },
        shape = shape
    ) {
'''
assert surf in mp, "MiniPlayer Surface block not found"
mp = mp.replace(surf, surf_new, 1)
mini_path.write_text(mp)

# 5) Make the theme descriptions explicitly promise a different visual language.
settings_repo = Path("app/src/main/java/com/wavelength/music/data/repository/SettingsRepository.kt")
sr = settings_repo.read_text()
sr = sr.replace('SOLID("Solid", "Clean solid interface")', 'SOLID("Solid", "Flat, compact geometry with restrained surfaces")')
sr = sr.replace('LIQUID("Liquid", "Translucent liquid surfaces")', 'LIQUID("Liquid", "Rounded flowing surfaces with soft translucent depth")')
sr = sr.replace('GLASSMORPHISM("Glassmorphism", "Frosted glass across the full app")', 'GLASSMORPHISM("Glassmorphism", "Large frosted glass shapes, luminous rims and blur")')
sr = sr.replace('NEOMORPHISM("Neomorphism", "Soft raised and inset surfaces")', 'NEOMORPHISM("Neomorphism", "Raised graphite controls with soft depth and shadows")')
sr = sr.replace('AMOLED("AMOLED", "True black OLED-friendly interface")', 'AMOLED("AMOLED", "True black, sharp minimal geometry and almost no glow")')
sr = sr.replace('ALBUM_ADAPTIVE("Album Adaptive", "Colors follow the current album artwork")', 'ALBUM_ADAPTIVE("Album Adaptive", "Album-driven surfaces, borders and ambient backdrop")')
sr = sr.replace('AURORA("Aurora", "Animated cyan, violet and blue atmosphere")', 'AURORA("Aurora", "Oversized rounded UI with animated cyan-violet atmosphere")')
settings_repo.write_text(sr)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Reworked every theme as a distinct visual system, not just a recolor: each now changes app-wide geometry, surface material, mini-player shape, bottom navigation, depth, borders and background treatment",\n'
assert needle in a, "About changelog anchor not found"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
