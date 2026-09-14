from pathlib import Path


def replace_once(path, old, new):
    p = Path(path)
    s = p.read_text()
    if old not in s:
        raise SystemExit(f'Marker not found in {path}: {old[:120]!r}')
    s = s.replace(old, new, 1)
    p.write_text(s)

# SettingsRepository: persistent preference.
path = 'app/src/main/java/com/wavelength/music/data/repository/SettingsRepository.kt'
replace_once(path,
'''    val syncVolumeWithSystem: Boolean = true,
    /** When enabled in Liquid themes, the current track artwork fills the Now Playing backdrop. */
    val liquidAlbumArtBackground: Boolean = false
)''',
'''    val syncVolumeWithSystem: Boolean = true,
    /** Frosted-glass Now Playing layout inspired by translucent modern music players. */
    val glassmorphismNowPlaying: Boolean = false,
    /** When enabled in Liquid themes, the current track artwork fills the Now Playing backdrop. */
    val liquidAlbumArtBackground: Boolean = false
)''')
replace_once(path,
'''        syncVolumeWithSystem = prefs.getBoolean(KEY_SYNC_VOLUME_WITH_SYSTEM, true),
        liquidAlbumArtBackground = false
    )''',
'''        syncVolumeWithSystem = prefs.getBoolean(KEY_SYNC_VOLUME_WITH_SYSTEM, true),
        glassmorphismNowPlaying = prefs.getBoolean(KEY_GLASSMORPHISM_NOW_PLAYING, false),
        liquidAlbumArtBackground = false
    )''')
replace_once(path,
'''    fun setSyncVolumeWithSystem(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SYNC_VOLUME_WITH_SYSTEM, enabled) }
        _state.update { it.copy(syncVolumeWithSystem = enabled) }
    }

    fun setLiquidAlbumArtBackground''',
'''    fun setSyncVolumeWithSystem(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SYNC_VOLUME_WITH_SYSTEM, enabled) }
        _state.update { it.copy(syncVolumeWithSystem = enabled) }
    }

    fun setGlassmorphismNowPlaying(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_GLASSMORPHISM_NOW_PLAYING, enabled) }
        _state.update { it.copy(glassmorphismNowPlaying = enabled) }
    }

    fun setLiquidAlbumArtBackground''')
replace_once(path,
'''        const val KEY_SYNC_VOLUME_WITH_SYSTEM = "sync_volume_with_system"''',
'''        const val KEY_SYNC_VOLUME_WITH_SYSTEM = "sync_volume_with_system"
        const val KEY_GLASSMORPHISM_NOW_PLAYING = "glassmorphism_now_playing"''')

# Settings ViewModel pass-through.
path = 'app/src/main/java/com/wavelength/music/ui/settings/AppSettingsViewModel.kt'
replace_once(path,
'''    fun setSyncVolumeWithSystem(enabled: Boolean) = settingsRepository.setSyncVolumeWithSystem(enabled)

    fun setLiquidAlbumArtBackground''',
'''    fun setSyncVolumeWithSystem(enabled: Boolean) = settingsRepository.setSyncVolumeWithSystem(enabled)

    fun setGlassmorphismNowPlaying(enabled: Boolean) =
        settingsRepository.setGlassmorphismNowPlaying(enabled)

    fun setLiquidAlbumArtBackground''')

# Settings UI toggle before Smart Queue.
path = 'app/src/main/java/com/wavelength/music/ui/settings/SettingsScreen.kt'
marker = '''            item {
                SettingsSection(title = "Smart Queue") {'''
insert = '''            item {
                SettingsSection(title = "Now Playing Style") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Glassmorphism Now Playing")
                            Text(
                                text = if (settings.glassmorphismNowPlaying) {
                                    "On • frosted glass with a blurred album-art backdrop"
                                } else {
                                    "Off • use the standard Now Playing layout"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.glassmorphismNowPlaying,
                            onCheckedChange = viewModel::setGlassmorphismNowPlaying
                        )
                    }
                    Text(
                        text = "Transforms the entire Now Playing screen into a translucent frosted-glass interface while keeping playback controls, lyrics, volume and Up Next fully usable.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

''' + marker
replace_once(path, marker, insert)

# Navigation wires setting into NowPlaying.
path = 'app/src/main/java/com/wavelength/music/ui/navigation/NavGraph.kt'
replace_once(path,
'''                        syncVolumeWithSystem = settings.syncVolumeWithSystem,
                        liquidAlbumArtBackground = false''',
'''                        syncVolumeWithSystem = settings.syncVolumeWithSystem,
                        glassmorphismNowPlaying = settings.glassmorphismNowPlaying,
                        liquidAlbumArtBackground = false''')

# Now Playing: add setting parameter, backdrop and glass shell.
path = 'app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt'
replace_once(path,
'''    syncVolumeWithSystem: Boolean = true,
    liquidAlbumArtBackground: Boolean = false''',
'''    syncVolumeWithSystem: Boolean = true,
    glassmorphismNowPlaying: Boolean = false,
    liquidAlbumArtBackground: Boolean = false''')
replace_once(path,
'''    ) {
        if (isLiquid && liquidAlbumArtBackground && !track?.albumArtUrl.isNullOrBlank()) {''',
'''    ) {
        if (glassmorphismNowPlaying && !track?.albumArtUrl.isNullOrBlank()) {
            AsyncImage(
                model = track?.albumArtUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.16f
                        scaleY = 1.16f
                    }
                    .blur(34.dp)
                    .alpha(0.78f),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.06f))
            )
        }
        if (isLiquid && liquidAlbumArtBackground && !track?.albumArtUrl.isNullOrBlank()) {''')
replace_once(path,
'''                .background(
                    if (isLiquid) Color.Transparent
                    else MaterialTheme.colorScheme.background
                )
                .padding(24.dp)''',
'''                .clip(
                    if (glassmorphismNowPlaying) RoundedCornerShape(34.dp)
                    else RoundedCornerShape(0.dp)
                )
                .background(
                    when {
                        glassmorphismNowPlaying -> Color.White.copy(alpha = 0.11f)
                        isLiquid -> Color.Transparent
                        else -> MaterialTheme.colorScheme.background
                    }
                )
                .border(
                    width = if (glassmorphismNowPlaying) 1.dp else 0.dp,
                    color = if (glassmorphismNowPlaying) Color.White.copy(alpha = 0.30f) else Color.Transparent,
                    shape = if (glassmorphismNowPlaying) RoundedCornerShape(34.dp) else RoundedCornerShape(0.dp)
                )
                .padding(if (glassmorphismNowPlaying) 20.dp else 24.dp)''')

# About changelog + feature list.
path = 'app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt'
replace_once(path,
'''private val latestUpdates = listOf(
''',
'''private val latestUpdates = listOf(
    "Optional Glassmorphism Now Playing mode with a blurred album-art backdrop and frosted translucent interface",
''')
replace_once(path,
'''        "Single variable accent color shared by Solid and Liquid appearance modes",
''',
'''        "Single variable accent color shared by Solid and Liquid appearance modes",
        "Optional Glassmorphism Now Playing interface with frosted album-art backdrop",
''')

print('Glassmorphism Now Playing patch applied.')
