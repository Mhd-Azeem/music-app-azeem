from pathlib import Path
p=Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s=p.read_text()
needle='import androidx.compose.foundation.layout.Row\n'
imp='import androidx.compose.foundation.layout.Spacer\n'
assert needle in s
if imp not in s:
    s=s.replace(needle, needle+imp, 1)
p.write_text(s)
