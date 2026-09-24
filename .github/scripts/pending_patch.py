from pathlib import Path

p = Path("app/src/main/java/com/wavelength/music/ui/settings/SettingsScreen.kt")
s = p.read_text()

# Expand/collapse icon.
if "import androidx.compose.material.icons.filled.ExpandLess\n" not in s:
    s = s.replace(
        "import androidx.compose.material.icons.filled.ExpandMore\n",
        "import androidx.compose.material.icons.filled.ExpandMore\nimport androidx.compose.material.icons.filled.ExpandLess\n",
        1
    )

# Accordion state: one category open at a time; Appearance starts open.
anchor = '''    var showThemeColorPicker by remember { mutableStateOf(false) }
'''
replacement = '''    var showThemeColorPicker by remember { mutableStateOf(false) }
    var expandedSettingsCategory by remember { mutableStateOf<String?>("Appearance & Interface") }
'''
assert anchor in s, "settings state anchor not found"
s = s.replace(anchor, replacement, 1)

# Appearance & Interface wraps the existing visual/theme/player-interface settings.
marker = '''        LazyColumn(modifier = Modifier.fillMaxWidth().padding(padding)) {
            item {
                SettingsSection(title = "App icon") {
'''
repl = '''        LazyColumn(modifier = Modifier.fillMaxWidth().padding(padding)) {
            item {
                SettingsCategoryHeader(
                    title = "Appearance & Interface",
                    subtitle = "App icon, backgrounds, colors, Now Playing and Glass theme",
                    expanded = expandedSettingsCategory == "Appearance & Interface",
                    onClick = {
                        expandedSettingsCategory =
                            if (expandedSettingsCategory == "Appearance & Interface") null else "Appearance & Interface"
                    }
                )
            }
            if (expandedSettingsCategory == "Appearance & Interface") {
            item {
                SettingsSection(title = "App icon") {
'''
assert marker in s, "Appearance category start not found"
s = s.replace(marker, repl, 1)

# Close Appearance after Glassmorphism, then open Playback.
marker = '''            item {
                SettingsSection(title = "Smart Queue") {
'''
repl = '''            }

            item {
                SettingsCategoryHeader(
                    title = "Playback",
                    subtitle = "Queue behavior, gapless playback, crossfade and visualizer",
                    expanded = expandedSettingsCategory == "Playback",
                    onClick = {
                        expandedSettingsCategory =
                            if (expandedSettingsCategory == "Playback") null else "Playback"
                    }
                )
            }
            if (expandedSettingsCategory == "Playback") {
            item {
                SettingsSection(title = "Smart Queue") {
'''
assert marker in s, "Playback category start not found"
s = s.replace(marker, repl, 1)

# Close Playback after Audio Visualizer, then Library & Data.
marker = '''            item {
                SettingsSection(title = "Downloads") {
'''
repl = '''            }

            item {
                SettingsCategoryHeader(
                    title = "Library & Data",
                    subtitle = "Downloads, listening statistics, backup and restore",
                    expanded = expandedSettingsCategory == "Library & Data",
                    onClick = {
                        expandedSettingsCategory =
                            if (expandedSettingsCategory == "Library & Data") null else "Library & Data"
                    }
                )
            }
            if (expandedSettingsCategory == "Library & Data") {
            item {
                SettingsSection(title = "Downloads") {
'''
assert marker in s, "Library category start not found"
s = s.replace(marker, repl, 1)

# Close Library & Data after Backup & Restore, then Audio.
marker = '''            item {
                SettingsSection(title = "Equalizer") {
'''
repl = '''            }

            item {
                SettingsCategoryHeader(
                    title = "Audio",
                    subtitle = "Equalizer, bass boost and volume booster",
                    expanded = expandedSettingsCategory == "Audio",
                    onClick = {
                        expandedSettingsCategory =
                            if (expandedSettingsCategory == "Audio") null else "Audio"
                    }
                )
            }
            if (expandedSettingsCategory == "Audio") {
            item {
                SettingsSection(title = "Equalizer") {
'''
assert marker in s, "Audio category start not found"
s = s.replace(marker, repl, 1)

# Close Audio after Volume Booster, then System & Account.
marker = '''            item {
                SettingsSection(title = "Cloudflare Usage") {
'''
repl = '''            }

            item {
                SettingsCategoryHeader(
                    title = "System & Account",
                    subtitle = "Cloudflare usage, email activation and app information",
                    expanded = expandedSettingsCategory == "System & Account",
                    onClick = {
                        expandedSettingsCategory =
                            if (expandedSettingsCategory == "System & Account") null else "System & Account"
                    }
                )
            }
            if (expandedSettingsCategory == "System & Account") {
            item {
                SettingsSection(title = "Cloudflare Usage") {
'''
assert marker in s, "System category start not found"
s = s.replace(marker, repl, 1)

# Close System & Account after About before developer credit.
marker = '''            item {
                Text(
                    text = "CREATED AND DEVELOPED BY MOHAMMED AZEEM©",
'''
repl = '''            }

            item {
                Text(
                    text = "CREATED AND DEVELOPED BY MOHAMMED AZEEM©",
'''
assert marker in s, "System category end not found"
s = s.replace(marker, repl, 1)

# Add reusable expandable category header above SettingsSection.
anchor = '''@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
'''
category = '''@Composable
private fun SettingsCategoryHeader(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (expanded) "Collapse $title" else "Expand $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
'''
assert anchor in s, "SettingsSection composable anchor not found"
s = s.replace(anchor, category, 1)

p.write_text(s)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Organized Settings into expandable Appearance & Interface, Playback, Library & Data, Audio, and System & Account categories",\n'
assert needle in a, "About changelog anchor not found"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
