package com.r2dapps.cutemascot

import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.*

/**
 * Custom floating mascot view.
 *
 * BATTERY & PERFORMANCE DESIGN:
 * - Uses Choreographer for vsync-aligned draws (max 30fps via frame skip)
 * - Only invalidates when state changes (direction, phoneme, reaction)
 * - All bitmaps pre-cached at startup, never reloaded
 * - No wake lock held
 */
class MascotView(context: Context) : View(context) {

    companion object {
        const val MASCOT_PX = 200   // dp will be applied in WindowManager params
        private const val FRAME_INTERVAL_NS = 33_333_333L  // ~30fps cap
        private const val PHONEME_INTERVAL_MS = 110L
        private const val GYRO_INTERVAL_MS = 100L
        private const val BUBBLE_DURATION_MS = 4000L
    }

    // Window references for dragging
    private var wm: WindowManager? = null
    private var wmParams: WindowManager.LayoutParams? = null

    // Sprite sheets
    private lateinit var spriteSheet: SpriteSheet
    private lateinit var phonemeTiles: Map<String, Bitmap>

    // State
    private var direction = "center"
    private var reaction: String? = null
    private var reactionEndMs = 0L
    private var isScreenOn = true

    // Speaking / phonemes
    private var isSpeaking = false
    private var speakingUntilMs = 0L
    private var speakingStartMs = 0L
    private var phonemeTimeline: List<Triple<Float, Float, String>> = emptyList()
    private var currentPhoneme = "M"

    // Talking kinetic bounce (lightweight)
    private var talkBounce = 0f

