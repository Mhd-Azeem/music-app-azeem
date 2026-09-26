from pathlib import Path

nav = Path("app/src/main/java/com/wavelength/music/ui/navigation/NavGraph.kt")
s = nav.read_text()

old = '''                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
'''
new = '''                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 10.dp)
                    ) {
'''
assert old in s, "bottom nav container not found"
s = s.replace(old, new, 1)
nav.write_text(s)

about = Path("app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt")
a = about.read_text()
needle = "private val latestUpdates = listOf(\n"
entry = '    "Slightly reduced the width of the Home, Search and Library floating tiles while keeping the draggable liquid overlay aligned",\n'
assert needle in a, "About changelog anchor not found"
if entry not in a:
    a = a.replace(needle, needle + entry, 1)
about.write_text(a)
