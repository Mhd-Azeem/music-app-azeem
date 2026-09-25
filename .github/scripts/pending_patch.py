from pathlib import Path

main = Path("app/src/main/java/com/wavelength/music/MainActivity.kt")
s = main.read_text()

s = s.replace("import android.widget.VideoView\n", "")
s = s.replace("import androidx.compose.ui.viewinterop.AndroidView\n", "")
anchor = "import com.wavelength.music.ui.settings.AppSettingsViewModel\n"
if "import com.wavelength.music.ui.splash.AzMusicSplashScreen\n" not in s:
    assert anchor in s, "splash import anchor not found"
    s = s.replace(anchor, anchor + "import com.wavelength.music.ui.splash.AzMusicSplashScreen\n", 1)

s = s.replace(
    "var showIntroVideo by remember { mutableStateOf(savedInstanceState == null) }",
    "var showSplash by remember { mutableStateOf(savedInstanceState == null) }",
    1
)
s = s.replace("LaunchedEffect(showIntroVideo) {", "LaunchedEffect(showSplash) {", 1)
s = s.replace("if (!showIntroVideo && Build.VERSION.SDK_INT", "if (!showSplash && Build.VERSION.SDK_INT", 1)

old_start = s.index("                if (showIntroVideo) {")
old_end = s.index("                }\n                }\n            }", old_start)
old_end += len("                }\n")
s = s[:old_start] + '''                if (showSplash) {
                    AzMusicSplashScreen(
                        onFinished = { showSplash = false }
                    )
                }
''' + s[old_end:]
main.write_text(s)


about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Completely redesigned startup with a native dark-purple launch screen and a new animated AZ Music glass splash with glowing logo, waveform bars and smooth handoff into the app",\n'
assert needle in a, "About changelog anchor missing"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
