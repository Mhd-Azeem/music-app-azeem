package com.wavelength.music.data.remote.jiosaavn

import com.wavelength.music.data.model.JioSaavnSong

fun JioSaavnSongDto.toDomain(): JioSaavnSong = JioSaavnSong(
    id = id,
    name = (name ?: song ?: "Unknown title").decodeHtmlEntities(),
    album = album.orEmpty().decodeHtmlEntities(),
    artist = (primaryArtists ?: singers ?: "Unknown artist").decodeHtmlEntities(),
    durationSeconds = duration?.toIntOrNull() ?: 0,
    imageUrl = image.lastOrNull()?.resolvedUrl ?: image.firstOrNull()?.resolvedUrl.orEmpty(),
    streamUrl = downloadUrl.lastOrNull()?.resolvedUrl ?: downloadUrl.firstOrNull()?.resolvedUrl.orEmpty()
)

// JioSaavn's API commonly HTML-escapes song/artist names (e.g. "&amp;", "&quot;").
private fun String.decodeHtmlEntities(): String = this
    .replace("&amp;", "&")
    .replace("&quot;", "\"")
    .replace("&#039;", "'")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
