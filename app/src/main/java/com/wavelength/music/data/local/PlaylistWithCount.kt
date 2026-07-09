package com.wavelength.music.data.local

data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val isFolder: Boolean,
    val parentFolderId: Long?,
    val trackCount: Int
)
