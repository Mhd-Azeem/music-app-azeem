from pathlib import Path

# SettingsRepository: add persistent Glass timeline + played-glow colors, guarded so they cannot
# be changed while Glass mode is disabled.
p = Path('app/src/main/java/com/wavelength/music/data/repository/SettingsRepository.kt')
s = p.read_text()
s = s.replace(
'''    /** Frosted-glass Now Playing layout inspired by translucent modern music players. */
    val glassmorphismNowPlaying: Boolean = false,
    /** When enabled in Liquid themes, the current track artwork fills the Now Playing backdrop. */''',
'''    /** Frosted-glass Now Playing layout inspired by translucent modern music players. */
    val glassmorphismNowPlaying: Boolean = false,
    /** Main unplayed Glass seek/timeline color. Editable only while Glass mode is enabled. */
    val glassTimelineArgb: Int = DEFAULT_GLASS_TIMELINE_ARGB,
    /** Played seek segment + glow color. Editable only while Glass mode is enabled. */
    val glassPlayedGlowArgb: Int = DEFAULT_GLASS_PLAYED_GLOW_ARGB,
    /** When enabled in Liquid themes, the current track artwork fills the Now Playing backdrop. */''', 1)
s = s.replace(
'''const val DEFAULT_CUSTOM_ACCENT_ARGB: Int = 0xFF22D3EE.toInt()''',
'''const val DEFAULT_CUSTOM_ACCENT_ARGB: Int = 0xFF22D3EE.toInt()
const val DEFAULT_GLASS_TIMELINE_ARGB: Int = 0xFF14B8E6.toInt()
const val DEFAULT_GLASS_PLAYED_GLOW_ARGB: Int = 0xFF54E8FF.toInt()''', 1)
s = s.replace(
'''        glassmorphismNowPlaying = prefs.getBoolean(KEY_GLASSMORPHISM_NOW_PLAYING, false),
        liquidAlbumArtBackground = false''',
'''        glassmorphismNowPlaying = prefs.getBoolean(KEY_GLASSMORPHISM_NOW_PLAYING, false),
        glassTimelineArgb = prefs.getInt(KEY_GLASS_TIMELINE_ARGB, DEFAULT_GLASS_TIMELINE_ARGB),
        glassPlayedGlowArgb = prefs.getInt(KEY_GLASS_PLAYED_GLOW_ARGB, DEFAULT_GLASS_PLAYED_GLOW_ARGB),
        liquidAlbumArtBackground = false''', 1)
s = s.replace(
'''    fun setGlassmorphismNowPlaying(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_GLASSMORPHISM_NOW_PLAYING, enabled) }
        _state.update { it.copy(glassmorphismNowPlaying = enabled) }
    }

    fun setLiquidAlbumArtBackground''',
'''    fun setGlassmorphismNowPlaying(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_GLASSMORPHISM_NOW_PLAYING, enabled) }
        _state.update { it.copy(glassmorphismNowPlaying = enabled) }
    }

    fun setGlassTimelineArgb(argb: Int) {
        if (!_state.value.glassmorphismNowPlaying) return
        prefs.edit { putInt(KEY_GLASS_TIMELINE_ARGB, argb) }
        _state.update { it.copy(glassTimelineArgb = argb) }
    }

    fun setGlassPlayedGlowArgb(argb: Int) {
        if (!_state.value.glassmorphismNowPlaying) return
        prefs.edit { putInt(KEY_GLASS_PLAYED_GLOW_ARGB, argb) }
        _state.update { it.copy(glassPlayedGlowArgb = argb) }
    }

    fun setLiquidAlbumArtBackground''', 1)
s = s.replace(
'''        const val KEY_GLASSMORPHISM_NOW_PLAYING = "glassmorphism_now_playing"
        const val KEY_LIQUID_ALBUM_ART_BACKGROUND''',
'''        const val KEY_GLASSMORPHISM_NOW_PLAYING = "glassmorphism_now_playing"
        const val KEY_GLASS_TIMELINE_ARGB = "glass_timeline_argb"
        const val KEY_GLASS_PLAYED_GLOW_ARGB = "glass_played_glow_argb"
        const val KEY_LIQUID_ALBUM_ART_BACKGROUND''', 1)
p.write_text(s)

