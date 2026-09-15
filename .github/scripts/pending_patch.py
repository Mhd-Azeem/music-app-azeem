from pathlib import Path

# 1) Smart Queue: expose an explicit top-up hook that the Glass Now Playing screen can request.
pc = Path('app/src/main/java/com/wavelength/music/playback/PlayerController.kt')
s = pc.read_text()
needle = '''    private fun extendQueueWithAiDj(justPlayed: Track) {\n        if (aiDjExtendJob?.isActive == true) return\n'''
assert needle in s, 'AI DJ extension hook not found'
insert = '''    fun ensureSmartQueue() {\n        val c = controller ?: return\n        if (!settingsRepository.state.value.aiDjEnabled) return\n        val index = c.currentMediaItemIndex\n        val track = currentQueue.getOrNull(index)?.track ?: return\n        val remaining = currentQueue.size - index - 1\n        if (remaining <= 2) {\n            extendQueueWithAiDj(track)\n        }\n    }\n\n'''
if 'fun ensureSmartQueue()' not in s:
    s = s.replace(needle, insert + needle, 1)
pc.write_text(s)

vm = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/PlayerViewModel.kt')
s = vm.read_text()
needle = '    fun smartShuffle() = playerController.smartShuffleQueue()\n'
assert needle in s, 'smartShuffle hook not found'
if 'fun ensureSmartQueue()' not in s:
    s = s.replace(needle, needle + '    fun ensureSmartQueue() = playerController.ensureSmartQueue()\n', 1)
vm.write_text(s)

# 2) Glass Now Playing visuals and Smart Queue refresh.
np = Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s = np.read_text()

# Ask Smart Queue to top up whenever the visible Glass queue becomes short/empty.
needle = '    val accentColor = dynamicAccent ?: MaterialTheme.colorScheme.primary\n'
assert needle in s, 'accentColor anchor not found'
smart_effect = '''\n    LaunchedEffect(glassmorphismNowPlaying, state.currentIndex, state.queue.size, state.currentTrack?.id) {\n        if (glassmorphismNowPlaying) {\n            viewModel.ensureSmartQueue()\n        }\n    }\n'''
if 'viewModel.ensureSmartQueue()' not in s:
    s = s.replace(needle, needle + smart_effect, 1)

# Make the main Glass artwork the blur source for the lower frosted player panel.
old = '''                    .blur(5.dp)\n                    .alpha(1.00f),'''
new = '''                    .blur(5.dp)\n                    .alpha(1.00f)\n                    .haze(state = hazeState),'''
assert old in s, 'Glass background blur source anchor not found'
s = s.replace(old, new, 1)

# Remove the old hard-clipped second blurred artwork layer that caused a visible horizontal blur seam.
start_marker = '''        if (glassmorphismNowPlaying && !track?.albumArtUrl.isNullOrBlank()) {\n            // The lower playback zone is deliberately more frosted than the artwork/queue zone.'''
start = s.find(start_marker)
assert start >= 0, 'old lower blur separation block not found'
end = s.find('\n        Column(', start)
assert end > start, 'end of old lower blur separation block not found'
s = s[:start] + s[end:]

# Replace the solid light-blue fill with real frosted glass blur sampled from the background.
old = '''                        .clip(lowerGlassShape)\n                        .background(Color(0xFFD7ECFF).copy(alpha = 0.82f))\n                        .border(1.5.dp, Color.White.copy(alpha = 0.78f), lowerGlassShape)'''
new = '''                        .clip(lowerGlassShape)\n                        .hazeChild(state = hazeState, style = glassStyle) { inputScale = HazeInputScale.Auto }\n                        .border(1.5.dp, Color.White.copy(alpha = 0.82f), lowerGlassShape)'''
assert old in s, 'solid lower Glass panel fill not found'
s = s.replace(old, new, 1)

# Strengthen the Glass timeline color and visibility while keeping the same arc geometry.
count = s.count('Color(0xFF087EAE)')
assert count >= 2, f'expected at least two timeline color uses, found {count}'
s = s.replace('Color(0xFF087EAE)', 'Color(0xFF00A9D6)')
s = s.replace('style = Stroke(width = 2.4.dp.toPx())', 'style = Stroke(width = 3.2.dp.toPx())', 1)
np.write_text(s)

# 3) About -> Latest updates.
about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
a = about.read_text()
needle = 'private val latestUpdates = listOf(\n'
entry = '    "Fixed Smart Queue in Glass Now Playing, removed the hard blur seam, replaced the blue player fill with real frosted blur, and strengthened the timeline accent",\n'
assert needle in a, 'About latestUpdates anchor not found'
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
