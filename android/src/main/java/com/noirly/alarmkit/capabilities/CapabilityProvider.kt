package com.noirly.alarmkit.capabilities

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

class CapabilityProvider(private val context: Context) {
  fun getCapabilitiesMap(): WritableMap {
    val map = Arguments.createMap()
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val supportsExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      alarmManager.canScheduleExactAlarms()
    } else {
      true
    }
    map.putBoolean("supportsExactAlarms", supportsExact)
    map.putBoolean("supportsCriticalAlerts", false)
    map.putBoolean("supportsFullScreenRinging", true)
    map.putNull("maxPendingAlarms")
    return map
  }
}
