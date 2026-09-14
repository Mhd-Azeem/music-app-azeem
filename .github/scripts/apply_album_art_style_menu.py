from pathlib import Path
import re

ROOT = Path('.')

def read(path):
    return (ROOT / path).read_text()

def write(path, text):
    (ROOT / path).write_text(text)

def replace_once(text, old, new, label):
    if old not in text:
        raise SystemExit(f'Missing expected block: {label}')
    return text.replace(old, new, 1)

# -----------------------------------------------------------------------------
# SettingsRepository: one persisted enum instead of independent style toggles.
# -----------------------------------------------------------------------------
p = 'app/src/main/java/com/wavelength/music/data/repository/SettingsRepository.kt'
s = read(p)

wallpaper_enum = '''enum class BuiltInWallpaper(val label: String) {
    DEFAULT("AZ Music"),
    AURORA("Aurora"),
    NEON("Neon Glow"),
    SUNSET_GLOW("Sunset Glow"),
    OCEAN_SHINE("Ocean Shine"),
    PURPLE_SHINE("Purple Shine")
}
'''
album_enum = wallpaper_enum + '''

enum class AlbumArtStyle(val label: String, val description: String) {
    OFF("Off", "Static album cover"),
    VINYL("Vinyl", "Spinning record-style artwork"),
    PARALLAX("Parallax", "Follows the phone's tilt"),
    DEPTH_FLOAT("Depth Float", "Slow floating depth movement"),
    BASS_ZOOM("Bass Zoom", "Smooth zoom driven by real bass strength"),
    SPATIAL_FLOAT("Spatial Float", "Strong gyro depth with inertial movement")
}
'''
s = replace_once(s, wallpaper_enum, album_enum, 'AlbumArtStyle enum')

s = replace_once(
    s,
    '''    val vinylStyleAlbumArt: Boolean = false,\n    val parallaxAlbumArt: Boolean = false,\n    val beatBounceAlbumArt: Boolean = false,\n''',
    '''    val albumArtStyle: AlbumArtStyle = AlbumArtStyle.OFF,\n''',
    'settings state style fields'
)

s = replace_once(
    s,
    '''        vinylStyleAlbumArt = prefs.getBoolean(KEY_VINYL_STYLE, false),\n        parallaxAlbumArt = prefs.getBoolean(KEY_PARALLAX_ALBUM_ART, false),\n        beatBounceAlbumArt = prefs.getBoolean(KEY_BEAT_BOUNCE_ALBUM_ART, false),\n''',
    '''        albumArtStyle = loadAlbumArtStyle(),\n''',
    'loadState style fields'
)

marker = '    fun setIconPreset(preset: IconPreset) {'
helper = '''    private fun loadAlbumArtStyle(): AlbumArtStyle {
        val stored = prefs.getString(KEY_ALBUM_ART_STYLE, null)
        if (!stored.isNullOrBlank()) {
            return runCatching { AlbumArtStyle.valueOf(stored) }.getOrDefault(AlbumArtStyle.OFF)
        }

        // One-time migration from the old independent switches. Beat Bounce is intentionally
        // removed; users who had it enabled move to the new continuous real-bass Bass Zoom.
        return when {
            prefs.getBoolean(KEY_BEAT_BOUNCE_ALBUM_ART, false) -> AlbumArtStyle.BASS_ZOOM
            prefs.getBoolean(KEY_PARALLAX_ALBUM_ART, false) -> AlbumArtStyle.PARALLAX
            prefs.getBoolean(KEY_VINYL_STYLE, false) -> AlbumArtStyle.VINYL
            else -> AlbumArtStyle.OFF
        }
    }

'''
s = replace_once(s, marker, helper + marker, 'loadAlbumArtStyle helper')

start = s.index('    fun setVinylStyleAlbumArt(enabled: Boolean) {')
end = s.index('    fun setAiDjEnabled(enabled: Boolean) {', start)
replacement = '''    fun setAlbumArtStyle(style: AlbumArtStyle) {
        prefs.edit {
            putString(KEY_ALBUM_ART_STYLE, style.name)
            // Clear legacy switches after migration so they can never fight the enum again.
            putBoolean(KEY_VINYL_STYLE, false)
            putBoolean(KEY_PARALLAX_ALBUM_ART, false)
            putBoolean(KEY_BEAT_BOUNCE_ALBUM_ART, false)
        }
        _state.update { it.copy(albumArtStyle = style) }
    }

'''
s = s[:start] + replacement + s[end:]

