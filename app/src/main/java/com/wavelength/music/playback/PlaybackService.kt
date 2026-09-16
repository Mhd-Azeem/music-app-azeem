package com.wavelength.music.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.wavelength.music.MainActivity
import com.wavelength.music.widget.MusicWidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var equalizerController: EqualizerController

    @Inject
    lateinit var visualizerController: VisualizerController

    @Inject
    lateinit var pcmBeatAnalyzer: PcmBeatAnalyzer

    private var mediaSession: MediaSession? = null
    private var streamCache: SimpleCache? = null

    private val audioSessionListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            equalizerController.onAudioSessionIdChanged(audioSessionId)
            visualizerController.onAudioSessionIdChanged(audioSessionId)
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (
                events.containsAny(
                    Player.EVENT_MEDIA_METADATA_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION
                )
            ) {
                MusicWidgetUpdater.update(this@PlaybackService, player)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Persist streamed song bytes on-device, Spotify-style. A track that has already been
        // played can be served from this cache on later plays, reducing re-buffering and network
        // use. LRU eviction caps the cache so it cannot grow without bound.
        val cache = SimpleCache(
            File(cacheDir, "azmusic_stream_cache"),
            LeastRecentlyUsedCacheEvictor(STREAM_CACHE_MAX_BYTES),
            StandaloneDatabaseProvider(this)
        )
        streamCache = cache

        val upstreamFactory = DefaultDataSource.Factory(this)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

        // Buffer farther ahead than the Media3 defaults so weak/patchy coverage has more headroom.
        // Buffered bytes are simultaneously written to the persistent cache above.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 30_000,
                /* maxBufferMs = */ 120_000,
                /* bufferForPlaybackMs = */ 2_500,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000
            )
            .build()

        // Feed decoded PCM directly into PcmBeatAnalyzer while passing the audio through unchanged.
        // This avoids Android Visualizer/vendor compatibility issues entirely for Beat Bounce.
        val beatTap = TeeAudioProcessor(pcmBeatAnalyzer)
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf<AudioProcessor>(beatTap))
                    .build()
            }
        }

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { it.addListener(audioSessionListener) }

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .build()

        equalizerController.onAudioSessionIdChanged(player.audioSessionId)
        visualizerController.onAudioSessionIdChanged(player.audioSessionId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val session = mediaSession
        if (session == null || !session.player.playWhenReady || session.player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        runCatching { streamCache?.release() }
        streamCache = null
        super.onDestroy()
    }

    private companion object {
        // Roughly 1 GB of recently streamed audio, automatically evicting the oldest cache data.
        const val STREAM_CACHE_MAX_BYTES = 1_073_741_824L
    }
}
