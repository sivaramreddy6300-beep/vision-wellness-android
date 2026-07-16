package com.example.visionwellness.optimization

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import timber.log.Timber

/**
 * Monitors device sensors (proximity, temperature) to optimize processing
 * Pauses eye tracking when screen is off or device is hot
 */
class SensorMonitor(
    private val context: Context,
    private val onProximityChanged: (isNear: Boolean) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val thermometer = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

    private var currentTemperature: Float = 25f  // Default room temperature
    private var isNearFace = false

    companion object {
        private const val TEMPERATURE_CRITICAL = 45f  // Stop processing above 45°C
        private const val TEMPERATURE_HIGH = 40f      // Throttle above 40°C
        private const val TEMPERATURE_NORMAL = 35f    // Resume full processing below 35°C
    }

    /**
     * Start monitoring sensors
     */
    fun startMonitoring() {
        try {
            proximitySensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                Timber.d("Proximity sensor monitoring started")
            }
            thermometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                Timber.d("Thermometer monitoring started")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error starting sensor monitoring")
        }
    }

    /**
     * Stop monitoring sensors
     */
    fun stopMonitoring() {
        try {
            sensorManager.unregisterListener(this)
            Timber.d("Sensor monitoring stopped")
        } catch (e: Exception) {
            Timber.e(e, "Error stopping sensor monitoring")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values[0]
                val maxRange = event.sensor.maxRange
                val isNear = distance < maxRange
                if (isNear != isNearFace) {
                    isNearFace = isNear
                    onProximityChanged(isNear)
                    Timber.d("Proximity changed: isNear=$isNear, distance=$distance")
                }
            }
            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                currentTemperature = event.values[0]
                Timber.v("Device temperature: $currentTemperature°C")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    /**
     * Get current device temperature
     */
    fun getCurrentTemperature(): Float = currentTemperature

    /**
     * Check if device is too hot for processing
     */
    fun isDeviceTooHot(): Boolean = currentTemperature >= TEMPERATURE_CRITICAL

    /**
     * Check if device is in high temperature zone (throttle processing)
     */
    fun isDeviceHot(): Boolean = currentTemperature >= TEMPERATURE_HIGH

    /**
     * Get temperature status
     */
    fun getTemperatureStatus(): TemperatureStatus {
        return when {
            currentTemperature >= TEMPERATURE_CRITICAL -> TemperatureStatus.CRITICAL
            currentTemperature >= TEMPERATURE_HIGH -> TemperatureStatus.HIGH
            currentTemperature >= TEMPERATURE_NORMAL -> TemperatureStatus.NORMAL
            else -> TemperatureStatus.COOL
        }
    }

    /**
     * Check if proximity sensor indicates face is near
     */
    fun isFaceNear(): Boolean = isNearFace

    enum class TemperatureStatus {
        CRITICAL,  // >= 45°C - Stop processing
        HIGH,      // 40-45°C - Throttle processing
        NORMAL,    // 35-40°C - Normal processing
        COOL       // < 35°C - Full processing
    }
}