s = replace_once(
    s,
    '        const val KEY_VINYL_STYLE = "vinyl_style_album_art"\n',
    '        const val KEY_ALBUM_ART_STYLE = "album_art_style"\n        const val KEY_VINYL_STYLE = "vinyl_style_album_art"\n',
    'album style preference key'
)
write(p, s)

# -----------------------------------------------------------------------------
# Settings view model.
# -----------------------------------------------------------------------------
p = 'app/src/main/java/com/wavelength/music/ui/settings/AppSettingsViewModel.kt'
s = read(p)
s = replace_once(
    s,
    'import com.wavelength.music.data.repository.AppSettingsState\n',
    'import com.wavelength.music.data.repository.AlbumArtStyle\nimport com.wavelength.music.data.repository.AppSettingsState\n',
    'viewmodel AlbumArtStyle import'
)
s = replace_once(
    s,
    '''    fun setVinylStyleAlbumArt(enabled: Boolean) = settingsRepository.setVinylStyleAlbumArt(enabled)\n\n    fun setParallaxAlbumArt(enabled: Boolean) = settingsRepository.setParallaxAlbumArt(enabled)\n\n    fun setBeatBounceAlbumArt(enabled: Boolean) = settingsRepository.setBeatBounceAlbumArt(enabled)\n''',
    '''    fun setAlbumArtStyle(style: AlbumArtStyle) = settingsRepository.setAlbumArtStyle(style)\n''',
    'viewmodel style setters'
)
write(p, s)

# -----------------------------------------------------------------------------
# Settings screen: replace switches with one popup/dropdown selector.
# -----------------------------------------------------------------------------
p = 'app/src/main/java/com/wavelength/music/ui/settings/SettingsScreen.kt'
s = read(p)
s = replace_once(
    s,
    'import androidx.compose.material3.CircularProgressIndicator\n',
    'import androidx.compose.material3.CircularProgressIndicator\nimport androidx.compose.material3.DropdownMenu\nimport androidx.compose.material3.DropdownMenuItem\n',
    'settings dropdown imports'
)
s = replace_once(
    s,
    'import com.wavelength.music.data.repository.BuiltInWallpaper\n',
    'import com.wavelength.music.data.repository.AlbumArtStyle\nimport com.wavelength.music.data.repository.BuiltInWallpaper\n',
    'settings AlbumArtStyle import'
)
s = replace_once(
    s,
    '    var showAbout by remember { mutableStateOf(false) }\n',
    '    var showAbout by remember { mutableStateOf(false) }\n    var showAlbumArtStyleMenu by remember { mutableStateOf(false) }\n',
    'settings menu state'
)

style_start = s.index('                    Row(\n                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),\n                        verticalAlignment = Alignment.CenterVertically\n                    ) {\n                        Text(\n                            text = "Spinning vinyl-style album art"')
style_end = s.index('                    Row(\n                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),\n                        verticalAlignment = Alignment.CenterVertically\n                    ) {\n                        Text(\n                            text = "Animate album art/title when the track changes"', style_start)
style_ui = '''                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Album art style",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = settings.albumArtStyle.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showAlbumArtStyleMenu = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = settings.albumArtStyle.label,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start
                                )
                                Icon(Icons.Filled.ExpandMore, contentDescription = "Choose album art style")
                            }
                            DropdownMenu(
                                expanded = showAlbumArtStyleMenu,
                                onDismissRequest = { showAlbumArtStyleMenu = false }
                            ) {
                                AlbumArtStyle.entries.forEach { style ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(style.label)
                                                Text(
                                                    style.description,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.setAlbumArtStyle(style)
                                            showAlbumArtStyleMenu = false
                                        },
                                        trailingIcon = {
                                            if (settings.albumArtStyle == style) {
                                                Icon(Icons.Filled.Check, contentDescription = null)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
'''
s = s[:style_start] + style_ui + s[style_end:]
write(p, s)

