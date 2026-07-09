package com.wavelength.music.data.repository

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.squareup.moshi.Moshi
import com.wavelength.music.data.backup.MAX_QR_PLAYLIST_TRACKS
import com.wavelength.music.data.backup.QrPlaylist
import com.wavelength.music.data.backup.QrTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QrPlaylistRepository @Inject constructor(
    moshi: Moshi,
    private val repository: MusicRepository
) {
    private val adapter = moshi.adapter(QrPlaylist::class.java)

    /** Encodes up to [MAX_QR_PLAYLIST_TRACKS] tracks (longer playlists are truncated) — a QR
     * code can only hold so much data before it gets too dense to scan reliably. */
    suspend fun encodePlaylistToQr(playlistId: Long, playlistName: String): Result<Bitmap> =
        withContext(Dispatchers.Default) {
            runCatching {
                val tracks = repository.observePlaylistTracks(playlistId).first().take(MAX_QR_PLAYLIST_TRACKS)
                if (tracks.isEmpty()) error("This playlist is empty")
                val payload = QrPlaylist(n = playlistName, t = tracks.map { QrTrack(it.id, it.name, it.artistName) })
                renderQrBitmap(adapter.toJson(payload))
            }
        }

    private fun renderQrBitmap(content: String, size: Int = 720): Bitmap {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    /** Re-finds each scanned track via a JioSaavn search (best-effort match by id, falling back
     * to the top search result), since the QR payload doesn't carry playback URLs. Returns how
     * many tracks were actually matched and added. */
    suspend fun importFromQr(content: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = adapter.fromJson(content) ?: error("This QR code isn't a Wavelength playlist")
            val playlistId = repository.createPlaylist(payload.n)
            var added = 0
            payload.t.forEach { qrTrack ->
                val results = repository.searchTracks("${qrTrack.n} ${qrTrack.a}", limit = 5).getOrDefault(emptyList())
                val match = results.firstOrNull { it.id == qrTrack.i } ?: results.firstOrNull()
                if (match != null) {
                    repository.addTrackToPlaylist(playlistId, match)
                    added++
                }
            }
            added
        }
    }
}
