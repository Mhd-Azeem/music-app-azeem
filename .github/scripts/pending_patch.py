from pathlib import Path

now = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = now.read_text()

old_root = '''    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {'''
new_root = '''    Box(
        modifier = Modifier
            .fillMaxSize()
            // In Glass mode the whole Now Playing surface is draggable downward, including
            // artwork, Up Next and the lower glass controls. This keeps the collapse gesture
            // available even when a child composable would otherwise consume the touch.
            .pointerInput(glassmorphismNowPlaying) {
                if (glassmorphismNowPlaying) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffset > collapseThresholdPx) {
                                onCollapse()
                            } else {
                                scope.launch {
                                    settleAnim.snapTo(dragOffset)
                                    settleAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    ) { dragOffset = value }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                settleAnim.snapTo(dragOffset)
                                settleAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) { dragOffset = value }
                            }
                        }
                    ) { change, dragAmount ->
                        if (dragAmount > 0f || dragOffset > 0f) {
                            change.consume()
                            dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                        }
                    }
                }
            }
            .graphicsLayer {'''
assert old_root in s, 'Root Now Playing Box anchor not found'
s = s.replace(old_root, new_root, 1)

old_inner = '''                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragOffset > collapseThresholdPx) {
                                    // Leave dragOffset where the drag ended rather than snapping it
                                    // to 0 first — this composable is about to be popped off the
                                    // back stack anyway, and resetting the offset here made the
                                    // screen visibly jump back to its start position for a frame
                                    // before the nav pop's own slide-out transition took over,
                                    // producing a jarring double-motion glitch on release.
                                    onCollapse()
                                } else {
                                    scope.launch {
                                        settleAnim.snapTo(dragOffset)
                                        settleAnim.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) { dragOffset = value }
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    settleAnim.snapTo(dragOffset)
                                    settleAnim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    ) { dragOffset = value }
                                }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                        }
                    }'''
new_inner = '''                    .pointerInput(glassmorphismNowPlaying) {
                        // Glass mode handles collapse on the full-screen parent so every visible
                        // glass surface can start the gesture. Keep this local handler only for
                        // the standard Now Playing layout.
                        if (!glassmorphismNowPlaying) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (dragOffset > collapseThresholdPx) {
                                        onCollapse()
                                    } else {
                                        scope.launch {
                                            settleAnim.snapTo(dragOffset)
                                            settleAnim.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            ) { dragOffset = value }
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        settleAnim.snapTo(dragOffset)
                                        settleAnim.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        ) { dragOffset = value }
                                    }
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                            }
                        }
                    }'''
assert old_inner in s, 'Existing collapse gesture block not found'
s = s.replace(old_inner, new_inner, 1)
now.write_text(s)

about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a = about.read_text()
needle = 'private val latestUpdates = listOf(\n'
entry = '    "Glass Now Playing can now be dragged down from anywhere on the screen to collapse smoothly into the mini player",\n'
assert needle in a, 'About latestUpdates list not found'
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
