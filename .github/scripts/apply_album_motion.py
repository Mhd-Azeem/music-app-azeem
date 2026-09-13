from pathlib import Path

ROOT = Path('.')


def replace_once(path, old, new):
    p = ROOT / path
    text = p.read_text()
    if old not in text:
        raise SystemExit(f'Pattern not found in {path}: {old[:120]!r}')
    p.write_text(text.replace(old, new, 1))

# SettingsRepository: persist two new mutually-exclusive album art motion styles.
path = 'app/src/main/java/com/wavelength/music/data/repository/SettingsRepository.kt'
replace_once(path,
'''    val vinylStyleAlbumArt: Boolean = false,\n    val aiDjEnabled: Boolean = false,''',
'''    val vinylStyleAlbumArt: Boolean = false,\n    val parallaxAlbumArt: Boolean = false,\n    val beatBounceAlbumArt: Boolean = false,\n    val aiDjEnabled: Boolean = false,''')
replace_once(path,
'''        vinylStyleAlbumArt = prefs.getBoolean(KEY_VINYL_STYLE, false),\n        aiDjEnabled = prefs.getBoolean(KEY_AI_DJ, false),''',
'''        vinylStyleAlbumArt = prefs.getBoolean(KEY_VINYL_STYLE, false),\n        parallaxAlbumArt = prefs.getBoolean(KEY_PARALLAX_ALBUM_ART, false),\n        beatBounceAlbumArt = prefs.getBoolean(KEY_BEAT_BOUNCE_ALBUM_ART, false),\n        aiDjEnabled = prefs.getBoolean(KEY_AI_DJ, false),''')
replace_once(path,
'''    fun setVinylStyleAlbumArt(enabled: Boolean) {\n        prefs.edit { putBoolean(KEY_VINYL_STYLE, enabled) }\n        _state.update { it.copy(vinylStyleAlbumArt = enabled) }\n    }''',
'''    fun setVinylStyleAlbumArt(enabled: Boolean) {\n        prefs.edit {\n            putBoolean(KEY_VINYL_STYLE, enabled)\n            if (enabled) {\n                putBoolean(KEY_PARALLAX_ALBUM_ART, false)\n                putBoolean(KEY_BEAT_BOUNCE_ALBUM_ART, false)\n            }\n        }\n        _state.update {\n            it.copy(\n                vinylStyleAlbumArt = enabled,\n                parallaxAlbumArt = if (enabled) false else it.parallaxAlbumArt,\n                beatBounceAlbumArt = if (enabled) false else it.beatBounceAlbumArt\n            )\n        }\n    }\n\n    fun setParallaxAlbumArt(enabled: Boolean) {\n        prefs.edit {\n            putBoolean(KEY_PARALLAX_ALBUM_ART, enabled)\n            if (enabled) {\n                putBoolean(KEY_VINYL_STYLE, false)\n                putBoolean(KEY_BEAT_BOUNCE_ALBUM_ART, false)\n            }\n        }\n        _state.update {\n            it.copy(\n                parallaxAlbumArt = enabled,\n                vinylStyleAlbumArt = if (enabled) false else it.vinylStyleAlbumArt,\n                beatBounceAlbumArt = if (enabled) false else it.beatBounceAlbumArt\n            )\n        }\n    }\n\n    fun setBeatBounceAlbumArt(enabled: Boolean) {\n        prefs.edit {\n            putBoolean(KEY_BEAT_BOUNCE_ALBUM_ART, enabled)\n            if (enabled) {\n                putBoolean(KEY_VINYL_STYLE, false)\n                putBoolean(KEY_PARALLAX_ALBUM_ART, false)\n            }\n        }\n        _state.update {\n            it.copy(\n                beatBounceAlbumArt = enabled,\n                vinylStyleAlbumArt = if (enabled) false else it.vinylStyleAlbumArt,\n                parallaxAlbumArt = if (enabled) false else it.parallaxAlbumArt\n            )\n        }\n    }''')
replace_once(path,
'''        const val KEY_VINYL_STYLE = "vinyl_style_album_art"\n        const val KEY_AI_DJ = "ai_dj_enabled"''',
'''        const val KEY_VINYL_STYLE = "vinyl_style_album_art"\n        const val KEY_PARALLAX_ALBUM_ART = "parallax_album_art"\n        const val KEY_BEAT_BOUNCE_ALBUM_ART = "beat_bounce_album_art"\n        const val KEY_AI_DJ = "ai_dj_enabled"''')

# ViewModel setters.
path = 'app/src/main/java/com/wavelength/music/ui/settings/AppSettingsViewModel.kt'
replace_once(path,
'''    fun setVinylStyleAlbumArt(enabled: Boolean) = settingsRepository.setVinylStyleAlbumArt(enabled)\n\n    fun setAiDjEnabled(enabled: Boolean) = settingsRepository.setAiDjEnabled(enabled)''',
'''    fun setVinylStyleAlbumArt(enabled: Boolean) = settingsRepository.setVinylStyleAlbumArt(enabled)\n\n    fun setParallaxAlbumArt(enabled: Boolean) = settingsRepository.setParallaxAlbumArt(enabled)\n\n    fun setBeatBounceAlbumArt(enabled: Boolean) = settingsRepository.setBeatBounceAlbumArt(enabled)\n\n    fun setAiDjEnabled(enabled: Boolean) = settingsRepository.setAiDjEnabled(enabled)''')

# Navigation: pass the two new settings into Now Playing.
path = 'app/src/main/java/com/wavelength/music/ui/navigation/NavGraph.kt'
replace_once(path,
'''                        vinylStyleAlbumArt = settings.vinylStyleAlbumArt,\n                        audioVisualizerEnabled = settings.audioVisualizerEnabled,''',
'''                        vinylStyleAlbumArt = settings.vinylStyleAlbumArt,\n                        parallaxAlbumArt = settings.parallaxAlbumArt,\n                        beatBounceAlbumArt = settings.beatBounceAlbumArt,\n                        audioVisualizerEnabled = settings.audioVisualizerEnabled,''')

