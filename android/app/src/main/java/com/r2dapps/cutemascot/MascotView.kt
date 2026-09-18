package com.r2dapps.cutemascot

import android.content.Context
import android.graphics.*
import android.os.Build
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
        const val MASCOT_PX = 200
        private const val FRAME_INTERVAL_NS = 33_333_333L  // ~30fps cap
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
    private var speakingReaction: String? = null

    // Talking kinetic bounce (subtle)
    private var talkBounce = 0f

    // Speech bubble
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(235, 18, 22, 34)
    }
    private val bubbleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val bubbleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(255, 255, 105, 180)
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
    }
    private var bubbleText = ""
    private var bubbleEndMs = 0L
    var isAlarmActive = false

    // Drag tracking
    private var dragRawStartX = 0f
    private var dragRawStartY = 0f
    private var dragWinStartX = 0
    private var dragWinStartY = 0
    private var isDragging = false

    // Touch position (for eye tracking)
    private var touchScreenX = -1f
    private var touchScreenY = -1f
    private var lastTouchMs = 0L

    // Sensor tracker (gyro only when trackingMode == "gyro")
    private lateinit var sensorTracker: SensorTracker

    private val speechController: SpeechController
    val soundManager = SoundManager(context)
    private val config: MascotConfig

    // Choreographer frame callback
    private var lastFrameNs = 0L
    private lateinit var frameCallback: Choreographer.FrameCallback

    init {
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
            // Gyro only drives look direction in gyro mode; recent touch can temporarily override
            if (config.trackingMode != "gyro") return@SensorTracker
            val now = System.currentTimeMillis()
            if (touchScreenX >= 0 && now - lastTouchMs < 3500L) return@SensorTracker
            updateDirectionFromAngle(gx, gy)
        }
        syncTrackingSensors()
        speechController.applyVoiceType(config.voiceType)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        spriteSheet = SpriteSheet(context, config.character)
        phonemeTiles = spriteSheet.loadPhonemeTiles()
        syncTrackingSensors()
        speechController.applyVoiceType(config.voiceType)
        Choreographer.getInstance().postFrameCallback(frameCallback)
        ReminderScheduler.reschedule(context)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        sensorTracker.stop()
    }

    fun attachWindowManager(wm: WindowManager, params: WindowManager.LayoutParams) {
        this.wm = wm
        this.wmParams = params
    }

    fun pause() {
        isScreenOn = false
        speechController.pause()
        sensorTracker.stop()
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    fun resume() {
        isScreenOn = true
        speechController.resume()
        syncTrackingSensors()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun destroy() {
        speechController.release()
        soundManager.release()
        sensorTracker.stop()
        spriteSheet.recycle()
    }

    fun onConfigChanged() {
        try {
            spriteSheet.recycle()
        } catch (_: Exception) {}
        spriteSheet = SpriteSheet(context, config.character)
        speechController.applyVoiceType(config.voiceType)
        syncTrackingSensors()
        ReminderScheduler.reschedule(context)
        postInvalidate()
    }

    /** Start gyro only when user selected gyro mode; otherwise stop it completely. */
    private fun syncTrackingSensors() {
        sensorTracker.invertTilt = config.invertGyro
        if (config.trackingMode == "gyro" && sensorTracker.hasGyro) {
            sensorTracker.enabled = true
            sensorTracker.start()
        } else {
            sensorTracker.stop()
        }
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
                speakingReaction = null
                if (!isAlarmActive) {
                    bubbleText = ""
                }
                talkBounce = 0f
            } else {
                val elapsed = (now - speakingStartMs) / 1000f
                val p = phonemeTimeline.firstOrNull { elapsed >= it.first && elapsed < it.second }?.third ?: "M"
                currentPhoneme = p
                talkBounce = sin(elapsed * 15f) * 0.025f
            }
        }

        // Touch-based direction: primary in touch mode; temporary override while gyro is active
        val isRecentTouch = (touchScreenX >= 0 && (now - lastTouchMs < 3500L))
        val useTouch = when {
            config.trackingMode == "touch" || !sensorTracker.hasGyro -> true
            isRecentTouch -> true
            else -> false
        }
        if (useTouch && touchScreenX >= 0) {
            val loc = IntArray(2)
            getLocationOnScreen(loc)
            val cx = loc[0] + width / 2f
            val cy = loc[1] + height / 2f
            updateDirectionFromAngle(touchScreenX - cx, touchScreenY - cy)
        }
    }

    fun onScreenTouch(rawX: Float, rawY: Float) {
        touchScreenX = rawX
        touchScreenY = rawY
        lastTouchMs = System.currentTimeMillis()
        postInvalidate()
    }

    private fun updateDirectionFromAngle(dx: Float, dy: Float) {
        val dist = hypot(dx, dy)
        val newDir = if (dist < 50) "center"
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
        val bubbleAreaH = h * 0.32f
        val mascotAreaH = h * 0.68f

        // Mascot rect centered in bottom 68%
        val mascotSize = minOf(w, mascotAreaH)
        val mascotLeft = (w - mascotSize) / 2f
        val mascotTop = bubbleAreaH + (mascotAreaH - mascotSize) / 2f
        val mascotRect = RectF(mascotLeft, mascotTop, mascotLeft + mascotSize, mascotTop + mascotSize)

        // Kinetic talk bounce
        if (talkBounce != 0f) {
            canvas.save()
            canvas.scale(1f - talkBounce, 1f + talkBounce, mascotRect.centerX(), mascotRect.centerY())
        }

        // Determine which tile to draw: phonemes ONLY for mascot character
        val tile: Bitmap? = when {
            isSpeaking && config.character == "mascot" && phonemeTiles.containsKey(currentPhoneme) -> {
                phonemeTiles[currentPhoneme]
            }
            isSpeaking -> {
                spriteSheet.getReactionTile(speakingReaction ?: "grin")
            }
            reaction != null -> spriteSheet.getReactionTile(reaction!!)
            else -> spriteSheet.getDirectionTile(direction)
        }
        tile?.let {
            canvas.drawBitmap(it, null, mascotRect, null)
        }

        if (talkBounce != 0f) canvas.restore()

        // Draw speech bubble above mascot
        if (bubbleText.isNotEmpty() && System.currentTimeMillis() < bubbleEndMs) {
            drawSpeechBubble(canvas, bubbleText, w, bubbleAreaH, mascotRect.centerX())
        }
    }

    private fun drawSpeechBubble(canvas: Canvas, text: String, totalW: Float, maxBubbleH: Float, mascotCenterX: Float) {
        val paragraphs = text.split("\n")
        val lines = mutableListOf<String>()
        for (para in paragraphs) {
            val words = para.split(" ")
            var cur = ""
            for (w in words) {
                val test = if (cur.isEmpty()) w else "$cur $w"
                if (bubbleTextPaint.measureText(test) > (totalW - 36f) && cur.isNotEmpty()) {
                    lines += cur
                    cur = w
                } else {
                    cur = test
                }
            }
            if (cur.isNotEmpty()) lines += cur
        }

        val lineH = 30f
        val padX = 14f
        val padY = 6f
        val bw = (lines.maxOfOrNull { bubbleTextPaint.measureText(it) } ?: 80f) + padX * 2
        val bh = lines.size * lineH + padY * 2

        val bx = ((totalW - bw) / 2f).coerceIn(6f, totalW - bw - 6f)
        val by = (maxBubbleH - bh - 6f).coerceAtLeast(4f)

        val rect = RectF(bx, by, bx + bw, by + bh)
        canvas.drawRoundRect(rect, 14f, 14f, bubblePaint)
        canvas.drawRoundRect(rect, 14f, 14f, bubbleStrokePaint)

        // Pointer
        val pointer = Path().apply {
            moveTo(mascotCenterX - 8f, by + bh)
            lineTo(mascotCenterX, by + bh + 8f)
            lineTo(mascotCenterX + 8f, by + bh)
            close()
        }
        canvas.drawPath(pointer, bubblePaint)

        lines.forEachIndexed { i, line ->
            canvas.drawText(line, bx + padX, by + padY + (i + 1) * lineH - 6f, bubbleTextPaint)
        }
    }

    // ---- Touch handling ----
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rawX = event.rawX
        val rawY = event.rawY
        val density = resources.displayMetrics.density
        val bubbleAreaH = 65f * density

        // If bubble is not visible and touch is in the empty space above mascot, ignore
        if (bubbleText.isEmpty() && event.y < bubbleAreaH && !isDragging) {
            return false
        }

        touchScreenX = rawX
        touchScreenY = rawY
        lastTouchMs = System.currentTimeMillis()

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
                    updateDirectionFromAngle(dx, dy)
                    wmParams?.apply {
                        x = (dragWinStartX - dx).toInt()
                        y = (dragWinStartY - dy).toInt()
                    }
                    wm?.updateViewLayout(this, wmParams)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    triggerBoop()
                }
                isDragging = false
            }
        }
        return true
    }

    private fun triggerBoop() {
        if (isAlarmActive) {
            dismissActiveReminder()
            ReminderReceiver.stopCurrentAlarmSound(context)
            soundManager.playReaction("heart")
            return
        }
        val r = listOf("blush", "blush", "sparkle", "heart", "grin").random()
        reaction = r
        reactionEndMs = System.currentTimeMillis() + 850
        
        if (config.soundEnabled) {
            soundManager.playReaction(r)
        }
        
        // Short haptic tap
        try {
            val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") vib?.vibrate(35)
            }
        } catch (_: Exception) {}
    }

    // ---- Reminders (AlarmManager-driven; see ReminderScheduler) ----
    fun triggerAlarmReminder(text: String, audioName: String?) {
        isAlarmActive = true
        bubbleText = "⏰ $text\n(Tap mascot to Stop)"
        bubbleEndMs = Long.MAX_VALUE

        speechController.applyVoiceType(config.voiceType)
        val durationMs = if (config.voiceEnabled) {
            speechController.speak(text, audioName) { }
        } else {
            4500
        }
        isSpeaking = config.voiceEnabled
        speakingStartMs = System.currentTimeMillis()
        speakingUntilMs = speakingStartMs + durationMs
        val talkReactions = listOf("grin", "sparkle", "calm")
        speakingReaction = talkReactions.random()
        if (config.voiceEnabled && config.character == "mascot") {
            phonemeTimeline = PhonemeTimeline.build(text, durationMs / 1000f)
            currentPhoneme = phonemeTimeline.firstOrNull()?.third ?: "A"
        }
        postInvalidate()
    }

    fun dismissActiveReminder() {
        if (!isAlarmActive && bubbleText.isEmpty()) return
        isAlarmActive = false
        bubbleText = ""
        bubbleEndMs = 0L
        isSpeaking = false
        speakingReaction = null
        speechController.stopCurrent()
        postInvalidate()
    }

    fun triggerReminder() {
        val pool = if (config.voiceType == "japanese") config.japaneseReminders else config.reminders
        if (pool.isEmpty()) return
        val item = pool.random()
        triggerReminder(item.text, item.audio)
    }

    fun triggerReminder(text: String, audioName: String?) {
        MascotOverlayService.instance?.showReminderNotification("Cute Mascot", text)
        speakDialogue(text, audioName)
    }

    fun speakDialogue(text: String, audioName: String?) {
        speechController.applyVoiceType(config.voiceType)
        val durationMs = if (config.voiceEnabled) {
            speechController.speak(text, audioName) { }
        } else {
            4500
        }
        isSpeaking = config.voiceEnabled
        speakingStartMs = System.currentTimeMillis()
        speakingUntilMs = speakingStartMs + durationMs
        val talkReactions = listOf("grin", "sparkle", "calm")
        speakingReaction = talkReactions.random()
        if (config.voiceEnabled && config.character == "mascot") {
            phonemeTimeline = PhonemeTimeline.build(text, durationMs / 1000f)
            currentPhoneme = phonemeTimeline.firstOrNull()?.third ?: "A"
        }
        bubbleText = text
        bubbleEndMs = speakingUntilMs + 800
        postInvalidate()
    }
}
