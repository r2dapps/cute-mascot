package com.r2dapps.cutemascot

import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import androidx.core.app.NotificationCompat

/**
 * Foreground service that holds the floating mascot overlay window.
 * Battery-safe: pauses all animation when screen is off.
 */
class MascotOverlayService : Service() {

    companion object {
        var isRunning = false
        private const val CHANNEL_ID = "MascotChannel"
        private const val NOTIF_ID = 1
    }

    private lateinit var windowManager: WindowManager
    private lateinit var mascotView: MascotView
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> mascotView.pause()   // freeze everything
                Intent.ACTION_SCREEN_ON  -> mascotView.resume()  // wake back up
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Create mascot view (handles all drawing, touch, sensors internally)
        mascotView = MascotView(this)

        val params = WindowManager.LayoutParams(
            MascotView.MASCOT_PX,
            MascotView.MASCOT_PX,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 40
            y = 120
        }

        windowManager.addView(mascotView, params)
        mascotView.attachWindowManager(windowManager, params)

        // Register screen on/off receiver (battery: pause when screen off)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        unregisterReceiver(screenReceiver)
        if (::mascotView.isInitialized) {
            mascotView.destroy()
            windowManager.removeView(mascotView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Mascot Overlay",
                NotificationManager.IMPORTANCE_MIN  // silent, no sound/vibration
            ).apply {
                setShowBadge(false)
                setSound(null, null)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cute Mascot is floating~")
            .setContentText("Tap to manage")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(tapIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }
}
