package com.noirly.alarmkit.events

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableArray
import com.noirly.alarmkit.NativeAlarmKitSpec
import com.noirly.alarmkit.permissions.PermissionManager
import com.noirly.alarmkit.repository.AlarmMapper
import com.noirly.alarmkit.repository.AlarmRecord

class AlarmEventEmitter(
  private var module: NativeAlarmKitSpec?,
) {
  fun attach(module: NativeAlarmKitSpec) {
    this.module = module
  }

  fun detach() {
    module = null
  }

  fun emitAlarmFired(record: AlarmRecord) {
    module?.emitOnAlarmFired(AlarmMapper.toWritableMap(record))
  }

  fun emitAlarmMissedThenFired(record: AlarmRecord) {
    module?.emitOnAlarmMissedThenFired(AlarmMapper.toWritableMap(record))
  }

  fun emitAlarmsReconciled(records: List<AlarmRecord>) {
    val array: WritableArray = Arguments.createArray()
    records.forEach { array.pushMap(AlarmMapper.toWritableMap(it)) }
    module?.emitOnAlarmsReconciled(array)
  }

  fun emitSnoozed(record: AlarmRecord) {
    module?.emitOnSnoozed(AlarmMapper.toWritableMap(record))
  }

  fun emitDismissed(record: AlarmRecord) {
    module?.emitOnDismissed(AlarmMapper.toWritableMap(record))
  }

  fun emitPermissionsChanged(context: ReactApplicationContext) {
    val moduleRef = module ?: return
    val status = PermissionManager(context).checkPermissionsMap()
    moduleRef.emitOnPermissionsChanged(status)
  }
}
