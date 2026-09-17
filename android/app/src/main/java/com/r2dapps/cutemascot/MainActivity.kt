package com.r2dapps.cutemascot

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_OVERLAY = 1001
        private const val REQ_NOTIF = 1002
    }

    private lateinit var config: MascotConfig
    private var testSpeechController: SpeechController? = null
    private var testSoundManager: SoundManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        config = MascotConfig(this)
        testSoundManager = SoundManager(this)
        setupSettingsUI()
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()

        // Request POST_NOTIFICATIONS on Android 13+ (API 33+) for reminder banners
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
            }
        }

        // Auto-start service if permission already granted
        if (Settings.canDrawOverlays(this) && !MascotOverlayService.isRunning) {
            startMascotService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        testSpeechController?.release()
        testSoundManager?.release()
    }

    // Touch tracking across the entire screen inside the application
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null && MascotOverlayService.isRunning) {
            MascotOverlayService.instance?.onScreenTouch(ev.rawX, ev.rawY)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun getSizeLabel(dp: Int): String = when {
        dp <= 110 -> "Mini"
        dp <= 150 -> "Small"
        dp <= 190 -> "Medium"
        dp <= 230 -> "Large"
        else -> "Jumbo"
    }

    private fun setupSettingsUI() {
        // --- 1. Mascot Size Slider & Presets ---
        val tvSizeValue = findViewById<TextView>(R.id.tvSizeValue)
        val seekSize = findViewById<SeekBar>(R.id.seekSize)

        fun applySize(sizeDp: Int) {
            config.mascotSizeDp = sizeDp
            tvSizeValue.text = "$sizeDp dp · ${getSizeLabel(sizeDp)}"
            MascotOverlayService.instance?.applyConfigUpdates()
        }

        val initialSize = config.mascotSizeDp.coerceIn(100, 260)
        seekSize.progress = initialSize - 100
        tvSizeValue.text = "$initialSize dp · ${getSizeLabel(initialSize)}"

        seekSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val sizeDp = 100 + progress
                    applySize(sizeDp)
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Quick Preset Chips
        fun setupPresetButton(btnId: Int, sizeDp: Int) {
            findViewById<Button>(btnId).setOnClickListener {
                seekSize.progress = sizeDp - 100
                applySize(sizeDp)
            }
        }
        setupPresetButton(R.id.btnSize100, 100)
        setupPresetButton(R.id.btnSize140, 140)
        setupPresetButton(R.id.btnSize180, 180)
        setupPresetButton(R.id.btnSize220, 220)
        setupPresetButton(R.id.btnSize260, 260)

        // --- 2. Character Skin (5 Styles) ---
        val rgSkin = findViewById<RadioGroup>(R.id.rgSkin)
        val rbSkinChibi = findViewById<RadioButton>(R.id.rbSkinChibi)
        val rbSkinMascot = findViewById<RadioButton>(R.id.rbSkinMascot)
        val rbSkinGuy = findViewById<RadioButton>(R.id.rbSkinGuy)
        val rbSkinPixel = findViewById<RadioButton>(R.id.rbSkinPixel)
        val rbSkinInk = findViewById<RadioButton>(R.id.rbSkinInk)

        when (config.character) {
            "mascot" -> rbSkinMascot.isChecked = true
            "guy" -> rbSkinGuy.isChecked = true
            "pixel" -> rbSkinPixel.isChecked = true
            "ink" -> rbSkinInk.isChecked = true
            else -> rbSkinChibi.isChecked = true
        }

        rgSkin.setOnCheckedChangeListener { _, checkedId ->
            config.character = when (checkedId) {
                R.id.rbSkinMascot -> "mascot"
                R.id.rbSkinGuy -> "guy"
                R.id.rbSkinPixel -> "pixel"
                R.id.rbSkinInk -> "ink"
                else -> "chibi"
            }
            MascotOverlayService.instance?.applyConfigUpdates()
        }

        // --- 3. Touch Tracking & Sound ---
        val rgTracking = findViewById<RadioGroup>(R.id.rgTracking)
        val rbTrackTouch = findViewById<RadioButton>(R.id.rbTrackTouch)
        val rbTrackGyro = findViewById<RadioButton>(R.id.rbTrackGyro)

        if (config.trackingMode == "gyro") rbTrackGyro.isChecked = true
        else rbTrackTouch.isChecked = true

        rgTracking.setOnCheckedChangeListener { _, checkedId ->
            config.trackingMode = if (checkedId == R.id.rbTrackGyro) "gyro" else "touch"
            MascotOverlayService.instance?.applyConfigUpdates()
        }

        val switchInvertGyro = findViewById<SwitchCompat>(R.id.switchInvertGyro)
        switchInvertGyro.isChecked = config.invertGyro
        switchInvertGyro.setOnCheckedChangeListener { _, isChecked ->
            config.invertGyro = isChecked
            MascotOverlayService.instance?.applyConfigUpdates()
        }

        val switchSound = findViewById<SwitchCompat>(R.id.switchSound)
        switchSound.isChecked = config.soundEnabled
        switchSound.setOnCheckedChangeListener { _, isChecked ->
            config.soundEnabled = isChecked
            if (isChecked) {
                testSoundManager?.playBoop()
            }
            MascotOverlayService.instance?.applyConfigUpdates()
        }

        findViewById<Button>(R.id.btnTestSound).setOnClickListener {
            testSoundManager?.playBoop()
        }

        // --- 4. Voice & Reminders ---
        val rgVoice = findViewById<RadioGroup>(R.id.rgVoice)
        val rbVoiceJapanese = findViewById<RadioButton>(R.id.rbVoiceJapanese)
        val rbVoiceGodavari = findViewById<RadioButton>(R.id.rbVoiceGodavari)

        if (config.voiceType == "godavari") rbVoiceGodavari.isChecked = true
        else rbVoiceJapanese.isChecked = true

        rgVoice.setOnCheckedChangeListener { _, checkedId ->
            config.voiceType = if (checkedId == R.id.rbVoiceGodavari) "godavari" else "japanese"
            MascotOverlayService.instance?.applyConfigUpdates()
        }

        val switchReminders = findViewById<SwitchCompat>(R.id.switchReminders)
        switchReminders.isChecked = config.remindersEnabled
        switchReminders.setOnCheckedChangeListener { _, isChecked ->
            config.remindersEnabled = isChecked
            MascotOverlayService.instance?.applyConfigUpdates()
        }

        val rgInterval = findViewById<RadioGroup>(R.id.rgInterval)
        val rbInt15 = findViewById<RadioButton>(R.id.rbInt15)
        val rbInt30 = findViewById<RadioButton>(R.id.rbInt30)
        val rbInt60 = findViewById<RadioButton>(R.id.rbInt60)

        when (config.reminderIntervalMin) {
            15L -> rbInt15.isChecked = true
            60L -> rbInt60.isChecked = true
            else -> rbInt30.isChecked = true
        }

        rgInterval.setOnCheckedChangeListener { _, checkedId ->
            config.reminderIntervalMin = when (checkedId) {
                R.id.rbInt15 -> 15L
                R.id.rbInt60 -> 60L
                else -> 30L
            }
            MascotOverlayService.instance?.applyConfigUpdates()
        }

        findViewById<Button>(R.id.btnTestVoice).setOnClickListener {
            playTestReminder()
        }

        // --- 5. Main Controls ---
        findViewById<Button>(R.id.btnStep1).setOnClickListener { openAppDetailsSettings() }
        findViewById<Button>(R.id.btnStep2).setOnClickListener { requestOverlayPermission() }

        findViewById<Button>(R.id.btnToggle).setOnClickListener {
            if (MascotOverlayService.isRunning) {
                stopService(Intent(this, MascotOverlayService::class.java))
                findViewById<Button>(R.id.btnToggle).text = "✨ Show Mascot"
            } else {
                startMascotService()
                findViewById<Button>(R.id.btnToggle).text = "💤 Hide Mascot"
            }
        }
    }

    private fun playTestReminder() {
        if (testSpeechController == null) {
            testSpeechController = SpeechController(this)
        }
        val list = if (config.voiceType == "japanese") config.japaneseReminders else config.reminders
        if (list.isNotEmpty()) {
            val item = list.random()
            testSpeechController?.applyVoiceType(config.voiceType)
            testSpeechController?.speak(item.text, item.audio)
        }
    }

    private fun updateUI() {
        val canDraw = Settings.canDrawOverlays(this)
        findViewById<TextView>(R.id.tvStatus).text = if (canDraw)
            "✨ Mascot Ready · Floating Active"
        else
            "⚠️ Display Permission Required"

        val cardGuide = findViewById<View>(R.id.cardPermissionGuide)
        cardGuide.visibility = if (canDraw) View.GONE else View.VISIBLE
        findViewById<Button>(R.id.btnToggle).text = if (MascotOverlayService.isRunning) "💤 Hide Mascot" else "✨ Show Mascot"
    }

    private fun openAppDetailsSettings() {
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun requestOverlayPermission() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQ_OVERLAY)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivityForResult(intent, REQ_OVERLAY)
        }
    }

    private fun startMascotService() {
        val intent = Intent(this, MascotOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OVERLAY) updateUI()
    }
}
