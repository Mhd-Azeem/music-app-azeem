package com.wavelength.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.widget.RemoteViews
import androidx.media3.common.Player
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.wavelength.music.MainActivity
import com.wavelength.music.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Pushes the current playback state to every placed [MusicWidgetProvider] instance. Called
 * directly from PlaybackService's player listener rather than relying on `updatePeriodMillis`
 * (capped at 30 minutes system-wide - far too infrequent for a "now playing" widget).
 */
object MusicWidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun update(context: Context, player: Player) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, MusicWidgetProvider::class.java))
        if (widgetIds.isEmpty()) return

        val hasTrack = player.currentMediaItem != null
        val metadata = player.mediaMetadata
        val views = buildViews(context, hasTrack, metadata.title?.toString(), metadata.artist?.toString(), player.isPlaying)
        appWidgetManager.updateAppWidget(widgetIds, views)

        val artworkUrl = metadata.artworkUri?.toString()
        if (!hasTrack || artworkUrl.isNullOrBlank()) return

        scope.launch {
            val bitmap = runCatching {
                val loader = Coil.imageLoader(context)
                val request = ImageRequest.Builder(context).data(artworkUrl).allowHardware(false).build()
                ((loader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
            }.getOrNull() ?: return@launch

            views.setImageViewBitmap(R.id.widget_album_art, bitmap)
            appWidgetManager.updateAppWidget(widgetIds, views)
        }
    }

    private fun buildViews(
        context: Context,
        hasTrack: Boolean,
        title: String?,
        artist: String?,
        isPlaying: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_music_player)

        views.setTextViewText(
            R.id.widget_track_title,
            if (hasTrack) title.orEmpty() else context.getString(R.string.widget_nothing_playing)
        )
        views.setTextViewText(
            R.id.widget_track_artist,
            if (hasTrack) artist.orEmpty() else context.getString(R.string.widget_open_app_hint)
        )
        views.setImageViewResource(
            R.id.widget_button_play_pause,
            if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        )
        if (!hasTrack) {
            views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_widget_album_placeholder)
        }

        views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))
        views.setOnClickPendingIntent(
            R.id.widget_button_previous,
            actionPendingIntent(context, MusicWidgetProvider.ACTION_PREVIOUS, requestCode = 1)
        )
        views.setOnClickPendingIntent(
            R.id.widget_button_play_pause,
            actionPendingIntent(context, MusicWidgetProvider.ACTION_PLAY_PAUSE, requestCode = 2)
        )
        views.setOnClickPendingIntent(
            R.id.widget_button_next,
            actionPendingIntent(context, MusicWidgetProvider.ACTION_NEXT, requestCode = 3)
        )

        return views
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun actionPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MusicWidgetProvider::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
