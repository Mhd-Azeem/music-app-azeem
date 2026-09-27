from pathlib import Path

np = Path("app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt")
ns = np.read_text()

old_shuffle = '''                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 29.dp, top = 4.dp)
                                        .size(UiDesignConfig.GLASS_ORB_SIZE_DP.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.28f),
                                                    Color(0xFFA7C9EA).copy(alpha = 0.30f)
                                                )
                                            )
                                        )
                                        .border(1.2.dp, Color.White.copy(alpha = 0.32f), CircleShape)
                                        .clickable(onClick = viewModel::toggleShuffle),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = if (state.shuffleEnabled) Color.White else Color.White.copy(alpha = 0.78f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
'''

new_shuffle = '''                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 29.dp, top = 4.dp)
                                        .size(UiDesignConfig.GLASS_ORB_SIZE_DP.dp)
                                        .shadow(
                                            elevation = if (state.shuffleEnabled) 14.dp else 3.dp,
                                            shape = CircleShape,
                                            ambientColor = if (state.shuffleEnabled) glassPlayedGlowColor.copy(alpha = 0.70f) else Color.Transparent,
                                            spotColor = if (state.shuffleEnabled) glassPlayedGlowColor.copy(alpha = 0.85f) else Color.Transparent
                                        )
                                        .clip(CircleShape)
                                        .background(
                                            if (state.shuffleEnabled) {
                                                Brush.radialGradient(
                                                    listOf(
                                                        glassPlayedGlowColor.copy(alpha = 0.95f),
                                                        accentColor.copy(alpha = 0.82f),
                                                        Color(0xFF6D5BFF).copy(alpha = 0.78f)
                                                    )
                                                )
                                            } else {
                                                Brush.radialGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = 0.14f),
                                                        Color(0xFFA7C9EA).copy(alpha = 0.16f)
                                                    )
                                                )
                                            }
                                        )
                                        .border(
                                            if (state.shuffleEnabled) 2.dp else 1.1.dp,
                                            if (state.shuffleEnabled) Color.White.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.24f),
                                            CircleShape
                                        )
                                        .clickable(onClick = viewModel::toggleShuffle),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Shuffle,
                                        contentDescription = if (state.shuffleEnabled) "Shuffle on" else "Shuffle off",
                                        tint = if (state.shuffleEnabled) Color.White else Color.White.copy(alpha = 0.48f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 5.dp)
                                            .size(if (state.shuffleEnabled) 6.dp else 4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (state.shuffleEnabled) Color.White
                                                else Color.White.copy(alpha = 0.26f)
                                            )
                                    )
                                }
'''
assert old_shuffle in ns, "Glass Shuffle control block not found"
ns = ns.replace(old_shuffle, new_shuffle, 1)

old_repeat = '''                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(end = 29.dp, top = 4.dp)
                                        .size(UiDesignConfig.GLASS_ORB_SIZE_DP.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.28f),
                                                    Color(0xFFA7C9EA).copy(alpha = 0.30f)
                                                )
                                            )
                                        )
                                        .border(1.2.dp, Color.White.copy(alpha = 0.32f), CircleShape)
                                        .clickable(onClick = viewModel::cycleRepeatMode),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                        contentDescription = "Repeat",
                                        tint = if (state.repeatMode != RepeatMode.OFF) Color.White else Color.White.copy(alpha = 0.78f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
'''

