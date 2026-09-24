from pathlib import Path

p = Path("app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt")
s = p.read_text()

# Add a compact-height breakpoint shared by both standard and Glass Now Playing layouts.
anchor = '''    val density = LocalDensity.current
    val collapseThresholdPx = with(density) { 80.dp.toPx() }
'''
replacement = '''    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val compactHeightLayout = screenHeightDp < 720
    val veryCompactHeightLayout = screenHeightDp < 620
    val collapseThresholdPx = with(density) { 80.dp.toPx() }
'''
assert anchor in s, "compact breakpoint anchor not found"
s = s.replace(anchor, replacement, 1)

# Standard/Vinyl artwork: cap height on short phones so controls and Up Next remain visible.
old = '''                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(20.dp)) {'''
new = '''                            Box(
                                modifier = if (compactHeightLayout) {
                                    Modifier.fillMaxWidth().height(if (veryCompactHeightLayout) 180.dp else 220.dp).padding(12.dp)
                                } else {
                                    Modifier.fillMaxWidth().aspectRatio(1f).padding(20.dp)
                                }
                            ) {'''
assert old in s, "vinyl artwork box not found"
s = s.replace(old, new, 1)

old = '''                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .graphicsLayer {'''
new = '''                                    modifier = (if (compactHeightLayout) {
                                        Modifier.fillMaxWidth().height(if (veryCompactHeightLayout) 180.dp else 220.dp)
                                    } else {
                                        Modifier.fillMaxWidth().aspectRatio(1f)
                                    })
                                        .graphicsLayer {'''
assert old in s, "standard artwork modifier not found"
s = s.replace(old, new, 1)

# Tighten vertical spacing in compact layouts.
s = s.replace(
    '''                        .padding(top = 32.dp)
''',
    '''                        .padding(top = if (compactHeightLayout) 12.dp else 32.dp)
''',
    1
)
s = s.replace(
    '''                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {''',
    '''                Column(modifier = Modifier.fillMaxWidth().padding(top = if (compactHeightLayout) 8.dp else 16.dp)) {''',
    1
)
s = s.replace(
    '''                var transportRowModifier = Modifier.fillMaxWidth().padding(top = 16.dp)''',
    '''                var transportRowModifier = Modifier.fillMaxWidth().padding(top = if (compactHeightLayout) 8.dp else 16.dp)''',
    1
)
s = s.replace(
    '''                var volumeRowModifier = Modifier.fillMaxWidth().padding(top = 16.dp)''',
    '''                var volumeRowModifier = Modifier.fillMaxWidth().padding(top = if (compactHeightLayout) 6.dp else 16.dp)''',
    1
)
s = s.replace(
    '''                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),''',
    '''                    modifier = Modifier.fillMaxWidth().padding(top = if (compactHeightLayout) 8.dp else 24.dp, bottom = 6.dp),''',
    1
)

# Keep a visible, usable Up Next viewport on short screens instead of forcing a 220dp panel
# below oversized artwork/controls.
old = '''                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                val upNextHeight by animateDpAsState(
                    targetValue = if (isUpNextExpanded) screenHeight / 2 else 220.dp,
                    label = "upNextHeight"
                )'''
new = '''                val screenHeight = screenHeightDp.dp
                val collapsedUpNextHeight = when {
                    veryCompactHeightLayout -> 112.dp
                    compactHeightLayout -> 150.dp
                    else -> 220.dp
                }
                val upNextHeight by animateDpAsState(
                    targetValue = if (isUpNextExpanded) screenHeight / 2 else collapsedUpNextHeight,
                    label = "upNextHeight"
                )'''
assert old in s, "Up Next height block not found"
s = s.replace(old, new, 1)

# Glass mode: reduce the fixed lower panel and fan/card geometry on short-height devices,
# otherwise the fixed 356dp player can starve the weighted queue area to nearly zero.
s = s.replace(
    '''                            state = fanState,
                            modifier = Modifier.fillMaxWidth().height(UiDesignConfig.GLASS_QUEUE_AREA_HEIGHT_DP.dp),''',
    '''                            state = fanState,
                            modifier = Modifier.fillMaxWidth().height(
                                when {
                                    veryCompactHeightLayout -> 126.dp
                                    compactHeightLayout -> 158.dp
                                    else -> UiDesignConfig.GLASS_QUEUE_AREA_HEIGHT_DP.dp
                                }
                            ),''',
    1
)
s = s.replace(
    '''                                        .width(UiDesignConfig.GLASS_QUEUE_CARD_WIDTH_DP.dp)
                                        .height(UiDesignConfig.GLASS_QUEUE_CARD_HEIGHT_DP.dp)''',
    '''                                        .width(if (compactHeightLayout) 72.dp else UiDesignConfig.GLASS_QUEUE_CARD_WIDTH_DP.dp)
                                        .height(
                                            when {
                                                veryCompactHeightLayout -> 104.dp
                                                compactHeightLayout -> 132.dp
                                                else -> UiDesignConfig.GLASS_QUEUE_CARD_HEIGHT_DP.dp
                                            }
                                        )''',
    1
)
s = s.replace(
    '''                                val lift = with(LocalDensity.current) { (circleY * UiDesignConfig.GLASS_QUEUE_ARC_DEPTH_DP).dp }''',
    '''                                val queueArcDepth = if (compactHeightLayout) 34f else UiDesignConfig.GLASS_QUEUE_ARC_DEPTH_DP
                                val lift = with(LocalDensity.current) { (circleY * queueArcDepth).dp }''',
    1
)
s = s.replace(
    '''                                                .height(60.dp)''',
    '''                                                .height(if (compactHeightLayout) 48.dp else 60.dp)''',
    1
)
s = s.replace(
    '''                                            modifier = Modifier.fillMaxWidth().height(94.dp),''',
    '''                                            modifier = Modifier.fillMaxWidth().height(if (compactHeightLayout) 66.dp else 94.dp),''',
    1
)

s = s.replace(
    '''                        .height(UiDesignConfig.GLASS_LOWER_PANEL_HEIGHT_DP.dp)''',
    '''                        .height(
                            when {
                                veryCompactHeightLayout -> 278.dp
                                compactHeightLayout -> 310.dp
                                else -> UiDesignConfig.GLASS_LOWER_PANEL_HEIGHT_DP.dp
                            }
                        )''',
    1
)
s = s.replace(
    '''                        modifier = Modifier.fillMaxSize().padding(top = UiDesignConfig.GLASS_LOWER_PANEL_TOP_PADDING_DP.dp, start = 0.dp, end = 0.dp, bottom = 0.dp),''',
    '''                        modifier = Modifier.fillMaxSize().padding(
                            top = if (compactHeightLayout) 24.dp else UiDesignConfig.GLASS_LOWER_PANEL_TOP_PADDING_DP.dp,
                            start = 0.dp,
                            end = 0.dp,
                            bottom = 0.dp
                        ),''',
    1
)
s = s.replace(
    '''                                .height(92.dp)''',
    '''                                .height(if (compactHeightLayout) 74.dp else 92.dp)''',
    1
)

p.write_text(s)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Made Now Playing responsive on short/small phones so artwork, controls and Up Next stay visible; Glass queue/player geometry now scales down automatically",\n'
assert needle in a, "About list anchor not found"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
