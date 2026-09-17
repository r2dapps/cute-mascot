package com.r2dapps.cutemascot

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_OVERLAY = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        // Auto-start service if permission already granted
        if (Settings.canDrawOverlays(this) && !MascotOverlayService.isRunning) {
            startMascotService()
            // Minimize to background once started
            moveTaskToBack(true)
        }
    }

    private fun updateUI() {
        val canDraw = Settings.canDrawOverlays(this)
        findViewById<TextView>(R.id.tvStatus).text = if (canDraw)
            "✅ Permission granted! Mascot is floating~"
        else
            "⚠️ Needs overlay permission to float on screen"

        val btnPermission = findViewById<Button>(R.id.btnPermission)
        val btnToggle = findViewById<Button>(R.id.btnToggle)

        btnPermission.visibility = if (canDraw) View.GONE else View.VISIBLE
        btnToggle.visibility = if (canDraw) View.VISIBLE else View.GONE
        btnToggle.text = if (MascotOverlayService.isRunning) "💤 Hide Mascot" else "✨ Show Mascot"

        btnPermission.setOnClickListener { requestOverlayPermission() }
        btnToggle.setOnClickListener {
            if (MascotOverlayService.isRunning) {
                stopService(Intent(this, MascotOverlayService::class.java))
            } else {
                startMascotService()
            }
            finish()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, REQ_OVERLAY)
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
