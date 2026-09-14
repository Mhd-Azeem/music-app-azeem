from pathlib import Path
p=Path('app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt')
s=p.read_text()
anchor='import androidx.compose.ui.graphics.Color\n'
for imp in ['import androidx.compose.ui.graphics.Brush\n','import androidx.compose.ui.graphics.Path\n']:
    if imp not in s:
        s=s.replace(anchor,anchor+imp,1)
p.write_text(s)
