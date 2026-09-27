from pathlib import Path

home = Path("app/src/main/java/com/wavelength/music/ui/home/HomeScreen.kt")
s = home.read_text()

if "import com.wavelength.music.data.repository.VisualThemeMode\n" not in s:
    s = s.replace(
        "import com.wavelength.music.data.model.Track\n",
        "import com.wavelength.music.data.model.Track\nimport com.wavelength.music.data.repository.VisualThemeMode\n",
        1
    )
if "import com.wavelength.music.ui.settings.AppSettingsViewModel\n" not in s:
    s = s.replace(
        "import com.wavelength.music.ui.design.UiDesignConfig\n",
        "import com.wavelength.music.ui.design.UiDesignConfig\nimport com.wavelength.music.ui.settings.AppSettingsViewModel\n",
        1
    )

state_anchor = '''    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    Scaffold(
'''
state_new = '''    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val settingsViewModel: AppSettingsViewModel = hiltViewModel()
    val settings by settingsViewModel.state.collectAsStateWithLifecycle()

    Scaffold(
'''
assert state_anchor in s, "Home settings state anchor not found"
s = s.replace(state_anchor, state_new, 1)

old = '''                val purpleGlassBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xCC332060),
                        Color(0xCC56367F),
                        Color(0xCC764595)
                    )
                )
                val glassBorder = Color.White.copy(alpha = 0.48f)

                Box(
                    modifier = Modifier
                        .shadow(12.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(purpleGlassBrush)
                        .border(1.3.dp, glassBorder, RoundedCornerShape(24.dp))
                        .padding(horizontal = 18.dp, vertical = 11.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .shadow(12.dp, RoundedCornerShape(23.dp))
                        .clip(RoundedCornerShape(23.dp))
                        .background(purpleGlassBrush)
                        .border(1.3.dp, glassBorder, RoundedCornerShape(23.dp))
                        .clickable(onClick = onStatisticsClick)
                        .padding(horizontal = 17.dp, vertical = 11.dp)
                ) {
                    Text(
                        "Stats",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(purpleGlassBrush)
                        .border(1.3.dp, glassBorder, CircleShape)
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(25.dp)
                    )
                }
'''

