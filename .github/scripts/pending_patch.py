from pathlib import Path

# 1) Persisted setting + mutual exclusivity with Glass.
p = Path("app/src/main/java/com/wavelength/music/data/repository/SettingsRepository.kt")
s = p.read_text()

s = s.replace(
    '''    /** Frosted-glass Now Playing layout inspired by translucent modern music players. */
    val glassmorphismNowPlaying: Boolean = false,
''',
    '''    /** Full-app soft raised/inset Neomorphism appearance. */
    val neomorphismEnabled: Boolean = false,
    /** Frosted-glass Now Playing layout inspired by translucent modern music players. */
    val glassmorphismNowPlaying: Boolean = false,
''',
    1
)
s = s.replace(
    '''        syncVolumeWithSystem = prefs.getBoolean(KEY_SYNC_VOLUME_WITH_SYSTEM, true),
        glassmorphismNowPlaying = prefs.getBoolean(KEY_GLASSMORPHISM_NOW_PLAYING, false),
''',
    '''        syncVolumeWithSystem = prefs.getBoolean(KEY_SYNC_VOLUME_WITH_SYSTEM, true),
        neomorphismEnabled = prefs.getBoolean(KEY_NEOMORPHISM_ENABLED, false),
        glassmorphismNowPlaying = prefs.getBoolean(KEY_GLASSMORPHISM_NOW_PLAYING, false),
''',
    1
)

old_glass = '''    fun setGlassmorphismNowPlaying(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_GLASSMORPHISM_NOW_PLAYING, enabled) }
        _state.update { it.copy(glassmorphismNowPlaying = enabled) }
    }
'''
new_glass = '''    fun setNeomorphismEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_NEOMORPHISM_ENABLED, enabled)
            if (enabled) putBoolean(KEY_GLASSMORPHISM_NOW_PLAYING, false)
        }
        _state.update {
            it.copy(
                neomorphismEnabled = enabled,
                glassmorphismNowPlaying = if (enabled) false else it.glassmorphismNowPlaying
            )
        }
    }

    fun setGlassmorphismNowPlaying(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_GLASSMORPHISM_NOW_PLAYING, enabled)
            if (enabled) putBoolean(KEY_NEOMORPHISM_ENABLED, false)
        }
        _state.update {
            it.copy(
                glassmorphismNowPlaying = enabled,
                neomorphismEnabled = if (enabled) false else it.neomorphismEnabled
            )
        }
    }
'''
assert old_glass in s, "glass setter not found"
s = s.replace(old_glass, new_glass, 1)

s = s.replace(
    '''        const val KEY_SYNC_VOLUME_WITH_SYSTEM = "sync_volume_with_system"
        const val KEY_GLASSMORPHISM_NOW_PLAYING = "glassmorphism_now_playing"
''',
    '''        const val KEY_SYNC_VOLUME_WITH_SYSTEM = "sync_volume_with_system"
        const val KEY_NEOMORPHISM_ENABLED = "neomorphism_enabled"
        const val KEY_GLASSMORPHISM_NOW_PLAYING = "glassmorphism_now_playing"
''',
    1
)
p.write_text(s)

# 2) ViewModel setter.
p = Path("app/src/main/java/com/wavelength/music/ui/settings/AppSettingsViewModel.kt")
s = p.read_text()
needle = '''    fun setGlassmorphismNowPlaying(enabled: Boolean) =
        settingsRepository.setGlassmorphismNowPlaying(enabled)
'''
replacement = '''    fun setNeomorphismEnabled(enabled: Boolean) =
        settingsRepository.setNeomorphismEnabled(enabled)

    fun setGlassmorphismNowPlaying(enabled: Boolean) =
        settingsRepository.setGlassmorphismNowPlaying(enabled)
'''
assert needle in s, "ViewModel glass setter not found"
s = s.replace(needle, replacement, 1)
p.write_text(s)

# 3) Settings toggle. Put it immediately before Glassmorphism.
p = Path("app/src/main/java/com/wavelength/music/ui/settings/SettingsScreen.kt")
s = p.read_text()
anchor = '''            item {
                SettingsSection(title = "Glassmorphism Theme") {
'''
neo = '''            item {
                SettingsSection(title = "Neomorphism Theme") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Full App Neomorphism")
                            Text(
                                text = if (settings.neomorphismEnabled) {
                                    "On • soft raised surfaces, rounded tiles and depth shadows"
                                } else {
                                    "Off • use the selected standard appearance"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.neomorphismEnabled,
                            onCheckedChange = viewModel::setNeomorphismEnabled
                        )
                    }
                    Text(
                        text = "Applies a soft dark Neomorphism style across Home, Search, Library, Settings and player surfaces. Enabling it turns Glassmorphism off so the two depth systems do not conflict.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (settings.neomorphismEnabled) Color(0xFF252B36)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (settings.neomorphismEnabled) Color.White.copy(alpha = 0.10f)
                                else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(24.dp)
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Text(
                                "Neomorphic preview",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Soft elevated card • subtle highlight • deep rounded surface",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

'''
assert anchor in s, "Glassmorphism section anchor not found"
s = s.replace(anchor, neo + anchor, 1)
p.write_text(s)

