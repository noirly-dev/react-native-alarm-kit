package com.noirly.alarmkit.events

import com.facebook.react.bridge.ReactApplicationContext
import com.noirly.alarmkit.NativeAlarmKitModule
import com.noirly.alarmkit.repository.AlarmRecord

class AlarmEventEmitter(
  private var module: NativeAlarmKitModule?,
) {
  fun attach(module: NativeAlarmKitModule) {
    this.module = module
  }

  fun detach() {
    module = null
  }

  fun emitAlarmFired(record: AlarmRecord) {
    module?.publishAlarmFired(record)
  }

  fun emitAlarmMissedThenFired(record: AlarmRecord) {
    module?.publishAlarmMissedThenFired(record)
  }

  fun emitAlarmsReconciled(records: List<AlarmRecord>) {
    module?.publishAlarmsReconciled(records)
  }

  fun emitSnoozed(record: AlarmRecord) {
    module?.publishSnoozed(record)
  }

  fun emitDismissed(record: AlarmRecord) {
    module?.publishDismissed(record)
  }

  fun emitPermissionsChanged(context: ReactApplicationContext) {
    module?.publishPermissionsChanged(context)
  }
}
