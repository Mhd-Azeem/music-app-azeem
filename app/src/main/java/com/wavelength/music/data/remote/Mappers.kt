package com.wavelength.music.data.remote

import com.wavelength.music.data.model.Album
import com.wavelength.music.data.model.Artist
import com.wavelength.music.data.model.Track
import com.wavelength.music.data.remote.dto.AlbumDto
import com.wavelength.music.data.remote.dto.ArtistDto
import com.wavelength.music.data.remote.dto.TrackDto

fun TrackDto.toDomain(
    fallbackAlbumArt: String = "",
    fallbackAlbumId: String = "",
    fallbackAlbumName: String = "",
    fallbackArtistId: String = "",
    fallbackArtistName: String = ""
): Track = Track(
    id = id,
    name = name,
    artistId = artistId.ifBlank { fallbackArtistId },
    artistName = artistName.ifBlank { fallbackArtistName },
    albumId = albumId.ifBlank { fallbackAlbumId },
    albumName = albumName.ifBlank { fallbackAlbumName },
    albumArtUrl = albumImage.ifBlank { image.ifBlank { fallbackAlbumArt } },
    audioUrl = audio,
    durationSeconds = duration
)

fun ArtistDto.toDomain(): Artist = Artist(
    id = id,
    name = name,
    imageUrl = image
)

fun AlbumDto.toDomain(): Album = Album(
    id = id,
    name = name,
    artistId = artistId,
    artistName = artistName,
    imageUrl = image
)
