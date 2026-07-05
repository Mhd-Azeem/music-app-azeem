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
        .setMediaMetadata(metadata)
        .build()
}
