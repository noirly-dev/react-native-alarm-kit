package com.noirly.alarmkit

import com.noirly.alarmkit.repository.AlarmMapper
import com.noirly.alarmkit.repository.RecurrenceRuleRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorMappingTest {
  @Test
  fun alarmNotFound_mapsToExpectedCode() {
    val error = AlarmKitException.AlarmNotFound("abc")
    assertEquals("E_ALARMKIT_ALARM_NOT_FOUND", error.code)
    assertEquals("abc", error.alarmId)
  }

  @Test
  fun invalidWeeklyRecurrence_throwsInvalidConfig() {
    try {
      AlarmMapper.validateRecurrenceRule("weekly", emptyList(), null)
      error("Expected exception")
    } catch (error: AlarmKitException.InvalidConfig) {
      assertEquals("E_ALARMKIT_INVALID_CONFIG", error.code)
    }
  }

  @Test
  fun customRecurrence_requiresInterval() {
    try {
      AlarmMapper.validateRecurrenceRule("custom", null, null)
      error("Expected exception")
    } catch (error: AlarmKitException.InvalidConfig) {
      assertEquals("E_ALARMKIT_INVALID_CONFIG", error.code)
    }
  }
}
