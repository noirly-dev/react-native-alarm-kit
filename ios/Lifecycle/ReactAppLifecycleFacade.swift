import Foundation
import UIKit

final class ReactAppLifecycleFacade {
  static let shared = ReactAppLifecycleFacade()

  var onDidBecomeActive: (() -> Void)?

  private init() {
    NotificationCenter.default.addObserver(
      self,
      selector: #selector(handleDidBecomeActive),
      name: UIApplication.didBecomeActiveNotification,
      object: nil
    )
  }

  @objc private func handleDidBecomeActive() {
    onDidBecomeActive?()
  }
}