new = '''                val headerBrush = when (settings.visualThemeMode) {
                    VisualThemeMode.SOLID -> Brush.linearGradient(
                        listOf(Color(0xFF181818), Color(0xFF101010))
                    )
                    VisualThemeMode.LIQUID -> Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.58f),
                            Color(0xCC29436B),
                            Color(0xAA142033)
                        )
                    )
                    VisualThemeMode.GLASSMORPHISM -> Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.20f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.26f),
                            Color(0x66152D48)
                        )
                    )
                    VisualThemeMode.NEOMORPHISM -> Brush.linearGradient(
                        listOf(Color(0xFF38414F), Color(0xFF272E39), Color(0xFF1A1F27))
                    )
                    VisualThemeMode.AMOLED -> Brush.linearGradient(
                        listOf(Color.Black, Color(0xFF050505))
                    )
                    VisualThemeMode.ALBUM_ADAPTIVE -> Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                            Color(0xCC090C12)
                        )
                    )
                    VisualThemeMode.AURORA -> Brush.linearGradient(
                        listOf(
                            Color(0xDD4338CA),
                            Color(0xDD7E22CE),
                            Color(0xBB0891B2)
                        )
                    )
                }

                val headerRadius = when (settings.visualThemeMode) {
                    VisualThemeMode.SOLID -> 10.dp
                    VisualThemeMode.LIQUID -> 28.dp
                    VisualThemeMode.GLASSMORPHISM -> 32.dp
                    VisualThemeMode.NEOMORPHISM -> 20.dp
                    VisualThemeMode.AMOLED -> 3.dp
                    VisualThemeMode.ALBUM_ADAPTIVE -> 22.dp
                    VisualThemeMode.AURORA -> 34.dp
                }
                val headerShape = RoundedCornerShape(headerRadius)
                val headerBorder = when (settings.visualThemeMode) {
                    VisualThemeMode.SOLID -> MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
                    VisualThemeMode.LIQUID -> Color.White.copy(alpha = 0.34f)
                    VisualThemeMode.GLASSMORPHISM -> Color.White.copy(alpha = 0.60f)
                    VisualThemeMode.NEOMORPHISM -> Color.White.copy(alpha = 0.10f)
                    VisualThemeMode.AMOLED -> Color.White.copy(alpha = 0.22f)
                    VisualThemeMode.ALBUM_ADAPTIVE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.96f)
                    VisualThemeMode.AURORA -> Color.White.copy(alpha = 0.72f)
                }
                val headerElevation = when (settings.visualThemeMode) {
                    VisualThemeMode.AMOLED -> 0.dp
                    VisualThemeMode.SOLID -> 4.dp
                    VisualThemeMode.NEOMORPHISM -> 18.dp
                    VisualThemeMode.AURORA -> 20.dp
                    VisualThemeMode.ALBUM_ADAPTIVE -> 14.dp
                    else -> 12.dp
                }
                val settingsShape = when (settings.visualThemeMode) {
                    VisualThemeMode.SOLID -> RoundedCornerShape(10.dp)
                    VisualThemeMode.LIQUID -> RoundedCornerShape(26.dp)
                    VisualThemeMode.GLASSMORPHISM -> CircleShape
                    VisualThemeMode.NEOMORPHISM -> RoundedCornerShape(16.dp)
                    VisualThemeMode.AMOLED -> RoundedCornerShape(2.dp)
                    VisualThemeMode.ALBUM_ADAPTIVE -> RoundedCornerShape(18.dp)
                    VisualThemeMode.AURORA -> CircleShape
                }

                Box(
                    modifier = Modifier
                        .shadow(
                            headerElevation,
                            headerShape,
                            ambientColor = if (settings.visualThemeMode == VisualThemeMode.AURORA) Color(0xFF7C3AED) else Color.Black,
                            spotColor = if (settings.visualThemeMode == VisualThemeMode.ALBUM_ADAPTIVE) MaterialTheme.colorScheme.primary else Color.Black
                        )
                        .clip(headerShape)
                        .background(headerBrush)
                        .border(
                            if (settings.visualThemeMode == VisualThemeMode.AMOLED) 1.dp else 1.3.dp,
                            headerBorder,
                            headerShape
                        )
                        .padding(
                            horizontal = when (settings.visualThemeMode) {
                                VisualThemeMode.AMOLED, VisualThemeMode.SOLID -> 14.dp
                                VisualThemeMode.AURORA -> 20.dp
                                else -> 18.dp
                            },
                            vertical = when (settings.visualThemeMode) {
                                VisualThemeMode.AMOLED -> 9.dp
                                else -> 11.dp
                            }
                        )
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .shadow(
                            headerElevation,
                            headerShape,
                            ambientColor = if (settings.visualThemeMode == VisualThemeMode.AURORA) Color(0xFF7C3AED) else Color.Black,
                            spotColor = if (settings.visualThemeMode == VisualThemeMode.ALBUM_ADAPTIVE) MaterialTheme.colorScheme.primary else Color.Black
                        )
                        .clip(headerShape)
                        .background(headerBrush)
                        .border(
                            if (settings.visualThemeMode == VisualThemeMode.AMOLED) 1.dp else 1.3.dp,
                            headerBorder,
                            headerShape
                        )
                        .clickable(onClick = onStatisticsClick)
                        .padding(
                            horizontal = when (settings.visualThemeMode) {
                                VisualThemeMode.AMOLED, VisualThemeMode.SOLID -> 13.dp
                                else -> 17.dp
                            },
                            vertical = when (settings.visualThemeMode) {
                                VisualThemeMode.AMOLED -> 9.dp
                                else -> 11.dp
                            }
                        )
                ) {
                    Text(
                        "Stats",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Box(
                    modifier = Modifier
                        .size(
                            when (settings.visualThemeMode) {
                                VisualThemeMode.AMOLED -> 44.dp
                                VisualThemeMode.AURORA -> 52.dp
                                else -> 48.dp
                            }
                        )
                        .shadow(
                            headerElevation,
                            settingsShape,
                            ambientColor = if (settings.visualThemeMode == VisualThemeMode.AURORA) Color(0xFF7C3AED) else Color.Black,
                            spotColor = if (settings.visualThemeMode == VisualThemeMode.ALBUM_ADAPTIVE) MaterialTheme.colorScheme.primary else Color.Black
                        )
                        .clip(settingsShape)
                        .background(headerBrush)
                        .border(
                            if (settings.visualThemeMode == VisualThemeMode.AMOLED) 1.dp else 1.3.dp,
                            headerBorder,
                            settingsShape
                        )
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(
                            when (settings.visualThemeMode) {
                                VisualThemeMode.AMOLED -> 22.dp
                                VisualThemeMode.AURORA -> 27.dp
                                else -> 25.dp
                            }
                        )
                    )
                }
'''
assert old in s, "Home header constant style block not found"
s = s.replace(old, new, 1)
home.write_text(s)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Home AZ Music, Stats and Settings controls now transform with the selected theme instead of staying as the same purple glass buttons",\n'
assert needle in a, "About changelog anchor not found"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
