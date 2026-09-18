package com.r2dapps.cutemascot

import android.app.AlarmManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_OVERLAY = 1001
        private const val REQ_NOTIF = 1002
        private const val REQ_PICK_AUDIO = 1003
        private const val REQ_PICK_CHAR_DIR = 1004
        private const val PREFS_UI = "cute_mascot_ui"
    }

    private lateinit var config: MascotConfig
    private var testSpeechController: SpeechController? = null
    private var testSoundManager: SoundManager? = null
    private var pendingRingtoneRefresh: (() -> Unit)? = null
    private var pendingCustomCharName: String? = null
    private val thumbCache = mutableMapOf<String, Bitmap?>()
    private var selectedCharViews = mutableMapOf<String, View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        config = MascotConfig(this)
        testSoundManager = SoundManager(this)
        setupCollapsibles()
        setupSettingsUI()
        setupCharacterGrid()
        refreshAlarmList()
        updateUI()
        maybePromptExactAlarms()
        ReminderScheduler.rescheduleAll(this)
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        refreshAlarmList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
            }
        }
        if (Settings.canDrawOverlays(this) && !MascotOverlayService.isRunning) {
            startMascotService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        testSpeechController?.release()
        testSoundManager?.release()
        thumbCache.values.forEach { it?.recycle() }
        thumbCache.clear()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && MascotOverlayService.isRunning && config.trackingMode == "touch") {
            MascotOverlayService.instance?.onScreenTouch(ev.rawX, ev.rawY)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun uiPrefs() = getSharedPreferences(PREFS_UI, MODE_PRIVATE)

    private fun setupCollapsibles() {
        bindCollapse("perm", R.id.headerPermissions, R.id.bodyPermissions, R.id.chevronPermissions, defaultOpen = !Settings.canDrawOverlays(this))
        bindCollapse("size", R.id.headerSize, R.id.bodySize, R.id.chevronSize, defaultOpen = false)
        bindCollapse("skin", R.id.headerSkin, R.id.bodySkin, R.id.chevronSkin, defaultOpen = true)
        bindCollapse("track", R.id.headerTrack, R.id.bodyTrack, R.id.chevronTrack, defaultOpen = false)
        bindCollapse("alarms", R.id.headerAlarms, R.id.bodyAlarms, R.id.chevronAlarms, defaultOpen = true)
    }

    private fun bindCollapse(
        key: String,
        headerId: Int,
        bodyId: Int,
        chevronId: Int,
        defaultOpen: Boolean
    ) {
        val body = findViewById<View>(bodyId)
        val chevron = findViewById<TextView>(chevronId)
        var open = uiPrefs().getBoolean("collapse_$key", defaultOpen)
        fun apply() {
            body.visibility = if (open) View.VISIBLE else View.GONE
            chevron.text = if (open) "▾" else "▸"
        }
        apply()
        findViewById<View>(headerId).setOnClickListener {
            open = !open
            uiPrefs().edit().putBoolean("collapse_$key", open).apply()
            apply()
        }
    }

    private fun getSizeLabel(dp: Int): String = when {
        dp <= 110 -> "Mini"
        dp <= 150 -> "Small"
        dp <= 190 -> "Medium"
        dp <= 230 -> "Large"
        else -> "Jumbo"
    }

    private fun setupSettingsUI() {
        val tvSizeValue = findViewById<TextView>(R.id.tvSizeValue)
        val seekSize = findViewById<SeekBar>(R.id.seekSize)

        fun applySize(sizeDp: Int) {
            config.mascotSizeDp = sizeDp
            tvSizeValue.text = "$sizeDp · ${getSizeLabel(sizeDp)}"
        }

        val initialSize = config.mascotSizeDp.coerceIn(100, 260)
        seekSize.progress = initialSize - 100
        tvSizeValue.text = "$initialSize · ${getSizeLabel(initialSize)}"
        seekSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) applySize(100 + progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        fun preset(id: Int, size: Int) {
            findViewById<Button>(id).setOnClickListener {
                seekSize.progress = size - 100
                applySize(size)
            }
        }
        preset(R.id.btnSize100, 100)
        preset(R.id.btnSize140, 140)
        preset(R.id.btnSize180, 180)
        preset(R.id.btnSize220, 220)
        preset(R.id.btnSize260, 260)

        val switchInvertGyro = findViewById<SwitchCompat>(R.id.switchInvertGyro)
        fun refreshGyroControls() {
            val gyro = config.trackingMode == "gyro"
            switchInvertGyro.isEnabled = gyro
            switchInvertGyro.alpha = if (gyro) 1f else 0.45f
        }
        val rbTrackTouch = findViewById<RadioButton>(R.id.rbTrackTouch)
        val rbTrackGyro = findViewById<RadioButton>(R.id.rbTrackGyro)
        if (config.trackingMode == "gyro") rbTrackGyro.isChecked = true else rbTrackTouch.isChecked = true
        refreshGyroControls()
        findViewById<RadioGroup>(R.id.rgTracking).setOnCheckedChangeListener { _, checkedId ->
            config.trackingMode = if (checkedId == R.id.rbTrackGyro) "gyro" else "touch"
            refreshGyroControls()
        }
        switchInvertGyro.isChecked = config.invertGyro
        switchInvertGyro.setOnCheckedChangeListener { _, checked -> config.invertGyro = checked }

        findViewById<SwitchCompat>(R.id.switchSound).apply {
            isChecked = config.soundEnabled
            setOnCheckedChangeListener { _, checked ->
                config.soundEnabled = checked
                if (checked) testSoundManager?.playBoop()
            }
        }
        findViewById<Button>(R.id.btnTestSound).setOnClickListener { testSoundManager?.playBoop() }

        // Voice type — 4 options: female Telugu, female Japanese, male Telugu, male Japanese
        val rbVoiceGodavari = findViewById<RadioButton>(R.id.rbVoiceGodavari)
        val rbVoiceJapanese = findViewById<RadioButton>(R.id.rbVoiceJapanese)
        val rbVoiceGodavariMale = findViewById<RadioButton>(R.id.rbVoiceGodavariMale)
        val rbVoiceJapaneseMale = findViewById<RadioButton>(R.id.rbVoiceJapaneseMale)
        when (config.voiceType) {
            "japanese"      -> rbVoiceJapanese.isChecked = true
            "godavari-male" -> rbVoiceGodavariMale.isChecked = true
            "japanese-male" -> rbVoiceJapaneseMale.isChecked = true
            else            -> rbVoiceGodavari.isChecked = true
        }
        findViewById<RadioGroup>(R.id.rgVoice).setOnCheckedChangeListener { _, checkedId ->
            config.voiceType = when (checkedId) {
                R.id.rbVoiceJapanese      -> "japanese"
                R.id.rbVoiceGodavariMale  -> "godavari-male"
                R.id.rbVoiceJapaneseMale  -> "japanese-male"
                else                      -> "godavari"
            }
        }

        findViewById<SwitchCompat>(R.id.switchVoice).apply {
            isChecked = config.voiceEnabled
            setOnCheckedChangeListener { _, checked -> config.voiceEnabled = checked }
        }

        findViewById<SwitchCompat>(R.id.switchReminders).apply {
            isChecked = config.remindersEnabled
            setOnCheckedChangeListener { _, checked ->
                config.remindersEnabled = checked
                ReminderScheduler.rescheduleAll(this@MainActivity)
                Toast.makeText(this@MainActivity, if (checked) "Alarms enabled" else "Alarms paused", Toast.LENGTH_SHORT).show()
                if (checked) maybePromptExactAlarms()
            }
        }

        findViewById<SwitchCompat>(R.id.switch24Hour).apply {
            isChecked = config.use24HourFormat
            setOnCheckedChangeListener { _, checked ->
                config.use24HourFormat = checked
                refreshAlarmList()
            }
        }

        findViewById<Button>(R.id.btnAddAlarm).setOnClickListener {
            showAlarmEditor(AlarmItem())
        }
        findViewById<Button>(R.id.btnImportVoiceMain).setOnClickListener { pickVoiceNote() }
        findViewById<Button>(R.id.btnTestVoice).setOnClickListener { playTestReminder() }

        findViewById<View>(R.id.btnStopAlarmMain)?.setOnClickListener {
            ReminderReceiver.stopCurrentAlarmSound(this)
            MascotOverlayService.instance?.dismissActiveReminder()
            findViewById<View>(R.id.cardActiveAlarm)?.visibility = View.GONE
            Toast.makeText(this, "Alarm stopped", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnSnoozeAlarmMain)?.setOnClickListener {
            val alarmId = ReminderReceiver.activeAlarmId
            ReminderReceiver.stopCurrentAlarmSound(this)
            MascotOverlayService.instance?.dismissActiveReminder()
            if (alarmId != null) {
                val alarm = AlarmStore.get(this, alarmId)
                if (alarm != null) {
                    ReminderScheduler.scheduleSnooze(this, alarm)
                    Toast.makeText(this, "Snoozed for ${alarm.snoozeMin} minutes", Toast.LENGTH_SHORT).show()
                }
            }
            findViewById<View>(R.id.cardActiveAlarm)?.visibility = View.GONE
        }

        findViewById<View>(R.id.btnOpenCredits)?.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nilbuild/page-mascot")))
            } catch (_: Exception) {
                Toast.makeText(this, "https://github.com/nilbuild/page-mascot", Toast.LENGTH_LONG).show()
            }
        }

        findViewById<Button>(R.id.btnStep1).setOnClickListener { openAppDetailsSettings() }
        findViewById<Button>(R.id.btnStep2).setOnClickListener { requestOverlayPermission() }
        findViewById<Button>(R.id.btnStepAlarmPermission)?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                } catch (_: Exception) {
                    openAppDetailsSettings()
                }
            } else {
                Toast.makeText(this, "Alarms are enabled natively on your Android version", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btnStepNotifPermission)?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
            } else {
                openAppDetailsSettings()
            }
        }
        findViewById<Button>(R.id.btnToggle).setOnClickListener {
            if (MascotOverlayService.isRunning) {
                MascotOverlayService.isRunning = false
                stopService(Intent(this, MascotOverlayService::class.java))
                findViewById<Button>(R.id.btnToggle).text = getString(R.string.show_mascot)
                updateUI()
            } else {
                if (!Settings.canDrawOverlays(this)) {
                    requestOverlayPermission()
                    return@setOnClickListener
                }
                MascotOverlayService.isRunning = true
                startMascotService()
                findViewById<Button>(R.id.btnToggle).text = getString(R.string.hide_mascot)
                updateUI()
                it.postDelayed({ updateUI() }, 350)
            }
        }
    }

    private fun setupCharacterGrid() {
        val grid = findViewById<GridLayout>(R.id.bodySkin)
        grid.removeAllViews()
        selectedCharViews.clear()
        val density = resources.displayMetrics.density
        val gap = (6 * density).toInt()
        CharacterThumb.getAllOptions(this).forEach { opt ->
            val item = layoutInflater.inflate(R.layout.item_character, grid, false)
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(gap, gap, gap, gap)
            }
            item.layoutParams = params
            item.findViewById<TextView>(R.id.tvName).text = opt.label
            val img = item.findViewById<ImageView>(R.id.imgThumb)
            val bmp = thumbCache.getOrPut(opt.id) { CharacterThumb.loadCenter(this, opt.id) }
            if (bmp != null) img.setImageBitmap(bmp)
            item.setOnClickListener {
                config.character = opt.id
                highlightCharacter(opt.id)
            }
            selectedCharViews[opt.id] = item
            grid.addView(item)
        }
        highlightCharacter(config.character)

        findViewById<View>(R.id.btnAIPrompts)?.setOnClickListener {
            showAIPromptsDialog()
        }

        findViewById<View>(R.id.btnImportCharacter)?.setOnClickListener {
            val input = com.google.android.material.textfield.TextInputEditText(this).apply {
                hint = "e.g. cat, hero, pikachu"
            }
            val frame = android.widget.FrameLayout(this).apply {
                setPadding(50, 20, 50, 10)
                addView(input)
            }
            AlertDialog.Builder(this)
                .setTitle("Import Custom Mascot")
                .setMessage("Enter a mascot name, then pick your 3x3 directions.png sprite sheet:")
                .setView(frame)
                .setPositiveButton("Pick Sprite Sheet") { _, _ ->
                    val name = input.text?.toString()?.trim()?.lowercase()?.replace(" ", "_")?.ifBlank { "custom_" + (System.currentTimeMillis() % 1000) }
                        ?: "custom_mascot"
                    pendingCustomCharName = name
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "image/*"
                    }
                    startActivityForResult(intent, REQ_PICK_CHAR_DIR)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showAIPromptsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_ai_prompts, null)
        val clipboard = getSystemService(android.content.ClipboardManager::class.java)

        val promptDir = dialogView.findViewById<TextView>(R.id.tvPromptDirections).text.toString()
        val promptReact = dialogView.findViewById<TextView>(R.id.tvPromptReactions).text.toString()

        dialogView.findViewById<View>(R.id.btnCopyDirections).setOnClickListener {
            val clip = android.content.ClipData.newPlainText("3x3 Directions Prompt", promptDir)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(this, "Copied Directions Prompt! Paste into ChatGPT or Midjourney", Toast.LENGTH_LONG).show()
        }

        dialogView.findViewById<View>(R.id.btnCopyReactions).setOnClickListener {
            val clip = android.content.ClipData.newPlainText("3x3 Reactions Prompt", promptReact)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(this, "Copied Reactions Prompt! Paste into ChatGPT or Midjourney", Toast.LENGTH_LONG).show()
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun highlightCharacter(id: String) {
        selectedCharViews.forEach { (cid, view) ->
            view.setBackgroundResource(
                if (cid == id) R.drawable.bg_char_item_selected else R.drawable.bg_char_item
            )
        }
    }

    private fun refreshAlarmList() {
        val list = findViewById<LinearLayout>(R.id.listAlarms)
        list.removeAllViews()
        AlarmStore.load(this).forEach { alarm ->
            val row = layoutInflater.inflate(R.layout.item_alarm, list, false)
            val timeStr = alarm.timeLabel(config.use24HourFormat)
            row.findViewById<TextView>(R.id.tvAlarmTime).text = timeStr
            row.findViewById<TextView>(R.id.tvAlarmMeta).text =
                "${alarm.title} · ${alarm.daysShortLabel()}"
            val sw = row.findViewById<SwitchCompat>(R.id.switchAlarmEnabled)
            sw.isChecked = alarm.enabled
            sw.setOnCheckedChangeListener { _, checked ->
                alarm.enabled = checked
                AlarmStore.upsert(this, alarm)
            }
            // Easy 1-Tap Delete button
            row.findViewById<View>(R.id.btnDeleteAlarm)?.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete Alarm?")
                    .setMessage("Remove alarm for $timeStr (${alarm.title})?")
                    .setPositiveButton("Delete") { _, _ ->
                        AlarmStore.delete(this, alarm.id)
                        refreshAlarmList()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            row.setOnClickListener { showAlarmEditor(alarm) }
            list.addView(row)
        }
    }

    private data class RingtoneOption(val label: String, val value: String)

    private fun ringtoneOptions(): List<RingtoneOption> {
        val opts = mutableListOf(RingtoneOption("System default", "system"))
        VoiceNotes.listAssetVoices(this).forEach {
            opts += RingtoneOption("Bundled · $it", "asset:$it")
        }
        VoiceNotes.listFiles(this).forEach {
            opts += RingtoneOption("My note · ${it.name}", "voice:${it.name}")
        }
        return opts
    }

    private fun showAlarmEditor(alarm: AlarmItem) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_alarm, null)
        val etTitle = view.findViewById<TextInputEditText>(R.id.etAlarmTitle)
        val etSpeak = view.findViewById<TextInputEditText>(R.id.etSpeakText)
        val btnTestVoice = view.findViewById<View>(R.id.btnTestVoice)
        val timePicker = view.findViewById<TimePicker>(R.id.timePicker)
        val spinnerRing = view.findViewById<Spinner>(R.id.spinnerRingtone)
        val spinnerSnooze = view.findViewById<Spinner>(R.id.spinnerSnooze)
        val switchVibrate = view.findViewById<SwitchCompat>(R.id.switchVibrate)
        val rowDays = view.findViewById<LinearLayout>(R.id.rowDays)

        etTitle.setText(alarm.title)
        etSpeak.setText(alarm.speakText)
        btnTestVoice?.setOnClickListener {
            val textToSpeak = etSpeak.text?.toString()?.ifBlank { etTitle.text?.toString() } ?: "Time to stretch and drink water!"
            SpeechController(this).speak(textToSpeak)
        }
        timePicker.setIs24HourView(config.use24HourFormat)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.hour = alarm.hour
            timePicker.minute = alarm.minute
        } else {
            @Suppress("DEPRECATION")
            timePicker.currentHour = alarm.hour
            @Suppress("DEPRECATION")
            timePicker.currentMinute = alarm.minute
        }
        switchVibrate.isChecked = alarm.vibrate

        val dayDefs = listOf(
            Calendar.SUNDAY to "S",
            Calendar.MONDAY to "M",
            Calendar.TUESDAY to "T",
            Calendar.WEDNESDAY to "W",
            Calendar.THURSDAY to "T",
            Calendar.FRIDAY to "F",
            Calendar.SATURDAY to "S"
        )
        val dayButtons = mutableMapOf<Int, MaterialButton>()
        rowDays.removeAllViews()
        dayDefs.forEach { (day, label) ->
            val btn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = label
                textSize = 12f
                minimumWidth = 0
                minWidth = (36 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = (4 * resources.displayMetrics.density).toInt()
                }
                isCheckable = true
                isChecked = alarm.hasDay(day)
                addOnCheckedChangeListener { _, checked ->
                    val bit = 1 shl day
                    alarm.daysMask = if (checked) alarm.daysMask or bit else alarm.daysMask and bit.inv()
                    if (alarm.daysMask == 0) {
                        alarm.daysMask = bit
                        isChecked = true
                    }
                }
            }
            dayButtons[day] = btn
            rowDays.addView(btn)
        }

        fun bindRingtoneSpinner() {
            val options = ringtoneOptions()
            spinnerRing.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                options.map { it.label }
            )
            val idx = options.indexOfFirst { it.value == alarm.ringtone }.let { if (it < 0) 0 else it }
            spinnerRing.setSelection(idx)
            spinnerRing.tag = options
        }
        bindRingtoneSpinner()
        pendingRingtoneRefresh = { bindRingtoneSpinner() }

        val snoozeValues = listOf(5, 10, 15, 30)
        spinnerSnooze.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            snoozeValues.map { "$it min" }
        )
        spinnerSnooze.setSelection(snoozeValues.indexOf(alarm.snoozeMin).let { if (it < 0) 1 else it })

        view.findViewById<Button>(R.id.btnImportVoice).setOnClickListener { pickVoiceNote() }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (AlarmStore.get(this, alarm.id) == null) "New alarm" else "Edit alarm")
            .setView(view)
            .setPositiveButton("Save", null)
            .setNeutralButton("Delete") { _, _ ->
                AlarmStore.delete(this, alarm.id)
                refreshAlarmList()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                alarm.title = etTitle.text?.toString()?.ifBlank { "Reminder" } ?: "Reminder"
                alarm.speakText = etSpeak.text?.toString().orEmpty()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarm.hour = timePicker.hour
                    alarm.minute = timePicker.minute
                } else {
                    @Suppress("DEPRECATION")
                    alarm.hour = timePicker.currentHour
                    @Suppress("DEPRECATION")
                    alarm.minute = timePicker.currentMinute
                }
                alarm.vibrate = switchVibrate.isChecked
                @Suppress("UNCHECKED_CAST")
                val options = spinnerRing.tag as List<RingtoneOption>
                alarm.ringtone = options.getOrNull(spinnerRing.selectedItemPosition)?.value ?: "system"
                alarm.snoozeMin = snoozeValues.getOrElse(spinnerSnooze.selectedItemPosition) { 10 }
                AlarmStore.upsert(this, alarm)
                refreshAlarmList()
                dialog.dismiss()
                Toast.makeText(this, "Alarm saved · ${alarm.timeLabel()}", Toast.LENGTH_SHORT).show()
            }
            // Hide delete for brand-new unsaved? still ok — deletes nothing harmful if not in store
            if (AlarmStore.get(this, alarm.id) == null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).visibility = View.GONE
            }
        }
        dialog.show()
    }

    private fun pickVoiceNote() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
        }
        startActivityForResult(intent, REQ_PICK_AUDIO)
    }

    private fun playTestReminder() {
        if (testSpeechController == null) testSpeechController = SpeechController(this)
        testSpeechController?.applyVoiceType(config.voiceType)
        testSpeechController?.speak("Hey this is Cute Mascot", null)
    }

    private fun updateUI() {
        val canDraw = Settings.canDrawOverlays(this)
        findViewById<TextView>(R.id.tvStatus).text =
            if (canDraw) getString(R.string.status_ready) else getString(R.string.status_need_permission)

        findViewById<View>(R.id.cardPermissionGuide)?.visibility = View.VISIBLE
        findViewById<Button>(R.id.btnStep2)?.text =
            if (canDraw) "2 · Display Over Apps (Granted ✓)" else "2 · Allow Display Over Apps ⚠️"

        val am = getSystemService(AlarmManager::class.java)
        val hasAlarmPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am?.canScheduleExactAlarms() == true else true
        findViewById<Button>(R.id.btnStepAlarmPermission)?.text =
            if (hasAlarmPerm) "3 · Alarms & Reminders (Allowed ✓)" else "3 · Allow Alarms & Reminders ⚠️"

        val hasNotifPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
        findViewById<Button>(R.id.btnStepNotifPermission)?.text =
            if (hasNotifPerm) "4 · Notifications (Allowed ✓)" else "4 · Allow Notifications ⚠️"

        findViewById<Button>(R.id.btnToggle).text =
            if (MascotOverlayService.isRunning) getString(R.string.hide_mascot)
            else getString(R.string.show_mascot)

        val isRinging = ReminderReceiver.activeMediaPlayer != null ||
                        ReminderReceiver.activeRingtone != null ||
                        ReminderReceiver.activeAlarmId != null
        val card = findViewById<View>(R.id.cardActiveAlarm)
        if (isRinging) {
            card?.visibility = View.VISIBLE
            val title = ReminderReceiver.activeAlarmTitle ?: "Reminder"
            findViewById<TextView>(R.id.tvActiveAlarmTitle)?.text = "⏰ $title Ringing!"
        } else {
            card?.visibility = View.GONE
        }
    }

    private fun maybePromptExactAlarms() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (!config.remindersEnabled) return
        val am = getSystemService(AlarmManager::class.java) ?: return
        if (am.canScheduleExactAlarms()) return
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        } catch (_: Exception) {}
    }

    private fun openAppDetailsSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            )
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun requestOverlayPermission() {
        try {
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                REQ_OVERLAY
            )
        } catch (_: Exception) {
            startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION), REQ_OVERLAY)
        }
    }

    private fun startMascotService() {
        val intent = Intent(this, MascotOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_OVERLAY -> updateUI()
            REQ_PICK_CHAR_DIR -> {
                if (resultCode == RESULT_OK) {
                    val uri = data?.data
                    val name = pendingCustomCharName ?: "custom"
                    if (uri != null) {
                        try {
                            val dir = java.io.File(getExternalFilesDir(null), "custom_characters/$name").apply { mkdirs() }
                            val outFile = java.io.File(dir, "directions.png")
                            contentResolver.openInputStream(uri)?.use { input ->
                                outFile.outputStream().use { output -> input.copyTo(output) }
                            }
                            thumbCache.remove(name)
                            config.character = name
                            setupCharacterGrid()
                            Toast.makeText(this, "Imported mascot: $name!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this, "Failed to import image", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            REQ_PICK_AUDIO -> {
                if (resultCode == RESULT_OK) {
                    val uri = data?.data
                    if (uri != null) {
                        val name = VoiceNotes.importFromUri(this, uri)
                        if (name != null) {
                            Toast.makeText(this, "Saved voice note: $name", Toast.LENGTH_SHORT).show()
                            pendingRingtoneRefresh?.invoke()
                        } else {
                            Toast.makeText(this, "Could not import audio", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
