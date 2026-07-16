package com.example.visionwellness.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.visionwellness.R
import timber.log.Timber

class EyeTrackingService : Service() {

    companion object {
        private const val CHANNEL_ID = "vision_wellness_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("EyeTrackingService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("EyeTrackingService started")

        // Start as foreground service with persistent notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // TODO: Initialize camera and start tracking
        // initializeCamera()
        // startEyeTracking()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("EyeTrackingService destroyed")
        // TODO: Cleanup camera resources
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vision Wellness Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors your blinking habits to prevent eye strain"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): NotificationCompat.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vision Wellness")
            .setContentText("Monitoring your eye health...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}