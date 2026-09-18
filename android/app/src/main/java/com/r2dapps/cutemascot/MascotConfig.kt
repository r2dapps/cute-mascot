package com.r2dapps.cutemascot

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

data class ReminderItem(val id: String, val text: String, val audio: String?)

/**
 * Loads mascot_config.json defaults from assets, overridden and persisted
 * via Android SharedPreferences.
 */
class MascotConfig(private val context: Context) {

    companion object {
        const val ACTION_CONFIG_CHANGED = "com.r2dapps.cutemascot.CONFIG_CHANGED"
        private const val PREFS_NAME = "cute_mascot_prefs"

        const val KEY_CHARACTER = "character"
        const val KEY_VOICE_TYPE = "voice_type"
        const val KEY_VOICE_ENABLED = "voice_enabled"
        const val KEY_MASCOT_SIZE = "mascot_size"
        const val KEY_REMINDERS_ENABLED = "reminders_enabled"
        const val KEY_REMINDER_INTERVAL = "reminder_interval_min"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_TRACKING_MODE = "tracking_mode"
        const val KEY_INVERT_GYRO = "invert_gyro"
        const val KEY_USE_24_HOUR = "use_24_hour"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var character: String
        get() = prefs.getString(KEY_CHARACTER, defaultCharacter) ?: defaultCharacter
        set(value) {
            prefs.edit().putString(KEY_CHARACTER, value).apply()
            notifyChange()
        }

    var voiceType: String
        get() = prefs.getString(KEY_VOICE_TYPE, defaultVoiceType) ?: defaultVoiceType
        set(value) {
            prefs.edit().putString(KEY_VOICE_TYPE, value).apply()
            notifyChange()
        }

    var voiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_ENABLED, defaultVoiceEnabled)
        set(value) {
            prefs.edit().putBoolean(KEY_VOICE_ENABLED, value).apply()
            notifyChange()
        }

    var mascotSizeDp: Int
        get() = prefs.getInt(KEY_MASCOT_SIZE, defaultMascotSize)
        set(value) {
            prefs.edit().putInt(KEY_MASCOT_SIZE, value).apply()
            notifyChange()
        }

    var remindersEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDERS_ENABLED, defaultRemindersEnabled)
        set(value) {
            prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, value).apply()
            ReminderScheduler.reschedule(context)
            notifyChange()
        }

    var reminderIntervalMin: Long
        get() = prefs.getLong(KEY_REMINDER_INTERVAL, defaultReminderIntervalMin)
        set(value) {
            prefs.edit().putLong(KEY_REMINDER_INTERVAL, value).apply()
            ReminderScheduler.reschedule(context)
            notifyChange()
        }

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, defaultSoundEnabled)
        set(value) {
            prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()
            notifyChange()
        }

    var trackingMode: String
        get() = prefs.getString(KEY_TRACKING_MODE, "touch") ?: "touch"
        set(value) {
            prefs.edit().putString(KEY_TRACKING_MODE, value).apply()
            notifyChange()
        }

    var invertGyro: Boolean
        get() = prefs.getBoolean(KEY_INVERT_GYRO, false)
        set(value) {
            prefs.edit().putBoolean(KEY_INVERT_GYRO, value).apply()
            notifyChange()
        }

    var use24HourFormat: Boolean
        get() = prefs.getBoolean(
            KEY_USE_24_HOUR,
            android.text.format.DateFormat.is24HourFormat(context)
        )
        set(value) {
            prefs.edit().putBoolean(KEY_USE_24_HOUR, value).apply()
            notifyChange()
        }

    var voicePitch = "+10Hz"
    var voiceRate = "+8%"
    val reminders = mutableListOf<ReminderItem>()
    val japaneseReminders = mutableListOf<ReminderItem>()

    private var defaultCharacter = "chibi"
    private var defaultVoiceType = "godavari"
    private var defaultVoiceEnabled = true
    private var defaultRemindersEnabled = true
    private var defaultReminderIntervalMin = 30L
    private var defaultSoundEnabled = true
    private var defaultMascotSize = 160

    init {
        try {
            val json = JSONObject(context.assets.open("mascot_config.json").bufferedReader().readText())
            defaultCharacter = json.optString("character", "chibi")
            defaultVoiceType = json.optString("voice_type", "godavari")
            voicePitch = json.optString("voice_pitch", "+10Hz")
            voiceRate = json.optString("voice_rate", "+8%")
            defaultVoiceEnabled = json.optBoolean("voice_enabled", true)
            defaultRemindersEnabled = json.optBoolean("reminders_enabled", true)
            defaultReminderIntervalMin = json.optLong("reminder_interval_min", 30)
            defaultSoundEnabled = json.optBoolean("sound_enabled", false)
            defaultMascotSize = json.optInt("size", 160).coerceIn(100, 260)

            val rl = json.optJSONArray("reminders_list")
            if (rl != null) {
                for (i in 0 until rl.length()) {
                    val o = rl.getJSONObject(i)
                    reminders += ReminderItem(
                        o.optString("id"),
                        o.optString("text"),
                        o.optString("audio").takeIf { it.isNotEmpty() }
                    )
                }
            }

            val jl = json.optJSONArray("japanese_reminders_list")
            if (jl != null) {
                for (i in 0 until jl.length()) {
                    val o = jl.getJSONObject(i)
                    japaneseReminders += ReminderItem(
                        o.optString("id"),
                        o.optString("text"),
                        o.optString("audio").takeIf { it.isNotEmpty() }
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun notifyChange() {
        MascotOverlayService.instance?.applyConfigUpdates()
    }
}
