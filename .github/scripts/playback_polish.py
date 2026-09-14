from pathlib import Path


def rep(path, old, new, required=True):
    p = Path(path)
    s = p.read_text()
    if new in s:
        return
    if old not in s:
        if required:
            raise SystemExit(f"pattern not found in {path}: {old[:80]!r}")
        return
    p.write_text(s.replace(old, new, 1))


settings = "app/src/main/java/com/wavelength/music/data/repository/SettingsRepository.kt"
vm = "app/src/main/java/com/wavelength/music/ui/settings/AppSettingsViewModel.kt"
screen = "app/src/main/java/com/wavelength/music/ui/settings/SettingsScreen.kt"
player = "app/src/main/java/com/wavelength/music/playback/PlayerController.kt"

rep(
    settings,
    "    val aiDjEnabled: Boolean = false,\n    /** Milliseconds to fade out the ending track and fade in the next one; 0 disables it. */",
    "    val aiDjEnabled: Boolean = false,\n    /** Native seamless playlist transitions when crossfade is off. */\n    val gaplessPlaybackEnabled: Boolean = true,\n    /** Milliseconds to fade out the ending track and fade in the next one; 0 disables it. */",
)
rep(
    settings,
    "        aiDjEnabled = prefs.getBoolean(KEY_AI_DJ, false),\n        crossfadeDurationMs = prefs.getInt(KEY_CROSSFADE, 0),",
    "        aiDjEnabled = prefs.getBoolean(KEY_AI_DJ, false),\n        gaplessPlaybackEnabled = prefs.getBoolean(KEY_GAPLESS_PLAYBACK, true),\n        crossfadeDurationMs = prefs.getInt(KEY_CROSSFADE, 0),",
)
rep(
    settings,
    "    fun setAiDjEnabled(enabled: Boolean) {\n        prefs.edit { putBoolean(KEY_AI_DJ, enabled) }\n        _state.update { it.copy(aiDjEnabled = enabled) }\n    }\n\n    fun setCrossfadeDurationMs",
    "    fun setAiDjEnabled(enabled: Boolean) {\n        prefs.edit { putBoolean(KEY_AI_DJ, enabled) }\n        _state.update { it.copy(aiDjEnabled = enabled) }\n    }\n\n    fun setGaplessPlaybackEnabled(enabled: Boolean) {\n        prefs.edit { putBoolean(KEY_GAPLESS_PLAYBACK, enabled) }\n        _state.update { it.copy(gaplessPlaybackEnabled = enabled) }\n    }\n\n    fun setCrossfadeDurationMs",
)
rep(
    settings,
    '        const val KEY_AI_DJ = "ai_dj_enabled"\n        const val KEY_CROSSFADE = "crossfade_duration_ms"',
    '        const val KEY_AI_DJ = "ai_dj_enabled"\n        const val KEY_GAPLESS_PLAYBACK = "gapless_playback_enabled"\n        const val KEY_CROSSFADE = "crossfade_duration_ms"',
)

rep(
    vm,
    "    fun setAiDjEnabled(enabled: Boolean) = settingsRepository.setAiDjEnabled(enabled)\n\n    fun setCrossfadeDurationMs",
    "    fun setAiDjEnabled(enabled: Boolean) = settingsRepository.setAiDjEnabled(enabled)\n\n    fun setGaplessPlaybackEnabled(enabled: Boolean) = settingsRepository.setGaplessPlaybackEnabled(enabled)\n\n    fun setCrossfadeDurationMs",
)

p = Path(screen)
s = p.read_text()
s = s.replace('SettingsSection(title = "AI DJ")', 'SettingsSection(title = "Smart Queue")')
s = s.replace(
    'Text("Keep the queue going automatically", modifier = Modifier.weight(1f))',
    'Text("Automatically continue Up Next", modifier = Modifier.weight(1f))',
)
s = s.replace(
    "When the queue is about to run out, automatically adds more songs similar to what you've been listening to, so playback never stops.",
    "When Up Next is almost empty, AzMusic adds related songs automatically so playback can continue.",
)
if 'SettingsSection(title = "Gapless Playback")' not in s:
    marker = '            item {\n                SettingsSection(title = "Crossfade") {'
    insert = '''            item {
                SettingsSection(title = "Gapless Playback") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Seamless track transitions")
                            Text(
                                text = if (settings.gaplessPlaybackEnabled) {
                                    "On • next track starts without an added pause"
                                } else {
                                    "Off • adds a short separation between tracks"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.gaplessPlaybackEnabled,
                            onCheckedChange = viewModel::setGaplessPlaybackEnabled
                        )
                    }
                    if (settings.crossfadeDurationMs > 0) {
                        Text(
                            text = "Crossfade takes priority while it is enabled.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

'''
    if marker not in s:
        raise SystemExit("Crossfade marker not found in SettingsScreen")
    s = s.replace(marker, insert + marker, 1)
p.write_text(s)

rep(
    player,
    "            val crossfadeMs = settingsRepository.state.value.crossfadeDurationMs\n            val isNaturalProgression = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||",
    "            val crossfadeMs = settingsRepository.state.value.crossfadeDurationMs\n            val gaplessEnabled = settingsRepository.state.value.gaplessPlaybackEnabled\n            val isNaturalProgression = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||",
)
rep(
    player,
    "            } else {\n                crossfadeJob?.cancel()\n                controller?.volume = targetVolume\n            }\n        }\n\n        override fun onShuffleModeEnabledChanged",
    '''            } else {
                crossfadeJob?.cancel()
                controller?.volume = targetVolume
                // Media3 playlists are naturally gapless for supported streams. When the user
                // explicitly disables Gapless Playback, add a short intentional separation only
                // for automatic transitions. Manual skips remain immediate.
                if (isNaturalProgression && !gaplessEnabled) {
                    val transitionIndex = index
                    controller?.pause()
                    controllerScope.launch {
                        delay(350L)
                        val active = controller
                        if (active != null && active.currentMediaItemIndex == transitionIndex &&
                            !requiresActivation(track)
                        ) {
                            active.play()
                        }
                    }
                }
            }
        }

        override fun onShuffleModeEnabledChanged''',
)

now_playing = Path("app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt").read_text()
if "detectVerticalDragGestures" not in now_playing or "collapseThresholdPx" not in now_playing:
    raise SystemExit("Swipe-down collapse implementation is missing")

final_player = Path(player).read_text()
for token in ["maybeStartCrossfadeOut", "extendQueueWithAiDj", "gaplessPlaybackEnabled"]:
    if token not in final_player:
        raise SystemExit(f"Missing playback token: {token}")
