from pathlib import Path

repo = Path("app/src/main/java/com/wavelength/music/data/repository/SettingsRepository.kt")
s = repo.read_text()
if "import com.wavelength.music.playback.StreamCacheControl\n" not in s:
    s = s.replace(
        "import com.wavelength.music.ui.settings.IconPreset\n",
        "import com.wavelength.music.ui.settings.IconPreset\nimport com.wavelength.music.playback.StreamCacheControl\n",
        1
    )

s = s.replace(
    '''    val liquidAlbumArtBackground: Boolean = false
)''',
    '''    val liquidAlbumArtBackground: Boolean = false,
    /** Persistent streamed-audio cache cap in MB. 0 disables retained stream caching. */
    val streamCacheLimitMb: Int = DEFAULT_STREAM_CACHE_LIMIT_MB
)''',
    1
)

s = s.replace(
    '''const val DEFAULT_GLASS_PLAYED_GLOW_ARGB: Int = 0xFF54E8FF.toInt()
''',
    '''const val DEFAULT_GLASS_PLAYED_GLOW_ARGB: Int = 0xFF54E8FF.toInt()
const val DEFAULT_STREAM_CACHE_LIMIT_MB: Int = 1024
''',
    1
)

s = s.replace(
    '''        glassPlayedGlowArgb = prefs.getInt(KEY_GLASS_PLAYED_GLOW_ARGB, DEFAULT_GLASS_PLAYED_GLOW_ARGB),
        liquidAlbumArtBackground = false
''',
    '''        glassPlayedGlowArgb = prefs.getInt(KEY_GLASS_PLAYED_GLOW_ARGB, DEFAULT_GLASS_PLAYED_GLOW_ARGB),
        liquidAlbumArtBackground = false,
        streamCacheLimitMb = prefs.getInt(KEY_STREAM_CACHE_LIMIT_MB, DEFAULT_STREAM_CACHE_LIMIT_MB)
            .coerceIn(0, DEFAULT_STREAM_CACHE_LIMIT_MB)
''',
    1
)

anchor = '''    fun setLiquidAlbumArtBackground(enabled: Boolean) {
        _state.update { it.copy(liquidAlbumArtBackground = enabled) }
    }
'''
if anchor not in s:
    candidates = [
        '''    fun setLiquidAlbumArtBackground(enabled: Boolean) =
        _state.update { it.copy(liquidAlbumArtBackground = enabled) }
''',
        '''    fun setLiquidAlbumArtBackground(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_LIQUID_ALBUM_ART_BACKGROUND, enabled) }
        _state.update { it.copy(liquidAlbumArtBackground = enabled) }
    }
'''
    ]
    anchor = next((x for x in candidates if x in s), None)
assert anchor is not None, "liquid album background setter anchor not found"
insert = anchor + '''

    fun setStreamCacheLimitMb(limitMb: Int) {
        val clamped = limitMb.coerceIn(0, DEFAULT_STREAM_CACHE_LIMIT_MB)
        prefs.edit { putInt(KEY_STREAM_CACHE_LIMIT_MB, clamped) }
        _state.update { it.copy(streamCacheLimitMb = clamped) }
        StreamCacheControl.updateLimitBytes(
            context,
            clamped.toLong() * 1024L * 1024L
        )
    }

    fun clearStreamCache() {
        StreamCacheControl.clear(context)
    }
'''
s = s.replace(anchor, insert, 1)

key_anchor = '''        const val KEY_THEME = "theme"
'''
assert key_anchor in s, "SettingsRepository key anchor not found"
if 'KEY_STREAM_CACHE_LIMIT_MB' not in s[s.index("private companion object"):]:
    s = s.replace(
        key_anchor,
        key_anchor + '        const val KEY_STREAM_CACHE_LIMIT_MB = "stream_cache_limit_mb"\n',
        1
    )
repo.write_text(s)

