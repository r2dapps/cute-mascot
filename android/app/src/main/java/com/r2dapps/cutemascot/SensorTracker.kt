package com.r2dapps.cutemascot

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.*

/**
 * Gyroscope tracker. Must be started only when trackingMode is "gyro".
 * Sampled at SENSOR_DELAY_NORMAL (~5Hz). Stopped when screen is off or touch mode is on.
 */
class SensorTracker(
    private val context: Context,
    private val onDirectionAngle: (dx: Float, dy: Float) -> Unit
) {
    val hasGyro: Boolean = context.packageManager
        .hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var gyroSensor: Sensor? = null
    private var listening = false

    var invertTilt: Boolean = false

    /** When false, sensor events are ignored even if registered. */
    var enabled: Boolean = false

    private var yawDeg = 0f
    private var pitchDeg = 0f
    private var lastTs = 0L

    private val gyroListener = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            if (!enabled) return
            if (lastTs == 0L) {
                lastTs = event.timestamp
                return
            }
            val dt = (event.timestamp - lastTs) * 1e-9f
            lastTs = event.timestamp

            yawDeg = (yawDeg + Math.toDegrees(event.values[1].toDouble()).toFloat() * dt).coerceIn(-90f, 90f)
            pitchDeg = (pitchDeg + Math.toDegrees(event.values[0].toDouble()).toFloat() * dt).coerceIn(-45f, 45f)

            val mult = if (invertTilt) 1f else -1f
            onDirectionAngle(yawDeg * 2.5f * mult, pitchDeg * 2.5f * mult)
        }
    }

    fun start() {
        if (!hasGyro || listening) return
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        gyroSensor?.let {
            sensorManager.registerListener(gyroListener, it, SensorManager.SENSOR_DELAY_NORMAL)
            listening = true
        }
    }

    fun stop() {
        if (listening) {
            sensorManager.unregisterListener(gyroListener)
            listening = false
        }
        lastTs = 0L
        yawDeg = 0f
        pitchDeg = 0f
        enabled = false
    }
}