    // Speech bubble
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 18, 22, 34)
    }
    private val bubbleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val bubbleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(255, 255, 105, 180)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private var bubbleText = ""
    private var bubbleEndMs = 0L

    // Drag tracking
    private var dragRawStartX = 0f
    private var dragRawStartY = 0f
    private var dragWinStartX = 0
    private var dragWinStartY = 0
    private var isDragging = false

    // Touch position (for eye tracking)
    private var touchScreenX = -1f
    private var touchScreenY = -1f

    // Sensor tracker (gyro + touch, auto-selects)
    private lateinit var sensorTracker: SensorTracker

    // Scheduled reminders
    private val reminderHandler = Handler(Looper.getMainLooper())
    private val speechController: SpeechController
    private val config: MascotConfig

    // Choreographer frame callback — stored as lateinit to avoid recursive type inference
    private var lastFrameNs = 0L
    private lateinit var frameCallback: Choreographer.FrameCallback

    init {
        // frameCallback assigned here (after class body init) to avoid recursive type inference
        frameCallback = Choreographer.FrameCallback { frameNs ->
            if (isScreenOn) {
                if (frameNs - lastFrameNs >= FRAME_INTERVAL_NS) {
                    lastFrameNs = frameNs
                    tickState()
                    invalidate()
                }
                Choreographer.getInstance().postFrameCallback(frameCallback)
            }
        }
        config = MascotConfig(context)
        speechController = SpeechController(context)
        sensorTracker = SensorTracker(context) { gx, gy ->
            // Gyro callback (called at ~10Hz only, not every frame)
            updateDirectionFromAngle(gx, gy)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Load all bitmaps once on main thread
        spriteSheet = SpriteSheet(context)
        phonemeTiles = spriteSheet.loadPhonemeTiles()
        sensorTracker.start()
        Choreographer.getInstance().postFrameCallback(frameCallback)
        scheduleNextReminder()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        reminderHandler.removeCallbacksAndMessages(null)
        sensorTracker.stop()
    }

    fun attachWindowManager(wm: WindowManager, params: WindowManager.LayoutParams) {
        this.wm = wm
        this.wmParams = params
    }

    fun pause() {
        isScreenOn = false
        speechController.pause()
    }

    fun resume() {
        isScreenOn = true
        speechController.resume()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun destroy() {
        speechController.release()
        sensorTracker.stop()
        reminderHandler.removeCallbacksAndMessages(null)
        spriteSheet.recycle()
    }

    // ---- Tick: update state without drawing ----
    private fun tickState() {
        val now = System.currentTimeMillis()

        // Expire reaction
        if (reaction != null && now > reactionEndMs) {
            reaction = null
        }

        // Phoneme/speech update
        if (isSpeaking) {
            if (now > speakingUntilMs) {
                isSpeaking = false
                currentPhoneme = "M"
                bubbleText = ""
                talkBounce = 0f
            } else {
                val elapsed = (now - speakingStartMs) / 1000f
                val p = phonemeTimeline.firstOrNull { elapsed >= it.first && elapsed < it.second }?.third ?: "M"
                currentPhoneme = p
                talkBounce = sin(elapsed * 15f) * 0.025f
            }
        }

        // Touch-based direction update (if no gyro)
        if (!sensorTracker.hasGyro && touchScreenX >= 0) {
            val loc = IntArray(2)
            getLocationOnScreen(loc)
            val cx = loc[0] + width / 2f
            val cy = loc[1] + height / 2f
            updateDirectionFromAngle(touchScreenX - cx, touchScreenY - cy)
        }
    }

    private fun updateDirectionFromAngle(dx: Float, dy: Float) {
        val dist = hypot(dx, dy)
        val newDir = if (dist < 60) "center"
        else {
            val angle = atan2(dy, dx)
            val sector = ((Math.round(angle / (Math.PI / 4)).toInt() + 8) % 8)
            arrayOf("right","downright","down","downleft","left","upleft","up","upright")[sector]
        }
        if (newDir != direction) {
            direction = newDir
        }
    }

    // ---- Drawing ----
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // Apply talk bounce (subtle y scale)
        if (talkBounce != 0f) {
            canvas.save()
            canvas.scale(1f - talkBounce, 1f + talkBounce, w / 2, h / 2)
        }

        // Determine which tile to draw
        val tile: Bitmap? = when {
            isSpeaking && phonemeTiles.containsKey(currentPhoneme) -> phonemeTiles[currentPhoneme]
            reaction != null -> spriteSheet.getReactionTile(reaction!!)
            else -> spriteSheet.getDirectionTile(direction)
        }
        tile?.let {
            canvas.drawBitmap(it, null, RectF(0f, 0f, w, h), null)
        }

        if (talkBounce != 0f) canvas.restore()

        // Draw speech bubble above mascot
        if (bubbleText.isNotEmpty() && System.currentTimeMillis() < bubbleEndMs) {
            drawSpeechBubble(canvas, bubbleText, w)
        }
    }

    private fun drawSpeechBubble(canvas: Canvas, text: String, mascotW: Float) {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var cur = ""
        for (w in words) {
            val test = if (cur.isEmpty()) w else "$cur $w"
            if (bubbleTextPaint.measureText(test) > 420f && cur.isNotEmpty()) {
                lines += cur; cur = w
            } else cur = test
        }
        if (cur.isNotEmpty()) lines += cur

        val lineH = 38f
        val padX = 20f
        val padY = 12f
        val bw = lines.maxOf { bubbleTextPaint.measureText(it) } + padX * 2
        val bh = lines.size * lineH + padY * 2

        val bx = (mascotW - bw) / 2
        val by = -bh - 12f

        val rect = RectF(bx, by, bx + bw, by + bh)
        canvas.drawRoundRect(rect, 16f, 16f, bubblePaint)
        canvas.drawRoundRect(rect, 16f, 16f, bubbleStrokePaint)

        lines.forEachIndexed { i, line ->
            canvas.drawText(line, bx + padX, by + padY + (i + 1) * lineH - 8f, bubbleTextPaint)
        }
    }

    // ---- Touch handling ----
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rawX = event.rawX
        val rawY = event.rawY

        // Always update touch position for eye tracking
        touchScreenX = rawX
        touchScreenY = rawY

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragRawStartX = rawX
                dragRawStartY = rawY
                dragWinStartX = wmParams?.x ?: 0
                dragWinStartY = wmParams?.y ?: 0
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = rawX - dragRawStartX
                val dy = rawY - dragRawStartY
                if (isDragging || hypot(dx, dy) > 12f) {
                    isDragging = true
                    wmParams?.apply {
                        x = (dragWinStartX - dx).toInt()
                        y = (dragWinStartY - dy).toInt()
                    }
                    wm?.updateViewLayout(this, wmParams)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    // Boop!
                    triggerBoop()
                }
                isDragging = false
            }
        }
        return true
    }

    private fun triggerBoop() {
        reaction = listOf("blush", "blush", "sparkle", "heart", "shock").random()
        reactionEndMs = System.currentTimeMillis() + 800
        // Short haptic (single 35ms tap, not a long buzz)
        try {
            val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") vib?.vibrate(35)
            }
        } catch (_: Exception) {}
    }

    // ---- Reminders ----
    private fun scheduleNextReminder() {
        val intervalMs = (config.reminderIntervalMin * 60 * 1000L).coerceAtLeast(60_000L)
        reminderHandler.postDelayed({
            if (isScreenOn) triggerReminder()
            scheduleNextReminder()
        }, intervalMs)
    }

    fun triggerReminder() {
        val pool = if (config.voiceType == "japanese") config.japaneseReminders else config.reminders
        if (pool.isEmpty()) return
        val item = pool.random()
        speakDialogue(item.text, item.audio)
    }

    fun speakDialogue(text: String, audioName: String?) {
        val durationMs = speechController.speak(text, audioName) { }
        isSpeaking = true
        speakingStartMs = System.currentTimeMillis()
        speakingUntilMs = speakingStartMs + durationMs
        phonemeTimeline = PhonemeTimeline.build(text, durationMs / 1000f)
        currentPhoneme = phonemeTimeline.firstOrNull()?.third ?: "A"
        bubbleText = text
        bubbleEndMs = speakingUntilMs + 500
    }
}