play = Path("app/src/main/java/com/wavelength/music/playback/PlaybackService.kt")
p = play.read_text()
p = p.replace("import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor\n", "")
if "import androidx.media3.datasource.cache.Cache\n" not in p:
    p = p.replace(
        "import androidx.media3.datasource.cache.CacheDataSource\n",
        "import androidx.media3.datasource.cache.Cache\nimport androidx.media3.datasource.cache.CacheDataSource\nimport androidx.media3.datasource.cache.CacheEvictor\nimport androidx.media3.datasource.cache.CacheSpan\n",
        1
    )

old_cache = '''        val cache = SimpleCache(
            File(cacheDir, "azmusic_stream_cache"),
            LeastRecentlyUsedCacheEvictor(STREAM_CACHE_MAX_BYTES),
            StandaloneDatabaseProvider(this)
        )
        streamCache = cache
'''
new_cache = '''        val savedLimitMb = getSharedPreferences(
            SETTINGS_PREFS_NAME,
            Context.MODE_PRIVATE
        ).getInt(KEY_STREAM_CACHE_LIMIT_MB, DEFAULT_STREAM_CACHE_LIMIT_MB)
            .coerceIn(0, DEFAULT_STREAM_CACHE_LIMIT_MB)

        val cacheEvictor = AdjustableCacheEvictor(savedLimitMb.toLong() * BYTES_PER_MB)
        val cache = SimpleCache(
            File(cacheDir, STREAM_CACHE_DIR),
            cacheEvictor,
            StandaloneDatabaseProvider(this)
        )
        streamCache = cache
        StreamCacheControl.attach(cache, cacheEvictor)
'''
assert old_cache in p, "PlaybackService cache construction not found"
p = p.replace(old_cache, new_cache, 1)

destroy_old = '''        runCatching { streamCache?.release() }
        streamCache = null
        super.onDestroy()
    }

    private companion object {
        // Roughly 1 GB of recently streamed audio, automatically evicting the oldest cache data.
        const val STREAM_CACHE_MAX_BYTES = 1_073_741_824L
    }
}
'''
destroy_new = '''        StreamCacheControl.detach(streamCache)
        runCatching { streamCache?.release() }
        streamCache = null
        super.onDestroy()
    }

    private companion object {
        const val SETTINGS_PREFS_NAME = "wavelength_settings"
        const val KEY_STREAM_CACHE_LIMIT_MB = "stream_cache_limit_mb"
        const val DEFAULT_STREAM_CACHE_LIMIT_MB = 1024
        const val BYTES_PER_MB = 1_048_576L
        const val STREAM_CACHE_DIR = "azmusic_stream_cache"
    }
}

@UnstableApi
class AdjustableCacheEvictor(initialMaxBytes: Long) : CacheEvictor {
    @Volatile
    private var maxBytes: Long = initialMaxBytes.coerceAtLeast(0L)

    override fun requiresCacheSpanTouches(): Boolean = true
    override fun onCacheInitialized() = Unit

    override fun onStartFile(cache: Cache, key: String, position: Long, length: Long) {
        evict(cache, if (length > 0L) length else 0L)
    }

    override fun onSpanAdded(cache: Cache, span: CacheSpan) {
        evict(cache, 0L)
    }

    override fun onSpanRemoved(cache: Cache, span: CacheSpan) = Unit
    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) = Unit

    fun updateMaxBytes(cache: Cache, newMaxBytes: Long) {
        maxBytes = newMaxBytes.coerceAtLeast(0L)
        evict(cache, 0L)
    }

    private fun evict(cache: Cache, requiredSpace: Long) {
        while (cache.cacheSpace + requiredSpace > maxBytes) {
            val oldest = cache.keys
                .asSequence()
                .flatMap { key -> cache.getCachedSpans(key).asSequence() }
                .minByOrNull { it.lastTouchTimestamp }
                ?: break
            try {
                cache.removeSpan(oldest)
            } catch (_: Exception) {
                break
            }
        }
    }
}

@UnstableApi
object StreamCacheControl {
    @Volatile private var activeCache: SimpleCache? = null
    @Volatile private var activeEvictor: AdjustableCacheEvictor? = null

    @Synchronized
    fun attach(cache: SimpleCache, evictor: AdjustableCacheEvictor) {
        activeCache = cache
        activeEvictor = evictor
    }

    @Synchronized
    fun detach(cache: SimpleCache?) {
        if (activeCache === cache) {
            activeCache = null
            activeEvictor = null
        }
    }

    @Synchronized
    fun updateLimitBytes(context: Context, maxBytes: Long) {
        val cache = activeCache
        val evictor = activeEvictor
        if (cache != null && evictor != null) {
            evictor.updateMaxBytes(cache, maxBytes)
        } else if (maxBytes == 0L) {
            File(context.cacheDir, "azmusic_stream_cache").deleteRecursively()
        }
    }

    @Synchronized
    fun clear(context: Context) {
        val cache = activeCache
        if (cache != null) {
            cache.keys.toList().forEach { key ->
                runCatching { cache.removeResource(key) }
            }
        } else {
            File(context.cacheDir, "azmusic_stream_cache").deleteRecursively()
        }
    }
}
'''
assert destroy_old in p, "PlaybackService footer not found"
p = p.replace(destroy_old, destroy_new, 1)
play.write_text(p)

