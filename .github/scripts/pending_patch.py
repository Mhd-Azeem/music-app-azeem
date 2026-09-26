from pathlib import Path

nav = Path("app/src/main/java/com/wavelength/music/ui/navigation/NavGraph.kt")
s = nav.read_text()

# Imports for finger-following draggable tile overlay.
s = s.replace(
    "import androidx.compose.animation.core.animateFloatAsState\n",
    "import androidx.compose.animation.core.animateFloatAsState\nimport androidx.compose.animation.core.snap\n"
)
s = s.replace(
    "import androidx.compose.foundation.clickable\n",
    "import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.gestures.detectHorizontalDragGestures\n"
)
s = s.replace(
    "import androidx.compose.foundation.layout.size\n",
    "import androidx.compose.foundation.layout.size\nimport androidx.compose.foundation.layout.height\n"
)
s = s.replace(
    "import androidx.compose.runtime.mutableStateOf\n",
    "import androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.mutableFloatStateOf\n"
)
s = s.replace(
    "import androidx.compose.ui.Modifier\n",
    "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.input.pointer.pointerInput\nimport androidx.compose.ui.platform.LocalDensity\n"
)

start_marker = '''                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
'''
end_marker = '''                    }
                }
            }
        }
    ) { padding ->
'''
start = s.index(start_marker)
end = s.index(end_marker, start)

replacement = '''                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        val navSpacing = 10.dp
                        val itemWidth = (maxWidth - navSpacing * 2) / 3
                        val selectedIndex = bottomNavScreens.indexOfFirst { currentRoute == it.route }
                            .coerceAtLeast(0)
                        val density = LocalDensity.current

                        var isDraggingNav by remember { mutableStateOf(false) }
                        var dragIndex by remember { mutableFloatStateOf(selectedIndex.toFloat()) }

                        LaunchedEffect(selectedIndex, isDraggingNav) {
                            if (!isDraggingNav) {
                                dragIndex = selectedIndex.toFloat()
                            }
                        }

                        val overlayIndex by animateFloatAsState(
                            targetValue = dragIndex,
                            animationSpec = if (isDraggingNav) {
                                snap()
                            } else {
                                spring(
                                    dampingRatio = 0.72f,
                                    stiffness = Spring.StiffnessLow
                                )
                            },
                            label = "bottomNavTileOverlayIndex"
                        )

                        val stepPx = with(density) { (itemWidth + navSpacing).toPx() }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(stepPx, selectedIndex) {
                                    detectHorizontalDragGestures(
                                        onDragStart = {
                                            isDraggingNav = true
                                            dragIndex = selectedIndex.toFloat()
                                        },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragIndex = (dragIndex + dragAmount / stepPx)
                                                .coerceIn(0f, (bottomNavScreens.size - 1).toFloat())
                                        },
                                        onDragCancel = {
                                            isDraggingNav = false
                                            dragIndex = selectedIndex.toFloat()
                                        },
                                        onDragEnd = {
                                            val targetIndex = kotlin.math.round(dragIndex)
                                                .toInt()
                                                .coerceIn(0, bottomNavScreens.lastIndex)
                                            isDraggingNav = false
                                            dragIndex = targetIndex.toFloat()

                                            val target = bottomNavScreens[targetIndex]
                                            if (currentRoute != target.route) {
                                                navController.navigate(target.route) {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
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
                                    val itemAlpha by animateFloatAsState(
                                        targetValue = if (selected) 1f else 0.82f,
                                        animationSpec = tween(180),
                                        label = "navItemAlpha$index"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .width(itemWidth)
                                            .graphicsLayer { alpha = itemAlpha }
                                            .shadow(if (selected) 13.dp else 9.dp, tileShape)
                                            .clip(tileShape)
                                            .background(purpleGlassBrush)
                                            .border(
                                                if (selected) 1.4.dp else 1.1.dp,
                                                Color.White.copy(alpha = if (selected) 0.56f else 0.34f),
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
                                                tint = Color.White
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

                            // Same pill/rounded-rectangle silhouette as the actual nav tile.
                            // It is only a few dp larger so the liquid overlay remains visible
                            // around the Home/Search/Library tile while following the finger.
                            val overlayExtra = 4.dp
                            val overlayShape = RoundedCornerShape(26.dp)
                            val overlayX = (itemWidth + navSpacing) * overlayIndex - overlayExtra / 2

                            Box(
                                modifier = Modifier
                                    .offset(x = overlayX, y = -overlayExtra / 2)
                                    .width(itemWidth + overlayExtra)
                                    .height(50.dp)
                                    .shadow(
                                        elevation = 18.dp,
                                        shape = overlayShape,
                                        ambientColor = Color(0xFFB86BFF),
                                        spotColor = Color(0xFF72E8FF)
                                    )
                                    .clip(overlayShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.16f),
                                                Color(0xFFB56BFF).copy(alpha = 0.17f),
                                                Color(0xFF60E9FF).copy(alpha = 0.10f)
                                            )
                                        )
                                    )
                                    .border(
                                        1.45.dp,
                                        Brush.linearGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.76f),
                                                Color(0xFFB96DFF).copy(alpha = 0.46f),
                                                Color(0xFF6DEBFF).copy(alpha = 0.36f)
                                            )
                                        ),
                                        overlayShape
                                    )
                            )
                        }
                    }
'''

s = s[:start] + replacement + s[end + len("                    }\n"):]
nav.write_text(s)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Changed the bottom navigation liquid selector from a circle to a tile-sized rounded overlay and added direct finger-dragging across Home, Search and Library with snap-to-tab navigation",\n'
assert needle in a, "About changelog anchor not found"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
