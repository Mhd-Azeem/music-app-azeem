package com.wavelength.music.data.backup

import com.squareup.moshi.JsonClass

/** On-disk shape of a local backup file. Folders aren't preserved (only playlists and their
 * tracks, plus favorites) — restoring rebuilds flat playlists, which is enough to get your
 * library back after a reinstall or update without needing any server.
 *
 * `@JsonClass(generateAdapter = true)` gives this (and [BackupPlaylist]/[BackupTrack]) a
 * compile-time-generated Moshi adapter instead of falling back to Moshi's runtime-reflection
 * Kotlin adapter, which needs extra ProGuard/R8 keep rules to survive minification and is slower
 * to resolve at runtime besides. */
@JsonClass(generateAdapter = true)
data class BackupData(
    val version: Int = 1,
    val favorites: List<BackupTrack> = emptyList(),
    val playlists: List<BackupPlaylist> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BackupPlaylist(
    val name: String,
    val tracks: List<BackupTrack> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BackupTrack(
    val id: String,
    val name: String,
    val artist: String,
    val albumArtUrl: String,
    val audioUrl: String,
    val source: String
)

data class ImportSummary(val favoriteCount: Int, val playlistCount: Int)