vm = Path("app/src/main/java/com/wavelength/music/ui/settings/AppSettingsViewModel.kt")
v = vm.read_text()
anchor = '''    fun setLiquidAlbumArtBackground(enabled: Boolean) =
        settingsRepository.setLiquidAlbumArtBackground(enabled)
'''
assert anchor in v, "ViewModel liquid setter anchor not found"
v = v.replace(
    anchor,
    anchor + '''

    fun setStreamCacheLimitMb(limitMb: Int) =
        settingsRepository.setStreamCacheLimitMb(limitMb)

    fun clearStreamCache() = settingsRepository.clearStreamCache()
''',
    1
)
vm.write_text(v)

screen = Path("app/src/main/java/com/wavelength/music/ui/settings/SettingsScreen.kt")
u = screen.read_text()

imports = [
    ("import androidx.compose.animation.core.animate\n", "import android.net.Uri\n"),
    ("import androidx.compose.foundation.gestures.Orientation\n", "import androidx.compose.foundation.gestures.detectDragGestures\n"),
    ("import androidx.compose.foundation.gestures.draggable\n", "import androidx.compose.foundation.gestures.Orientation\n"),
    ("import androidx.compose.foundation.gestures.rememberDraggableState\n", "import androidx.compose.foundation.gestures.draggable\n"),
    ("import androidx.compose.foundation.layout.offset\n", "import androidx.compose.foundation.layout.height\n"),
    ("import androidx.compose.runtime.mutableFloatStateOf\n", "import androidx.compose.runtime.mutableStateOf\n"),
    ("import androidx.compose.ui.layout.onSizeChanged\n", "import androidx.compose.ui.layout.ContentScale\n"),
    ("import androidx.compose.ui.unit.IntOffset\n", "import androidx.compose.ui.unit.dp\n"),
]
for imp, after in imports:
    if imp not in u:
        u = u.replace(after, after + imp, 1)

u = u.replace(
    '''    var showClearDownloadsConfirm by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
''',
    '''    var showClearDownloadsConfirm by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
''',
    1
)

dialog_anchor = '''    if (showAbout) {
        AboutSheet(onDismiss = { showAbout = false })
    }
'''
dialog = '''    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Clear streamed-song cache?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("This removes cached streamed audio only. Downloads, playlists and favorites are not deleted.")
                    CacheSwipeToConfirm(
                        onConfirmed = {
                            viewModel.clearStreamCache()
                            showClearCacheConfirm = false
                            Toast.makeText(context, "Stream cache cleared", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

''' + dialog_anchor
assert dialog_anchor in u, "About dialog anchor not found"
u = u.replace(dialog_anchor, dialog, 1)

