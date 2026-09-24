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

old = '''                if (showIntroVideo) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(
                                    Uri.parse(
                                        "android.resource://\${ctx.packageName}/\${R.raw.azmusic_intro}"
                                    )
                                )
                                setOnPreparedListener { mediaPlayer ->
                                    mediaPlayer.setVolume(0f, 0f)
                                    mediaPlayer.isLooping = false
                                    start()
                                }
                                setOnCompletionListener {
                                    showIntroVideo = false
                                }
                                setOnErrorListener { _, _, _ ->
                                    showIntroVideo = false
                                    true
                                }
                                setOnClickListener {
                                    stopPlayback()
                                    showIntroVideo = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }
'''
new = '''                if (showSplash) {
                    AzMusicSplashScreen(
                        onFinished = { showSplash = false }
                    )
                }
'''
assert old in s, "old intro video block not found"
s = s.replace(old, new, 1)
main.write_text(s)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Completely redesigned startup with a native dark-purple launch screen and a new animated AZ Music glass splash with glowing logo, waveform bars and smooth handoff into the app",\n'
assert needle in a, "About changelog anchor missing"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
