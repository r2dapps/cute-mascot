package com.r2dapps.cutemascot

import android.content.Context
import org.json.JSONObject

data class ReminderItem(val id: String, val text: String, val audio: String?)

/**
 * Loads mascot_config.json from assets.
 * Same structure as desktop mascot_config.json.
 */
class MascotConfig(context: Context) {
    var voiceType = "godavari"
    var voicePitch = "+10Hz"
    var voiceRate = "+8%"
    var remindersEnabled = true
    var reminderIntervalMin = 30L
    val reminders = mutableListOf<ReminderItem>()
    val japaneseReminders = mutableListOf<ReminderItem>()

    init {
        try {
            val json = JSONObject(context.assets.open("mascot_config.json").bufferedReader().readText())
            voiceType = json.optString("voice_type", "godavari")
            voicePitch = json.optString("voice_pitch", "+10Hz")
            voiceRate = json.optString("voice_rate", "+8%")
            remindersEnabled = json.optBoolean("reminders_enabled", true)
            reminderIntervalMin = json.optLong("reminder_interval_min", 30)

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
}