new_repeat = '''                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(end = 29.dp, top = 4.dp)
                                        .size(UiDesignConfig.GLASS_ORB_SIZE_DP.dp)
                                        .shadow(
                                            elevation = if (state.repeatMode != RepeatMode.OFF) 14.dp else 3.dp,
                                            shape = CircleShape,
                                            ambientColor = if (state.repeatMode != RepeatMode.OFF) glassPlayedGlowColor.copy(alpha = 0.70f) else Color.Transparent,
                                            spotColor = if (state.repeatMode != RepeatMode.OFF) glassPlayedGlowColor.copy(alpha = 0.85f) else Color.Transparent
                                        )
                                        .clip(CircleShape)
                                        .background(
                                            if (state.repeatMode != RepeatMode.OFF) {
                                                Brush.radialGradient(
                                                    listOf(
                                                        glassPlayedGlowColor.copy(alpha = 0.95f),
                                                        accentColor.copy(alpha = 0.82f),
                                                        Color(0xFF6D5BFF).copy(alpha = 0.78f)
                                                    )
                                                )
                                            } else {
                                                Brush.radialGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = 0.14f),
                                                        Color(0xFFA7C9EA).copy(alpha = 0.16f)
                                                    )
                                                )
                                            }
                                        )
                                        .border(
                                            if (state.repeatMode != RepeatMode.OFF) 2.dp else 1.1.dp,
                                            if (state.repeatMode != RepeatMode.OFF) Color.White.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.24f),
                                            CircleShape
                                        )
                                        .clickable(onClick = viewModel::cycleRepeatMode),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                        contentDescription = when (state.repeatMode) {
                                            RepeatMode.ONE -> "Repeat one"
                                            RepeatMode.ALL -> "Repeat all"
                                            else -> "Repeat off"
                                        },
                                        tint = if (state.repeatMode != RepeatMode.OFF) Color.White else Color.White.copy(alpha = 0.48f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 5.dp)
                                            .size(if (state.repeatMode != RepeatMode.OFF) 6.dp else 4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (state.repeatMode != RepeatMode.OFF) Color.White
                                                else Color.White.copy(alpha = 0.26f)
                                            )
                                    )
                                }
'''
assert old_repeat in ns, "Glass Repeat control block not found"
ns = ns.replace(old_repeat, new_repeat, 1)
np.write_text(ns)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Made Glass Shuffle and Repeat states clearly visible: active controls now glow cyan-purple with a bright rim and status dot, while inactive controls are strongly dimmed",\n'
assert needle in a, "About changelog anchor not found"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)


# Add a shared Shuffle button to every TrackListScreen, including language pages and user playlists.
tls = Path("app/src/main/java/com/wavelength/music/ui/components/TrackListScreen.kt")
t = tls.read_text()

if "import androidx.compose.material.icons.filled.Shuffle\n" not in t:
    t = t.replace(
        "import androidx.compose.material.icons.filled.PlayArrow\n",
        "import androidx.compose.material.icons.filled.PlayArrow\nimport androidx.compose.material.icons.filled.Shuffle\n",
        1
    )

t = t.replace(
    '''    onRetry: () -> Unit,
    onPlayAll: () -> Unit,
    onTrackClick: (Int) -> Unit,
''',
    '''    onRetry: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onTrackClick: (Int) -> Unit,
''',
    1
)

old_button = '''                        Button(
                            onClick = onPlayAll,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Text(
                                text = stringResource(R.string.play_all),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
'''
new_button = '''                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(onClick = onPlayAll) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Text(
                                    text = stringResource(R.string.play_all),
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            Button(onClick = onShuffle) {
                                Icon(Icons.Filled.Shuffle, contentDescription = null)
                                Text(
                                    text = "Shuffle",
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
'''
assert old_button in t, "TrackListScreen Play All button block not found"
t = t.replace(old_button, new_button, 1)
tls.write_text(t)

# Language/genre pages.
g = Path("app/src/main/java/com/wavelength/music/ui/home/GenreScreen.kt")
gs = g.read_text()
needle = '''        onPlayAll = {
            viewModel.playAll()
            onTrackClick()
        },
        onTrackClick = { index ->
'''
replacement = '''        onPlayAll = {
            viewModel.playAll()
            onTrackClick()
        },
        onShuffle = {
            viewModel.shuffleAll()
            onTrackClick()
        },
        onTrackClick = { index ->
'''
assert needle in gs, "GenreScreen Play All anchor not found"
gs = gs.replace(needle, replacement, 1)
g.write_text(gs)

gvm = Path("app/src/main/java/com/wavelength/music/ui/home/GenreViewModel.kt")
gv = gvm.read_text()
anchor = '''    fun playAll() {
        val current = _tracks.value
        if (current is ScreenState.Success) {
            playerController.playQueue(current.data, 0)
        }
    }
'''
insert = anchor + '''
    fun shuffleAll() {
        val current = _tracks.value
        if (current is ScreenState.Success && current.data.isNotEmpty()) {
            playerController.playQueue(current.data.shuffled(), 0)
        }
    }
'''
assert anchor in gv, "GenreViewModel playAll anchor not found"
gv = gv.replace(anchor, insert, 1)
gvm.write_text(gv)

