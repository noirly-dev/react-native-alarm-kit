import Foundation

final class AlarmEventEmitter {
  static let shared = AlarmEventEmitter()

  private weak var module: NativeAlarmKit?

  func attach(_ module: NativeAlarmKit) {
    self.module = module
  }

  func detach() {
    module = nil
  }

  func emitAlarmFired(_ record: AlarmRecord) {
    module?.emitOnAlarmFired(AlarmMapper.toDictionary(record))
  }

  func emitAlarmMissedThenFired(_ record: AlarmRecord) {
    module?.emitOnAlarmMissedThenFired(AlarmMapper.toDictionary(record))
  }

  func emitAlarmsReconciled(_ records: [AlarmRecord]) {
    module?.emitOnAlarmsReconciled(records.map { AlarmMapper.toDictionary($0) })
  }

  func emitSnoozed(_ record: AlarmRecord) {
    module?.emitOnSnoozed(AlarmMapper.toDictionary(record))
  }

  func emitDismissed(_ record: AlarmRecord) {
    module?.emitOnDismissed(AlarmMapper.toDictionary(record))
  }

  func emitPermissionsChanged(_ status: [String: Any]) {
    module?.emitOnPermissionsChanged(status)
  }
}
