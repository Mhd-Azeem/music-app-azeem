from pathlib import Path

search_path = Path('app/src/main/java/com/wavelength/music/ui/search/SearchViewModel.kt')
text = search_path.read_text()
repls = {
    'Regex("\\bslowed\\b")': 'Regex("""\\bslowed\\b""")',
    'Regex("\\breverb\\b")': 'Regex("""\\breverb\\b""")',
    'Regex("\\b(lofi|lo fi)\\b")': 'Regex("""\\b(lofi|lo fi)\\b""")',
    'Regex("\\b(sped up|speed up|nightcore)\\b")': 'Regex("""\\b(sped up|speed up|nightcore)\\b""")',
    'Regex("\\bremix\\b")': 'Regex("""\\bremix\\b""")',
    'Regex("\\binstrumental\\b")': 'Regex("""\\binstrumental\\b""")',
    'Regex("\\bkaraoke\\b")': 'Regex("""\\bkaraoke\\b""")',
    'Regex("\\b${Regex.escape(p)}\\b")': 'Regex("""\\b${Regex.escape(p)}\\b""")',
    'Regex("\\s+")': 'Regex("""\\s+""")',
}
for old, new in repls.items():
    text = text.replace(old, new)
search_path.write_text(text)

about_path = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')
about = about_path.read_text()
item = '    "Fixed exact song-variant search matching and ranking for slowed, reverb, lofi, remix, and sped-up queries",\n'
marker = 'private val latestUpdates = listOf(\n'
if item not in about:
    about = about.replace(marker, marker + item)
about_path.write_text(about)
