from pathlib import Path

search_path = Path('app/src/main/java/com/wavelength/music/ui/search/SearchViewModel.kt')
about_path = Path('app/src/main/java/com/wavelength/music/ui/settings/AboutSheet.kt')

s = search_path.read_text()

old = '''        repository.searchTracks(effectiveQuery, page = 0, limit = SEARCH_PAGE_SIZE).fold(
            onSuccess = { tracks ->
                // Show normal metadata results immediately.
                val ranked = rankTracks(dedupeTracks(tracks), q)
'''
new = '''        repository.searchTracks(effectiveQuery, page = 0, limit = SEARCH_PAGE_SIZE).fold(
            onSuccess = { tracks ->
                // Modifier-heavy queries such as "song name slowed reverb" often make JioSaavn
                // over-weight the generic modifier words. Fetch the base title too, merge both
                // result sets, then let our local ranker prefer the exact song + requested variant.
                val baseQuery = baseTitleQuery(q)
                val baseTracks = if (baseQuery.isNotBlank() && baseQuery != normalizeForMatch(q)) {
                    val baseEffective = if (selected == "All") baseQuery else "$baseQuery $selected"
                    repository.searchTracks(baseEffective, page = 0, limit = SEARCH_PAGE_SIZE)
                        .getOrElse { emptyList() }
                } else {
                    emptyList()
                }
                val ranked = rankTracks(dedupeTracks(tracks + baseTracks), q)
'''
if old not in s:
    raise SystemExit('runSearch marker not found')
s = s.replace(old, new, 1)

old = '''            val canonicalTitle = canonicalSongTitle(track.name)
            val canonicalLanguage = track.language.lowercase().trim()

            // JioSaavn often returns the same recording several times with different IDs,
            // album metadata, featured-artist ordering, or suffixes such as "(From ...)",
            // "- Single", "Original Motion Picture Soundtrack", etc. For search results,
            // title + language is intentionally the primary identity so those copies collapse.
            val key = "$canonicalLanguage|$canonicalTitle"
'''
new = '''            val canonicalTitle = canonicalSongTitle(track.name)
            val canonicalLanguage = track.language.lowercase().trim()
            val variant = variantSignature(track.name)

            // Keep true duplicates collapsed, but preserve meaningful versions such as
            // slowed/reverb, lofi, sped-up, remix and instrumental as separate results.
            val key = "$canonicalLanguage|$canonicalTitle|$variant"
'''
if old not in s:
    raise SystemExit('dedupe marker not found')
s = s.replace(old, new, 1)

start = s.index('    private fun rankTracks(tracks: List<Track>, query: String): List<Track> {')
end = s.index('    private fun normalizeForMatch(value: String): String', start)
replacement = '''    private fun rankTracks(tracks: List<Track>, query: String): List<Track> {
        val q = normalizeForMatch(query)
        if (q.isBlank()) return tracks
        val base = baseTitleQuery(query)
        val requestedVariants = requestedVariantTokens(query)
        val queryWords = base.split(' ').filter { it.isNotBlank() }

        fun tokenSimilarity(a: String, b: String): Int {
            if (a == b) return 100
            if (a.startsWith(b) || b.startsWith(a)) return 70
            if (a.length >= 5 && b.length >= 5 && levenshtein(a, b) <= 2) return 55
            return 0
        }

        fun baseMatchScore(titleBase: String): Int {
            if (base.isBlank()) return 0
            if (titleBase == base) return 1800
            if (titleBase.startsWith(base) || base.startsWith(titleBase)) return 1200
            if (titleBase.contains(base) || base.contains(titleBase)) return 900
            val titleWords = titleBase.split(' ').filter { it.isNotBlank() }
            return queryWords.sumOf { qw -> titleWords.maxOfOrNull { tw -> tokenSimilarity(qw, tw) } ?: 0 } * 5
        }

        fun score(track: Track): Int {
            val title = normalizeForMatch(track.name)
            val titleBase = canonicalSongTitle(track.name)
            val artist = normalizeForMatch(track.artistName)
            val album = normalizeForMatch(track.albumName)
            val variants = variantSignature(track.name).split('+').filter { it.isNotBlank() }.toSet()
            var score = baseMatchScore(titleBase)

            // Requested versions are strong positive signals only after title relevance.
            if (requestedVariants.isNotEmpty()) {
                score += requestedVariants.count { it in variants } * 420
                if (requestedVariants.all { it in variants }) score += 500
                if (variants.isEmpty()) score -= 120
            } else if (variants.isNotEmpty()) {
                score -= 40
            }

            if (title == q) score += 700
            if (artist == q) score += 350
            if (artist.contains(base)) score += 120
            if (album.contains(base)) score += 80
            if (track.source != com.wavelength.music.data.model.TrackSource.JIOSAAVN) score += 40
            return score
        }

        return tracks.withIndex()
            .sortedWith(compareByDescending<IndexedValue<Track>> { score(it.value) }.thenBy { it.index })
            .map { it.value }
    }

    private fun requestedVariantTokens(value: String): Set<String> {
        val n = normalizeForMatch(value)
        val out = linkedSetOf<String>()
        if (Regex("\\bslowed\\b").containsMatchIn(n)) out += "slowed"
        if (Regex("\\breverb\\b").containsMatchIn(n)) out += "reverb"
        if (Regex("\\b(lofi|lo fi)\\b").containsMatchIn(n)) out += "lofi"
        if (Regex("\\b(sped up|speed up|nightcore)\\b").containsMatchIn(n)) out += "spedup"
        if (Regex("\\bremix\\b").containsMatchIn(n)) out += "remix"
        if (Regex("\\binstrumental\\b").containsMatchIn(n)) out += "instrumental"
        if (Regex("\\bkaraoke\\b").containsMatchIn(n)) out += "karaoke"
        return out
    }

    private fun variantSignature(value: String): String = requestedVariantTokens(value).sorted().joinToString("+")

    private fun baseTitleQuery(value: String): String {
        var n = normalizeForMatch(value)
        val phrases = listOf("sped up", "speed up", "nightcore", "slowed", "reverb", "lofi", "lo fi", "remix", "instrumental", "karaoke", "version", "edit")
        phrases.forEach { p -> n = n.replace(Regex("\\b${Regex.escape(p)}\\b"), " ") }
        return n.replace(Regex("\\s+"), " ").trim()
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + cost)
            }
            previous = current
        }
        return previous[b.length]
    }

'''
s = s[:start] + replacement + s[end:]
search_path.write_text(s)

about = about_path.read_text()
needle = 'private val latestUpdates = listOf(\n'
entry = '    "Search now prioritizes the exact song title before version words like slowed, reverb, lofi, remix and sped-up",\n'
if entry not in about:
    if needle not in about:
        raise SystemExit('About latestUpdates marker not found')
    about = about.replace(needle, needle + entry, 1)
about_path.write_text(about)
