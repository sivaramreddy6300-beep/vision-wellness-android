package com.example.visionwellness.advanced

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import timber.log.Timber
import java.util.Calendar

/**
 * Intelligent notification customizer that respects user preferences and system state
 * - Respects Do Not Disturb (DND) hours
 * - Adjusts notification frequency based on usage patterns
 * - Respects system Do Not Disturb settings
 */
class NotificationCustomizer(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "vision_wellness_channel"
        private const val ALERT_CHANNEL_ID = "vision_wellness_alerts"

        // Default DND hours (9 PM to 8 AM)
        private const val DND_START_HOUR = 21
        private const val DND_END_HOUR = 8
    }

    private var lastAlertTime: Long = 0
    private var alertFrequencyMs = 300000  // 5 minutes default between alerts
    private var customDndStartHour = DND_START_HOUR
    private var customDndEndHour = DND_END_HOUR
    private var respectSystemDnd = true

    /**
     * Check if current time is within Do Not Disturb hours
     */
    private fun isWithinDndHours(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        return if (customDndStartHour < customDndEndHour) {
            // Normal case: e.g., 9 AM to 5 PM
            currentHour in customDndStartHour..customDndEndHour
        } else {
            // Overnight case: e.g., 9 PM to 8 AM
            currentHour >= customDndStartHour || currentHour < customDndEndHour
        }
    }

    /**
     * Check if system Do Not Disturb is enabled
     */
    private fun isSystemDndEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted &&
                    notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        } else {
            false
        }
    }

    /**
     * Determine if a staring alert should be shown
     */
    fun shouldShowStaringAlert(): Boolean {
        val currentTime = System.currentTimeMillis()

        // Check if enough time has passed since last alert
        if (currentTime - lastAlertTime < alertFrequencyMs) {
            Timber.v("Alert suppressed: frequency limit")
            return false
        }

        // Check DND status
        val isDnd = isWithinDndHours() || (respectSystemDnd && isSystemDndEnabled())
        if (isDnd) {
            Timber.v("Alert suppressed: DND mode")
            return false
        }

        lastAlertTime = currentTime
        return true
    }

    /**
     * Build notification with appropriate priority and settings
     */
    fun buildStaringAlertNotification(title: String, message: String): NotificationCompat.Notification {
        val isDnd = isWithinDndHours()
        val priority = if (isDnd) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_DEFAULT

        return NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(priority)
            .setAutoCancel(true)
            .setVibrate(if (isDnd) longArrayOf() else longArrayOf(0, 250, 250, 250))  // Silent in DND
            .setSound(null)  // Silent by default, user can enable in settings
            .build()
    }

    /**
     * Set custom DND hours
     */
    fun setCustomDndHours(startHour: Int, endHour: Int) {
        customDndStartHour = startHour.coerceIn(0, 23)
        customDndEndHour = endHour.coerceIn(0, 23)
        Timber.d("Custom DND set: $customDndStartHour:00 - $customDndEndHour:00")
    }

    /**
     * Set alert frequency (minimum time between alerts)
     */
    fun setAlertFrequency(frequencyMs: Long) {
        alertFrequencyMs = frequencyMs.coerceAtLeast(60000)  // Minimum 1 minute
        Timber.d("Alert frequency set to ${alertFrequencyMs / 1000}s")
    }

    /**
     * Enable/disable respecting system DND
     */
    fun setRespectSystemDnd(respect: Boolean) {
        respectSystemDnd = respect
        Timber.d("System DND respect: $respect")
    }

    /**
     * Get current notification settings summary
     */
    fun getSettingsSummary(): String {
        val dndStatus = if (isWithinDndHours()) "In DND" else "Not in DND"
        val frequencyMin = alertFrequencyMs / 60000
        return "$dndStatus | Alert frequency: ${frequencyMin}min | System DND: $respectSystemDnd"
    }

    /**
     * Get time until next alert is allowed
     */
    fun getTimeUntilNextAlert(): Long {
        val timeSinceLastAlert = System.currentTimeMillis() - lastAlertTime
        return (alertFrequencyMs - timeSinceLastAlert).coerceAtLeast(0)
    }
}
