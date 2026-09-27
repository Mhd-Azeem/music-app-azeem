from pathlib import Path

p = Path("app/src/main/java/com/wavelength/music/ui/settings/SettingsScreen.kt")
s = p.read_text()

# Import Material 3 divider once.
if "import androidx.compose.material3.HorizontalDivider\n" not in s:
    s = s.replace(
        "import androidx.compose.material3.FilterChip\n",
        "import androidx.compose.material3.FilterChip\nimport androidx.compose.material3.HorizontalDivider\n",
        1
    )

old = '''@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}
'''

new = '''@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, top = 12.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        )
    }
}
'''

assert old in s, "SettingsSection helper not found"
s = s.replace(old, new, 1)
p.write_text(s)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Added subtle inset horizontal separators between settings inside every expanded Settings category for clearer visual grouping",\n'
assert needle in a, "About changelog anchor not found"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
