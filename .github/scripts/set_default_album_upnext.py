from pathlib import Path

path = Path('app/src/main/java/com/wavelength/music/data/repository/SettingsRepository.kt')
text = path.read_text()

text = text.replace(
    'val expandUpNextOnScroll: Boolean = false,',
    'val expandUpNextOnScroll: Boolean = true,',
    1,
)
text = text.replace(
    'expandUpNextOnScroll = prefs.getBoolean(KEY_EXPAND_UP_NEXT, false),',
    'expandUpNextOnScroll = prefs.getBoolean(KEY_EXPAND_UP_NEXT, true),',
    1,
)

# AlbumArtStyle is intentionally kept OFF as the default for fresh installs.
assert 'val albumArtStyle: AlbumArtStyle = AlbumArtStyle.OFF,' in text
assert 'getOrDefault(AlbumArtStyle.OFF)' in text
assert 'else -> AlbumArtStyle.OFF' in text
assert 'val expandUpNextOnScroll: Boolean = true,' in text
assert 'expandUpNextOnScroll = prefs.getBoolean(KEY_EXPAND_UP_NEXT, true),' in text

path.write_text(text)
print('Default album art OFF and Up next ON applied.')
