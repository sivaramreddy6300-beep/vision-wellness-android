package com.example.visionwellness.ui

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import timber.log.Timber

/**
 * Manages system-wide overlay window for staring alerts
 * Displays overlay on top of all other apps without blocking interaction
 */
class AlertOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: OverlayView? = null
    private var isOverlayShown = false

    /**
     * Show alert overlay on screen
     */
    fun showAlert() {
        if (isOverlayShown) return

        try {
            overlayView = OverlayView(context).apply {
                setBackgroundColor(Color.TRANSPARENT)
            }

            val params = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                format = PixelFormat.TRANSLUCENT
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT

                x = 0
                y = 0
                gravity = Gravity.TOP or Gravity.START
            }

            windowManager.addView(overlayView, params)
            isOverlayShown = true

            // Trigger animation
            overlayView?.triggerStaringAlert(durationMs = 800)

            Timber.d("Alert overlay shown")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show overlay: ${e.message}")
        }
    }

    /**
     * Hide alert overlay
     */
    fun hideAlert() {
        try {
            if (overlayView != null && isOverlayShown) {
                windowManager.removeView(overlayView)
                overlayView = null
                isOverlayShown = false
                Timber.d("Alert overlay hidden")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to hide overlay: ${e.message}")
        }
    }

    /**
     * Trigger a staring alert
     */
    fun triggerStaringAlert() {
        showAlert()
    }

    /**
     * Check if overlay is currently shown
     */
    fun isAlertShown(): Boolean = isOverlayShown

    /**
     * Release resources
     */
    fun release() {
        hideAlert()
        Timber.d("AlertOverlayManager released")
    }
}
