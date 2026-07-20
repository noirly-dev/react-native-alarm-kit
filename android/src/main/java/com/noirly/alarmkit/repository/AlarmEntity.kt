package com.noirly.alarmkit.repository

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
  @PrimaryKey val id: String,
  val triggerAtMillis: Double,
  val recurrenceFrequency: String?,
  val recurrenceDaysOfWeek: String?,
  val recurrenceIntervalDays: Int?,
  val title: String,
  val payload: String?,
  val soundRef: String?,
  val snoozeDefaultMinutes: Int?,
  val snoozeMaxCount: Int?,
  val state: String,
  val createdAtMillis: Double,
  val updatedAtMillis: Double,
  val lastFiredAtMillis: Double?,
  val snoozeCount: Int,
)
