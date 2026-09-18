package com.r2dapps.cutemascot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import java.io.File

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        var activeMediaPlayer: MediaPlayer? = null
        var activeRingtone: android.media.Ringtone? = null
        var activeAlarmId: String? = null
        var activeAlarmTitle: String? = null

        fun stopCurrentAlarmSound(context: Context? = null) {
            try {
                activeMediaPlayer?.stop()
                activeMediaPlayer?.release()
            } catch (_: Exception) {}
            activeMediaPlayer = null

            try {
                activeRingtone?.stop()
            } catch (_: Exception) {}
            activeRingtone = null

            context?.let { ctx ->
                activeAlarmId?.let { id ->
                    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    nm?.cancel(8000 + (id.hashCode() and 0xFFFF))
                }
            }
            activeAlarmId = null
            activeAlarmTitle = null
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                ReminderScheduler.rescheduleAll(context)
                return
            }
            ReminderScheduler.ACTION_DISMISS -> {
                val id = intent.getStringExtra(ReminderScheduler.EXTRA_ALARM_ID) ?: return
                stopCurrentAlarmSound(context)
                cancelNotif(context, id)
                MascotOverlayService.instance?.dismissActiveReminder()
                return
            }
            ReminderScheduler.ACTION_SNOOZE -> {
                val id = intent.getStringExtra(ReminderScheduler.EXTRA_ALARM_ID) ?: return
                val alarm = AlarmStore.get(context, id) ?: return
                stopCurrentAlarmSound(context)
                ReminderScheduler.scheduleSnooze(context, alarm)
                cancelNotif(context, id)
                MascotOverlayService.instance?.dismissActiveReminder()
                return
            }
            ReminderScheduler.ACTION_REMINDER -> {
                val id = intent.getStringExtra(ReminderScheduler.EXTRA_ALARM_ID) ?: return
                val isSnooze = intent.getBooleanExtra(ReminderScheduler.EXTRA_SNOOZE, false)
                val alarm = AlarmStore.get(context, id) ?: return
                if (!alarm.enabled && !isSnooze) {
                    ReminderScheduler.rescheduleAll(context)
                    return
                }
                deliver(context, alarm)
                if (!isSnooze) {
                    // Re-arm next daily/weekly occurrence
                    ReminderScheduler.scheduleAlarm(context, alarm)
                }
            }
        }
    }

    private fun deliver(context: Context, alarm: AlarmItem) {
        stopCurrentAlarmSound(context)
        activeAlarmId = alarm.id
        activeAlarmTitle = alarm.title
        showNotification(context, alarm)
        if (alarm.vibrate) vibrate(context)
        playRingtone(context, alarm)

        val text = alarm.speakText.ifBlank { alarm.title }
        val audioName = when {
            alarm.ringtone.startsWith("voice:") -> alarm.ringtone.removePrefix("voice:")
            alarm.ringtone.startsWith("asset:") -> alarm.ringtone.removePrefix("asset:")
            else -> null
        }
        // Shows persistent bubble on mascot until user taps Stop or taps mascot
        MascotOverlayService.instance?.triggerReminderFromAlarm(text, audioName)
    }

    private fun playRingtone(context: Context, alarm: AlarmItem) {
        try {
            when {
                alarm.ringtone == "system" || alarm.ringtone.isBlank() -> {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val rt = RingtoneManager.getRingtone(context, uri)
                    activeRingtone = rt
                    rt?.play()
                }
                alarm.ringtone.startsWith("voice:") -> {
                    val name = alarm.ringtone.removePrefix("voice:")
                    val file = File(File(context.getExternalFilesDir(null), "voice_notes"), name)
                    if (file.exists()) playFile(file.absolutePath)
                }
                alarm.ringtone.startsWith("asset:") -> {
                    val name = alarm.ringtone.removePrefix("asset:")
                    try {
                        val afd = context.assets.openFd("voices_cache/$name")
                        val mp = MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                    .build()
                            )
                            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            afd.close()
                            prepare()
                            setOnCompletionListener { 
                                it.release()
                                if (activeMediaPlayer == it) activeMediaPlayer = null
                            }
                            start()
                        }
                        activeMediaPlayer = mp
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }

    private fun playFile(path: String) {
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(path)
                prepare()
                setOnCompletionListener { 
                    it.release()
                    if (activeMediaPlayer == it) activeMediaPlayer = null
                }
                start()
            }
            activeMediaPlayer = mp
        } catch (_: Exception) {}
    }

    private fun vibrate(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vib?.vibrate(longArrayOf(0, 400, 200, 400), -1)
                }
            }
        } catch (_: Exception) {}
    }

    private fun showNotification(context: Context, alarm: AlarmItem) {
        val channelId = "MascotReminders"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Cute Mascot Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Clock-style mascot alarms"
                    enableVibration(true)
                }
            )
        }

        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val dismissPi = PendingIntent.getBroadcast(
            context,
            ("dismiss_btn_" + alarm.id).hashCode(),
            Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderScheduler.ACTION_DISMISS
                putExtra(ReminderScheduler.EXTRA_ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val snoozePi = PendingIntent.getBroadcast(
            context,
            ("snooze_btn_" + alarm.id).hashCode(),
            Intent(context, ReminderReceiver::class.java).apply {
                action = ReminderScheduler.ACTION_SNOOZE
                putExtra(ReminderScheduler.EXTRA_ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val body = alarm.speakText.ifBlank { alarm.daysShortLabel() + " · " + alarm.timeLabel() }
        val notif = NotificationCompat.Builder(context, channelId)
            .setContentTitle("⏰ " + alarm.title)
            .setContentText("Tap Stop to turn off or Snooze (${alarm.snoozeMin}m)")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body + "\n\n▶ Action: Tap 'Stop Alarm' or 'Snooze'"))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(0, "⏹ Stop Alarm", dismissPi)
            .addAction(0, "💤 Snooze ${alarm.snoozeMin}m", snoozePi)
            .build()
        nm.notify(notifId(alarm.id), notif)
    }

    private fun cancelNotif(context: Context, id: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notifId(id))
    }

    private fun notifId(alarmId: String): Int = 8000 + (alarmId.hashCode() and 0xFFFF)
}
