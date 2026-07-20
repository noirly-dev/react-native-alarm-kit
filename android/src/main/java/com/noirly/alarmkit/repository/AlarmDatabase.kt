package com.noirly.alarmkit.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AlarmEntity::class], version = 1, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {
  abstract fun alarmDao(): AlarmDao

  companion object {
    @Volatile
    private var instance: AlarmDatabase? = null

    fun getInstance(context: Context): AlarmDatabase {
      return instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          AlarmDatabase::class.java,
          "noirly_alarm_kit.db",
        ).build().also { instance = it }
      }
    }
  }
}
