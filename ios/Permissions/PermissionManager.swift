import Foundation
import UserNotifications

final class PermissionManager {
  func checkPermissions(completion: @escaping ([String: Any]) -> Void) {
    UNUserNotificationCenter.current().getNotificationSettings { settings in
      let notificationStatus: String
      switch settings.authorizationStatus {
      case .authorized, .ephemeral:
        notificationStatus = "granted"
      case .denied:
        notificationStatus = "denied"
      case .provisional:
        notificationStatus = "provisional"
      case .notDetermined:
        notificationStatus = "notDetermined"
      @unknown default:
        notificationStatus = "notDetermined"
      }

      let criticalStatus: String
      if settings.criticalAlertSetting == .enabled {
        criticalStatus = "granted"
      } else if settings.criticalAlertSetting == .disabled {
        criticalStatus = "denied"
      } else {
        criticalStatus = "notApplicable"
      }

      completion([
        "notificationStatus": notificationStatus,
        "exactAlarmStatus": "notApplicable",
        "criticalAlertStatus": criticalStatus,
      ])
    }
  }

  func requestPermissions(completion: @escaping (Result<[String: Any], AlarmKitError>) -> Void) {
    UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
      if let error = error {
        completion(.failure(.init(.permissionDenied, error.localizedDescription)))
        return
      }
      if !granted {
        completion(.failure(.init(.permissionDenied, "Notification permission denied")))
        return
      }
      self.checkPermissions { status in
        completion(.success(status))
      }
    }
  }

  func hasRequiredPermissions(completion: @escaping (Bool) -> Void) {
    checkPermissions { status in
      completion(status["notificationStatus"] as? String == "granted")
    }
  }
}