# User playlists.
pl = Path("app/src/main/java/com/wavelength/music/ui/playlist/PlaylistDetailScreen.kt")
pls = pl.read_text()
needle = '''        onPlayAll = {
            viewModel.playAll()
            onTrackClick()
        },
        onTrackClick = { index ->
'''
replacement = '''        onPlayAll = {
            viewModel.playAll()
            onTrackClick()
        },
        onShuffle = {
            viewModel.shuffleAll()
            onTrackClick()
        },
        onTrackClick = { index ->
'''
assert needle in pls, "PlaylistDetailScreen Play All anchor not found"
pls = pls.replace(needle, replacement, 1)
pl.write_text(pls)

pvm = Path("app/src/main/java/com/wavelength/music/ui/playlist/PlaylistDetailViewModel.kt")
pvs = pvm.read_text()
anchor = '''    fun playAll() {
        val current = tracks.value
        if (current is ScreenState.Success) {
            playerController.playQueue(current.data, 0)
        }
    }
'''
insert = anchor + '''
    fun shuffleAll() {
        val current = tracks.value
        if (current is ScreenState.Success && current.data.isNotEmpty()) {
            playerController.playQueue(current.data.shuffled(), 0)
        }
    }
'''
assert anchor in pvs, "PlaylistDetailViewModel playAll anchor not found"
pvs = pvs.replace(anchor, insert, 1)
pvm.write_text(pvs)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Added a Shuffle button beside Play All on every playlist-style page, including language pages and user-created playlists",\n'
assert needle in a, "About changelog anchor not found for playlist shuffle"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)


# Expose "Hide in this playlist" on dynamic language/genre pages too.
g = Path("app/src/main/java/com/wavelength/music/ui/home/GenreScreen.kt")
gs = g.read_text()
needle = '''        emptyMessage = stringResource(R.string.search_empty_hint),
        onLoadMore = viewModel::loadMore,
        isLoadingMore = isLoadingMore
'''
replacement = '''        emptyMessage = stringResource(R.string.search_empty_hint),
        onLoadMore = viewModel::loadMore,
        isLoadingMore = isLoadingMore,
        onRemoveFromPlaylist = viewModel::hideTrack
'''
assert needle in gs, "GenreScreen list tail anchor not found"
gs = gs.replace(needle, replacement, 1)
g.write_text(gs)

gvm = Path("app/src/main/java/com/wavelength/music/ui/home/GenreViewModel.kt")
gv = gvm.read_text()

# Keep hidden IDs for the lifetime of the language page so pagination cannot add them back.
anchor = '''    private var hasMore = true
    private val pageSize = 30
'''
replacement = '''    private var hasMore = true
    private val pageSize = 30
    private val hiddenTrackIds = mutableSetOf<String>()
'''
assert anchor in gv, "GenreViewModel paging state anchor not found"
gv = gv.replace(anchor, replacement, 1)

load_unique = '''                    val unique = list.distinctBy { it.id }
                    _tracks.value = if (unique.isEmpty()) ScreenState.Empty else ScreenState.Success(unique)
'''
load_unique_new = '''                    val unique = list
                        .distinctBy { it.id }
                        .filterNot { it.id in hiddenTrackIds }
                    _tracks.value = if (unique.isEmpty()) ScreenState.Empty else ScreenState.Success(unique)
'''
assert load_unique in gv, "GenreViewModel initial list block not found"
gv = gv.replace(load_unique, load_unique_new, 1)

incoming = '''                    val newTracks = incoming.filterNot { it.id in existingIds }
'''
incoming_new = '''                    val newTracks = incoming.filterNot {
                        it.id in existingIds || it.id in hiddenTrackIds
                    }
'''
assert incoming in gv, "GenreViewModel loadMore filter not found"
gv = gv.replace(incoming, incoming_new, 1)

