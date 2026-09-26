from pathlib import Path

nav = Path("app/src/main/java/com/wavelength/music/ui/navigation/NavGraph.kt")
s = nav.read_text()

# Animation/layout imports.
s = s.replace(
    "import androidx.compose.animation.core.tween\n",
    "import androidx.compose.animation.core.tween\nimport androidx.compose.animation.core.animateFloatAsState\nimport androidx.compose.animation.core.spring\nimport androidx.compose.animation.core.Spring\n"
)
s = s.replace(
    "import androidx.compose.foundation.layout.Box\n",
    "import androidx.compose.foundation.layout.Box\nimport androidx.compose.foundation.layout.BoxWithConstraints\n"
)
s = s.replace(
    "import androidx.compose.foundation.layout.padding\n",
    "import androidx.compose.foundation.layout.padding\nimport androidx.compose.foundation.layout.offset\nimport androidx.compose.foundation.layout.size\n"
)
s = s.replace(
    "import androidx.compose.ui.draw.shadow\n",
    "import androidx.compose.ui.draw.shadow\nimport androidx.compose.ui.graphics.graphicsLayer\n"
)

old = '''                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val purpleGlassBrush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xCC332060),
                                Color(0xCC56367F),
                                Color(0xCC764595)
                            )
                        )
                        bottomNavScreens.forEach { screen ->
                            val selected = currentRoute == screen.route
                            val tileShape = RoundedCornerShape(24.dp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(if (selected) 15.dp else 10.dp, tileShape)
                                    .clip(tileShape)
                                    .background(purpleGlassBrush)
                                    .border(
                                        if (selected) 1.6.dp else 1.2.dp,
                                        Color.White.copy(alpha = if (selected) 0.62f else 0.42f),
                                        tileShape
                                    )
                                    .clickable {
                                        if (!selected) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        iconFor(screen),
                                        contentDescription = navLabelFor(screen),
                                        tint = Color.White
                                    )
                                    Text(
                                        navLabelFor(screen),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
'''

new = '''                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        val navSpacing = 10.dp
                        val itemWidth = (maxWidth - navSpacing * 2) / 3
                        val selectedIndex = bottomNavScreens.indexOfFirst { currentRoute == it.route }
                            .coerceAtLeast(0)

                        // Reference-style liquid selector: a translucent circular lens glides
                        // between Home, Search and Library with a soft spring and slight stretch.
                        val animatedIndex by animateFloatAsState(
                            targetValue = selectedIndex.toFloat(),
                            animationSpec = spring(
                                dampingRatio = 0.72f,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "bottomNavLiquidIndex"
                        )
                        val lensCenterX = (itemWidth + navSpacing) * animatedIndex + itemWidth / 2
                        val lensStretch by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = 0.58f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "bottomNavLiquidStretch"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(navSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val purpleGlassBrush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xCC332060),
                                    Color(0xCC56367F),
                                    Color(0xCC764595)
                                )
                            )

                            bottomNavScreens.forEachIndexed { index, screen ->
                                val selected = selectedIndex == index
                                val tileShape = RoundedCornerShape(24.dp)
                                val itemScale by animateFloatAsState(
                                    targetValue = if (selected) 1.055f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = 0.68f,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "navItemScale$index"
                                )
                                val itemAlpha by animateFloatAsState(
                                    targetValue = if (selected) 1f else 0.80f,
                                    animationSpec = tween(180),
                                    label = "navItemAlpha$index"
                                )

                                Box(
                                    modifier = Modifier
                                        .width(itemWidth)
                                        .graphicsLayer {
                                            scaleX = itemScale
                                            scaleY = itemScale
                                            alpha = itemAlpha
                                        }
                                        .shadow(if (selected) 16.dp else 9.dp, tileShape)
                                        .clip(tileShape)
                                        .background(purpleGlassBrush)
                                        .border(
                                            if (selected) 1.7.dp else 1.1.dp,
                                            Color.White.copy(alpha = if (selected) 0.68f else 0.36f),
                                            tileShape
                                        )
                                        .clickable {
                                            if (!selected) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            iconFor(screen),
                                            contentDescription = navLabelFor(screen),
                                            tint = Color.White,
                                            modifier = Modifier.graphicsLayer {
                                                scaleX = if (selected) 1.10f else 1f
                                                scaleY = if (selected) 1.10f else 1f
                                            }
                                        )
                                        Text(
                                            navLabelFor(screen),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            modifier = Modifier.padding(start = 7.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // The moving lens is drawn last so it behaves like the magnifying/liquid
                        // bubble in the reference clip without intercepting touches.
                        Box(
                            modifier = Modifier
                                .offset(x = lensCenterX - 36.dp, y = 2.dp)
                                .size(72.dp)
                                .graphicsLayer {
                                    scaleX = 1.06f * lensStretch
                                    scaleY = 0.96f
                                    alpha = 0.92f
                                }
                                .shadow(
                                    elevation = 18.dp,
                                    shape = RoundedCornerShape(36.dp),
                                    ambientColor = Color(0xFFB86BFF),
                                    spotColor = Color(0xFF72E8FF)
                                )
                                .clip(RoundedCornerShape(36.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.20f),
                                            Color(0xFFB56BFF).copy(alpha = 0.18f),
                                            Color(0xFF60E9FF).copy(alpha = 0.10f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .border(
                                    1.35.dp,
                                    Brush.linearGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.72f),
                                            Color(0xFFB96DFF).copy(alpha = 0.42f),
                                            Color(0xFF6DEBFF).copy(alpha = 0.34f)
                                        )
                                    ),
                                    RoundedCornerShape(36.dp)
                                )
                        )
                    }
'''

assert old in s, "bottom nav block not found"
s = s.replace(old, new, 1)
nav.write_text(s)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Added a reference-style liquid selector animation to Home, Search and Library: the selected tile springs larger while a glowing glass lens glides between tabs",\n'
assert needle in a, "About changelog anchor not found"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
