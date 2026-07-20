import Foundation
import UserNotifications

@objc(AlarmNotificationDelegate)
final class AlarmNotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
  @objc static let shared = AlarmNotificationDelegate()

  private override init() {
    super.init()
  }

  func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    willPresent notification: UNNotification,
    withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
  ) {
    handleNotification(notification)
    completionHandler([.banner, .sound, .badge])
  }

  func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    didReceive response: UNNotificationResponse,
    withCompletionHandler completionHandler: @escaping () -> Void
  ) {
    let alarmId = response.notification.request.content.userInfo["alarmId"] as? String
    guard let alarmId = alarmId else {
      completionHandler()
      return
    }

    AlarmKitQueue.shared.async {
      switch response.actionIdentifier {
      case "SNOOZE":
        AlarmEngine.shared.snoozeAlarm(id: alarmId, overrideMinutes: nil) { _ in }
      case "DISMISS", UNNotificationDismissActionIdentifier:
        AlarmEngine.shared.dismissAlarm(id: alarmId) { _ in }
      default:
        AlarmEngine.shared.handleAlarmFire(alarmId: alarmId, missedFire: false)
      }
      completionHandler()
    }
  }

  private func handleNotification(_ notification: UNNotification) {
    guard let alarmId = notification.request.content.userInfo["alarmId"] as? String else {
      return
    }
    AlarmKitQueue.shared.async {
      let record = AlarmRepository.shared.getById(alarmId)
      let missed = record.map { AlarmScheduler.shared.isMissedFire($0.triggerAtMillis) } ?? false
      AlarmEngine.shared.handleAlarmFire(alarmId: alarmId, missedFire: missed)
    }
  }
}
