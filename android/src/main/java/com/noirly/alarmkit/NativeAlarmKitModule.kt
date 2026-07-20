package com.noirly.alarmkit

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.module.annotations.ReactModule
import com.noirly.alarmkit.capabilities.CapabilityProvider
import com.noirly.alarmkit.permissions.PermissionManager
import com.noirly.alarmkit.repository.AlarmMapper
import com.noirly.alarmkit.repository.AlarmRecord
import kotlinx.coroutines.launch

@ReactModule(name = NativeAlarmKitModule.NAME)
class NativeAlarmKitModule(
  reactContext: ReactApplicationContext,
) : NativeAlarmKitSpec(reactContext) {

  private val engine = AlarmEngine(reactContext)
  private val permissionManager = PermissionManager(reactContext)
  private val capabilityProvider = CapabilityProvider(reactContext)

  init {
    AlarmKitFacade.initialize(reactContext)
    AlarmKitFacade.eventEmitter(reactContext).attach(this)
  }

  override fun invalidate() {
    AlarmKitFacade.eventEmitter(reactApplicationContext).detach()
    super.invalidate()
  }

  override fun scheduleAlarm(config: ReadableMap, promise: Promise) {
    AlarmKitScope.scope.launch {
      try {
        if (!permissionManager.hasRequiredPermissions()) {
          rejectPromise(promise, AlarmKitException.PermissionDenied("Required permissions not granted"))
          return@launch
        }
        val record = engine.scheduleFromConfig(
          triggerAtMillis = config.getDouble("triggerAtMillis"),
          recurrenceRule = AlarmMapper.parseRecurrenceRule(
            if (config.hasKey("recurrenceRule") && !config.isNull("recurrenceRule")) {
              config.getMap("recurrenceRule")
            } else {
              null
            },
          ),
          title = config.getString("title") ?: throw AlarmKitException.InvalidConfig("title is required"),
          payload = if (config.hasKey("payload") && !config.isNull("payload")) config.getString("payload") else null,
          soundRef = if (config.hasKey("soundRef") && !config.isNull("soundRef")) config.getString("soundRef") else null,
          snoozeConfig = AlarmMapper.parseSnoozeConfig(
            if (config.hasKey("snoozeConfig") && !config.isNull("snoozeConfig")) {
              config.getMap("snoozeConfig")
            } else {
              null
            },
          ),
        )
        promise.resolve(AlarmMapper.toWritableMap(record))
      } catch (error: AlarmKitException) {
        rejectPromise(promise, error)
      } catch (error: Throwable) {
        rejectPromise(promise, AlarmKitException.Internal(error.message ?: "Unknown error", error))
      }
    }
  }

  override fun updateAlarm(id: String, partialConfig: ReadableMap, promise: Promise) {
    AlarmKitScope.scope.launch {
      try {
        val record = engine.updateAlarm(
          id = id,
          triggerAtMillis = if (partialConfig.hasKey("triggerAtMillis")) partialConfig.getDouble("triggerAtMillis") else null,
          recurrenceRule = AlarmMapper.parseRecurrenceRule(
            if (partialConfig.hasKey("recurrenceRule") && !partialConfig.isNull("recurrenceRule")) {
              partialConfig.getMap("recurrenceRule")
            } else {
              null
            },
          ),
          title = if (partialConfig.hasKey("title")) partialConfig.getString("title") else null,
          payload = if (partialConfig.hasKey("payload") && !partialConfig.isNull("payload")) {
            partialConfig.getString("payload")
          } else {
            null
          },
          soundRef = if (partialConfig.hasKey("soundRef") && !partialConfig.isNull("soundRef")) {
            partialConfig.getString("soundRef")
          } else {
            null
          },
          snoozeConfig = AlarmMapper.parseSnoozeConfig(
            if (partialConfig.hasKey("snoozeConfig") && !partialConfig.isNull("snoozeConfig")) {
              partialConfig.getMap("snoozeConfig")
            } else {
              null
            },
          ),
        )
        promise.resolve(AlarmMapper.toWritableMap(record))
      } catch (error: AlarmKitException) {
        rejectPromise(promise, error)
      } catch (error: Throwable) {
        rejectPromise(promise, AlarmKitException.Internal(error.message ?: "Unknown error", error))
      }
    }
  }

  override fun cancelAlarm(id: String, promise: Promise) {
    AlarmKitScope.scope.launch {
      try {
        engine.cancelAlarm(id)
        promise.resolve(null)
      } catch (error: AlarmKitException) {
        rejectPromise(promise, error)
      } catch (error: Throwable) {
        rejectPromise(promise, AlarmKitException.Internal(error.message ?: "Unknown error", error))
      }
    }
  }

  override fun cancelAllAlarms(promise: Promise) {
    AlarmKitScope.scope.launch {
      try {
        engine.cancelAllAlarms()
        promise.resolve(null)
      } catch (error: AlarmKitException) {
        rejectPromise(promise, error)
      } catch (error: Throwable) {
        rejectPromise(promise, AlarmKitException.Internal(error.message ?: "Unknown error", error))
      }
    }
  }

  override fun getAlarm(id: String, promise: Promise) {
    AlarmKitScope.scope.launch {
      try {
        val record = engine.getAlarm(id)
        promise.resolve(record?.let { AlarmMapper.toWritableMap(it) })
      } catch (error: AlarmKitException) {
        rejectPromise(promise, error)
      } catch (error: Throwable) {
        rejectPromise(promise, AlarmKitException.Internal(error.message ?: "Unknown error", error))
      }
    }
  }

  override fun getAllAlarms(promise: Promise) {
    AlarmKitScope.scope.launch {
      try {
        val array = Arguments.createArray()
        engine.getAllAlarms().forEach { array.pushMap(AlarmMapper.toWritableMap(it)) }
        promise.resolve(array)
      } catch (error: AlarmKitException) {
        rejectPromise(promise, error)
      } catch (error: Throwable) {
        rejectPromise(promise, AlarmKitException.Internal(error.message ?: "Unknown error", error))
      }
    }
  }

  override fun snoozeAlarm(id: String, overrideMinutes: Double?, promise: Promise) {
    AlarmKitScope.scope.launch {
      try {
        engine.snoozeAlarm(id, overrideMinutes?.toInt())
        promise.resolve(null)
      } catch (error: AlarmKitException) {
        rejectPromise(promise, error)
      } catch (error: Throwable) {
        rejectPromise(promise, AlarmKitException.Internal(error.message ?: "Unknown error", error))
      }
    }
  }

  override fun dismissAlarm(id: String, promise: Promise) {
    AlarmKitScope.scope.launch {
      try {
        engine.dismissAlarm(id)
        promise.resolve(null)
      } catch (error: AlarmKitException) {
        rejectPromise(promise, error)
      } catch (error: Throwable) {
        rejectPromise(promise, AlarmKitException.Internal(error.message ?: "Unknown error", error))
      }
    }
  }

  override fun checkPermissions(promise: Promise) {
    AlarmKitScope.scope.launch {
      try {
        promise.resolve(permissionManager.checkPermissionsMap())
      } catch (error: AlarmKitException) {
        rejectPromise(promise, error)
      } catch (error: Throwable) {
        rejectPromise(promise, AlarmKitException.Internal(error.message ?: "Unknown error", error))
      }
    }
  }

  override fun requestPermissions(promise: Promise) {
    AlarmKitScope.scope.launch {
      try {
        promise.resolve(permissionManager.requestPermissions())
      } catch (error: AlarmKitException) {
        rejectPromise(promise, error)
      } catch (error: Throwable) {
        rejectPromise(promise, AlarmKitException.Internal(error.message ?: "Unknown error", error))
      }
    }
  }

  override fun getCapabilities(promise: Promise) {
    AlarmKitScope.scope.launch {
      try {
        promise.resolve(capabilityProvider.getCapabilitiesMap())
      } catch (error: AlarmKitException) {
        rejectPromise(promise, error)
      } catch (error: Throwable) {
        rejectPromise(promise, AlarmKitException.Internal(error.message ?: "Unknown error", error))
      }
    }
  }

  private fun rejectPromise(promise: Promise, error: AlarmKitException) {
    val userInfo = Arguments.createMap().apply {
      putString("code", error.code)
      error.alarmId?.let { putString("alarmId", it) } ?: putNull("alarmId")
      error.platformErrorCode?.let { putString("platformErrorCode", it) } ?: putNull("platformErrorCode")
    }
    promise.reject(error.code, error.message, userInfo)
  }

  companion object {
    const val NAME = "NativeAlarmKit"
  }
}