# Settings VM forwarding methods.
p = Path('app/src/main/java/com/wavelength/music/ui/settings/AppSettingsViewModel.kt')
s = p.read_text()
s = s.replace(
'''    fun setGlassmorphismNowPlaying(enabled: Boolean) =
        settingsRepository.setGlassmorphismNowPlaying(enabled)

    fun setLiquidAlbumArtBackground''',
'''    fun setGlassmorphismNowPlaying(enabled: Boolean) =
        settingsRepository.setGlassmorphismNowPlaying(enabled)

    fun setGlassTimelineArgb(argb: Int) = settingsRepository.setGlassTimelineArgb(argb)

    fun setGlassPlayedGlowArgb(argb: Int) = settingsRepository.setGlassPlayedGlowArgb(argb)

    fun setLiquidAlbumArtBackground''', 1)
p.write_text(s)

# Settings UI: two collapsible continuous color pickers under Glassmorphism. They are visually
# disabled and non-interactive until Full App Glassmorphism is switched on.
p = Path('app/src/main/java/com/wavelength/music/ui/settings/SettingsScreen.kt')
s = p.read_text()
s = s.replace(
'''    var showThemeColorPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current''',
'''    var showThemeColorPicker by remember { mutableStateOf(false) }
    var showGlassTimelineColorPicker by remember { mutableStateOf(false) }
    var showGlassGlowColorPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current''', 1)
needle = '''                    Text(
                        text = "Applies the translucent frosted-glass theme to Home, Search, Library, Settings, dialogs, cards and Now Playing. The player also uses the reference-style curved queue, seek arc and connected transport control.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
'''
insert = needle + '''
                    Text(
                        text = if (settings.glassmorphismNowPlaying) "Glass timeline colors" else "Glass timeline colors • enable Glassmorphism to edit",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (settings.glassmorphismNowPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = settings.glassmorphismNowPlaying) {
                                showGlassTimelineColorPicker = !showGlassTimelineColorPicker
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(settings.glassTimelineArgb))
                                .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                "Timeline color",
                                color = if (settings.glassmorphismNowPlaying) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Base curved seek line",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            enabled = settings.glassmorphismNowPlaying,
                            onClick = { viewModel.setGlassTimelineArgb(0xFF14B8E6.toInt()) }
                        ) { Text("Reset") }
                    }
                    if (settings.glassmorphismNowPlaying && showGlassTimelineColorPicker) {
                        LiquidColorPicker(
                            selectedArgb = settings.glassTimelineArgb,
                            onColorSelected = viewModel::setGlassTimelineArgb,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .height(160.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = settings.glassmorphismNowPlaying) {
                                showGlassGlowColorPicker = !showGlassGlowColorPicker
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(settings.glassPlayedGlowArgb))
                                .border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                "Played portion glow",
                                color = if (settings.glassmorphismNowPlaying) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Glow and bright border around the played arc",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            enabled = settings.glassmorphismNowPlaying,
                            onClick = { viewModel.setGlassPlayedGlowArgb(0xFF54E8FF.toInt()) }
                        ) { Text("Reset") }
                    }
                    if (settings.glassmorphismNowPlaying && showGlassGlowColorPicker) {
                        LiquidColorPicker(
                            selectedArgb = settings.glassPlayedGlowArgb,
                            onColorSelected = viewModel::setGlassPlayedGlowArgb,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .height(160.dp)
                        )
                    }
'''
assert needle in s, 'Glassmorphism description anchor missing'
s = s.replace(needle, insert, 1)
p.write_text(s)

# NavGraph passes new colors to Now Playing.
p = Path('app/src/main/java/com/wavelength/music/ui/navigation/NavGraph.kt')
s = p.read_text()
s = s.replace(
'''                        glassmorphismNowPlaying = settings.glassmorphismNowPlaying,
                        liquidAlbumArtBackground = false''',
'''                        glassmorphismNowPlaying = settings.glassmorphismNowPlaying,
                        glassTimelineArgb = settings.glassTimelineArgb,
                        glassPlayedGlowArgb = settings.glassPlayedGlowArgb,
                        liquidAlbumArtBackground = false''', 1)
p.write_text(s)

# Make base timeline thicker in UI config.
p = Path('app/src/main/java/com/wavelength/music/ui/design/UiDesignConfig.kt')
s = p.read_text().replace('const val GLASS_SEEK_STROKE_DP = 3.2f', 'const val GLASS_SEEK_STROKE_DP = 4.8f', 1)
p.write_text(s)

# Now Playing: accept colors and draw unplayed base + glowing played segment.
p = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = p.read_text()
s = s.replace(
'''    syncVolumeWithSystem: Boolean = true,
    glassmorphismNowPlaying: Boolean = false,
    liquidAlbumArtBackground: Boolean = false''',
'''    syncVolumeWithSystem: Boolean = true,
    glassmorphismNowPlaying: Boolean = false,
    glassTimelineArgb: Int = 0xFF14B8E6.toInt(),
    glassPlayedGlowArgb: Int = 0xFF54E8FF.toInt(),
    liquidAlbumArtBackground: Boolean = false''', 1)
