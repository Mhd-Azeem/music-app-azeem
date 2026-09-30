package com.wavelength.music.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.Icon
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.load
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.wavelength.music.MainActivity
import com.wavelength.music.R

/**
 * AzMusic's optional Dynamic-Island-style overlay.
 * Enabled explicitly from Settings. This is deliberately independent of vendor
 * Live Island APIs: it follows the app's Media3 session and uses Android's standard overlay
 * permission, so it can work on devices whose SystemUI only allowlists selected music apps.
 */
class FloatingIslandService : Service() {
    private lateinit var windowManager: WindowManager
    private var root: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var expanded = false
    private val handler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable { showControls() }

    private lateinit var artwork: ImageView
    private lateinit var title: TextView
    private lateinit var controls: LinearLayout
    private lateinit var playPause: TextView

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh(player)
        override fun onPlaybackStateChanged(playbackState: Int) {
            controller?.let { refresh(it) }
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            controller?.let { refresh(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()
        createBubbleChannel()
        val filter = android.content.IntentFilter().apply {
            addAction(ACTION_APP_FOREGROUND)
            addAction(ACTION_APP_BACKGROUND)
        }
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            appVisibilityReceiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
        connectController()
    }

    private val appVisibilityReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_APP_FOREGROUND -> {
                    root?.visibility = View.GONE
                    cancelBubble()
                }
                ACTION_APP_BACKGROUND -> controller?.let { refresh(it) }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    private fun createOverlay() {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        artwork = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply {
                marginEnd = dp(8)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(Color.rgb(38, 38, 42), dp(10).toFloat())
            clipToOutline = true
        }

        title = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        playPause = controlButton("▶") { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
        controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            addView(controlButton("‹") { controller?.seekToPreviousMediaItem() })
            addView(playPause)
            addView(controlButton("›") { controller?.seekToNextMediaItem() })
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(artwork)
            addView(labels, LinearLayout.LayoutParams(dp(92), dp(34)))
        }

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = rounded(Color.argb(242, 8, 8, 10), dp(24).toFloat())
            elevation = dp(10).toFloat()
            addView(topRow)
            addView(controls)
            setOnTouchListener(IslandTouchListener())
        }

        params = WindowManager.LayoutParams(
            dp(146),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Anchor directly to the top screen edge.
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 0
        }
        windowManager.addView(root, params)
        root?.visibility = View.GONE
    }

    private fun controlButton(symbol: String, action: () -> Unit) = TextView(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(62), dp(48)).apply {
            marginStart = dp(3)
            marginEnd = dp(3)
        }
        text = symbol
        setTextColor(Color.WHITE)
        textSize = 30f
        gravity = Gravity.CENTER
        contentDescription = when (symbol) {
            "‹" -> "Previous"
            "›" -> "Next"
            else -> "Play or pause"
        }
        background = rounded(Color.rgb(42, 42, 46), dp(20).toFloat())
        setOnClickListener { action() }
    }

