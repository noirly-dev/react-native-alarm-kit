package com.noirly.alarmkit.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.noirly.alarmkit.AlarmEngine
import com.noirly.alarmkit.AlarmKitFacade
import com.noirly.alarmkit.AlarmKitScope
import com.noirly.alarmkit.scheduler.AlarmScheduler
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != AlarmScheduler.ACTION_ALARM_FIRE) return
    val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: return
    val pendingResult = goAsync()
    AlarmKitFacade.initialize(context)
    AlarmKitScope.scope.launch {
      try {
        val engine = AlarmEngine(context)
        val scheduler = AlarmKitFacade.scheduler(context)
        val record = AlarmKitFacade.repository(context).getById(alarmId)
        val missed = record?.let { scheduler.isMissedFire(it.triggerAtMillis) } ?: false
        engine.handleAlarmFire(alarmId, missedFire = missed)
      } finally {
        pendingResult.finish()
      }
    }
  }
}
