package com.wavelength.music.data.remote.jiosaavn

import com.wavelength.music.data.model.JioSaavnSong

fun JioSaavnSongDto.toDomain(): JioSaavnSong = JioSaavnSong(
    id = id,
    name = (name ?: "Unknown title").decodeHtmlEntities(),
    album = album?.name.orEmpty().decodeHtmlEntities(),
    artist = artists.resolveArtistName().decodeHtmlEntities(),
    durationSeconds = duration ?: 0,
    imageUrl = image.lastOrNull()?.resolvedUrl ?: image.firstOrNull()?.resolvedUrl.orEmpty(),
    streamUrl = downloadUrl.lastOrNull()?.resolvedUrl ?: downloadUrl.firstOrNull()?.resolvedUrl.orEmpty()
)

private fun JioSaavnArtistsDto?.resolveArtistName(): String {
    val names = this?.primary?.mapNotNull { it.name }?.filter { it.isNotBlank() }.orEmpty()
    if (names.isNotEmpty()) return names.joinToString(", ")
    return this?.all?.firstOrNull { !it.name.isNullOrBlank() }?.name ?: "Unknown artist"
}

// JioSaavn's API commonly HTML-escapes song/artist names (e.g. "&amp;", "&quot;").
private fun String.decodeHtmlEntities(): String = this
    .replace("&amp;", "&")
    .replace("&quot;", "\"")
    .replace("&#039;", "'")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
