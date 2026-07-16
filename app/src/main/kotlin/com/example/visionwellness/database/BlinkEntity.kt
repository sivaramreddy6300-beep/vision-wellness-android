package com.example.visionwellness.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "blink_data")
data class BlinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val blinkCount: Int,
    val averageEAR: Float,
    val staringDuration: Long,  // in milliseconds
    val hourOfDay: Int,
    val dateString: String  // YYYY-MM-DD format for grouping
)