# -----------------------------------------------------------------------------
# Navigation passes the single enum to Now Playing.
# -----------------------------------------------------------------------------
p = 'app/src/main/java/com/wavelength/music/ui/navigation/NavGraph.kt'
s = read(p)
s = replace_once(
    s,
    '''                        vinylStyleAlbumArt = settings.vinylStyleAlbumArt,\n                        parallaxAlbumArt = settings.parallaxAlbumArt,\n                        beatBounceAlbumArt = settings.beatBounceAlbumArt,\n''',
    '''                        albumArtStyle = settings.albumArtStyle,\n''',
    'NavGraph style args'
)
write(p, s)

# -----------------------------------------------------------------------------
# PCM analyzer: expose a continuously-smoothed real bass level for Bass Zoom.
# -----------------------------------------------------------------------------
p = 'app/src/main/java/com/wavelength/music/playback/PcmBeatAnalyzer.kt'
s = read(p)
s = replace_once(
    s,
    '''    private val _beatPulse = MutableStateFlow(BeatPulse())\n    val beatPulse: StateFlow<BeatPulse> = _beatPulse.asStateFlow()\n''',
    '''    private val _beatPulse = MutableStateFlow(BeatPulse())\n    val beatPulse: StateFlow<BeatPulse> = _beatPulse.asStateFlow()\n\n    private val _bassLevel = MutableStateFlow(0f)\n    val bassLevel: StateFlow<Float> = _bassLevel.asStateFlow()\n''',
    'bass level flow'
)
s = replace_once(
    s,
    '''        previousFull = 0f\n        lastBeatAtMs = 0L\n''',
    '''        previousFull = 0f\n        lastBeatAtMs = 0L\n        _bassLevel.value = 0f\n''',
    'reset bass level'
)
needle = '''        val bassRise = (bassRms - previousBass).coerceAtLeast(0f)\n        val fullRise = (fullRms - previousFull).coerceAtLeast(0f)\n        val now = SystemClock.elapsedRealtime()\n'''
insert = '''        val bassRise = (bassRms - previousBass).coerceAtLeast(0f)\n        val fullRise = (fullRms - previousFull).coerceAtLeast(0f)\n        val now = SystemClock.elapsedRealtime()\n\n        // Continuous 0..1 bass intensity for Bass Zoom. Relative energy handles quiet masters;\n        // absolute energy prevents a tiny amount of bass from looking huge merely because the\n        // whole song is quiet. Smooth attack/release avoids jitter while preserving real dynamics.\n        val relativeBass = ((bassRatio - 0.82f) / 0.95f).coerceIn(0f, 1f)\n        val absoluteBass = ((bassRms - 0.006f) / 0.085f).coerceIn(0f, 1f)\n        val measuredBass = (relativeBass * 0.68f + absoluteBass * 0.32f).coerceIn(0f, 1f)\n        val previousLevel = _bassLevel.value\n        val smoothing = if (measuredBass > previousLevel) 0.38f else 0.16f\n        _bassLevel.value = previousLevel + (measuredBass - previousLevel) * smoothing\n'''
s = replace_once(s, needle, insert, 'continuous bass level calculation')
write(p, s)

# -----------------------------------------------------------------------------
# PlayerViewModel exposes continuous bass level.
# -----------------------------------------------------------------------------
p = 'app/src/main/java/com/wavelength/music/ui/nowplaying/PlayerViewModel.kt'
s = read(p)
s = replace_once(
    s,
    '''    val pcmBeatPulse: StateFlow<PcmBeatAnalyzer.BeatPulse> = pcmBeatAnalyzer.beatPulse\n''',
    '''    val pcmBeatPulse: StateFlow<PcmBeatAnalyzer.BeatPulse> = pcmBeatAnalyzer.beatPulse\n    val pcmBassLevel: StateFlow<Float> = pcmBeatAnalyzer.bassLevel\n''',
    'PlayerViewModel bass level'
)
write(p, s)

