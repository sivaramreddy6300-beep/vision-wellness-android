package com.example.visionwellness.optimization

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import timber.log.Timber

/**
 * Monitors and optimizes memory usage
 * Triggers garbage collection when memory pressure is high
 */
class MemoryProfiler(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val runtime = Runtime.getRuntime()

    companion object {
        private const val MEMORY_PRESSURE_HIGH = 0.85f  // 85% of max memory used
        private const val MEMORY_PRESSURE_CRITICAL = 0.95f  // 95% of max memory used
    }

    /**
     * Get current memory usage info
     */
    fun getMemoryUsage(): MemoryUsageInfo {
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory

        val percentageUsed = usedMemory.toFloat() / maxMemory.toFloat()

        return MemoryUsageInfo(
            maxMemory = maxMemory,
            totalMemory = totalMemory,
            usedMemory = usedMemory,
            freeMemory = freeMemory,
            percentageUsed = percentageUsed
        )
    }

    /**
     * Check if memory pressure is high
     */
    fun isMemoryPressureHigh(): Boolean {
        val usage = getMemoryUsage()
        return usage.percentageUsed >= MEMORY_PRESSURE_HIGH
    }

    /**
     * Check if memory pressure is critical
     */
    fun isMemoryPressureCritical(): Boolean {
        val usage = getMemoryUsage()
        return usage.percentageUsed >= MEMORY_PRESSURE_CRITICAL
    }

    /**
     * Get native heap size
     */
    fun getNativeHeapSize(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Debug.getNativeHeap().sumOf { it.memUsage }
        } else {
            0L
        }
    }

    /**
     * Log memory usage details
     */
    fun logMemoryUsage() {
        val usage = getMemoryUsage()
        Timber.d(
            "Memory Usage - Used: ${formatBytes(usage.usedMemory)} / ${formatBytes(usage.maxMemory)} " +
            "(${(usage.percentageUsed * 100).toInt()}%) | Free: ${formatBytes(usage.freeMemory)}"
        )
    }

    /**
     * Trigger garbage collection if needed
     */
    fun optimizeMemoryIfNeeded(): Boolean {
        if (isMemoryPressureHigh()) {
            Timber.w("Memory pressure high - triggering GC")
            System.gc()
            return true
        }
        return false
    }

    /**
     * Format bytes to human-readable string
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> "${bytes / 1_000_000_000}GB"
            bytes >= 1_000_000 -> "${bytes / 1_000_000}MB"
            bytes >= 1_000 -> "${bytes / 1_000}KB"
            else -> "${bytes}B"
        }
    }

    data class MemoryUsageInfo(
        val maxMemory: Long,
        val totalMemory: Long,
        val usedMemory: Long,
        val freeMemory: Long,
        val percentageUsed: Float
    )
}
