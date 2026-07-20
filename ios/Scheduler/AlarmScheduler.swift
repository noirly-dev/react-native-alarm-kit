import Foundation
import UserNotifications

protocol NotificationCenterFacade {
  func add(_ request: UNNotificationRequest, completion: @escaping (Error?) -> Void)
  func removePendingNotificationRequests(withIdentifiers identifiers: [String])
  func getPendingNotificationRequests(completion: @escaping ([UNNotificationRequest]) -> Void)
}

final class SystemNotificationCenterFacade: NotificationCenterFacade {
  private let center = UNUserNotificationCenter.current()

  func add(_ request: UNNotificationRequest, completion: @escaping (Error?) -> Void) {
    center.add(request, withCompletionHandler: completion)
  }

  func removePendingNotificationRequests(withIdentifiers identifiers: [String]) {
    center.removePendingNotificationRequests(withIdentifiers: identifiers)
  }

  func getPendingNotificationRequests(completion: @escaping ([UNNotificationRequest]) -> Void) {
    center.getPendingNotificationRequests(completionHandler: completion)
  }
}

final class AlarmScheduler {
  static let shared = AlarmScheduler()

  private let center: NotificationCenterFacade
  private let maxPending = 64

  init(center: NotificationCenterFacade = SystemNotificationCenterFacade()) {
    self.center = center
  }

  func schedule(_ record: AlarmRecord, completion: @escaping (Result<Void, AlarmKitError>) -> Void) {
    let triggerDate = Date(timeIntervalSince1970: record.triggerAtMillis / 1000.0)
    if triggerDate.timeIntervalSinceNow < -60 {
      completion(.failure(.init(.invalidConfig, "triggerAtMillis must be in the future or recent")))
      return
    }

    let content = UNMutableNotificationContent()
    content.title = record.title
    content.body = "Alarm"
    content.sound = .default
    content.categoryIdentifier = "NOIRLY_ALARM"
    content.userInfo = ["alarmId": record.id]

    let components = Calendar.current.dateComponents(
      [.year, .month, .day, .hour, .minute, .second],
      from: triggerDate
    )
    let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
    let request = UNNotificationRequest(identifier: record.id, content: content, trigger: trigger)

    center.add(request) { error in
      if let error = error {
        completion(.failure(.init(.schedulingFailed, error.localizedDescription, platformErrorCode: "\(error)")))
      } else {
        completion(.success(()))
      }
    }
  }

  func cancel(_ alarmId: String) {
    center.removePendingNotificationRequests(withIdentifiers: [alarmId])
  }

  func computeNextTrigger(for record: AlarmRecord, fromMillis: Double = Date().timeIntervalSince1970 * 1000) -> Double? {
    guard let rule = record.recurrenceRule else { return nil }
    let dayMillis = 86_400_000.0
    switch rule.frequency {
    case "none":
      return nil
    case "daily":
      return fromMillis + dayMillis
    case "weekly":
      return fromMillis + 7 * dayMillis
    case "custom":
      return fromMillis + Double(rule.intervalDays ?? 1) * dayMillis
    default:
      return nil
    }
  }

  func isMissedFire(_ triggerAtMillis: Double, nowMillis: Double = Date().timeIntervalSince1970 * 1000) -> Bool {
    triggerAtMillis < nowMillis - 60_000
  }

  var pendingLimit: Int { maxPending }

  func getPendingNotificationRequests(completion: @escaping ([UNNotificationRequest]) -> Void) {
    center.getPendingNotificationRequests(completion: completion)
  }
}