# -----------------------------------------------------------------------------
# Now Playing: six mutually-exclusive styles. Beat Bounce is removed.
# -----------------------------------------------------------------------------
p = 'app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt'
s = read(p)
s = replace_once(
    s,
    'import androidx.compose.animation.core.Animatable\n',
    'import androidx.compose.animation.core.Animatable\nimport androidx.compose.animation.core.animateFloatAsState\n',
    'animateFloatAsState import'
)
s = replace_once(
    s,
    'import com.wavelength.music.data.model.LyricLine\n',
    'import com.wavelength.music.data.model.LyricLine\nimport com.wavelength.music.data.repository.AlbumArtStyle\n',
    'NowPlaying AlbumArtStyle import'
)
s = replace_once(
    s,
    '''    vinylStyleAlbumArt: Boolean = false,\n    parallaxAlbumArt: Boolean = false,\n    beatBounceAlbumArt: Boolean = false,\n''',
    '''    albumArtStyle: AlbumArtStyle = AlbumArtStyle.OFF,\n''',
    'NowPlaying style signature'
)

transition_marker = '''    val trackTransitionSpec: FiniteAnimationSpec<Float> = if (trackTransitionEnabled) {\n        tween(trackTransitionDurationMs)\n    } else {\n        snap()\n    }\n'''
style_locals = transition_marker + '''    val vinylStyleAlbumArt = albumArtStyle == AlbumArtStyle.VINYL\n    val parallaxAlbumArt = albumArtStyle == AlbumArtStyle.PARALLAX\n    val depthFloatAlbumArt = albumArtStyle == AlbumArtStyle.DEPTH_FLOAT\n    val bassZoomAlbumArt = albumArtStyle == AlbumArtStyle.BASS_ZOOM\n    val spatialFloatAlbumArt = albumArtStyle == AlbumArtStyle.SPATIAL_FLOAT\n    val sensorMotionEnabled = parallaxAlbumArt || spatialFloatAlbumArt\n'''
s = replace_once(s, transition_marker, style_locals, 'NowPlaying local style flags')

s = s.replace('DisposableEffect(parallaxAlbumArt, context)', 'DisposableEffect(sensorMotionEnabled, spatialFloatAlbumArt, context)')
s = s.replace('if (!parallaxAlbumArt) return', 'if (!sensorMotionEnabled) return')
s = s.replace('if (parallaxAlbumArt && motionSensor != null)', 'if (sensorMotionEnabled && motionSensor != null)')
s = s.replace('if (parallaxAlbumArt && gyroSensor != null)', 'if (spatialFloatAlbumArt && gyroSensor != null)')

beat_start = s.index('    // Beat Bounce is driven directly by decoded PCM from ExoPlayer')
beat_end = s.index('    // RECORD_AUDIO is now needed only for the optional waveform visualizer UI.', beat_start)
new_motion = '''    // Depth Float is intentionally ambient rather than beat-driven: a slow forward/backward\n    // movement gives the artwork a calm floating-card feel.\n    val depthFloatPhase = remember { Animatable(0f) }\n    LaunchedEffect(depthFloatAlbumArt, state.isPlaying) {\n        if (depthFloatAlbumArt && state.isPlaying) {\n            while (true) {\n                depthFloatPhase.animateTo(1f, animationSpec = tween(2400, easing = LinearEasing))\n                depthFloatPhase.animateTo(-1f, animationSpec = tween(2400, easing = LinearEasing))\n            }\n        } else {\n            depthFloatPhase.animateTo(0f, animationSpec = tween(280))\n        }\n    }\n\n    // Bass Zoom uses the continuous decoded-PCM bass level, not the old Beat Bounce onset event.\n    // Quiet/low-bass sections therefore stay almost still while strong bass smoothly zooms in.\n    val pcmBassLevel by viewModel.pcmBassLevel.collectAsStateWithLifecycle()\n    val bassZoomScale by animateFloatAsState(\n        targetValue = if (bassZoomAlbumArt && state.isPlaying) {\n            1f + pcmBassLevel.coerceIn(0f, 1f) * 0.115f\n        } else {\n            1f\n        },\n        animationSpec = spring(\n            dampingRatio = 0.74f,\n            stiffness = Spring.StiffnessMedium\n        ),\n        label = "bassZoomScale"\n    )\n\n'''
s = s[:beat_start] + new_motion + s[beat_end:]