# 4) Theme palette.
p = Path("app/src/main/java/com/wavelength/music/ui/theme/Theme.kt")
s = p.read_text()
s = s.replace(
    '''    customLiquidAccent: Color? = null,
    glassmorphismEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseScheme = schemeFor(theme, customLiquidAccent)
    val appScheme = if (glassmorphismEnabled) {
''',
    '''    customLiquidAccent: Color? = null,
    glassmorphismEnabled: Boolean = false,
    neomorphismEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseScheme = schemeFor(theme, customLiquidAccent)
    val appScheme = if (glassmorphismEnabled) {
''',
    1
)
old_tail = '''            onSurfaceVariant = Color.White.copy(alpha = 0.78f)
        )
    } else {
        baseScheme
    }
'''
new_tail = '''            onSurfaceVariant = Color.White.copy(alpha = 0.78f)
        )
    } else if (neomorphismEnabled) {
        baseScheme.copy(
            background = Color.Transparent,
            surface = Color(0xFF222833),
            surfaceVariant = Color(0xFF2A313D),
            surfaceContainerLowest = Color(0xFF171C24),
            surfaceContainerLow = Color(0xFF1E242E),
            surfaceContainer = Color(0xFF252C37),
            surfaceContainerHigh = Color(0xFF2B3340),
            surfaceContainerHighest = Color(0xFF323B49),
            outline = Color(0xFF4B5667).copy(alpha = 0.52f),
            outlineVariant = Color.White.copy(alpha = 0.10f),
            onBackground = Color(0xFFF1F4F8),
            onSurface = Color(0xFFF1F4F8),
            onSurfaceVariant = Color(0xFFBFC8D6)
        )
    } else {
        baseScheme
    }
'''
assert old_tail in s, "Theme scheme tail not found"
s = s.replace(old_tail, new_tail, 1)
p.write_text(s)

# 5) MainActivity wires the toggle and gives it a soft embossed background.
p = Path("app/src/main/java/com/wavelength/music/MainActivity.kt")
s = p.read_text()
s = s.replace(
    '''                customLiquidAccent = Color(settings.customAccentArgb),
                glassmorphismEnabled = settings.glassmorphismNowPlaying
''',
    '''                customLiquidAccent = Color(settings.customAccentArgb),
                glassmorphismEnabled = settings.glassmorphismNowPlaying,
                neomorphismEnabled = settings.neomorphismEnabled
''',
    1
)
old_surface = '''                    val appSurface = if (settings.glassmorphismNowPlaying) {
                        Modifier.background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(settings.customAccentArgb).copy(alpha = 0.24f),
                                    Color(0xFF0A1628),
                                    Color(0xFF111827)
                                )
                            )
                        )
                    } else {
                        Modifier.background(Color.Black)
                    }
'''
new_surface = '''                    val appSurface = when {
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
assert old_surface in s, "MainActivity appSurface block not found"
s = s.replace(old_surface, new_surface, 1)
p.write_text(s)

# 6) Bottom nav adopts matte raised Neomorphism surfaces when enabled.
p = Path("app/src/main/java/com/wavelength/music/ui/navigation/NavGraph.kt")
s = p.read_text()
old_brush = '''                                val purpleGlassBrush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xCC332060),
                                        Color(0xCC56367F),
                                        Color(0xCC764595)
                                    )
                                )
'''
new_brush = '''                                val purpleGlassBrush = if (settings.neomorphismEnabled) {
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
assert old_brush in s, "nav brush block not found"
s = s.replace(old_brush, new_brush, 1)

s = s.replace(
    '''                                            .shadow(if (selected) 13.dp else 9.dp, tileShape)
''',
    '''                                            .shadow(
                                                if (selected) 16.dp else 10.dp,
                                                tileShape,
                                                ambientColor = if (settings.neomorphismEnabled) Color.Black.copy(alpha = 0.55f) else Color.Black,
                                                spotColor = if (settings.neomorphismEnabled) Color.Black.copy(alpha = 0.70f) else Color.Black
                                            )
''',
    1
)
p.write_text(s)

# 7) About changelog.
p = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
s = p.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Added a full-app Neomorphism theme toggle with soft raised dark surfaces, rounded depth styling and automatic Glassmorphism conflict handling",\n'
assert needle in s, "About changelog anchor not found"
if entry not in s:
    s = s.replace(needle, needle + entry, 1)
p.write_text(s)