play_anchor = '''    fun playAll() {
'''
hide_fun = '''    fun hideTrack(track: Track) {
        hiddenTrackIds += track.id
        val current = _tracks.value
        if (current is ScreenState.Success) {
            val visible = current.data.filterNot { it.id == track.id }
            _tracks.value = if (visible.isEmpty()) ScreenState.Empty else ScreenState.Success(visible)
        }
    }

'''
assert play_anchor in gv, "GenreViewModel playAll marker not found"
gv = gv.replace(play_anchor, hide_fun + play_anchor, 1)
gvm.write_text(gv)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Added Hide in this playlist to language/genre pages as well as user playlists; hidden language-page songs stay removed while browsing and are filtered out of later pagination",\n'
assert needle in a, "About changelog anchor not found for language hide"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)


# === Unified seven-theme system + animated transitions ===
repo = Path("app/src/main/java/com/wavelength/music/data/repository/SettingsRepository.kt")
s = repo.read_text()

if "enum class VisualThemeMode" not in s:
    insert_after = '''enum class BuiltInWallpaper(val label: String) {
    DEFAULT("AZ Music"),
    AURORA("Aurora"),
    NEON("Neon Glow"),
    SUNSET_GLOW("Sunset Glow"),
    OCEAN_SHINE("Ocean Shine"),
    PURPLE_SHINE("Purple Shine")
}


'''
    theme_enum = '''enum class VisualThemeMode(val label: String, val description: String) {
    SOLID("Solid", "Clean solid interface"),
    LIQUID("Liquid", "Translucent liquid surfaces"),
    GLASSMORPHISM("Glassmorphism", "Frosted glass across the full app"),
    NEOMORPHISM("Neomorphism", "Soft raised and inset surfaces"),
    AMOLED("AMOLED", "True black OLED-friendly interface"),
    ALBUM_ADAPTIVE("Album Adaptive", "Colors follow the current album artwork"),
    AURORA("Aurora", "Animated cyan, violet and blue atmosphere")
}


'''
    assert insert_after in s, "BuiltInWallpaper enum anchor not found"
    s = s.replace(insert_after, insert_after + theme_enum, 1)

s = s.replace(
    '''    val theme: AppTheme = AppTheme.CLASSIC,
''',
    '''    val theme: AppTheme = AppTheme.CLASSIC,
    val visualThemeMode: VisualThemeMode = VisualThemeMode.SOLID,
    val animateThemeTransitions: Boolean = true,
''',
    1
)

s = s.replace(
    '''        theme = runCatching {
            AppTheme.valueOf(prefs.getString(KEY_THEME, null) ?: AppTheme.CLASSIC.name)
        }.getOrDefault(AppTheme.CLASSIC),
''',
    '''        theme = runCatching {
            AppTheme.valueOf(prefs.getString(KEY_THEME, null) ?: AppTheme.CLASSIC.name)
        }.getOrDefault(AppTheme.CLASSIC),
        visualThemeMode = runCatching {
            VisualThemeMode.valueOf(
                prefs.getString(KEY_VISUAL_THEME_MODE, null)
                    ?: when {
                        prefs.getBoolean(KEY_GLASSMORPHISM_NOW_PLAYING, false) -> VisualThemeMode.GLASSMORPHISM.name
                        prefs.getBoolean(KEY_NEOMORPHISM_ENABLED, false) -> VisualThemeMode.NEOMORPHISM.name
                        runCatching {
                            AppTheme.valueOf(prefs.getString(KEY_THEME, null) ?: AppTheme.CLASSIC.name)
                        }.getOrDefault(AppTheme.CLASSIC).isGlass -> VisualThemeMode.LIQUID.name
                        else -> VisualThemeMode.SOLID.name
                    }
            )
        }.getOrDefault(VisualThemeMode.SOLID),
        animateThemeTransitions = prefs.getBoolean(KEY_ANIMATE_THEME_TRANSITIONS, true),
''',
    1
)

