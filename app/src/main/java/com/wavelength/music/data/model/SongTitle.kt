package com.wavelength.music.data.model

internal fun requestedVariantTokens(value: String): Set<String> {
    val n = normalizeForMatch(value)
    val out = linkedSetOf<String>()
    if (Regex("""\bslowed\b""").containsMatchIn(n)) out += "slowed"
    if (Regex("""\breverb\b""").containsMatchIn(n)) out += "reverb"
    if (Regex("""\b(lofi|lo fi)\b""").containsMatchIn(n)) out += "lofi"
    if (Regex("""\b(sped up|speed up|nightcore)\b""").containsMatchIn(n)) out += "spedup"
    if (Regex("""\bremix\b""").containsMatchIn(n)) out += "remix"
    if (Regex("""\binstrumental\b""").containsMatchIn(n)) out += "instrumental"
    if (Regex("""\bkaraoke\b""").containsMatchIn(n)) out += "karaoke"
    return out
}

internal fun variantSignature(value: String): String = requestedVariantTokens(value).sorted().joinToString("+")

internal fun baseTitleQuery(value: String): String {
    var n = normalizeForMatch(value)
    val phrases = listOf("sped up", "speed up", "nightcore", "slowed", "reverb", "lofi", "lo fi", "remix", "instrumental", "karaoke", "version", "edit")
    phrases.forEach { p -> n = n.replace(Regex("""\b${Regex.escape(p)}\b"""), " ") }
    return n.replace(Regex("""\s+"""), " ").trim()
}

internal fun normalizeForMatch(value: String): String = value
    .lowercase()
    .replace("&", " and ")
    .replace(Regex("[^a-z0-9\\p{L}]+"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

internal fun canonicalSongTitle(raw: String): String {
    return raw
        .lowercase()
        // Remove bracketed metadata/version labels.
        .replace(Regex("\\([^)]*(from|movie|film|soundtrack|version|remix|mix|edit|single|theme|ost|original|lofi|lo-fi|slowed|reverb|karaoke|instrumental)[^)]*\\)", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("\\[[^]]*(from|movie|film|soundtrack|version|remix|mix|edit|single|theme|ost|original|lofi|lo-fi|slowed|reverb|karaoke|instrumental)[^]]*]", RegexOption.IGNORE_CASE), " ")
        // Remove common dash suffixes added by catalog metadata.
        .replace(Regex("\\s*[-–—:]\\s*(from|original motion picture soundtrack|motion picture soundtrack|soundtrack|ost|single|song|theme|version|remix|mix|edit|lofi|lo-fi|slowed|reverb|karaoke|instrumental).*", RegexOption.IGNORE_CASE), " ")
        // Remove explicit 'from <movie>' tail even without punctuation.
        .replace(Regex("\\s+from\\s+.+$", RegexOption.IGNORE_CASE), " ")
        // Normalize punctuation/spacing so tiny naming differences collapse.
        .replace("&", " and ")
        .replace(Regex("[^a-z0-9\\p{L}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
