package com.example.visionwellness.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BlinkDao {

    @Insert
    suspend fun insertBlinkData(blink: BlinkEntity)

    @Query("SELECT * FROM blink_data WHERE dateString = :date ORDER BY hourOfDay")
    suspend fun getBlinkDataForDate(date: String): List<BlinkEntity>

    @Query("SELECT SUM(blinkCount) FROM blink_data WHERE dateString = :date")
    suspend fun getTotalBlinksForDate(date: String): Int?

    @Query("SELECT AVG(staringDuration) FROM blink_data WHERE dateString = :date")
    suspend fun getAverageStaringDurationForDate(date: String): Long?

    @Query("SELECT * FROM blink_data ORDER BY timestamp DESC LIMIT 100")
    suspend fun getRecentBlinkData(): List<BlinkEntity>

    @Query("DELETE FROM blink_data WHERE dateString < :cutoffDate")
    suspend fun deleteOldData(cutoffDate: String)
}