old_set_theme = '''    fun setTheme(theme: AppTheme) {
        prefs.edit { putString(KEY_THEME, theme.name) }
        _state.update { it.copy(theme = theme) }
    }
'''
new_set_theme = '''    fun setTheme(theme: AppTheme) {
        prefs.edit { putString(KEY_THEME, theme.name) }
        _state.update { it.copy(theme = theme) }
    }

    fun setVisualThemeMode(mode: VisualThemeMode) {
        val mappedTheme = when (mode) {
            VisualThemeMode.LIQUID, VisualThemeMode.GLASSMORPHISM -> AppTheme.LIQUID
            VisualThemeMode.AMOLED -> AppTheme.BLACK
            else -> AppTheme.CLASSIC
        }
        val glass = mode == VisualThemeMode.GLASSMORPHISM
        val neo = mode == VisualThemeMode.NEOMORPHISM
        val adaptive = mode == VisualThemeMode.ALBUM_ADAPTIVE
        prefs.edit {
            putString(KEY_VISUAL_THEME_MODE, mode.name)
            putString(KEY_THEME, mappedTheme.name)
            putBoolean(KEY_GLASSMORPHISM_NOW_PLAYING, glass)
            putBoolean(KEY_NEOMORPHISM_ENABLED, neo)
            putBoolean(KEY_DYNAMIC_THEME, adaptive)
        }
        _state.update {
            it.copy(
                visualThemeMode = mode,
                theme = mappedTheme,
                glassmorphismNowPlaying = glass,
                neomorphismEnabled = neo,
                dynamicThemeFromAlbumArt = adaptive
            )
        }
    }

    fun setAnimateThemeTransitions(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ANIMATE_THEME_TRANSITIONS, enabled) }
        _state.update { it.copy(animateThemeTransitions = enabled) }
    }
'''
assert old_set_theme in s, "setTheme block not found"
s = s.replace(old_set_theme, new_set_theme, 1)

# Keep legacy toggles synchronized with unified mode.
s = s.replace(
    '''                neomorphismEnabled = enabled,
                glassmorphismNowPlaying = if (enabled) false else it.glassmorphismNowPlaying
''',
    '''                neomorphismEnabled = enabled,
                glassmorphismNowPlaying = if (enabled) false else it.glassmorphismNowPlaying,
                visualThemeMode = if (enabled) VisualThemeMode.NEOMORPHISM else VisualThemeMode.SOLID
''',
    1
)
s = s.replace(
    '''                glassmorphismNowPlaying = enabled,
                neomorphismEnabled = if (enabled) false else it.neomorphismEnabled
''',
    '''                glassmorphismNowPlaying = enabled,
                neomorphismEnabled = if (enabled) false else it.neomorphismEnabled,
                visualThemeMode = if (enabled) VisualThemeMode.GLASSMORPHISM else VisualThemeMode.SOLID
''',
    1
)

s = s.replace(
    '''        const val KEY_THEME = "theme"
''',
    '''        const val KEY_THEME = "theme"
        const val KEY_VISUAL_THEME_MODE = "visual_theme_mode"
        const val KEY_ANIMATE_THEME_TRANSITIONS = "animate_theme_transitions"
''',
    1
)
repo.write_text(s)

# ViewModel API.
vm = Path("app/src/main/java/com/wavelength/music/ui/settings/AppSettingsViewModel.kt")
v = vm.read_text()
if "import com.wavelength.music.data.repository.VisualThemeMode\n" not in v:
    v = v.replace(
        "import com.wavelength.music.data.repository.SettingsRepository\n",
        "import com.wavelength.music.data.repository.SettingsRepository\nimport com.wavelength.music.data.repository.VisualThemeMode\n",
        1
    )
v = v.replace(
    '''    fun selectTheme(theme: AppTheme) = settingsRepository.setTheme(theme)
''',
    '''    fun selectTheme(theme: AppTheme) = settingsRepository.setTheme(theme)

    fun setVisualThemeMode(mode: VisualThemeMode) = settingsRepository.setVisualThemeMode(mode)

    fun setAnimateThemeTransitions(enabled: Boolean) =
        settingsRepository.setAnimateThemeTransitions(enabled)
''',
    1
)
vm.write_text(v)

