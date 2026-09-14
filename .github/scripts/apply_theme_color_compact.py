from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match in {path}, found {count}: {old[:100]!r}")
    p.write_text(text.replace(old, new, 1))


settings = "app/src/main/java/com/wavelength/music/ui/settings/SettingsScreen.kt"
replace_once(
    settings,
    '    var showAlbumArtStyleMenu by remember { mutableStateOf(false) }\n',
    '    var showAlbumArtStyleMenu by remember { mutableStateOf(false) }\n'
    '    var showThemeColorPicker by remember { mutableStateOf(false) }\n'
)

old_color = '''            item {
                SettingsSection(title = "Liquid Theme Color") {
                    Text(
                        text = "Tap or drag anywhere in the color box to choose from millions of shades.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LiquidColorPicker(
                        selectedArgb = settings.customAccentArgb,
                        onColorSelected = viewModel::setCustomAccentArgb,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(190.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(settings.customAccentArgb))
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            )
                            Text(
                                text = "#%08X".format(settings.customAccentArgb),
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                        TextButton(onClick = { viewModel.setCustomAccentArgb(0xFF22D3EE.toInt()) }) {
                            Text("Reset")
                        }
                    }
                }
            }
'''

new_color = '''            item {
                SettingsSection(title = "Theme Accent Color") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showThemeColorPicker = !showThemeColorPicker }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(settings.customAccentArgb))
                                    .border(2.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                            )
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(text = "#%08X".format(settings.customAccentArgb))
                                Text(
                                    text = if (showThemeColorPicker) {
                                        "Tap to collapse color picker"
                                    } else {
                                        "Tap to customize all theme colors"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { showThemeColorPicker = !showThemeColorPicker }) {
                            Icon(Icons.Filled.ExpandMore, contentDescription = "Expand theme color picker")
                        }
                        TextButton(onClick = { viewModel.setCustomAccentArgb(0xFF22D3EE.toInt()) }) {
                            Text("Reset")
                        }
                    }
                    if (showThemeColorPicker) {
                        Text(
                            text = "Tap or drag anywhere in the color box. This accent applies to both solid and Liquid themes.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        LiquidColorPicker(
                            selectedArgb = settings.customAccentArgb,
                            onColorSelected = viewModel::setCustomAccentArgb,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .height(190.dp)
                        )
                    }
                }
            }
'''
replace_once(settings, old_color, new_color)

old_cover = '''                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Song cover as Liquid background")
                            Text(
                                text = if (settings.liquidAlbumArtBackground) {
                                    "On • cover artwork shown at 100% opacity"
                                } else {
                                    "Off • use the normal app wallpaper instead"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.liquidAlbumArtBackground,
                            onCheckedChange = viewModel::setLiquidAlbumArtBackground
                        )
                    }
'''
replace_once(settings, old_cover, '')

# Make the saved accent global, not Liquid-only. Keep each theme's surfaces/background identity,
# while primary/secondary controls and highlights use the selected user accent.
theme = "app/src/main/java/com/wavelength/music/ui/theme/Theme.kt"
for old, new in [
    ('        primary = WavelengthGreen,\n        secondary = WavelengthGreenLight,',
     '        primary = customAccent ?: WavelengthGreen,\n        secondary = customAccent?.copy(alpha = 0.78f) ?: WavelengthGreenLight,'),
    ('        primary = Color(0xFFED1D24),\n        secondary = Color(0xFFFFD700),',
     '        primary = customAccent ?: Color(0xFFED1D24),\n        secondary = customAccent?.copy(alpha = 0.78f) ?: Color(0xFFFFD700),'),
    ('        primary = Color(0xFFEC4899),\n        secondary = Color(0xFFF9A8D4),',
     '        primary = customAccent ?: Color(0xFFEC4899),\n        secondary = customAccent?.copy(alpha = 0.78f) ?: Color(0xFFF9A8D4),'),
    ('        primary = Color(0xFFE0E0E0),\n        secondary = Color(0xFF9E9E9E),',
     '        primary = customAccent ?: Color(0xFFE0E0E0),\n        secondary = customAccent?.copy(alpha = 0.78f) ?: Color(0xFF9E9E9E),'),
    ('        primary = Color(0xFF9E9E9E),\n        secondary = Color(0xFFBDBDBD),',
     '        primary = customAccent ?: Color(0xFF9E9E9E),\n        secondary = customAccent?.copy(alpha = 0.78f) ?: Color(0xFFBDBDBD),'),
    ('        primary = Color(0xFF22D3EE),\n        secondary = Color(0xFF0EA5E9),',
     '        primary = customAccent ?: Color(0xFF22D3EE),\n        secondary = customAccent?.copy(alpha = 0.78f) ?: Color(0xFF0EA5E9),'),
    ('        primary = Color(0xFFFF7A45),\n        secondary = Color(0xFFFFB84D),',
     '        primary = customAccent ?: Color(0xFFFF7A45),\n        secondary = customAccent?.copy(alpha = 0.78f) ?: Color(0xFFFFB84D),'),
]:
    replace_once(theme, old, new)
replace_once(
    theme,
    '        colorScheme = schemeFor(theme, if (theme.isGlass) customLiquidAccent else null),',
    '        colorScheme = schemeFor(theme, customLiquidAccent),'
)

# The removed control must not leave the old album-cover backdrop enabled invisibly.
nav = "app/src/main/java/com/wavelength/music/ui/navigation/NavGraph.kt"
replace_once(
    nav,
    '                        liquidAlbumArtBackground = settings.liquidAlbumArtBackground\n',
    '                        liquidAlbumArtBackground = false\n'
)

now_playing = "app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt"
replace_once(
    now_playing,
    '    liquidAlbumArtBackground: Boolean = true\n',
    '    liquidAlbumArtBackground: Boolean = false\n'
)

repo = "app/src/main/java/com/wavelength/music/data/repository/SettingsRepository.kt"
replace_once(
    repo,
    '    val liquidAlbumArtBackground: Boolean = true\n',
    '    val liquidAlbumArtBackground: Boolean = false\n'
)
replace_once(
    repo,
    '        liquidAlbumArtBackground = prefs.getBoolean(KEY_LIQUID_ALBUM_ART_BACKGROUND, true)\n',
    '        liquidAlbumArtBackground = false\n'
)

print("Compact global theme color patch applied.")
