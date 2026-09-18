package com.r2dapps.cutemascot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

/**
 * Clock-style alarm stored in SharedPreferences as JSON.
 * daysMask uses Calendar day bits: (1 shl SUNDAY) … (1 shl SATURDAY).
 */
data class AlarmItem(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "Reminder",
    var hour: Int = 9,
    var minute: Int = 0,
    var daysMask: Int = AlarmItem.EVERY_DAY,
    var enabled: Boolean = true,
    var vibrate: Boolean = true,
    /** "system" | "voice:filename.mp3" | "asset:filename.mp3" */
    var ringtone: String = "system",
    var snoozeMin: Int = 10,
    var speakText: String = ""
) {
    companion object {
        val EVERY_DAY: Int =
            (Calendar.SUNDAY..Calendar.SATURDAY).fold(0) { acc, d -> acc or (1 shl d) }
        val WEEKDAYS: Int =
            (Calendar.MONDAY..Calendar.FRIDAY).fold(0) { acc, d -> acc or (1 shl d) }

        fun fromJson(o: JSONObject): AlarmItem = AlarmItem(
            id = o.optString("id", UUID.randomUUID().toString()),
            title = o.optString("title", "Reminder"),
            hour = o.optInt("hour", 9).coerceIn(0, 23),
            minute = o.optInt("minute", 0).coerceIn(0, 59),
            daysMask = o.optInt("days_mask", EVERY_DAY),
            enabled = o.optBoolean("enabled", true),
            vibrate = o.optBoolean("vibrate", true),
            ringtone = o.optString("ringtone", "system"),
            snoozeMin = o.optInt("snooze_min", 10).coerceIn(1, 60),
            speakText = o.optString("speak_text", "")
        )
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("hour", hour)
        put("minute", minute)
        put("days_mask", daysMask)
        put("enabled", enabled)
        put("vibrate", vibrate)
        put("ringtone", ringtone)
        put("snooze_min", snoozeMin)
        put("speak_text", speakText)
    }

    fun hasDay(calendarDay: Int): Boolean = (daysMask and (1 shl calendarDay)) != 0

    fun toggleDay(calendarDay: Int) {
        daysMask = daysMask xor (1 shl calendarDay)
        if (daysMask == 0) daysMask = EVERY_DAY
    }

    fun timeLabel(use24h: Boolean = true): String {
        return if (use24h) {
            String.format("%02d:%02d", hour, minute)
        } else {
            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val ampm = if (hour < 12) "AM" else "PM"
            String.format("%d:%02d %s", h, minute, ampm)
        }
    }

    fun daysShortLabel(): String {
        if (daysMask == EVERY_DAY) return "Every day"
        if (daysMask == WEEKDAYS) return "Weekdays"
        val labels = listOf(
            Calendar.SUNDAY to "S",
            Calendar.MONDAY to "M",
            Calendar.TUESDAY to "T",
            Calendar.WEDNESDAY to "W",
            Calendar.THURSDAY to "T",
            Calendar.FRIDAY to "F",
            Calendar.SATURDAY to "S"
        )
        return labels.filter { hasDay(it.first) }.joinToString("") { it.second }
    }

    /** Next trigger time in epoch millis, or null if no days selected. */
    fun nextTriggerMillis(from: Long = System.currentTimeMillis()): Long? {
        if (daysMask == 0) return null
        val cal = Calendar.getInstance().apply { timeInMillis = from }
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        // Start search from "now"; if today's time already passed, begin tomorrow
        for (offset in 0..7) {
            val c = Calendar.getInstance().apply { timeInMillis = from }
            c.add(Calendar.DAY_OF_YEAR, offset)
            c.set(Calendar.HOUR_OF_DAY, hour)
            c.set(Calendar.MINUTE, minute)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            if (c.timeInMillis <= from) continue
            if (hasDay(c.get(Calendar.DAY_OF_WEEK))) return c.timeInMillis
        }
        return null
    }
}

object AlarmStore {
    private const val PREFS = "cute_mascot_alarms"
    private const val KEY = "alarms_json"

    fun load(context: Context): MutableList<AlarmItem> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null)
        if (raw.isNullOrBlank()) {
            val seeded = seedDefaults()
            save(context, seeded)
            return seeded
        }
        return try {
            val arr = JSONArray(raw)
            MutableList(arr.length()) { AlarmItem.fromJson(arr.getJSONObject(it)) }
        } catch (_: Exception) {
            seedDefaults()
        }
    }

    fun save(context: Context, alarms: List<AlarmItem>) {
        val arr = JSONArray()
        alarms.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun upsert(context: Context, item: AlarmItem) {
        val list = load(context)
        val idx = list.indexOfFirst { it.id == item.id }
        if (idx >= 0) list[idx] = item else list.add(item)
        save(context, list)
        ReminderScheduler.rescheduleAll(context)
    }

    fun delete(context: Context, id: String) {
        val list = load(context).filterNot { it.id == id }
        save(context, list)
        ReminderScheduler.rescheduleAll(context)
    }

    fun get(context: Context, id: String): AlarmItem? = load(context).find { it.id == id }

    private fun seedDefaults(): MutableList<AlarmItem> = mutableListOf(
        AlarmItem(
            title = "Drink water",
            hour = 10,
            minute = 0,
            daysMask = AlarmItem.EVERY_DAY,
            ringtone = "asset:water_godavari.mp3",
            speakText = "పోయి వాటర్ తాగు రా!"
        ),
        AlarmItem(
            title = "Stretch break",
            hour = 15,
            minute = 30,
            daysMask = AlarmItem.WEEKDAYS,
            ringtone = "asset:break_godavari.mp3",
            speakText = "లేసి కాసేపు నడువు రా!"
        )
    )
}