# Replace two-choice Theme Style with the requested seven-theme selector + transition toggle.
screen = Path("app/src/main/java/com/wavelength/music/ui/settings/SettingsScreen.kt")
ss = screen.read_text()
if "import com.wavelength.music.data.repository.VisualThemeMode\n" not in ss:
    # Add near other repository imports if available.
    marker = "import com.wavelength.music.data.repository.BuiltInWallpaper\n"
    if marker in ss:
        ss = ss.replace(marker, marker + "import com.wavelength.music.data.repository.VisualThemeMode\n", 1)
    else:
        pkg = "package com.wavelength.music.ui.settings\n"
        ss = ss.replace(pkg, pkg + "\nimport com.wavelength.music.data.repository.VisualThemeMode\n", 1)

start = ss.index('''            item {
                SettingsSection(title = "Theme Style") {
''')
end = ss.index('''            item {
                SettingsSection(title = "Now Playing") {
''', start)
new_section = '''            item {
                SettingsSection(title = "Theme Style") {
                    Text(
                        text = "Choose the complete visual style for AzMusic.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )

                    VisualThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.setVisualThemeMode(mode) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.visualThemeMode == mode,
                                onClick = { viewModel.setVisualThemeMode(mode) }
                            )
                            Column(modifier = Modifier.padding(start = 6.dp).weight(1f)) {
                                Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    mode.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Animate theme transitions")
                            Text(
                                "Crossfade theme and accent colors instead of changing instantly",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.animateThemeTransitions,
                            onCheckedChange = viewModel::setAnimateThemeTransitions
                        )
                    }
                }
            }

'''
ss = ss[:start] + new_section + ss[end:]
screen.write_text(ss)

# Theme engine: AMOLED support + animated Material colors.
theme = Path("app/src/main/java/com/wavelength/music/ui/theme/Theme.kt")
ts = theme.read_text()
if "import androidx.compose.animation.core.animateColorAsState\n" not in ts:
    ts = ts.replace(
        "package com.wavelength.music.ui.theme\n\n",
        "package com.wavelength.music.ui.theme\n\nimport androidx.compose.animation.core.animateColorAsState\nimport androidx.compose.animation.core.tween\n",
        1
    )
ts = ts.replace(
    '''    customLiquidAccent: Color? = null,
    glassmorphismEnabled: Boolean = false,
    neomorphismEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
''',
    '''    customLiquidAccent: Color? = null,
    glassmorphismEnabled: Boolean = false,
    neomorphismEnabled: Boolean = false,
    amoledEnabled: Boolean = false,
    animateTransitions: Boolean = true,
    content: @Composable () -> Unit
) {
''',
    1
)

old_tail = '''    } else {
        baseScheme
    }
    MaterialTheme(
        colorScheme = appScheme,
        typography = WavelengthTypography,
        content = content
    )
}
'''
new_tail = '''    } else if (amoledEnabled) {
        baseScheme.copy(
            background = Color.Transparent,
            surface = Color.Black,
            surfaceVariant = Color(0xFF090909),
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainer = Color(0xFF050505),
            surfaceContainerHigh = Color(0xFF0B0B0B),
            surfaceContainerHighest = Color(0xFF111111),
            outline = Color.White.copy(alpha = 0.24f),
            outlineVariant = Color.White.copy(alpha = 0.10f),
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFCECECE)
        )
    } else {
        baseScheme
    }

    val duration = if (animateTransitions) 420 else 0
    val animatedScheme = appScheme.copy(
        primary = animateColorAsState(appScheme.primary, tween(duration), label = "themePrimary").value,
        secondary = animateColorAsState(appScheme.secondary, tween(duration), label = "themeSecondary").value,
        surface = animateColorAsState(appScheme.surface, tween(duration), label = "themeSurface").value,
        surfaceVariant = animateColorAsState(appScheme.surfaceVariant, tween(duration), label = "themeSurfaceVariant").value,
        surfaceContainer = animateColorAsState(appScheme.surfaceContainer, tween(duration), label = "themeSurfaceContainer").value,
        surfaceContainerHigh = animateColorAsState(appScheme.surfaceContainerHigh, tween(duration), label = "themeSurfaceContainerHigh").value,
        onSurface = animateColorAsState(appScheme.onSurface, tween(duration), label = "themeOnSurface").value,
        onSurfaceVariant = animateColorAsState(appScheme.onSurfaceVariant, tween(duration), label = "themeOnSurfaceVariant").value
    )

    MaterialTheme(
        colorScheme = animatedScheme,
        typography = WavelengthTypography,
        content = content
    )
}
'''
assert old_tail in ts, "Theme.kt tail not found"
ts = ts.replace(old_tail, new_tail, 1)
theme.write_text(ts)

