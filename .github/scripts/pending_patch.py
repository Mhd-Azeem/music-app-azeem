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