    private fun connectController() {
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching { future.get() }.onSuccess {
                controller = it
                it.addListener(listener)
                refresh(it)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun refresh(player: Player) {
        val appVisible = getSharedPreferences(VISIBILITY_PREFS, MODE_PRIVATE)
            .getBoolean(KEY_APP_VISIBLE, false)
        // A queued media item alone is not enough: after playback is stopped/dismissed the
        // controller can still retain the last item. Only show for an active/paused playback session.
        val hasActivePlayback = player.mediaItemCount > 0 &&
            player.playbackState != Player.STATE_IDLE &&
            player.playbackState != Player.STATE_ENDED
        // Android 11+ uses the native Bubble UI: a circular dock that expands into a
        // floating activity. Older Android versions retain the compact overlay fallback.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root?.visibility = View.GONE
            if (hasActivePlayback && !appVisible) showBubble(player) else cancelBubble()
        } else {
            root?.visibility = if (hasActivePlayback && !appVisible) View.VISIBLE else View.GONE
        }
        if (!hasActivePlayback) {
            hideControls()
            cancelBubble()
            return
        }
        val metadata: MediaMetadata = player.mediaMetadata
        title.text = metadata.title?.toString().orEmpty().ifBlank { "AzMusic" }
        metadata.artworkUri?.let { artwork.load(it) }
        playPause.text = if (player.isPlaying) "Ⅱ" else "▶"
        playPause.contentDescription = if (player.isPlaying) "Pause" else "Play"
    }

    private fun createBubbleChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            BUBBLE_CHANNEL_ID,
            "AzMusic floating player",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Shows AzMusic as a floating playback bubble"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) setAllowBubbles(true)
        }
        manager.createNotificationChannel(channel)
    }

    private fun showBubble(player: Player) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val manager = getSystemService(NotificationManager::class.java)
        val bubbleIntent = PendingIntent.getActivity(
            this,
            701,
            Intent(this, MainActivity::class.java).apply {
                putExtra(EXTRA_BUBBLE_MODE, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val bubble = Notification.BubbleMetadata.Builder(
            bubbleIntent,
            Icon.createWithResource(this, R.drawable.ic_launcher_classic)
        )
            .setDesiredHeight(640)
            .setAutoExpandBubble(false)
            .setSuppressNotification(true)
            .build()

        val metadata = player.mediaMetadata
        val notification = Notification.Builder(this, BUBBLE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_classic)
            .setContentTitle(metadata.title?.toString().orEmpty().ifBlank { "AzMusic" })
            .setContentText(metadata.artist?.toString().orEmpty().ifBlank { "Music playing" })
            .setBubbleMetadata(bubble)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setOngoing(player.isPlaying)
            .build()
        manager.notify(BUBBLE_NOTIFICATION_ID, notification)
    }

    private fun cancelBubble() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getSystemService(NotificationManager::class.java).cancel(BUBBLE_NOTIFICATION_ID)
        }
    }

    private fun openAzMusic() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
    }

    private fun showControls() {
        expanded = true
        controls.visibility = View.VISIBLE
        updateSize()
    }

    private fun hideControls() {
        expanded = false
        controls.visibility = View.GONE
        updateSize()
    }

    private fun updateSize() {
        val p = params ?: return
        p.height = WindowManager.LayoutParams.WRAP_CONTENT
        p.width = dp(if (expanded) 224 else 146)
        root?.let { windowManager.updateViewLayout(it, p) }
    }

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private inner class IslandTouchListener : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var dragged = false
        private var longPressed = false

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val p = params ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = p.x
                    dragged = false
                    longPressed = false
                    handler.postDelayed({
                        if (!dragged) {
                            longPressed = true
                            showControls()
                        }
                    }, 450L)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (kotlin.math.abs(dx) > 12 || kotlin.math.abs(dy) > 12) {
                        dragged = true
                        handler.removeCallbacksAndMessages(null)
                    }
                    if (dragged) {
                        p.x = startX + dx
                        p.y = 0
                        root?.let { windowManager.updateViewLayout(it, p) }
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacksAndMessages(null)
                    when {
                        dragged -> Unit
                        longPressed -> Unit
                        expanded -> hideControls()
                        else -> openAzMusic()
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacksAndMessages(null)
                    return true
                }
            }
            return false
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping AzMusic away from Recents must not leave a stale overlay on screen.
        root?.visibility = View.GONE
        cancelBubble()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        cancelBubble()
        runCatching { unregisterReceiver(appVisibilityReceiver) }
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
        root?.let { runCatching { windowManager.removeView(it) } }
        root = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_APP_FOREGROUND = "com.wavelength.music.APP_FOREGROUND"
        const val ACTION_APP_BACKGROUND = "com.wavelength.music.APP_BACKGROUND"
        const val VISIBILITY_PREFS = "floating_island_visibility"
        const val KEY_APP_VISIBLE = "app_visible"
        const val EXTRA_BUBBLE_MODE = "azmusic_bubble_mode"
        private const val BUBBLE_CHANNEL_ID = "azmusic_floating_player"
        private const val BUBBLE_NOTIFICATION_ID = 701
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
