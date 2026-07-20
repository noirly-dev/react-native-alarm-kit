package com.noirly.alarmkit.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.noirly.alarmkit.AlarmKitException
import com.noirly.alarmkit.receivers.AlarmReceiver
import com.noirly.alarmkit.repository.AlarmRecord
import com.noirly.alarmkit.repository.RecurrenceRuleRecord
import java.util.Calendar
import java.util.TimeZone

interface AlarmManagerFacade {
  fun setExact(triggerAtMillis: Long, pendingIntent: PendingIntent)
  fun cancel(pendingIntent: PendingIntent)
  fun canScheduleExactAlarms(): Boolean
}

class SystemAlarmManagerFacade(private val alarmManager: AlarmManager) : AlarmManagerFacade {
  override fun setExact(triggerAtMillis: Long, pendingIntent: PendingIntent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerAtMillis,
        pendingIntent,
      )
    } else {
      alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
  }

  override fun cancel(pendingIntent: PendingIntent) {
    alarmManager.cancel(pendingIntent)
  }

  override fun canScheduleExactAlarms(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      alarmManager.canScheduleExactAlarms()
    } else {
      true
    }
  }
}

class AlarmScheduler(
  private val context: Context,
  private val alarmManager: AlarmManagerFacade,
) {
  fun schedule(record: AlarmRecord) {
    if (!alarmManager.canScheduleExactAlarms()) {
      throw AlarmKitException.PermissionDenied("Exact alarm permission not granted")
    }
    val triggerAt = record.triggerAtMillis.toLong()
    if (triggerAt <= 0) {
      throw AlarmKitException.InvalidConfig("triggerAtMillis must be positive")
    }
    try {
      alarmManager.setExact(triggerAt, createPendingIntent(record.id))
    } catch (error: SecurityException) {
      throw AlarmKitException.SchedulingFailed(
        error.message ?: "Failed to schedule alarm",
        error.javaClass.simpleName,
      )
    }
  }

  fun cancel(alarmId: String) {
    alarmManager.cancel(createPendingIntent(alarmId))
  }

  fun computeNextTrigger(record: AlarmRecord, fromMillis: Long = System.currentTimeMillis()): Double? {
    val rule = record.recurrenceRule ?: return null
    return when (rule.frequency) {
      "none" -> null
      "daily" -> fromMillis + DAY_MILLIS
      "weekly" -> computeNextWeekly(record.triggerAtMillis, rule, fromMillis)
      "custom" -> fromMillis + (rule.intervalDays ?: 1) * DAY_MILLIS
      else -> null
    }.let { it?.toDouble() }
  }

  fun isMissedFire(triggerAtMillis: Double, nowMillis: Long = System.currentTimeMillis()): Boolean {
    return triggerAtMillis < nowMillis - MISSED_THRESHOLD_MILLIS
  }

  private fun computeNextWeekly(
    originalTrigger: Double,
    rule: RecurrenceRuleRecord,
    fromMillis: Long,
  ): Long {
    val days = rule.daysOfWeek ?: return fromMillis + 7 * DAY_MILLIS
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.timeInMillis = fromMillis
    val original = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    original.timeInMillis = originalTrigger.toLong()

    for (offset in 0..13) {
      calendar.timeInMillis = fromMillis + offset * DAY_MILLIS
      val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
      if (days.contains(dayOfWeek)) {
        calendar.set(Calendar.HOUR_OF_DAY, original.get(Calendar.HOUR_OF_DAY))
        calendar.set(Calendar.MINUTE, original.get(Calendar.MINUTE))
        calendar.set(Calendar.SECOND, original.get(Calendar.SECOND))
        calendar.set(Calendar.MILLISECOND, original.get(Calendar.MILLISECOND))
        if (calendar.timeInMillis > fromMillis) {
          return calendar.timeInMillis
        }
      }
    }
    return fromMillis + 7 * DAY_MILLIS
  }

  private fun createPendingIntent(alarmId: String): PendingIntent {
    val intent = Intent(context, AlarmReceiver::class.java).apply {
      action = ACTION_ALARM_FIRE
      putExtra(EXTRA_ALARM_ID, alarmId)
    }
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    return PendingIntent.getBroadcast(context, alarmId.hashCode(), intent, flags)
  }

  companion object {
    const val ACTION_ALARM_FIRE = "com.noirly.alarmkit.ACTION_ALARM_FIRE"
    const val EXTRA_ALARM_ID = "alarmId"
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    private const val MISSED_THRESHOLD_MILLIS = 60_000L

    fun create(context: Context): AlarmScheduler {
      val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
      return AlarmScheduler(context, SystemAlarmManagerFacade(manager))
    }
  }
}
