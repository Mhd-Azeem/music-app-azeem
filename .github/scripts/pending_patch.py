from pathlib import Path

p = Path("app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt")
s = p.read_text()

old = '''                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (glassUpcoming.isNotEmpty()) {
                        LazyRow(
'''
new = '''                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (glassUpcoming.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 14.dp, top = 4.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.26f),
                                    RoundedCornerShape(22.dp)
                                )
                                .clickable { viewModel.smartShuffle() }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Shuffle Up Next",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Shuffle",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    if (glassUpcoming.isNotEmpty()) {
                        LazyRow(
'''
assert old in s, "Glass Up Next container not found"
s = s.replace(old, new, 1)
p.write_text(s)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Added a visible Shuffle pill to the Glassmorphism Up Next area so the upcoming queue can be reshuffled directly from the Glass player",\n'
assert needle in a, "About changelog anchor not found"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)


# Keep Home/Search/Library readable on small-width phones and large font scales.
nav = Path("app/src/main/java/com/wavelength/music/ui/navigation/NavGraph.kt")
n = nav.read_text()

if "import androidx.compose.ui.text.style.TextOverflow\n" not in n:
    n = n.replace(
        "import androidx.compose.ui.graphics.Color\n",
        "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.text.style.TextOverflow\n",
        1
    )

n = n.replace(
    '''                        val navSpacing = 10.dp
                        val itemWidth = (maxWidth - navSpacing * 2) / 3
''',
    '''                        val compactBottomNav = maxWidth < 340.dp
                        val navSpacing = if (compactBottomNav) 6.dp else 10.dp
                        val itemWidth = (maxWidth - navSpacing * 2) / 3
''',
    1
)

n = n.replace(
    '''                                    val tileShape = RoundedCornerShape(24.dp)
''',
    '''                                    val tileShape = RoundedCornerShape(if (compactBottomNav) 20.dp else 24.dp)
''',
    1
)

old_row = '''                                        Row(
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
'''
new_row = '''                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = if (compactBottomNav) 5.dp else 10.dp,
                                                    vertical = if (compactBottomNav) 10.dp else 11.dp
                                                ),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                iconFor(screen),
                                                contentDescription = navLabelFor(screen),
                                                tint = Color.White,
                                                modifier = Modifier.size(if (compactBottomNav) 20.dp else 24.dp)
                                            )
                                            Text(
                                                navLabelFor(screen),
                                                style = if (compactBottomNav) {
                                                    MaterialTheme.typography.labelMedium
                                                } else {
                                                    MaterialTheme.typography.labelLarge
                                                },
                                                color = Color.White,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Clip,
                                                modifier = Modifier.padding(start = if (compactBottomNav) 4.dp else 7.dp)
                                            )
                                        }
'''
assert old_row in n, "bottom nav label row not found"
n = n.replace(old_row, new_row, 1)

n = n.replace(
    '''                            val overlayExtra = 4.dp
                            val overlayShape = RoundedCornerShape(26.dp)
''',
    '''                            val overlayExtra = if (compactBottomNav) 3.dp else 4.dp
                            val overlayShape = RoundedCornerShape(if (compactBottomNav) 22.dp else 26.dp)
''',
    1
)

n = n.replace(
    '''                                    .height(50.dp)
''',
    '''                                    .height(if (compactBottomNav) 44.dp else 50.dp)
''',
    1
)

nav.write_text(n)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Fixed Home, Search and Library bottom tiles on small-width phones so labels stay on one line with compact spacing and icons instead of wrapping vertically",\n'
assert needle in a, "About changelog anchor not found for compact nav"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)


# Match Spotify-style playlist action wording for each song's three-dot menu.
opts = Path("app/src/main/java/com/wavelength/music/ui/components/TrackOptionsSheet.kt")
o = opts.read_text()

if "import androidx.compose.material.icons.filled.Close\n" not in o:
    o = o.replace(
        "import androidx.compose.material.icons.filled.Album\n",
        "import androidx.compose.material.icons.filled.Album\nimport androidx.compose.material.icons.filled.Close\n",
        1
    )

old = '''            if (onRemoveFromPlaylist != null) {
                TrackOptionRow(Icons.Filled.Delete, "Remove from playlist") {
                    onRemoveFromPlaylist()
                }
            }
'''
new = '''            if (onRemoveFromPlaylist != null) {
                TrackOptionRow(Icons.Filled.Close, "Hide in this playlist") {
                    onRemoveFromPlaylist()
                    onDismiss()
                }
            }
'''
assert old in o, "playlist remove option not found"
o = o.replace(old, new, 1)
opts.write_text(o)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Added a Spotify-style Hide in this playlist action to each song menu inside user playlists",\n'
assert needle in a, "About changelog anchor not found for playlist hide"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)


