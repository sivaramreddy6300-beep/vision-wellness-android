package com.example.visionwellness.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import timber.log.Timber

class OverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.BLUE
        alpha = 0
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    private var alphaValue: Int = 0

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    /**
     * Trigger a staring alert with a fade-in fade-out animation
     */
    fun triggerStaringAlert(durationMs: Long = 800) {
        Timber.d("Staring alert triggered")

        val animator = ValueAnimator.ofInt(0, 200, 0).apply {
            duration = durationMs
            addUpdateListener { animation ->
                alphaValue = animation.animatedValue as Int
                invalidate()
            }
        }
        animator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

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