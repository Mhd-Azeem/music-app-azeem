package com.wavelength.music.data.model

data class LyricLine(val timestampMs: Long, val text: String)

private val LRC_TIMESTAMP_REGEX = Regex("""\[(\d+):(\d+(?:\.\d+)?)]""")

/** Parses standard LRC format (`[mm:ss.xx]lyric text` per line) into timestamped lines, dropping
 * any metadata lines (`[ar:...]`, `[ti:...]`, etc.) that don't match the timestamp pattern. Some
 * lines carry multiple leading timestamp tags sharing one piece of text (e.g. a repeated chorus:
 * `[00:12.34][00:45.67]Chorus text`) — each tag becomes its own [LyricLine] with that same text,
 * rather than letting the second tag leak into the first line's displayed text. */
fun parseLrc(lrc: String): List<LyricLine> = lrc.lineSequence()
    .flatMap { line ->
        val matches = LRC_TIMESTAMP_REGEX.findAll(line).toList()
        if (matches.isEmpty()) return@flatMap emptySequence()
        val text = line.substring(matches.last().range.last + 1).trim()
        matches.asSequence().mapNotNull { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val seconds = match.groupValues[2].toDoubleOrNull() ?: return@mapNotNull null
            val timestampMs = minutes * 60_000L + (seconds * 1000).toLong()
            LyricLine(timestampMs, text)
        }
    }
    .sortedBy { it.timestampMs }
    .toList()
