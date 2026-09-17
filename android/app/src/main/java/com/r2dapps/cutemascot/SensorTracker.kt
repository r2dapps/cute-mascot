package com.r2dapps.cutemascot

import android.content.Context
import android.hardware.*
import android.content.pm.PackageManager
import kotlin.math.*

/**
 * Smart sensor tracker: uses gyroscope if available, silently falls back to touch-only.
 *
 * BATTERY OPTIMIZATION:
 * - Gyro sampled at SENSOR_DELAY_NORMAL (~5Hz) — not GAME or FASTEST
 * - Gyro integrates heading only, does not continuously drain CPU
 * - Completely stopped when screen is off (called from MascotView.pause/resume)
 */
class SensorTracker(
    private val context: Context,
    private val onDirectionAngle: (dx: Float, dy: Float) -> Unit
) {
    val hasGyro: Boolean = context.packageManager
        .hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var gyroSensor: Sensor? = null

    var invertTilt: Boolean = false

    // Gyro integration state
    private var yawDeg = 0f   // left-right tilt
    private var pitchDeg = 0f // up-down tilt
    private var lastTs = 0L

    private val gyroListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            if (lastTs == 0L) { lastTs = event.timestamp; return }
            val dt = (event.timestamp - lastTs) * 1e-9f  // ns → seconds
            lastTs = event.timestamp

            // event.values: [x=pitch, y=yaw, z=roll] in rad/s
            yawDeg   = (yawDeg   + Math.toDegrees(event.values[1].toDouble()).toFloat() * dt).coerceIn(-90f, 90f)
            pitchDeg = (pitchDeg + Math.toDegrees(event.values[0].toDouble()).toFloat() * dt).coerceIn(-45f, 45f)

            // Convert tilt angles to virtual dx/dy for direction calc (natural direction by default, invertable via UI)
            val mult = if (invertTilt) 1f else -1f
            onDirectionAngle(yawDeg * 2.5f * mult, pitchDeg * 2.5f * mult)
        }
    }

    fun start() {
        if (!hasGyro) return
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        gyroSensor?.let {
            // SENSOR_DELAY_NORMAL = ~5Hz — minimal battery impact
            sensorManager.registerListener(gyroListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        if (hasGyro) sensorManager.unregisterListener(gyroListener)
        lastTs = 0L
        yawDeg = 0f
        pitchDeg = 0f
    }
}
