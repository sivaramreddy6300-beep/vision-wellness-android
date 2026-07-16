package com.example.visionwellness.optimization

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.content.IntentFilter
import timber.log.Timber

/**
 * Monitors device battery status and temperature
 * Used to adjust processing intensity based on battery health
 */
class BatteryMonitor(private val context: Context) {

    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    companion object {
        const val BATTERY_LEVEL_CRITICAL = 15  // Below 15%
        const val BATTERY_LEVEL_LOW = 30       // Below 30%
        const val BATTERY_LEVEL_NORMAL = 50    // Below 50%
    }

    /**
     * Get current battery percentage
     */
    fun getBatteryPercentage(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            } else {
                val ifilter = IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
                val batteryStatus = context.registerReceiver(null, ifilter)
                val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level >= 0 && scale > 0) (level * 100) / scale else 50
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting battery percentage")
            50  // Default to 50% if error
        }
    }

    /**
     * Get battery health status
     */
    fun getBatteryHealth(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_HEALTH)
            } else {
                val ifilter = IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
                val batteryStatus = context.registerReceiver(null, ifilter)
                batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                    ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting battery health")
            BatteryManager.BATTERY_HEALTH_UNKNOWN
        }
    }

    /**
     * Check if device is low power mode
     */
    fun isLowPowerMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            batteryManager.isCharging.not()
        } else {
            false
        }
    }

    /**
     * Get battery status level
     */
    fun getBatteryStatusLevel(): BatteryStatusLevel {
        val percentage = getBatteryPercentage()
        return when {
            percentage <= BATTERY_LEVEL_CRITICAL -> BatteryStatusLevel.CRITICAL
            percentage <= BATTERY_LEVEL_LOW -> BatteryStatusLevel.LOW
            percentage <= BATTERY_LEVEL_NORMAL -> BatteryStatusLevel.NORMAL
            else -> BatteryStatusLevel.HEALTHY
        }
    }

    enum class BatteryStatusLevel {
        CRITICAL,  // < 15%
        LOW,       // 15-30%
        NORMAL,    // 30-50%
        HEALTHY    // > 50%
    }
}
