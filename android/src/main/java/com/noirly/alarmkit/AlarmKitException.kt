package com.noirly.alarmkit

sealed class AlarmKitException(
  val code: String,
  message: String,
  val alarmId: String? = null,
  val platformErrorCode: String? = null,
) : Exception(message) {
  class InvalidConfig(message: String) :
    AlarmKitException("E_ALARMKIT_INVALID_CONFIG", message)

  class AlarmNotFound(alarmId: String) :
    AlarmKitException("E_ALARMKIT_ALARM_NOT_FOUND", "Alarm not found: $alarmId", alarmId)

  class PermissionDenied(message: String) :
    AlarmKitException("E_ALARMKIT_PERMISSION_DENIED", message)

  class PlatformLimitExceeded(message: String) :
    AlarmKitException("E_ALARMKIT_PLATFORM_LIMIT_EXCEEDED", message)

  class SchedulingFailed(message: String, platformErrorCode: String? = null) :
    AlarmKitException("E_ALARMKIT_SCHEDULING_FAILED", message, platformErrorCode = platformErrorCode)

  class RingingStateConflict(alarmId: String, message: String) :
    AlarmKitException("E_ALARMKIT_RINGING_STATE_CONFLICT", message, alarmId)

  class UnsupportedOperation(message: String) :
    AlarmKitException("E_ALARMKIT_UNSUPPORTED_OPERATION", message)

  class Internal(message: String, cause: Throwable? = null) :
    AlarmKitException("E_ALARMKIT_INTERNAL", message) {
    init {
      cause?.let { initCause(it) }
    }
  }
}
