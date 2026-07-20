import Foundation
import UserNotifications

final class CapabilityProvider {
  func getCapabilities(completion: @escaping ([String: Any]) -> Void) {
    UNUserNotificationCenter.current().getNotificationSettings { settings in
      let supportsCritical = settings.criticalAlertSetting == .enabled
      completion([
        "supportsExactAlarms": true,
        "supportsCriticalAlerts": supportsCritical,
        "supportsFullScreenRinging": true,
        "maxPendingAlarms": 64,
      ])
    }
  }
}
