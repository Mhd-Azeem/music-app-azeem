package com.wavelength.music.playback

import android.animation.ValueAnimator
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
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
    private var overlayContainer: FrameLayout? = null
    private var root: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var collapsedEdge = IslandEdge.TOP
    private var collapsedX = 0
    private var collapsedY = 0
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var expanded = false
    private var sizeAnimator: ValueAnimator? = null
    /** Once the 5s pause/stop grace period expires, stale Media3 callbacks must not resurrect it. */
    private var dismissedForInactivePlayback = false
    private var appWasVisible = true
    private val handler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable { showControls() }
    private val hideAfterStopRunnable = Runnable {
        hideControls(animate = false)
        overlayContainer?.visibility = View.GONE
        dismissedForInactivePlayback = true
    }
    private val progressTicker = object : Runnable {
        override fun run() {
            val player = controller ?: return
            updatePlaybackProgress(player)
            if (player.isPlaying) {
                handler.postDelayed(this, 500L)
            }
        }
    }

    private lateinit var artwork: ImageView
    private lateinit var bufferingIndicator: ProgressBar
    private lateinit var title: TextView
    private lateinit var artist: TextView
    private lateinit var progress: SeekBar
    private lateinit var timeRow: LinearLayout
    private lateinit var elapsed: TextView
    private lateinit var remaining: TextView
    private lateinit var controls: LinearLayout
    private lateinit var playPause: TextView
    private lateinit var visualizer: WaveformView

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
                    appWasVisible = true
                    root?.animate()?.cancel()
                    overlayContainer?.visibility = View.GONE
                }
                ACTION_APP_BACKGROUND -> controller?.let {
                    // A paused player must stay silent when AzMusic leaves the foreground.
                    // The island is an active-playback surface, not a paused-session reminder.
                    if (!it.isPlaying) {
                        appWasVisible = false
                        handler.removeCallbacks(hideAfterStopRunnable)
                        hideControls(animate = false)
                        overlayContainer?.visibility = View.GONE
                        return@let
                    }
                    val shouldAnimate = appWasVisible && it.mediaItemCount > 0 &&
                        it.playbackState != Player.STATE_IDLE &&
                        it.playbackState != Player.STATE_ENDED
                    appWasVisible = false
                    refresh(it, animateEntrance = shouldAnimate)
                }
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
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(Color.rgb(38, 38, 42), dp(12).toFloat())
            clipToOutline = true
        }
        bufferingIndicator = ProgressBar(this).apply {
            isIndeterminate = true
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
        }
        val artworkFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginEnd = dp(8) }
            addView(artwork)
            addView(bufferingIndicator)
        }
        title = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 13f; maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        artist = TextView(this).apply {
            setTextColor(Color.argb(180, 255, 255, 255)); textSize = 10f; maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            visibility = View.GONE
        }
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
            addView(title, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(artist, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        visualizer = WaveformView(this)
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(artworkFrame)
            addView(labels, LinearLayout.LayoutParams(0, dp(36), 1f))
            addView(visualizer, LinearLayout.LayoutParams(dp(44), dp(36)))
        }

        progress = SeekBar(this).apply {
            max = 1000
            visibility = View.GONE
            setPadding(0, 0, 0, 0)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) controller?.let { player ->
                        if (player.duration > 0) player.seekTo(player.duration * value / 1000L)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        elapsed = timeLabel(Gravity.START)
        remaining = timeLabel(Gravity.END)
        timeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
            addView(elapsed, LinearLayout.LayoutParams(0, dp(20), 1f))
            addView(remaining, LinearLayout.LayoutParams(0, dp(20), 1f))
        }

        playPause = controlButton("▶", primary = true) {
            controller?.let { if (it.isPlaying) it.pause() else it.play() }
        }
        controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(0, dp(4), 0, dp(6))
            addView(controlButton("‹", primary = false) { controller?.seekToPreviousMediaItem() })
            addView(playPause)
            addView(controlButton("›", primary = false) { controller?.seekToNextMediaItem() })
        }

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = islandBackground(dp(24).toFloat())
            elevation = dp(8).toFloat()
            addView(topRow)
            addView(progress, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(24)))
            addView(timeRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(20)))
            addView(controls)
            setOnTouchListener(IslandTouchListener())
        }

        overlayContainer = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
            addView(
                root,
                FrameLayout.LayoutParams(dp(220), dp(44), Gravity.CENTER)
            )
        }

        val screenWidth = resources.displayMetrics.widthPixels
        collapsedX = ((screenWidth - dp(220)) / 2).coerceAtLeast(0)
        collapsedY = 0
        params = WindowManager.LayoutParams(
            dp(220), dp(44),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = collapsedX
            y = collapsedY
        }
        windowManager.addView(overlayContainer, params)
        overlayContainer?.visibility = View.GONE
    }

    private fun timeLabel(gravityValue: Int) = TextView(this).apply {
        setTextColor(Color.argb(165, 255, 255, 255))
        textSize = 10f
        gravity = gravityValue or Gravity.CENTER_VERTICAL
    }

    private fun controlButton(
        symbol: String,
        primary: Boolean,
        action: () -> Unit
    ) = TextView(this).apply {
        val size = if (primary) dp(64) else dp(52)
        layoutParams = LinearLayout.LayoutParams(size, size).apply {
            marginStart = dp(if (primary) 14 else 8)
            marginEnd = dp(if (primary) 14 else 8)
        }
        text = symbol
        gravity = Gravity.CENTER
        includeFontPadding = false
        textSize = if (primary) 31f else 36f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setTextColor(if (primary) Color.rgb(18, 18, 20) else Color.WHITE)
        contentDescription = when (symbol) {
            "‹" -> "Previous"
            "›" -> "Next"
            else -> "Play or pause"
        }
        background = controlBackground(primary)
        elevation = dp(if (primary) 8 else 4).toFloat()
        isClickable = true
        isFocusable = true
        setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.animate().cancel()
                    view.animate().scaleX(0.90f).scaleY(0.90f).alpha(0.82f)
                        .setDuration(90L).start()
                }
                MotionEvent.ACTION_UP -> {
                    view.animate().cancel()
                    view.animate().scaleX(1f).scaleY(1f).alpha(1f)
                        .setDuration(130L).start()
                    action()
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.animate().cancel()
                    view.animate().scaleX(1f).scaleY(1f).alpha(1f)
                        .setDuration(130L).start()
                }
            }
            true
        }
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

    private fun refresh(player: Player, animateEntrance: Boolean = false) {
        val appVisible = getSharedPreferences(VISIBILITY_PREFS, MODE_PRIVATE)
            .getBoolean(KEY_APP_VISIBLE, false)
        val hasMedia = player.mediaItemCount > 0
        val stopped = !hasMedia ||
            player.playbackState == Player.STATE_IDLE ||
            player.playbackState == Player.STATE_ENDED

        if (stopped) {
            handler.removeCallbacks(hideAfterStopRunnable)
            if (appVisible) {
                overlayContainer?.visibility = View.GONE
            } else if (!dismissedForInactivePlayback && overlayContainer?.visibility == View.VISIBLE) {
                handler.postDelayed(hideAfterStopRunnable, 5_000L)
            } else {
                overlayContainer?.visibility = View.GONE
            }
            return
        }

        // Resuming AzMusic from the notification, headset/Bluetooth controls, lock screen,
        // or any other MediaSession controller must revive the island after its paused-state
        // timeout hid it. MediaController is attached only to AzMusic's PlaybackService, so this
        // cannot be triggered by unrelated audio apps.
        if (player.isPlaying) {
            dismissedForInactivePlayback = false
            handler.removeCallbacks(hideAfterStopRunnable)
            if (!appVisible) overlayContainer?.visibility = View.VISIBLE
        } else {
            handler.removeCallbacks(hideAfterStopRunnable)
        }

        if (appVisible) {
            overlayContainer?.visibility = View.GONE
        } else if (player.isPlaying || !dismissedForInactivePlayback) {
            // Player callbacks (pause/next/previous/seek) update the contents in-place. They must
            // never collapse an island the user deliberately expanded.
            if (overlayContainer?.visibility != View.VISIBLE) {
                showCollapsedIsland(animateEntrance)
            } else if (animateEntrance && !expanded) {
                showCollapsedIsland(true)
            }
        } else {
            overlayContainer?.visibility = View.GONE
        }

        if (!player.isPlaying && !appVisible && !dismissedForInactivePlayback) {
            handler.postDelayed(hideAfterStopRunnable, 5_000L)
        }

        val metadata: MediaMetadata = player.mediaMetadata
        title.text = metadata.title?.toString().orEmpty().ifBlank { "AzMusic" }
        artist.text = metadata.artist?.toString().orEmpty()
        metadata.artworkUri?.let { artwork.load(it) }
        updatePlaybackProgress(player)
        handler.removeCallbacks(progressTicker)
        if (player.isPlaying) {
            handler.post(progressTicker)
        }

        val buffering = player.playbackState == Player.STATE_BUFFERING
        bufferingIndicator.visibility = if (buffering) View.VISIBLE else View.GONE
        artwork.alpha = if (buffering) 0.45f else 1f

        visualizer.setPlaying(player.isPlaying && !buffering)
        playPause.text = if (player.isPlaying) "Ⅱ" else "▶"
        playPause.contentDescription = if (player.isPlaying) "Pause" else "Play"
    }

    private fun showCollapsedIsland(animateEntrance: Boolean) {
        val view = root ?: return
        hideControls(animate = false)
        applyCollapsedPlacement()
        overlayContainer?.visibility = View.VISIBLE
        if (!animateEntrance) return
        view.animate().cancel()
        view.pivotX = view.width / 2f
        view.pivotY = view.height / 2f
        view.scaleX = 1.20f
        view.scaleY = 1.20f
        view.alpha = 0f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(260L)
            .start()
    }

    private fun openAzMusic() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
    }

    private fun showControls() {
        if (expanded) return
        val view = root ?: return
        val p = params ?: return
        val startX = p.x
        val startY = p.y
        val startWidth = p.width
        val startHeight = p.height
        val startRotation = view.rotation

        expanded = true
        p.flags = p.flags or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

        artist.visibility = View.VISIBLE
        progress.visibility = View.VISIBLE
        timeRow.visibility = View.VISIBLE
        controls.visibility = View.VISIBLE
        listOf(artist, progress, timeRow, controls).forEach {
            it.alpha = 0f
            it.scaleX = 0.94f
            it.scaleY = 0.94f
        }

        animateIslandSize(
            fromWidth = startWidth,
            fromHeight = startHeight,
            toWidth = resources.displayMetrics.widthPixels,
            toHeight = dp(136),
            fromX = startX,
            fromY = startY,
            toX = 0,
            toY = 0,
            fromRotation = startRotation,
            toRotation = 0f,
            duration = 300L
        ) { fraction ->
            val contentFraction = ((fraction - 0.16f) / 0.84f).coerceIn(0f, 1f)
            listOf(artist, progress, timeRow, controls).forEach {
                it.alpha = contentFraction
                it.scaleX = 0.94f + (0.06f * contentFraction)
                it.scaleY = 0.94f + (0.06f * contentFraction)
            }
        }
    }

    private fun hideControls(animate: Boolean = true) {
        if (!expanded) {
            if (!animate) updateSize()
            return
        }

        sizeAnimator?.cancel()
        if (!animate) {
            expanded = false
            params?.flags = (params?.flags ?: 0) and
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH.inv() and
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
            artist.visibility = View.GONE
            progress.visibility = View.GONE
            timeRow.visibility = View.GONE
            controls.visibility = View.GONE
            listOf(artist, progress, timeRow, controls).forEach {
                it.alpha = 1f
                it.scaleX = 1f
                it.scaleY = 1f
            }
            updateSize()
            return
        }

        val startWidth = params?.width ?: resources.displayMetrics.widthPixels
        val startHeight = params?.height ?: dp(136)
        val target = collapsedWindowGeometry()
        animateIslandSize(
            fromWidth = startWidth,
            fromHeight = startHeight,
            toWidth = target.width,
            toHeight = target.height,
            fromX = params?.x ?: 0,
            fromY = params?.y ?: 0,
            toX = target.x,
            toY = target.y,
            fromRotation = root?.rotation ?: 0f,
            toRotation = target.rotation,
            duration = 260L
        ) { fraction ->
            val contentFraction = 1f - fraction
            listOf(artist, progress, timeRow, controls).forEach {
                it.alpha = contentFraction
                it.scaleX = 0.94f + (0.06f * contentFraction)
                it.scaleY = 0.94f + (0.06f * contentFraction)
            }
        }.doOnEnd {
            expanded = false
            params?.flags = (params?.flags ?: 0) and
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH.inv() and
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
            artist.visibility = View.GONE
            progress.visibility = View.GONE
            timeRow.visibility = View.GONE
            controls.visibility = View.GONE
            listOf(artist, progress, timeRow, controls).forEach {
                it.alpha = 1f
                it.scaleX = 1f
                it.scaleY = 1f
            }
            updateSize()
        }
    }

    private fun animateIslandSize(
        fromWidth: Int,
        fromHeight: Int,
        toWidth: Int,
        toHeight: Int,
        fromX: Int = params?.x ?: 0,
        fromY: Int = params?.y ?: 0,
        toX: Int = 0,
        toY: Int = 0,
        fromRotation: Float = root?.rotation ?: 0f,
        toRotation: Float = 0f,
        duration: Long,
        onFrame: (Float) -> Unit
    ): ValueAnimator {
        sizeAnimator?.cancel()
        return ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                val p = params ?: return@addUpdateListener
                p.width = (fromWidth + (toWidth - fromWidth) * fraction).toInt()
                p.height = (fromHeight + (toHeight - fromHeight) * fraction).toInt()
                p.x = (fromX + (toX - fromX) * fraction).toInt()
                p.y = (fromY + (toY - fromY) * fraction).toInt()
                root?.rotation = fromRotation + (toRotation - fromRotation) * fraction
                syncRootToWindow(expandedLayout = true)
                overlayContainer?.let { windowManager.updateViewLayout(it, p) }
                onFrame(fraction)
            }
            sizeAnimator = this
            start()
        }
    }

    private fun ValueAnimator.doOnEnd(block: () -> Unit) {
        addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) = block()
        })
    }

    private fun updateSize() {
        val p = params ?: return
        if (expanded) {
            p.height = dp(136)
            p.width = resources.displayMetrics.widthPixels
            p.x = 0
            p.y = 0
            root?.rotation = 0f
            syncRootToWindow(expandedLayout = true)
        } else {
            applyCollapsedPlacement(updateWindow = false)
        }
        overlayContainer?.let { windowManager.updateViewLayout(it, p) }
    }

    private data class IslandGeometry(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        val rotation: Float
    )

    private fun collapsedWindowGeometry(): IslandGeometry {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val horizontalWidth = dp(220)
        val horizontalHeight = dp(44)
        val verticalWidth = dp(44)
        val verticalHeight = dp(220)

        return when (collapsedEdge) {
            IslandEdge.TOP -> IslandGeometry(
                collapsedX.coerceIn(0, (screenWidth - horizontalWidth).coerceAtLeast(0)),
                0,
                horizontalWidth,
                horizontalHeight,
                0f
            )
            IslandEdge.LEFT -> IslandGeometry(
                0,
                collapsedY.coerceIn(0, (screenHeight - verticalHeight).coerceAtLeast(0)),
                verticalWidth,
                verticalHeight,
                -90f
            )
            IslandEdge.RIGHT -> IslandGeometry(
                (screenWidth - verticalWidth).coerceAtLeast(0),
                collapsedY.coerceIn(0, (screenHeight - verticalHeight).coerceAtLeast(0)),
                verticalWidth,
                verticalHeight,
                90f
            )
        }
    }

    private fun applyCollapsedPlacement(updateWindow: Boolean = true) {
        val p = params ?: return
        val geometry = collapsedWindowGeometry()
        p.width = geometry.width
        p.height = geometry.height
        p.x = geometry.x
        p.y = geometry.y
        root?.rotation = geometry.rotation
        syncRootToWindow(expandedLayout = false)
        if (updateWindow) {
            overlayContainer?.let { windowManager.updateViewLayout(it, p) }
        }
    }

    private fun syncRootToWindow(expandedLayout: Boolean) {
        val view = root ?: return
        val lp = view.layoutParams as? FrameLayout.LayoutParams ?: return
        if (expandedLayout) {
            lp.width = FrameLayout.LayoutParams.MATCH_PARENT
            lp.height = FrameLayout.LayoutParams.MATCH_PARENT
            lp.gravity = Gravity.CENTER
        } else {
            lp.width = dp(220)
            lp.height = dp(44)
            lp.gravity = Gravity.CENTER
        }
        view.layoutParams = lp
    }

    private fun updateCollapsedDrag(rawX: Float, rawY: Float) {
        if (expanded) return
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val edgeDistances = mapOf(
            IslandEdge.TOP to rawY,
            IslandEdge.LEFT to rawX,
            IslandEdge.RIGHT to (screenWidth - rawX)
        )
        collapsedEdge = edgeDistances.minByOrNull { it.value }?.key ?: collapsedEdge

        when (collapsedEdge) {
            IslandEdge.TOP -> {
                collapsedX = (rawX - dp(110)).toInt()
                    .coerceIn(0, (screenWidth - dp(220)).coerceAtLeast(0))
            }
            IslandEdge.LEFT, IslandEdge.RIGHT -> {
                collapsedY = (rawY - dp(110)).toInt()
                    .coerceIn(0, (screenHeight - dp(220)).coerceAtLeast(0))
            }
        }
        applyCollapsedPlacement()
    }

    private fun updatePlaybackProgress(player: Player) {
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        val position = player.currentPosition.coerceAtLeast(0L)
        progress.progress = if (duration > 0) {
            ((position * 1000L) / duration).toInt().coerceIn(0, 1000)
        } else {
            0
        }
        elapsed.text = formatTime(position)
        remaining.text = formatTime(duration)
    }

    private fun formatTime(ms: Long): String {
        val total = (ms / 1000L).coerceAtLeast(0L)
        return "%d:%02d".format(total / 60L, total % 60L)
    }

    private fun islandBackground(radius: Float) = GradientDrawable().apply {
        setColor(Color.argb(248, 4, 4, 6))
        cornerRadius = radius
        setStroke(dp(1), Color.argb(90, 255, 255, 255))
    }

    private fun controlBackground(primary: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        if (primary) {
            setColor(Color.rgb(244, 244, 247))
            setStroke(dp(1), Color.argb(90, 255, 255, 255))
        } else {
            setColor(Color.argb(235, 34, 34, 39))
            setStroke(dp(1), Color.argb(85, 255, 255, 255))
        }
    }

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private inner class IslandTouchListener : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var moved = false
        private var longPressed = false
        private val expandRunnable = Runnable {
            if (!moved && !expanded) {
                longPressed = true
                showControls()
            }
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_OUTSIDE -> {
                    if (expanded) hideControls(animate = true)
                    return true
                }
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    moved = false
                    longPressed = false
                    if (!expanded) handler.postDelayed(expandRunnable, 450L)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (expanded) return true
                    val dx = kotlin.math.abs(event.rawX - downX)
                    val dy = kotlin.math.abs(event.rawY - downY)
                    if (dx > 12f || dy > 12f) {
                        moved = true
                        handler.removeCallbacks(expandRunnable)
                    }
                    if (moved) {
                        updateCollapsedDrag(event.rawX, event.rawY)
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(expandRunnable)
                    when {
                        moved -> applyCollapsedPlacement()
                        longPressed -> Unit
                        expanded -> Unit
                        else -> openAzMusic()
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(expandRunnable)
                    if (moved && !expanded) applyCollapsedPlacement()
                    return true
                }
            }
            return false
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping AzMusic away from Recents must not leave a stale overlay on screen.
        overlayContainer?.visibility = View.GONE
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        handler.removeCallbacks(hideAfterStopRunnable)
        handler.removeCallbacks(progressTicker)
        sizeAnimator?.cancel()
        sizeAnimator = null
        runCatching { unregisterReceiver(appVisibilityReceiver) }
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
        overlayContainer?.let { runCatching { windowManager.removeView(it) } }
        overlayContainer = null
        root = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class WaveformView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(224, 58, 70)
            strokeCap = Paint.Cap.ROUND
            strokeWidth = dp(3).toFloat()
        }
        private val heights = floatArrayOf(.30f, .62f, .42f, .82f, .48f, .70f, .34f)
        private var phase = 0f
        private var playing = false
        private val animate = object : Runnable {
            override fun run() {
                if (!playing) return
                phase += .42f
                invalidate()
                postDelayed(this, 90L)
            }
        }

        fun setPlaying(value: Boolean) {
            if (playing == value) return
            playing = value
            removeCallbacks(animate)
            if (playing) post(animate) else invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val count = heights.size
            val gap = width.toFloat() / (count + 1)
            val center = height / 2f
            for (i in 0 until count) {
                val motion = if (playing) {
                    (kotlin.math.sin(phase + i * .85f) + 1f) * .22f
                } else 0f
                val fraction = (heights[i] + motion).coerceIn(.18f, .96f)
                val half = height * fraction * .36f
                val x = gap * (i + 1)
                canvas.drawLine(x, center - half, x, center + half, paint)
            }
        }

        override fun onDetachedFromWindow() {
            removeCallbacks(animate)
            super.onDetachedFromWindow()
        }
    }

    private enum class IslandEdge { TOP, RIGHT, LEFT }

    companion object {
        const val ACTION_APP_FOREGROUND = "com.wavelength.music.APP_FOREGROUND"
        const val ACTION_APP_BACKGROUND = "com.wavelength.music.APP_BACKGROUND"
        const val VISIBILITY_PREFS = "floating_island_visibility"
        const val KEY_APP_VISIBLE = "app_visible"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
