from pathlib import Path

settings = Path('app/src/main/java/com/wavelength/music/ui/settings/SettingsScreen.kt')
about = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')

s = settings.read_text()
old = '''                        if (settings.hasCustomBackground) {
                            OutlinedButton(onClick = viewModel::resetBackground) {
                                Text("Reset")
                            }
                        }
                    }
                }
            }
'''
new = '''                        if (settings.hasCustomBackground) {
                            OutlinedButton(onClick = viewModel::resetBackground) {
                                Text("Reset")
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Background opacity: ${(settings.backgroundOpacity * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = settings.backgroundOpacity,
                            onValueChange = viewModel::setBackgroundOpacity,
                            valueRange = 0f..1f
                        )
                    }
                }
            }
'''
if 'Background opacity: ${(settings.backgroundOpacity * 100).roundToInt()}%' not in s:
    if old not in s:
        raise SystemExit('Background section marker not found')
    s = s.replace(old, new, 1)
settings.write_text(s)

a = about.read_text()
entry = '    "Background opacity slider restored with live 0–100% control",\n'
marker = 'private val latestUpdates = listOf(\n'
if entry.strip() not in a:
    if marker not in a:
        raise SystemExit('Latest updates marker not found')
    a = a.replace(marker, marker + entry, 1)
about.write_text(a)
