package com.wavelength.music.playback

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.wavelength.music.data.model.Track

fun Track.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setArtist(artistName)
        .setAlbumTitle(albumName)
        .setArtworkUri(albumArtUrl.takeIf { it.isNotBlank() }?.toUri())
        .build()

    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(audioUrl.toUri())
        // Keep a stable cache key even if the remote stream URL changes between requests.
        // That lets a song already heard once reuse the same on-device audio cache later.
        .setCustomCacheKey("azmusic:$id")
        .setMediaMetadata(metadata)
        .build()
}
