package com.wavelength.music.data.backup

/** Compact QR payload — a QR code can only hold so much data before it's too dense to scan
 * reliably, so this only carries enough per track (id, name, artist) to re-find it on JioSaavn on
 * the receiving end, not full playback URLs like [BackupTrack] does. */
data class QrPlaylist(
    val n: String,
    val t: List<QrTrack>
)

data class QrTrack(
    val i: String,
    val n: String,
    val a: String
)

const val MAX_QR_PLAYLIST_TRACKS = 15
