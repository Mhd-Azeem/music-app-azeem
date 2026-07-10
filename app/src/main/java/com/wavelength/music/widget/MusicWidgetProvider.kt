package com.wavelength.music.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.wavelength.music.playback.PlaybackService

/**
 * Home screen widget showing the current track and transport controls. Live updates come from
 * [MusicWidgetUpdater], called directly by [PlaybackService]'s player listener; this class only
 * handles widget lifecycle callbacks (initial placement) and button taps, both of which need a
 * short-lived [MediaController] connection to the running (or freshly started) session.
 */
class MusicWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PLAY_PAUSE, ACTION_NEXT, ACTION_PREVIOUS -> handleTransportAction(context, intent.action!!)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        withMediaController(context) { controller -> MusicWidgetUpdater.update(context, controller) }
    }

    private fun handleTransportAction(context: Context, action: String) {
        withMediaController(context) { controller ->
            when (action) {
                ACTION_PLAY_PAUSE -> if (controller.isPlaying) controller.pause() else controller.play()
                ACTION_NEXT -> controller.seekToNext()
                ACTION_PREVIOUS -> controller.seekToPrevious()
            }
        }
    }

    private fun withMediaController(context: Context, action: (MediaController) -> Unit) {
        val pendingResult = goAsync()
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                runCatching {
                    val controller = controllerFuture.get()
                    action(controller)
                    controller.release()
                }
                pendingResult.finish()
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.wavelength.music.widget.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.wavelength.music.widget.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.wavelength.music.widget.ACTION_PREVIOUS"
    }
}