# MainActivity: Album Adaptive full-app accent, Aurora background, AMOLED pure black,
# and smooth background/accent color transitions.
main = Path("app/src/main/java/com/wavelength/music/MainActivity.kt")
ms = main.read_text()

imports = {
    "import androidx.compose.animation.core.animateColorAsState\n": "import androidx.compose.foundation.background\n",
    "import androidx.compose.animation.core.infiniteRepeatable\n": "import androidx.compose.animation.core.animateColorAsState\n",
    "import androidx.compose.animation.core.rememberInfiniteTransition\n": "import androidx.compose.animation.core.infiniteRepeatable\n",
    "import androidx.compose.animation.core.RepeatMode\n": "import androidx.compose.animation.core.rememberInfiniteTransition\n",
    "import androidx.compose.animation.core.tween\n": "import androidx.compose.animation.core.RepeatMode\n",
    "import androidx.core.graphics.drawable.toBitmap\n": "import androidx.core.content.ContextCompat\n",
    "import androidx.palette.graphics.Palette\n": "import androidx.core.graphics.drawable.toBitmap\n",
    "import coil.Coil\n": "import androidx.palette.graphics.Palette\n",
    "import coil.request.ImageRequest\n": "import coil.Coil\n",
    "import com.wavelength.music.data.repository.VisualThemeMode\n": "import com.wavelength.music.ui.components.OfflineBanner\n",
    "import com.wavelength.music.ui.nowplaying.PlayerViewModel\n": "import com.wavelength.music.data.repository.VisualThemeMode\n",
}
for imp, after in imports.items():
    if imp not in ms:
        ms = ms.replace(after, after + imp, 1)

# Need Dispatchers already exists; withContext exists.
ms = ms.replace(
    '''            val settingsViewModel: AppSettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.state.collectAsStateWithLifecycle()
''',
    '''            val settingsViewModel: AppSettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.state.collectAsStateWithLifecycle()
            val playerViewModel: PlayerViewModel = hiltViewModel()
            val playbackState by playerViewModel.state.collectAsStateWithLifecycle()
            var albumAdaptiveAccent by remember { mutableStateOf<Color?>(null) }

            LaunchedEffect(
                playbackState.currentTrack?.albumArtUrl,
                settings.visualThemeMode
            ) {
                albumAdaptiveAccent = if (settings.visualThemeMode == VisualThemeMode.ALBUM_ADAPTIVE) {
                    loadAppAlbumAccent(this@MainActivity, playbackState.currentTrack?.albumArtUrl)
                } else {
                    null
                }
            }
''',
    1
)

old_theme_call = '''            WavelengthTheme(
                theme = settings.theme,
                customLiquidAccent = Color(settings.customAccentArgb),
                glassmorphismEnabled = settings.glassmorphismNowPlaying,
                neomorphismEnabled = settings.neomorphismEnabled
            ) {
'''
new_theme_call = '''            val targetAccent = albumAdaptiveAccent ?: Color(settings.customAccentArgb)
            val animatedAccent by animateColorAsState(
                targetValue = targetAccent,
                animationSpec = tween(if (settings.animateThemeTransitions) 500 else 0),
                label = "appThemeAccent"
            )

            WavelengthTheme(
                theme = settings.theme,
                customLiquidAccent = animatedAccent,
                glassmorphismEnabled = settings.visualThemeMode == VisualThemeMode.GLASSMORPHISM,
                neomorphismEnabled = settings.visualThemeMode == VisualThemeMode.NEOMORPHISM,
                amoledEnabled = settings.visualThemeMode == VisualThemeMode.AMOLED,
                animateTransitions = settings.animateThemeTransitions
            ) {
'''
assert old_theme_call in ms, "MainActivity WavelengthTheme call not found"
ms = ms.replace(old_theme_call, new_theme_call, 1)