s = s.replace(
'''    val accentColor = dynamicAccent ?: MaterialTheme.colorScheme.primary
''',
'''    val accentColor = dynamicAccent ?: MaterialTheme.colorScheme.primary
    val glassTimelineColor = Color(glassTimelineArgb)
    val glassPlayedGlowColor = Color(glassPlayedGlowArgb)
''', 1)
old = '''                                drawArc(
                                    color = Color(0xFF00A9D6),
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = Offset(0f, top),
                                    size = arcSize,
                                    style = Stroke(width = UiDesignConfig.GLASS_SEEK_STROKE_DP.dp.toPx())
                                )
                                val angle = Math.toRadians((startAngle + sweepAngle * progressFraction).toDouble())
                                val cx = size.width / 2f
                                val cy = top + arcSize.height / 2f
                                val thumb = Offset(
                                    cx + (arcSize.width / 2f * kotlin.math.cos(angle)).toFloat(),
                                    cy + (arcSize.height / 2f * kotlin.math.sin(angle)).toFloat()
                                )
                                drawCircle(Color(0xFF00A9D6), UiDesignConfig.GLASS_SEEK_THUMB_RADIUS_DP.dp.toPx(), thumb)
'''
new = '''                                val baseStroke = UiDesignConfig.GLASS_SEEK_STROKE_DP.dp.toPx()
                                // Slightly thicker base timeline.
                                drawArc(
                                    color = glassTimelineColor.copy(alpha = 0.88f),
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = Offset(0f, top),
                                    size = arcSize,
                                    style = Stroke(width = baseStroke)
                                )
                                // Spotify-style played portion: soft outer bloom plus a crisp,
                                // brighter border sitting on top of the base timeline.
                                val playedSweep = sweepAngle * progressFraction
                                if (playedSweep > 0.15f) {
                                    drawArc(
                                        color = glassPlayedGlowColor.copy(alpha = 0.12f),
                                        startAngle = startAngle,
                                        sweepAngle = playedSweep,
                                        useCenter = false,
                                        topLeft = Offset(0f, top),
                                        size = arcSize,
                                        style = Stroke(width = baseStroke + 11.dp.toPx())
                                    )
                                    drawArc(
                                        color = glassPlayedGlowColor.copy(alpha = 0.24f),
                                        startAngle = startAngle,
                                        sweepAngle = playedSweep,
                                        useCenter = false,
                                        topLeft = Offset(0f, top),
                                        size = arcSize,
                                        style = Stroke(width = baseStroke + 6.dp.toPx())
                                    )
                                    drawArc(
                                        color = glassPlayedGlowColor.copy(alpha = 0.98f),
                                        startAngle = startAngle,
                                        sweepAngle = playedSweep,
                                        useCenter = false,
                                        topLeft = Offset(0f, top),
                                        size = arcSize,
                                        style = Stroke(width = baseStroke + 1.2.dp.toPx())
                                    )
                                }
                                val angle = Math.toRadians((startAngle + sweepAngle * progressFraction).toDouble())
                                val cx = size.width / 2f
                                val cy = top + arcSize.height / 2f
                                val thumb = Offset(
                                    cx + (arcSize.width / 2f * kotlin.math.cos(angle)).toFloat(),
                                    cy + (arcSize.height / 2f * kotlin.math.sin(angle)).toFloat()
                                )
                                val thumbRadius = UiDesignConfig.GLASS_SEEK_THUMB_RADIUS_DP.dp.toPx()
                                drawCircle(glassPlayedGlowColor.copy(alpha = 0.14f), thumbRadius + 9.dp.toPx(), thumb)
                                drawCircle(glassPlayedGlowColor.copy(alpha = 0.28f), thumbRadius + 5.dp.toPx(), thumb)
                                drawCircle(glassPlayedGlowColor, thumbRadius, thumb)
'''
assert old in s, 'Glass seek Canvas anchor missing'
s = s.replace(old, new, 1)
p.write_text(s)

# About changelog.
p = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
s = p.read_text()
needle = 'private val latestUpdates = listOf(\n'
entry = '    "Glass timeline is now thicker with a glowing played segment, plus Glass-only editable timeline and glow colors in Settings",\n'
assert needle in s
if entry not in s:
    s = s.replace(needle, needle + entry, 1)
p.write_text(s)