downloads_block = '''            item {
                SettingsSection(title = "Downloads") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (downloadsSummary.count == 0) {
                                "No downloads yet"
                            } else {
                                "${downloadsSummary.count} songs · ${formatStorageSize(downloadsSummary.totalSizeBytes)}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (downloadsSummary.count > 0) {
                            OutlinedButton(onClick = { showClearDownloadsConfirm = true }) {
                                Text("Clear all")
                            }
                        }
                    }
                }
            }
'''
cache_block = downloads_block + '''

            item {
                SettingsSection(title = "Stream Cache") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = when (settings.streamCacheLimitMb) {
                                0 -> "Cache limit: Off (0 MB)"
                                1024 -> "Cache limit: 1.0 GB"
                                else -> "Cache limit: ${settings.streamCacheLimitMb} MB"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = settings.streamCacheLimitMb.toFloat(),
                            onValueChange = { raw ->
                                val snapped = ((raw / 64f).roundToInt() * 64).coerceIn(0, 1024)
                                viewModel.setStreamCacheLimitMb(snapped)
                            },
                            valueRange = 0f..1024f,
                            steps = 15
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "0 MB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "1 GB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Limits persistent streamed-song cache storage. Lowering the limit immediately evicts older cached audio. 0 MB disables retained stream caching.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
                        )
                        OutlinedButton(
                            onClick = { showClearCacheConfirm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clear cache")
                        }
                    }
                }
            }
'''
assert downloads_block in u, "Downloads settings block not found"
u = u.replace(downloads_block, cache_block, 1)

helper_anchor = '''private fun formatBandFrequency(hz: Int): String =
'''
swipe_helper = '''@Composable
private fun CacheSwipeToConfirm(onConfirmed: () -> Unit) {
    var offsetPx by remember { mutableFloatStateOf(0f) }
    var maxOffsetPx by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(29.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { size ->
                maxOffsetPx = (size.width - size.height.toFloat()).coerceAtLeast(0f)
                offsetPx = offsetPx.coerceIn(0f, maxOffsetPx)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Swipe to clear cache  →",
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge
        )
        androidx.compose.material3.Surface(
            modifier = Modifier
                .padding(4.dp)
                .size(50.dp)
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetPx = (offsetPx + delta).coerceIn(0f, maxOffsetPx)
                    },
                    onDragStopped = {
                        if (maxOffsetPx > 0f && offsetPx >= maxOffsetPx * 0.85f) {
                            offsetPx = maxOffsetPx
                            onConfirmed()
                        } else {
                            val start = offsetPx
                            scope.launch {
                                animate(start, 0f) { value, _ -> offsetPx = value }
                            }
                        }
                    }
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "✓",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

'''
assert helper_anchor in u, "Settings helper anchor not found"
u = u.replace(helper_anchor, swipe_helper + helper_anchor, 1)
screen.write_text(u)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
start = a.find("private val latestUpdates = listOf(")
end = a.find("private val featureGroups = listOf(", start)
assert start >= 0 and end > start, "About latestUpdates block not found"
a = a[:start] + a[end:]

render = '''            Text(
                text = "Latest updates",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)
            )
            latestUpdates.forEach { update ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("•  ", color = MaterialTheme.colorScheme.primary)
                    Text(update, style = MaterialTheme.typography.bodySmall)
                }
            }

'''
assert render in a, "About latest updates renderer not found"
a = a.replace(render, '''            Text(
                text = "Features",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)
            )

''', 1)

a = a.replace(
    '"Persistent streamed-song cache for faster replays and smoother playback in weak coverage",',
    '"Persistent streamed-song cache with an adjustable 0–1 GB storage limit and safe clear-cache control",'
)
a = a.replace(
    '"Single variable accent color shared by Solid and Liquid appearance modes",',
    '"Seven distinct visual systems: Solid, Liquid, Glassmorphism, Neomorphism, AMOLED, Album Adaptive and Aurora",'
)
a = a.replace(
    '"Optional full-app Glassmorphism theme with frosted translucent surfaces and reference-style playback controls",',
    '"Theme-specific geometry, surfaces, navigation, mini-player styling, backgrounds and animated transitions",'
)
a = a.replace(
    '"Solid or Liquid appearance selection",',
    '"Album-adaptive colors and animated Aurora backgrounds",'
)
about.write_text(a)
