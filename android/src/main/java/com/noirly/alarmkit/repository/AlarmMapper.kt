package com.noirly.alarmkit.repository

import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments
import com.noirly.alarmkit.AlarmKitException
import java.util.UUID

data class AlarmRecord(
  val id: String,
  val triggerAtMillis: Double,
  val recurrenceRule: RecurrenceRuleRecord?,
  val title: String,
  val payload: String?,
  val soundRef: String?,
  val snoozeConfig: SnoozeConfigRecord?,
  val state: String,
  val createdAtMillis: Double,
  val updatedAtMillis: Double,
  val lastFiredAtMillis: Double?,
  val snoozeCount: Int,
)

data class RecurrenceRuleRecord(
  val frequency: String,
  val daysOfWeek: List<Int>?,
  val intervalDays: Int?,
)

data class SnoozeConfigRecord(
  val defaultMinutes: Int,
  val maxSnoozeCount: Int,
)

object AlarmMapper {
  fun toWritableMap(record: AlarmRecord): WritableMap {
    val map = Arguments.createMap()
    map.putString("id", record.id)
    map.putDouble("triggerAtMillis", record.triggerAtMillis)
    map.putString("title", record.title)
    map.putString("payload", record.payload)
    map.putString("soundRef", record.soundRef)
    map.putString("state", record.state)
    map.putDouble("createdAtMillis", record.createdAtMillis)
    map.putDouble("updatedAtMillis", record.updatedAtMillis)

    if (record.recurrenceRule != null) {
      val rule = Arguments.createMap()
      rule.putString("frequency", record.recurrenceRule.frequency)
      record.recurrenceRule.daysOfWeek?.let { days ->
        val array = Arguments.createArray()
        days.forEach { array.pushInt(it) }
        rule.putArray("daysOfWeek", array)
      } ?: rule.putNull("daysOfWeek")
      record.recurrenceRule.intervalDays?.let { rule.putInt("intervalDays", it) }
        ?: rule.putNull("intervalDays")
      map.putMap("recurrenceRule", rule)
    } else {
      map.putNull("recurrenceRule")
    }

    if (record.snoozeConfig != null) {
      val snooze = Arguments.createMap()
      snooze.putInt("defaultMinutes", record.snoozeConfig.defaultMinutes)
      snooze.putInt("maxSnoozeCount", record.snoozeConfig.maxSnoozeCount)
      map.putMap("snoozeConfig", snooze)
    } else {
      map.putNull("snoozeConfig")
    }

    return map
  }

  fun entityToRecord(entity: AlarmEntity): AlarmRecord {
    val recurrence = entity.recurrenceFrequency?.let { frequency ->
      RecurrenceRuleRecord(
        frequency = frequency,
        daysOfWeek = entity.recurrenceDaysOfWeek?.split(",")?.mapNotNull { it.toIntOrNull() },
        intervalDays = entity.recurrenceIntervalDays,
      )
    }
    val snooze = if (entity.snoozeDefaultMinutes != null && entity.snoozeMaxCount != null) {
      SnoozeConfigRecord(entity.snoozeDefaultMinutes, entity.snoozeMaxCount)
    } else {
      null
    }
    return AlarmRecord(
      id = entity.id,
      triggerAtMillis = entity.triggerAtMillis,
      recurrenceRule = recurrence,
      title = entity.title,
      payload = entity.payload,
      soundRef = entity.soundRef,
      snoozeConfig = snooze,
      state = entity.state,
      createdAtMillis = entity.createdAtMillis,
      updatedAtMillis = entity.updatedAtMillis,
      lastFiredAtMillis = entity.lastFiredAtMillis,
      snoozeCount = entity.snoozeCount,
    )
  }

  fun recordToEntity(record: AlarmRecord): AlarmEntity {
    return AlarmEntity(
      id = record.id,
      triggerAtMillis = record.triggerAtMillis,
      recurrenceFrequency = record.recurrenceRule?.frequency,
      recurrenceDaysOfWeek = record.recurrenceRule?.daysOfWeek?.joinToString(","),
      recurrenceIntervalDays = record.recurrenceRule?.intervalDays,
      title = record.title,
      payload = record.payload,
      soundRef = record.soundRef,
      snoozeDefaultMinutes = record.snoozeConfig?.defaultMinutes,
      snoozeMaxCount = record.snoozeConfig?.maxSnoozeCount,
      state = record.state,
      createdAtMillis = record.createdAtMillis,
      updatedAtMillis = record.updatedAtMillis,
      lastFiredAtMillis = record.lastFiredAtMillis,
      snoozeCount = record.snoozeCount,
    )
  }

  fun parseRecurrenceRule(map: ReadableMap?): RecurrenceRuleRecord? {
    if (map == null || !map.hasKey("frequency")) return null
    val frequency = map.getString("frequency") ?: throw AlarmKitException.InvalidConfig("Missing recurrence frequency")
    val daysOfWeek = if (map.hasKey("daysOfWeek") && !map.isNull("daysOfWeek")) {
      val array = map.getArray("daysOfWeek") ?: throw AlarmKitException.InvalidConfig("Invalid daysOfWeek")
      (0 until array.size()).map { array.getInt(it) }
    } else {
      null
    }
    val intervalDays = if (map.hasKey("intervalDays") && !map.isNull("intervalDays")) {
      map.getInt("intervalDays")
    } else {
      null
    }
    validateRecurrenceRule(frequency, daysOfWeek, intervalDays)
    return RecurrenceRuleRecord(frequency, daysOfWeek, intervalDays)
  }

  fun parseSnoozeConfig(map: ReadableMap?): SnoozeConfigRecord? {
    if (map == null) return null
    return SnoozeConfigRecord(
      defaultMinutes = map.getInt("defaultMinutes"),
      maxSnoozeCount = map.getInt("maxSnoozeCount"),
    )
  }

  fun validateRecurrenceRule(frequency: String, daysOfWeek: List<Int>?, intervalDays: Int?) {
    when (frequency) {
      "none", "daily" -> Unit
      "weekly" -> {
        if (daysOfWeek.isNullOrEmpty()) {
          throw AlarmKitException.InvalidConfig("Weekly recurrence requires daysOfWeek")
        }
      }
      "custom" -> {
        if (intervalDays == null || intervalDays <= 0) {
          throw AlarmKitException.InvalidConfig("Custom recurrence requires positive intervalDays")
        }
      }
      else -> throw AlarmKitException.InvalidConfig("Unknown recurrence frequency: $frequency")
    }
  }

  fun newId(): String = UUID.randomUUID().toString()
}
