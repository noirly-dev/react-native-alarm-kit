package com.noirly.alarmkit.scheduler

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSchedulerTest {
  private val scheduler = AlarmScheduler(
    context = androidx.test.core.app.ApplicationProvider.getApplicationContext(),
    alarmManager = object : AlarmManagerFacade {
      override fun setExact(triggerAtMillis: Long, pendingIntent: android.app.PendingIntent) {}
      override fun cancel(pendingIntent: android.app.PendingIntent) {}
      override fun canScheduleExactAlarms(): Boolean = true
    },
  )

  @Test
  fun isMissedFire_detectsPastTrigger() {
    val now = 1_000_000L
    assertTrue(scheduler.isMissedFire(now - 120_000.0, now))
  }

  @Test
  fun isMissedFire_ignoresRecentTrigger() {
    val now = 1_000_000L
    assertFalse(scheduler.isMissedFire(now - 10_000.0, now))
  }
}