# Let Up Next items use the same hide/remove action semantics.
pc = Path("app/src/main/java/com/wavelength/music/playback/PlayerController.kt")
ps = pc.read_text()
anchor = '''    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val c = controller ?: return
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices) return
        c.moveMediaItem(fromIndex, toIndex)
        currentQueue = currentQueue.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        _state.update { it.copy(queue = currentQueue, currentIndex = c.currentMediaItemIndex) }
    }
'''
insert = anchor + '''
    fun removeQueueItem(index: Int) {
        val c = controller ?: return
        // "Hide from Up Next" is intended for future items only; never remove the active song.
        if (index !in currentQueue.indices || index <= c.currentMediaItemIndex) return
        c.removeMediaItem(index)
        currentQueue = currentQueue.toMutableList().apply { removeAt(index) }
        _state.update { it.copy(queue = currentQueue, currentIndex = c.currentMediaItemIndex) }
    }
'''
assert anchor in ps, "moveQueueItem anchor not found"
ps = ps.replace(anchor, insert, 1)
pc.write_text(ps)

vm = Path("app/src/main/java/com/wavelength/music/ui/nowplaying/PlayerViewModel.kt")
vs = vm.read_text()
needle = '''    fun moveQueueItem(from: Int, to: Int) = playerController.moveQueueItem(from, to)
'''
replacement = needle + '''    fun removeQueueItem(index: Int) = playerController.removeQueueItem(index)
'''
assert needle in vs, "PlayerViewModel moveQueueItem anchor not found"
vs = vs.replace(needle, replacement, 1)
vm.write_text(vs)

opts = Path("app/src/main/java/com/wavelength/music/ui/components/TrackOptionsSheet.kt")
o = opts.read_text()
sig = '''    onDismiss: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
'''
sig2 = '''    onDismiss: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onRemoveFromQueue: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
'''
assert sig in o, "TrackOptionsSheet signature anchor not found"
o = o.replace(sig, sig2, 1)

menu_anchor = '''            if (onRemoveFromPlaylist != null) {
                TrackOptionRow(Icons.Filled.Close, "Hide in this playlist") {
                    onRemoveFromPlaylist()
                    onDismiss()
                }
            }
'''
menu_insert = menu_anchor + '''            if (onRemoveFromQueue != null) {
                TrackOptionRow(Icons.Filled.Close, "Hide from Up Next") {
                    onRemoveFromQueue()
                    onDismiss()
                }
            }
'''
assert menu_anchor in o, "playlist hide action not found"
o = o.replace(menu_anchor, menu_insert, 1)
opts.write_text(o)

np = Path("app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt")
ns = np.read_text()

queue_sheet = '''            onDismiss = { menuQueueIndex = null },
            onMoveUp = if (queueIndex > state.currentIndex + 1) {
'''
queue_sheet_new = '''            onDismiss = { menuQueueIndex = null },
            onRemoveFromQueue = { viewModel.removeQueueItem(queueIndex) },
            onMoveUp = if (queueIndex > state.currentIndex + 1) {
'''
assert queue_sheet in ns, "Up Next TrackOptionsSheet anchor not found"
ns = ns.replace(queue_sheet, queue_sheet_new, 1)

# Glass Up Next cards: long-press opens the same options sheet, including Hide from Up Next.
if "import androidx.compose.foundation.combinedClickable\n" not in ns:
    ns = ns.replace(
        "import androidx.compose.foundation.clickable\n",
        "import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.combinedClickable\n",
        1
    )

glass_click = '''                                        .background(Color(0xFFD7ECFF).copy(alpha = 0.52f))
                                        .clickable { viewModel.playQueueItem(state.currentIndex + 1 + index) }
                                        .padding(5.dp),
'''
glass_combined = '''                                        .background(Color(0xFFD7ECFF).copy(alpha = 0.52f))
                                        .combinedClickable(
                                            onClick = { viewModel.playQueueItem(state.currentIndex + 1 + index) },
                                            onLongClick = { menuQueueIndex = state.currentIndex + 1 + index }
                                        )
                                        .padding(5.dp),
'''
assert glass_click in ns, "Glass queue card click anchor not found"
ns = ns.replace(glass_click, glass_combined, 1)
np.write_text(ns)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Extended the hide action to Up Next: queued songs now have Hide from Up Next, and Glass queue cards expose the same menu on long-press",\n'
assert needle in a, "About changelog anchor not found for Up Next hide"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
