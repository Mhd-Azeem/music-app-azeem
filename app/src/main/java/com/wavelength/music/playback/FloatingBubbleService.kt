package com.wavelength.music.playback

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.load
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.wavelength.music.MainActivity

class FloatingBubbleService : Service() {
    private lateinit var wm: WindowManager
    private var root: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null
    private val handler = Handler(Looper.getMainLooper())
    private var expanded = false

    private lateinit var art: ImageView
    private lateinit var title: TextView
    private lateinit var artist: TextView
    private lateinit var seek: SeekBar
    private lateinit var time: TextView
    private lateinit var upNext: LinearLayout

    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 500L)
        }
    }
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh()
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) = refresh()
    }

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()
        connect()
        handler.post(ticker)
    }

    private fun createOverlay() {
        art = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = bg(Color.rgb(28, 28, 31), dp(28).toFloat())
            clipToOutline = true
        }
        title = label(16f, Color.WHITE)
        artist = label(12f, Color.LTGRAY)
        seek = SeekBar(this).apply {
            max = 1000
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) controller?.let { c -> if (c.duration > 0) c.seekTo(c.duration * value / 1000L) }
                }
                override fun onStartTrackingTouch(s: SeekBar?) = Unit
                override fun onStopTrackingTouch(s: SeekBar?) = Unit
            })
        }
        time = label(11f, Color.LTGRAY)
        upNext = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = bg(Color.argb(248, 13, 13, 16), dp(30).toFloat(), true)
            elevation = dp(12).toFloat()
            setPadding(dp(5), dp(5), dp(5), dp(5))
            addView(art, LinearLayout.LayoutParams(dp(52), dp(52)))
            setOnTouchListener(BubbleTouch())
        }
        params = WindowManager.LayoutParams(
            dp(62), dp(62), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12); y = dp(150)
        }
        wm.addView(root, params)
    }

    private fun showPanel() {
        if (expanded) return
        expanded = true
        root?.apply {
            removeAllViews()
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val header = LinearLayout(this@FloatingBubbleService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(art, LinearLayout.LayoutParams(dp(58), dp(58)).apply { marginEnd = dp(12) })
                addView(LinearLayout(this@FloatingBubbleService).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(title)
                    addView(artist)
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
            addView(header)
            addView(seek, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(34)))
            addView(time)
            addView(TextView(this@FloatingBubbleService).apply {
                text = "Up Next"; textSize = 14f; setTextColor(Color.WHITE)
                setPadding(0, dp(8), 0, dp(4))
            })
            addView(upNext)
        }
        params?.let {
            it.width = dp(330); it.height = WindowManager.LayoutParams.WRAP_CONTENT
            wm.updateViewLayout(root, it)
        }
        refresh()
    }

    private fun collapse() {
        if (!expanded) return
        expanded = false
        root?.apply {
            // Detach the artwork from the expanded header before rebuilding the collapsed bubble.
            // Android Views may only have one parent; adding it while still attached can crash
            // the overlay service and take the app process down with it.
            (art.parent as? android.view.ViewGroup)?.removeView(art)
            removeAllViews()
            setPadding(dp(5), dp(5), dp(5), dp(5))
            addView(art, LinearLayout.LayoutParams(dp(52), dp(52)))
        }
        params?.let {
            it.width = dp(62); it.height = dp(62)
            wm.updateViewLayout(root, it)
        }
    }

    private fun refresh() {
        val c = controller ?: return
        val md = c.mediaMetadata
        md.artworkUri?.let { art.load(it) }
        if (!expanded) return
        title.text = md.title?.toString().orEmpty().ifBlank { "Now Playing" }
        artist.text = md.artist?.toString().orEmpty()
        val d = c.duration.takeIf { it > 0 } ?: 0L
        val p = c.currentPosition.coerceAtLeast(0L)
        seek.progress = if (d > 0) ((p * 1000L) / d).toInt() else 0
        time.text = "${fmt(p)}  •  ${fmt(d)}"
        upNext.removeAllViews()
        val start = c.currentMediaItemIndex + 1
        val end = minOf(c.mediaItemCount, start + 5)
        for (i in start until end) {
            val item = c.getMediaItemAt(i)
            val row = TextView(this).apply {
                val m = item.mediaMetadata
                text = "${i - start + 1}. ${m.title ?: "Unknown"}  ·  ${m.artist ?: ""}"
                textSize = 13f
                setTextColor(Color.WHITE)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(dp(4), dp(8), dp(4), dp(8))
                setOnClickListener { c.seekToDefaultPosition(i); c.play() }
            }
            upNext.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(38)))
        }
        if (end == start) {
            upNext.addView(label(13f, Color.GRAY).apply { text = "No more songs in queue" })
        }
    }

    private fun connect() {
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        future = MediaController.Builder(this, token).buildAsync().also { f ->
            f.addListener({
                runCatching { f.get() }.onSuccess { c ->
                    controller = c
                    c.addListener(listener)
                    refresh()
                }
            }, MoreExecutors.directExecutor())
        }
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
    }

    private inner class BubbleTouch : View.OnTouchListener {
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
        var moved = false; var held = false
        val hold = Runnable { if (!moved) { held = true; showPanel() } }
        override fun onTouch(v: View, e: MotionEvent): Boolean {
            val p = params ?: return false
            when (e.actionMasked) {
                MotionEvent.ACTION_OUTSIDE -> {
                    if (expanded) collapse()
                    return true
                }
                MotionEvent.ACTION_DOWN -> {
                    downX=e.rawX; downY=e.rawY; startX=p.x; startY=p.y; moved=false; held=false
                    handler.postDelayed(hold, 450L); return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx=(e.rawX-downX).toInt(); val dy=(e.rawY-downY).toInt()
                    if (kotlin.math.abs(dx)>12 || kotlin.math.abs(dy)>12) { moved=true; handler.removeCallbacks(hold) }
                    if (moved && !expanded) { p.x=(startX-dx).coerceAtLeast(0); p.y=(startY+dy).coerceAtLeast(0); wm.updateViewLayout(root,p) }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(hold)
                    if (!moved && !held) {
                        if (expanded) {
                            // A short tap on the expanded surface simply collapses it safely.
                            collapse()
                        } else {
                            openApp()
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL -> { handler.removeCallbacks(hold); return true }
            }
            return false
        }
    }

    private fun label(size: Float, color: Int) = TextView(this).apply {
        textSize=size; setTextColor(color); maxLines=1; ellipsize=android.text.TextUtils.TruncateAt.END
    }
    private fun bg(color: Int, radius: Float, stroke: Boolean=false) = GradientDrawable().apply {
        setColor(color); cornerRadius=radius
        if (stroke) setStroke(dp(1), Color.argb(80,255,255,255))
    }
    private fun fmt(ms: Long): String { val s=(ms/1000).coerceAtLeast(0); return "%d:%02d".format(s/60,s%60) }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        root?.let { runCatching { wm.removeView(it) } }
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