old_layer = '''                                                val spatialX = (sensorTiltX + sensorGyroX * 0.45f).coerceIn(-1.35f, 1.35f)\n                                                val spatialY = (sensorTiltY + sensorGyroY * 0.45f).coerceIn(-1.35f, 1.35f)\n                                                rotationX = if (parallaxAlbumArt) spatialY * 11f else 0f\n                                                rotationY = if (parallaxAlbumArt) -spatialX * 14f else 0f\n                                                translationX = if (parallaxAlbumArt) spatialX * 28f else 0f\n                                                translationY = if (parallaxAlbumArt) spatialY * 22f else 0f\n                                                val motionScale = when {\n                                                    beatBounceAlbumArt -> beatBounceScale.value\n                                                    parallaxAlbumArt -> 0.90f\n                                                    else -> 1f\n                                                }\n                                                scaleX = motionScale\n                                                scaleY = motionScale\n                                                cameraDistance = 24f\n'''
new_layer = '''                                                val spatialX = (sensorTiltX + sensorGyroX * 0.58f).coerceIn(-1.4f, 1.4f)\n                                                val spatialY = (sensorTiltY + sensorGyroY * 0.58f).coerceIn(-1.4f, 1.4f)\n                                                when {\n                                                    spatialFloatAlbumArt -> {\n                                                        rotationX = spatialY * 12f\n                                                        rotationY = -spatialX * 15f\n                                                        translationX = spatialX * 30f\n                                                        translationY = spatialY * 24f\n                                                    }\n                                                    parallaxAlbumArt -> {\n                                                        rotationX = sensorTiltY * 7.5f\n                                                        rotationY = -sensorTiltX * 9.5f\n                                                        translationX = sensorTiltX * 18f\n                                                        translationY = sensorTiltY * 14f\n                                                    }\n                                                    depthFloatAlbumArt -> {\n                                                        rotationX = depthFloatPhase.value * 1.4f\n                                                        translationY = -depthFloatPhase.value * 9f\n                                                        shadowElevation = 14f + depthFloatPhase.value * 4f\n                                                    }\n                                                }\n                                                val motionScale = when {\n                                                    bassZoomAlbumArt -> bassZoomScale\n                                                    depthFloatAlbumArt -> 1f + depthFloatPhase.value * 0.018f\n                                                    spatialFloatAlbumArt -> 0.88f\n                                                    parallaxAlbumArt -> 0.93f\n                                                    else -> 1f\n                                                }\n                                                scaleX = motionScale\n                                                scaleY = motionScale\n                                                cameraDistance = if (spatialFloatAlbumArt) 18f else 24f\n'''
count = s.count(old_layer)
if count != 1:
    raise SystemExit(f'Expected first graphics layer once, found {count}')
s = s.replace(old_layer, new_layer, 1)

old_layer2 = '''                                            val spatialX = (sensorTiltX + sensorGyroX * 0.45f).coerceIn(-1.35f, 1.35f)\n                                            val spatialY = (sensorTiltY + sensorGyroY * 0.45f).coerceIn(-1.35f, 1.35f)\n                                            rotationX = if (parallaxAlbumArt) spatialY * 11f else 0f\n                                            rotationY = if (parallaxAlbumArt) -spatialX * 14f else 0f\n                                            translationX = if (parallaxAlbumArt) spatialX * 28f else 0f\n                                            translationY = if (parallaxAlbumArt) spatialY * 22f else 0f\n                                            val motionScale = when {\n                                                beatBounceAlbumArt -> beatBounceScale.value\n                                                parallaxAlbumArt -> 0.90f\n                                                else -> 1f\n                                            }\n                                            scaleX = motionScale\n                                            scaleY = motionScale\n                                            cameraDistance = 24f\n'''
new_layer2 = '''                                            val spatialX = (sensorTiltX + sensorGyroX * 0.58f).coerceIn(-1.4f, 1.4f)\n                                            val spatialY = (sensorTiltY + sensorGyroY * 0.58f).coerceIn(-1.4f, 1.4f)\n                                            when {\n                                                spatialFloatAlbumArt -> {\n                                                    rotationX = spatialY * 12f\n                                                    rotationY = -spatialX * 15f\n                                                    translationX = spatialX * 30f\n                                                    translationY = spatialY * 24f\n                                                }\n                                                parallaxAlbumArt -> {\n                                                    rotationX = sensorTiltY * 7.5f\n                                                    rotationY = -sensorTiltX * 9.5f\n                                                    translationX = sensorTiltX * 18f\n                                                    translationY = sensorTiltY * 14f\n                                                }\n                                                depthFloatAlbumArt -> {\n                                                    rotationX = depthFloatPhase.value * 1.4f\n                                                    translationY = -depthFloatPhase.value * 9f\n                                                    shadowElevation = 14f + depthFloatPhase.value * 4f\n                                                }\n                                            }\n                                            val motionScale = when {\n                                                bassZoomAlbumArt -> bassZoomScale\n                                                depthFloatAlbumArt -> 1f + depthFloatPhase.value * 0.018f\n                                                spatialFloatAlbumArt -> 0.88f\n                                                parallaxAlbumArt -> 0.93f\n                                                else -> 1f\n                                            }\n                                            scaleX = motionScale\n                                            scaleY = motionScale\n                                            cameraDistance = if (spatialFloatAlbumArt) 18f else 24f\n'''
count = s.count(old_layer2)
if count != 1:
    raise SystemExit(f'Expected second graphics layer once, found {count}')
