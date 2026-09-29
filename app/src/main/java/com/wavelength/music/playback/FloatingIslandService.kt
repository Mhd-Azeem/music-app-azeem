package com.wavelength.music.playback

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
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

/**
 * AzMusic's optional Dynamic-Island-style overlay. This is deliberately independent of vendor
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

    private lateinit var artwork: ImageView
    private lateinit var title: TextView
    private lateinit var artist: TextView
    private lateinit var controls: LinearLayout
    private lateinit var playPause: TextView

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh(player)
    }

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()
        connectController()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
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
        artist = TextView(this).apply {
            setTextColor(Color.rgb(180, 180, 186))
            textSize = 10f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(artist, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
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
            addView(labels, LinearLayout.LayoutParams(dp(160), dp(38)))
        }

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = rounded(Color.argb(242, 8, 8, 10), dp(24).toFloat())
            elevation = dp(10).toFloat()
            addView(topRow)
            addView(controls)
            setOnClickListener {
                expanded = !expanded
                controls.visibility = if (expanded) View.VISIBLE else View.GONE
                updateSize()
            }
            setOnTouchListener(DragTouchListener())
        }

        params = WindowManager.LayoutParams(
            dp(226),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(8)
        }
        windowManager.addView(root, params)
        root?.visibility = View.GONE
    }

    private fun controlButton(symbol: String, action: () -> Unit) = TextView(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(52), dp(42)).apply {
            marginStart = dp(3)
            marginEnd = dp(3)
        }
        text = symbol
        setTextColor(Color.WHITE)
        textSize = 24f
        gravity = Gravity.CENTER
        contentDescription = when (symbol) {
            "‹" -> "Previous"
            "›" -> "Next"
            else -> "Play or pause"
        }
        background = rounded(Color.argb(32, 255, 255, 255), dp(18).toFloat())
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
        root?.visibility = if (player.mediaItemCount > 0) View.VISIBLE else View.GONE
        if (player.mediaItemCount == 0) return
        val metadata: MediaMetadata = player.mediaMetadata
        title.text = metadata.title?.toString().orEmpty().ifBlank { "AzMusic" }
        artist.text = metadata.artist?.toString().orEmpty()
        metadata.artworkUri?.let { artwork.load(it) }
        playPause.text = if (player.isPlaying) "Ⅱ" else "▶"\n        playPause.contentDescription = if (player.isPlaying) "Pause" else "Play"
    }

    private fun updateSize() {
        val p = params ?: return
        p.height = WindowManager.LayoutParams.WRAP_CONTENT
        p.width = (if (expanded) 250 else 226) * resources.displayMetrics.density.toInt().coerceAtLeast(1)
        root?.let { windowManager.updateViewLayout(it, p) }
    }

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private inner class DragTouchListener : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var dragged = false

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val p = params ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = p.x
                    startY = p.y
                    dragged = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) dragged = true
                    p.x = startX + dx
                    p.y = (startY + dy).coerceAtLeast(0)
                    root?.let { windowManager.updateViewLayout(it, p) }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) v.performClick()
                    return true
                }
            }
            return false
        }
    }

    override fun onDestroy() {
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
        root?.let { runCatching { windowManager.removeView(it) } }
        root = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
