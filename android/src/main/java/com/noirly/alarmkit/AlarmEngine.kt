package com.noirly.alarmkit

import android.content.Context
import com.noirly.alarmkit.repository.AlarmMapper
import com.noirly.alarmkit.repository.AlarmRecord
import com.noirly.alarmkit.repository.RecurrenceRuleRecord
import com.noirly.alarmkit.repository.SnoozeConfigRecord
import com.noirly.alarmkit.scheduler.AlarmScheduler
import com.noirly.alarmkit.service.AlarmRingingService

class AlarmEngine(private val context: Context) {
  private val repository = AlarmKitFacade.repository(context)
  private val scheduler = AlarmKitFacade.scheduler(context)
  private val eventEmitter = AlarmKitFacade.eventEmitter(context)

  suspend fun scheduleFromConfig(
    triggerAtMillis: Double,
    recurrenceRule: RecurrenceRuleRecord?,
    title: String,
    payload: String?,
    soundRef: String?,
    snoozeConfig: SnoozeConfigRecord?,
  ): AlarmRecord {
    if (triggerAtMillis <= 0) {
      throw AlarmKitException.InvalidConfig("triggerAtMillis must be positive")
    }
    recurrenceRule?.let {
      AlarmMapper.validateRecurrenceRule(it.frequency, it.daysOfWeek, it.intervalDays)
    }
    val now = System.currentTimeMillis().toDouble()
    val record = AlarmRecord(
      id = AlarmMapper.newId(),
      triggerAtMillis = triggerAtMillis,
      recurrenceRule = recurrenceRule,
      title = title,
      payload = payload,
      soundRef = soundRef,
      snoozeConfig = snoozeConfig,
      state = "scheduled",
      createdAtMillis = now,
      updatedAtMillis = now,
      lastFiredAtMillis = null,
      snoozeCount = 0,
    )
    repository.insert(record)
    scheduler.schedule(record)
    return record
  }

  suspend fun updateAlarm(
    id: String,
    triggerAtMillis: Double?,
    recurrenceRule: RecurrenceRuleRecord?,
    title: String?,
    payload: String?,
    soundRef: String?,
    snoozeConfig: SnoozeConfigRecord?,
  ): AlarmRecord {
    val existing = repository.requireById(id)
    recurrenceRule?.let {
      AlarmMapper.validateRecurrenceRule(it.frequency, it.daysOfWeek, it.intervalDays)
    }
    val updated = existing.copy(
      triggerAtMillis = triggerAtMillis ?: existing.triggerAtMillis,
      recurrenceRule = recurrenceRule ?: existing.recurrenceRule,
      title = title ?: existing.title,
      payload = payload ?: existing.payload,
      soundRef = soundRef ?: existing.soundRef,
      snoozeConfig = snoozeConfig ?: existing.snoozeConfig,
      state = "scheduled",
      updatedAtMillis = System.currentTimeMillis().toDouble(),
    )
    repository.update(updated)
    scheduler.cancel(id)
    scheduler.schedule(updated)
    return updated
  }

  suspend fun cancelAlarm(id: String) {
    repository.requireById(id)
    scheduler.cancel(id)
    repository.deleteById(id)
  }

  suspend fun cancelAllAlarms() {
    repository.getAll().forEach { scheduler.cancel(it.id) }
    repository.deleteAll()
  }

  suspend fun getAlarm(id: String): AlarmRecord? = repository.getById(id)

  suspend fun getAllAlarms(): List<AlarmRecord> = repository.getAll()

  suspend fun snoozeAlarm(id: String, overrideMinutes: Int?) {
    val existing = repository.requireById(id)
    if (existing.state != "ringing" && existing.state != "snoozed") {
      throw AlarmKitException.RingingStateConflict(
        id,
        "Alarm must be ringing to snooze (current: ${existing.state})",
      )
    }
    val snoozeMinutes = overrideMinutes ?: existing.snoozeConfig?.defaultMinutes ?: 5
    val maxCount = existing.snoozeConfig?.maxSnoozeCount ?: Int.MAX_VALUE
    if (existing.snoozeCount >= maxCount) {
      throw AlarmKitException.RingingStateConflict(id, "Maximum snooze count reached")
    }
    val nextTrigger = System.currentTimeMillis() + snoozeMinutes * 60_000L
    val updated = existing.copy(
      triggerAtMillis = nextTrigger.toDouble(),
      state = "snoozed",
      snoozeCount = existing.snoozeCount + 1,
      updatedAtMillis = System.currentTimeMillis().toDouble(),
    )
    repository.update(updated)
    scheduler.cancel(id)
    scheduler.schedule(updated)
    AlarmRingingService.stop(context)
    eventEmitter.emitSnoozed(updated)
  }

  suspend fun dismissAlarm(id: String) {
    val existing = repository.requireById(id)
    if (existing.state != "ringing" && existing.state != "snoozed") {
      throw AlarmKitException.RingingStateConflict(
        id,
        "Alarm must be ringing to dismiss (current: ${existing.state})",
      )
    }
    val updated = existing.copy(
      state = "dismissed",
      updatedAtMillis = System.currentTimeMillis().toDouble(),
    )
    repository.update(updated)
    scheduler.cancel(id)
    AlarmRingingService.stop(context)
    eventEmitter.emitDismissed(updated)
  }

  suspend fun handleAlarmFire(alarmId: String, missedFire: Boolean) {
    val existing = repository.getById(alarmId) ?: return
    val now = System.currentTimeMillis().toDouble()

    val nextTrigger = scheduler.computeNextTrigger(existing, System.currentTimeMillis())
    if (nextTrigger != null) {
      val rearmed = existing.copy(
        triggerAtMillis = nextTrigger,
        state = "scheduled",
        lastFiredAtMillis = now,
        updatedAtMillis = now,
      )
      repository.update(rearmed)
      scheduler.schedule(rearmed)
    }

    val ringing = existing.copy(
      state = if (missedFire) "missed_fired" else "ringing",
      lastFiredAtMillis = now,
      updatedAtMillis = now,
    )
    repository.update(ringing)
    AlarmRingingService.start(context, alarmId)

    if (missedFire) {
      eventEmitter.emitAlarmMissedThenFired(ringing)
    } else {
      eventEmitter.emitAlarmFired(ringing)
    }
  }

  suspend fun reconcileAfterBoot(): List<AlarmRecord> {
    val all = repository.getAll()
    val reconciled = mutableListOf<AlarmRecord>()
    val now = System.currentTimeMillis()

    all.filter { it.state == "scheduled" || it.state == "snoozed" }.forEach { record ->
      val trigger = record.triggerAtMillis.toLong()
      if (trigger <= now) {
        handleAlarmFire(record.id, missedFire = true)
        reconciled.add(repository.requireById(record.id))
      } else {
        scheduler.schedule(record)
        reconciled.add(record)
      }
    }
    if (reconciled.isNotEmpty()) {
      eventEmitter.emitAlarmsReconciled(reconciled)
    }
    return reconciled
  }
}