s = s.replace(old_layer2, new_layer2, 1)

# Ensure old Beat Bounce is fully gone from the screen.
if 'beatBounceAlbumArt' in s or 'beatBounceScale' in s:
    raise SystemExit('Old Beat Bounce references still remain in NowPlayingScreen')
write(p, s)

# -----------------------------------------------------------------------------
# Backups: store new enum, keep old vinyl field optional for older backup compatibility.
# -----------------------------------------------------------------------------
p = 'app/src/main/java/com/wavelength/music/data/backup/BackupModels.kt'
s = read(p)
s = replace_once(
    s,
    '''    val dynamicThemeFromAlbumArt: Boolean,\n    val vinylStyleAlbumArt: Boolean,\n    val aiDjEnabled: Boolean,\n''',
    '''    val dynamicThemeFromAlbumArt: Boolean,\n    val albumArtStyle: String? = null,\n    val vinylStyleAlbumArt: Boolean = false,\n    val aiDjEnabled: Boolean,\n''',
    'backup settings style model'
)
write(p, s)

p = 'app/src/main/java/com/wavelength/music/data/repository/BackupRepository.kt'
s = read(p)
s = replace_once(
    s,
    '''                dynamicThemeFromAlbumArt = settingsState.dynamicThemeFromAlbumArt,\n                vinylStyleAlbumArt = settingsState.vinylStyleAlbumArt,\n                aiDjEnabled = settingsState.aiDjEnabled,\n''',
    '''                dynamicThemeFromAlbumArt = settingsState.dynamicThemeFromAlbumArt,\n                albumArtStyle = settingsState.albumArtStyle.name,\n                vinylStyleAlbumArt = settingsState.albumArtStyle == AlbumArtStyle.VINYL,\n                aiDjEnabled = settingsState.aiDjEnabled,\n''',
    'backup export style'
)
s = replace_once(
    s,
    '''        settingsRepository.setDynamicThemeFromAlbumArt(settings.dynamicThemeFromAlbumArt)\n        settingsRepository.setVinylStyleAlbumArt(settings.vinylStyleAlbumArt)\n        settingsRepository.setAiDjEnabled(settings.aiDjEnabled)\n''',
    '''        settingsRepository.setDynamicThemeFromAlbumArt(settings.dynamicThemeFromAlbumArt)\n        val restoredStyle = settings.albumArtStyle\n            ?.let { runCatching { AlbumArtStyle.valueOf(it) }.getOrNull() }\n            ?: if (settings.vinylStyleAlbumArt) AlbumArtStyle.VINYL else AlbumArtStyle.OFF\n        settingsRepository.setAlbumArtStyle(restoredStyle)\n        settingsRepository.setAiDjEnabled(settings.aiDjEnabled)\n''',
    'backup restore style'
)
write(p, s)

print('Album art style menu migration applied successfully.')