# Settings UI: add Parallax and Beat Bounce next to the existing spinning option.
path = 'app/src/main/java/com/wavelength/music/ui/settings/SettingsScreen.kt'
replace_once(path,
'''                        Switch(\n                            checked = settings.vinylStyleAlbumArt,\n                            onCheckedChange = { viewModel.setVinylStyleAlbumArt(it) }\n                        )\n                    }\n                    Row(''',
'''                        Switch(\n                            checked = settings.vinylStyleAlbumArt,\n                            onCheckedChange = { viewModel.setVinylStyleAlbumArt(it) }\n                        )\n                    }\n                    Row(\n                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),\n                        verticalAlignment = Alignment.CenterVertically\n                    ) {\n                        Text(\n                            text = "Parallax album art",\n                            modifier = Modifier.weight(1f)\n                        )\n                        Switch(\n                            checked = settings.parallaxAlbumArt,\n                            onCheckedChange = { viewModel.setParallaxAlbumArt(it) }\n                        )\n                    }\n                    Row(\n                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),\n                        verticalAlignment = Alignment.CenterVertically\n                    ) {\n                        Text(\n                            text = "Beat bounce album art",\n                            modifier = Modifier.weight(1f)\n                        )\n                        Switch(\n                            checked = settings.beatBounceAlbumArt,\n                            onCheckedChange = { viewModel.setBeatBounceAlbumArt(it) }\n                        )\n                    }\n                    Row(''')

# Now Playing: accept and render the new styles.
path = 'app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt'
replace_once(path,
'''    vinylStyleAlbumArt: Boolean = false,\n    audioVisualizerEnabled: Boolean = false,''',
'''    vinylStyleAlbumArt: Boolean = false,\n    parallaxAlbumArt: Boolean = false,\n    beatBounceAlbumArt: Boolean = false,\n    audioVisualizerEnabled: Boolean = false,''')
replace_once(path,
'''    LaunchedEffect(vinylStyleAlbumArt, state.isPlaying) {\n        if (vinylStyleAlbumArt && state.isPlaying) {\n            while (true) {\n                vinylAngle.animateTo(\n                    targetValue = vinylAngle.value + 360f,\n                    animationSpec = tween(durationMillis = 6000, easing = LinearEasing)\n                )\n            }\n        }\n    }\n\n    var hasRecordAudioPermission''',
'''    LaunchedEffect(vinylStyleAlbumArt, state.isPlaying) {\n        if (vinylStyleAlbumArt && state.isPlaying) {\n            while (true) {\n                vinylAngle.animateTo(\n                    targetValue = vinylAngle.value + 360f,\n                    animationSpec = tween(durationMillis = 6000, easing = LinearEasing)\n                )\n            }\n        }\n    }\n\n    // Subtle 3D parallax: a slow, premium floating tilt rather than a distracting wobble.\n    val parallaxPhase = remember { Animatable(0f) }\n    LaunchedEffect(parallaxAlbumArt, state.isPlaying) {\n        if (parallaxAlbumArt && state.isPlaying) {\n            while (true) {\n                parallaxPhase.animateTo(\n                    1f,\n                    animationSpec = tween(durationMillis = 1700, easing = LinearEasing)\n                )\n                parallaxPhase.animateTo(\n                    -1f,\n                    animationSpec = tween(durationMillis = 1700, easing = LinearEasing)\n                )\n            }\n        } else {\n            parallaxPhase.animateTo(0f, animationSpec = tween(220))\n        }\n    }\n\n    // Neat beat-bounce style: compact pulse with a soft spring return while music is playing.\n    val beatBounceScale = remember { Animatable(1f) }\n    LaunchedEffect(beatBounceAlbumArt, state.isPlaying) {\n        if (beatBounceAlbumArt && state.isPlaying) {\n            while (true) {\n                beatBounceScale.animateTo(\n                    1.045f,\n                    animationSpec = tween(durationMillis = 105, easing = LinearEasing)\n                )\n                beatBounceScale.animateTo(\n                    1f,\n                    animationSpec = spring(\n                        dampingRatio = Spring.DampingRatioMediumBouncy,\n                        stiffness = Spring.StiffnessMedium\n                    )\n                )\n                kotlinx.coroutines.delay(300)\n            }\n        } else {\n            beatBounceScale.animateTo(1f, animationSpec = tween(180))\n        }\n    }\n\n    var hasRecordAudioPermission''')
replace_once(path,
'''                                            .fillMaxSize()\n                                            .graphicsLayer { rotationZ = vinylAngle.value }\n                                            .clip(CircleShape)''',
'''                                            .fillMaxSize()\n                                            .graphicsLayer {\n                                                rotationZ = if (vinylStyleAlbumArt) vinylAngle.value else 0f\n                                                rotationX = if (parallaxAlbumArt) parallaxPhase.value * 3.2f else 0f\n                                                rotationY = if (parallaxAlbumArt) -parallaxPhase.value * 5.2f else 0f\n                                                val motionScale = when {\n                                                    beatBounceAlbumArt -> beatBounceScale.value\n                                                    parallaxAlbumArt -> 1.025f\n                                                    else -> 1f\n                                                }\n                                                scaleX = motionScale\n                                                scaleY = motionScale\n                                                cameraDistance = 24f\n                                            }\n                                            .clip(if (vinylStyleAlbumArt) CircleShape else RoundedCornerShape(22.dp))''')

print('Album motion patch applied successfully.')
