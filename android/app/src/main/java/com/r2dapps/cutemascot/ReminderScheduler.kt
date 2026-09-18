package com.r2dapps.cutemascot

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules each enabled [AlarmItem] like the Clock app (RTC wall-clock + day mask).
 * Also supports one-shot snooze alarms.
 */
object ReminderScheduler {

    const val ACTION_REMINDER = "com.r2dapps.cutemascot.ACTION_REMINDER"
    const val ACTION_SNOOZE = "com.r2dapps.cutemascot.ACTION_SNOOZE"
    const val ACTION_DISMISS = "com.r2dapps.cutemascot.ACTION_DISMISS"
    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_SNOOZE = "is_snooze"

    fun rescheduleAll(context: Context) {
        val alarms = AlarmStore.load(context)
        // Cancel all known request codes then re-arm enabled ones
        alarms.forEach { cancel(context, it.id) }
        cancel(context, "legacy_interval")

        // Schedule only if master alarms switch is enabled
        if (MascotConfig(context).remindersEnabled) {
            alarms.filter { it.enabled }.forEach { scheduleAlarm(context, it) }
        }
    }

    fun scheduleAlarm(context: Context, alarm: AlarmItem) {
        val trigger = alarm.nextTriggerMillis() ?: return
        setExact(context, alarm.id, trigger, snooze = false)
    }

    fun scheduleSnooze(context: Context, alarm: AlarmItem) {
        val trigger = System.currentTimeMillis() + alarm.snoozeMin * 60_000L
        setExact(context, "snooze_${alarm.id}", trigger, snooze = true, alarmId = alarm.id)
    }

    fun cancel(context: Context, id: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context, id, snooze = id.startsWith("snooze_")))
    }

    /** @deprecated kept so old interval callers compile during migration */
    fun reschedule(context: Context) = rescheduleAll(context)

    private fun setExact(
        context: Context,
        requestKey: String,
        triggerAt: Long,
        snooze: Boolean,
        alarmId: String = requestKey.removePrefix("snooze_")
    ) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, requestKey, snooze, alarmId)
        try {
            // Use setAlarmClock: standard for clock apps, bypasses SCHEDULE_EXACT_ALARM restrictions & wakes from Doze
            val showIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val clockInfo = AlarmManager.AlarmClockInfo(triggerAt, showIntent)
            am.setAlarmClock(clockInfo, pi)
        } catch (e: Exception) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } else {
                    @Suppress("DEPRECATION")
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            } catch (e2: Exception) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        }
    }

    private fun pendingIntent(
        context: Context,
        requestKey: String,
        snooze: Boolean,
        alarmId: String = requestKey.removePrefix("snooze_")
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_SNOOZE, snooze)
        }
        val req = requestKey.hashCode()
        return PendingIntent.getBroadcast(
            context,
            req,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
