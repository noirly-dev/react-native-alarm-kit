package com.noirly.alarmkit.permissions

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.PermissionAwareActivity
import com.noirly.alarmkit.AlarmKitException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PermissionManager(private val context: ReactApplicationContext) {
  fun checkPermissionsMap(): WritableMap {
    val map = Arguments.createMap()
    map.putString("notificationStatus", notificationStatus())
    map.putString("exactAlarmStatus", exactAlarmStatus())
    map.putString("criticalAlertStatus", "notApplicable")
    return map
  }

  suspend fun checkPermissions(): WritableMap = checkPermissionsMap()

  suspend fun requestPermissions(): WritableMap {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) {
        val activity = context.currentActivity as? PermissionAwareActivity
          ?: throw AlarmKitException.PermissionDenied("No active activity for permission request")
        val result = suspendCancellableCoroutine<Boolean> { continuation ->
          activity.requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_NOTIFICATIONS,
          ) { requestCode, _, grantResults ->
            if (requestCode == REQUEST_NOTIFICATIONS) {
              val allowed = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
              continuation.resume(allowed) {}
              true
            } else {
              false
            }
          }
        }
        if (!result) {
          throw AlarmKitException.PermissionDenied("Notification permission denied")
        }
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
      if (!alarmManager.canScheduleExactAlarms()) {
        val activity = context.currentActivity
          ?: throw AlarmKitException.PermissionDenied("No active activity for exact alarm settings")
        val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
          data = android.net.Uri.parse("package:${context.packageName}")
        }
        activity.startActivity(intent)
      }
    }

    return checkPermissionsMap()
  }

  fun hasRequiredPermissions(): Boolean {
    return notificationStatus() == "granted" && exactAlarmStatus() != "denied"
  }

  private fun notificationStatus(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      when (
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
      ) {
        PackageManager.PERMISSION_GRANTED -> "granted"
        PackageManager.PERMISSION_DENIED -> "denied"
        else -> "notDetermined"
      }
    } else {
      "granted"
    }
  }

  private fun exactAlarmStatus(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
      if (alarmManager.canScheduleExactAlarms()) "granted" else "denied"
    } else {
      "notApplicable"
    }
  }

  companion object {
    private const val REQUEST_NOTIFICATIONS = 91001
  }
}
