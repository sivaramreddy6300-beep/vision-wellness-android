package com.example.visionwellness.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BlinkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BlinkDatabase : RoomDatabase() {

    abstract fun blinkDao(): BlinkDao

    companion object {
        private const val DATABASE_NAME = "vision_wellness_db"
        @Volatile
        private var instance: BlinkDatabase? = null

        fun getInstance(context: Context): BlinkDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    BlinkDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
        }
    }
}