old_surface = '''                    val appSurface = when {
                        settings.glassmorphismNowPlaying -> Modifier.background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(settings.customAccentArgb).copy(alpha = 0.24f),
                                    Color(0xFF0A1628),
                                    Color(0xFF111827)
                                )
                            )
                        )
                        settings.neomorphismEnabled -> Modifier.background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF303846),
                                    Color(0xFF242B36),
                                    Color(0xFF1B2029)
                                )
                            )
                        )
                        else -> Modifier.background(Color.Black)
                    }
'''
new_surface = '''                    val aurora = rememberInfiniteTransition(label = "auroraTheme")
                    val auroraA by aurora.animateColor(
                        initialValue = Color(0xFF0A2342),
                        targetValue = Color(0xFF4C1D95),
                        animationSpec = infiniteRepeatable(
                            animation = tween(4200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "auroraA"
                    )
                    val auroraB by aurora.animateColor(
                        initialValue = Color(0xFF0E7490),
                        targetValue = Color(0xFF312E81),
                        animationSpec = infiniteRepeatable(
                            animation = tween(5200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "auroraB"
                    )

                    val bgTopTarget = when (settings.visualThemeMode) {
                        VisualThemeMode.GLASSMORPHISM -> animatedAccent.copy(alpha = 0.32f)
                        VisualThemeMode.NEOMORPHISM -> Color(0xFF303846)
                        VisualThemeMode.AMOLED -> Color.Black
                        VisualThemeMode.ALBUM_ADAPTIVE -> animatedAccent.copy(alpha = 0.42f)
                        VisualThemeMode.AURORA -> auroraA
                        VisualThemeMode.LIQUID -> animatedAccent.copy(alpha = 0.22f)
                        VisualThemeMode.SOLID -> Color.Black
                    }
                    val bgBottomTarget = when (settings.visualThemeMode) {
                        VisualThemeMode.GLASSMORPHISM -> Color(0xFF0A1628)
                        VisualThemeMode.NEOMORPHISM -> Color(0xFF1B2029)
                        VisualThemeMode.AMOLED -> Color.Black
                        VisualThemeMode.ALBUM_ADAPTIVE -> Color(0xFF080B12)
                        VisualThemeMode.AURORA -> auroraB
                        VisualThemeMode.LIQUID -> Color(0xFF101522)
                        VisualThemeMode.SOLID -> Color.Black
                    }
                    val transitionMs = if (settings.animateThemeTransitions) 520 else 0
                    val bgTop by animateColorAsState(bgTopTarget, tween(transitionMs), label = "themeBgTop")
                    val bgBottom by animateColorAsState(bgBottomTarget, tween(transitionMs), label = "themeBgBottom")
                    val appSurface = Modifier.background(
                        Brush.verticalGradient(listOf(bgTop, bgBottom))
                    )
'''
assert old_surface in ms, "MainActivity appSurface block not found"
ms = ms.replace(old_surface, new_surface, 1)

# Add image-color helper before final class helper/data declarations.
helper_anchor = '''    override fun onResume() {
'''
helper = '''    private suspend fun loadAppAlbumAccent(context: Context, url: String?): Color? {
        if (url.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .build()
                val drawable = Coil.imageLoader(context).execute(request).drawable ?: return@runCatching null
                val bitmap = drawable.toBitmap()
                val palette = Palette.from(bitmap).generate()
                val fallback = palette.dominantSwatch?.rgb ?: return@runCatching null
                Color(
                    palette.vibrantSwatch?.rgb
                        ?: palette.lightVibrantSwatch?.rgb
                        ?: palette.mutedSwatch?.rgb
                        ?: fallback
                )
            }.getOrNull()
        }
    }

'''
assert helper_anchor in ms, "MainActivity onResume anchor not found"
ms = ms.replace(helper_anchor, helper + helper_anchor, 1)
main.write_text(ms)

# About.
about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Expanded themes to Solid, Liquid, Glassmorphism, Neomorphism, AMOLED, Album Adaptive and Aurora, plus an Animate theme transitions toggle for smooth color crossfades",\n'
assert needle in a, "About changelog anchor not found for themes"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
