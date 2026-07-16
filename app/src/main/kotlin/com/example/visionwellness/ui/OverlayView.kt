package com.example.visionwellness.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import timber.log.Timber

/**
 * Custom view for displaying eye-tracking status and alerts
 * Renders a blue border gradient that appears when staring is detected
 */
class OverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.BLUE
        alpha = 0
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    private var alphaValue: Int = 0
    private var isAnimating = false

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    /**
     * Trigger a staring alert with fade-in, hold, fade-out animation
     * Animation timeline:
     * - 0-200ms: Fade in
     * - 200-600ms: Hold at full opacity
     * - 600-800ms: Fade out
     */
    fun triggerStaringAlert(durationMs: Long = 800) {
        if (isAnimating) return
        
        Timber.d("Staring alert triggered")
        isAnimating = true

        val animator = ValueAnimator.ofInt(0, 200, 200, 0).apply {
            duration = durationMs
            // Set keyframe positions for timing
            setCurrentFraction(0f)
            addUpdateListener { animation ->
                alphaValue = animation.animatedValue as Int
                alphaValue = alphaValue.coerceIn(0, 200)  // Ensure within 0-200 range
                invalidate()
            }
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {
                    Timber.v("Alert animation started")
                }

                override fun onAnimationEnd(animation: android.animation.Animator) {
                    alphaValue = 0
                    isAnimating = false
                    invalidate()
                    Timber.v("Alert animation completed")
                }

                override fun onAnimationCancel(animation: android.animation.Animator) {
                    isAnimating = false
                }

                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
        }
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Only draw if there's an active animation
        if (alphaValue > 0) {
            // Draw blue border gradient
            paint.alpha = alphaValue
            val margin = 10f
            canvas.drawRect(
                margin,
                margin,
                width - margin,
                height - margin,
                paint
            )
        }
    }
}
