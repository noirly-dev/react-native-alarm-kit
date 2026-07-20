import Foundation

final class AlarmRingingCoordinator {
  static let shared = AlarmRingingCoordinator()

  private var activeAlarmId: String?

  func beginRinging(alarmId: String) {
    activeAlarmId = alarmId
  }

  func endRinging() {
    activeAlarmId = nil
  }

  func isRinging(_ alarmId: String) -> Bool {
    activeAlarmId == alarmId
  }
}
