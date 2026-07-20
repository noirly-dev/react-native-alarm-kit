package com.noirly.alarmkit.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AlarmDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(alarm: AlarmEntity)

  @Update
  suspend fun update(alarm: AlarmEntity)

  @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
  suspend fun getById(id: String): AlarmEntity?

  @Query("SELECT * FROM alarms")
  suspend fun getAll(): List<AlarmEntity>

  @Query("DELETE FROM alarms WHERE id = :id")
  suspend fun deleteById(id: String)

  @Query("DELETE FROM alarms")
  suspend fun deleteAll()

  @Query("SELECT COUNT(*) FROM alarms")
  suspend fun count(): Int
}
