package com.wavelength.music.ui.radio

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.wavelength.music.MainActivity
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/** Owns the live stream independently of the station list and activity lifecycle. */
@OptIn(UnstableApi::class)
class RadioPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
        val openRadio = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        session = MediaSession.Builder(this, player)
            .setId("AzRadioFM")
            .setSessionActivity(openRadio)
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: List<MediaItem>
                ): ListenableFuture<List<MediaItem>> = Futures.immediateFuture(
                    mediaItems.mapNotNull { requested ->
                        stations.firstOrNull { it.id == requested.mediaId }?.let { station ->
                            val artwork = resources.openRawResource(station.cover).use { it.readBytes() }
                            val metadata = MediaMetadata.Builder()
                                .setTitle(station.name)
                                .setArtist("AzRadioFM")
                                .setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                                .build()
                            MediaItem.Builder().setMediaId(station.id).setUri(station.stream)
                                .setMediaMetadata(metadata).build()
                        }
                    }
                )
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.let {
            it.player.release()
            it.release()
        }
        session = null
        super.onDestroy()
    }
}
