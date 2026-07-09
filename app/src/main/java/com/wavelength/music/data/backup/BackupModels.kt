package com.wavelength.music.data.backup

/** On-disk shape of a local backup file. Folders aren't preserved (only playlists and their
 * tracks, plus favorites) — restoring rebuilds flat playlists, which is enough to get your
 * library back after a reinstall or update without needing any server. */
data class BackupData(
    val version: Int = 1,
    val favorites: List<BackupTrack> = emptyList(),
    val playlists: List<BackupPlaylist> = emptyList()
)

data class BackupPlaylist(
    val name: String,
    val tracks: List<BackupTrack> = emptyList()
)

data class BackupTrack(
    val id: String,
    val name: String,
    val artist: String,
    val albumArtUrl: String,
    val audioUrl: String,
    val source: String
)

data class ImportSummary(val favoriteCount: Int, val playlistCount: Int)
