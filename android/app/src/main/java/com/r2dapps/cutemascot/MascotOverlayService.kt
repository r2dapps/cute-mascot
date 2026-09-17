package com.r2dapps.cutemascot

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import androidx.core.app.NotificationCompat

/**
 * Foreground service that holds the floating mascot overlay window.
 * Battery-safe: pauses all animation when screen is off.
 * Live-updates size, character skin, and tracking mode on config change.
 */
class MascotOverlayService : Service() {

    companion object {
        var isRunning = false
        var instance: MascotOverlayService? = null

        private const val CHANNEL_ID = "MascotChannel"
        private const val REMINDER_CHANNEL_ID = "MascotReminders"
        private const val NOTIF_ID = 1
        private var nextReminderNotifId = 100
    }

    private lateinit var windowManager: WindowManager
    private lateinit var mascotView: MascotView
    private lateinit var config: MascotConfig
    private lateinit var overlayParams: WindowManager.LayoutParams

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (!::mascotView.isInitialized) return
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> mascotView.pause()   // freeze everything
                Intent.ACTION_SCREEN_ON  -> mascotView.resume()  // wake back up
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = true
        config = MascotConfig(this)

        createNotificationChannels()

        // Safe startForeground with Android 14 API 34 foregroundServiceType support
        try {
            val notif = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIF_ID, notif)
            }
        } catch (e: Exception) {
            try {
                startForeground(NOTIF_ID, buildNotification())
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Create mascot view (handles all drawing, touch, sensors internally)
        mascotView = MascotView(this)

        val (widthPx, heightPx) = getWindowDimensions()
        overlayParams = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 40
            y = 120
        }

        try {
            windowManager.addView(mascotView, overlayParams)
            mascotView.attachWindowManager(windowManager, overlayParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Register screen on/off receiver with API 33+ RECEIVER_NOT_EXPORTED flag to prevent Android 14 SecurityException
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenReceiver, screenFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(screenReceiver, screenFilter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getMascotSizePx(): Int {
        val density = resources.displayMetrics.density
        return (config.mascotSizeDp * density).toInt().coerceAtLeast(100)
    }

    fun getWindowDimensions(): Pair<Int, Int> {
        val density = resources.displayMetrics.density
        val mascotSize = getMascotSizePx()
        val extraH = (65 * density).toInt()
        val w = maxOf(mascotSize, (190 * density).toInt())
        val h = mascotSize + extraH
        return Pair(w, h)
    }

    fun applyConfigUpdates() {
        if (!::mascotView.isInitialized) return
        val (newW, newH) = getWindowDimensions()
        if (overlayParams.width != newW || overlayParams.height != newH) {
            overlayParams.width = newW
            overlayParams.height = newH
            try {
                windowManager.updateViewLayout(mascotView, overlayParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mascotView.onConfigChanged()
    }

    fun onScreenTouch(rawX: Float, rawY: Float) {
        if (::mascotView.isInitialized) {
            mascotView.onScreenTouch(rawX, rawY)
        }
    }

    fun showReminderNotification(title: String, message: String) {
        try {
            val tapIntent = PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notif = NotificationCompat.Builder(this, REMINDER_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(tapIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(nextReminderNotifId++, notif)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isRunning = false
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {}

        if (::mascotView.isInitialized) {
            mascotView.destroy()
            try {
                windowManager.removeView(mascotView)
            } catch (_: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            
            // 1. Silent persistent channel for foreground service
            val chService = NotificationChannel(
                CHANNEL_ID,
                "Mascot Overlay Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                setSound(null, null)
            }
            nm.createNotificationChannel(chService)

            // 2. High-importance heads-up channel for reminders
            val chReminder = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Cute Mascot Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Periodic anime health reminders & alerts"
                enableLights(true)
                enableVibration(true)
            }
            nm.createNotificationChannel(chReminder)
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
            .setContentText("Tap to open settings & skins")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(tapIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }
}
