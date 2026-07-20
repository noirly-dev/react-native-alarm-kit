package com.noirly.alarmkit.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.noirly.alarmkit.AlarmEngine
import com.noirly.alarmkit.AlarmKitFacade
import com.noirly.alarmkit.AlarmKitScope
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (Intent.ACTION_BOOT_COMPLETED != intent.action &&
      "android.intent.action.QUICKBOOT_POWERON" != intent.action
    ) {
      return
    }
    val pendingResult = goAsync()
    AlarmKitFacade.initialize(context)
    AlarmKitScope.scope.launch {
      try {
        AlarmEngine(context).reconcileAfterBoot()
      } finally {
        pendingResult.finish()
      }
    }
  }
}
