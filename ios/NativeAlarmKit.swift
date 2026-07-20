import Foundation

@objc(NativeAlarmKit)
class NativeAlarmKit: NativeAlarmKitSpec {
  private let permissionManager = PermissionManager()
  private let capabilityProvider = CapabilityProvider()

  override init() {
    super.init()
    AlarmEventEmitter.shared.attach(self)
    ReactAppLifecycleFacade.shared.onDidBecomeActive = { [weak self] in
      self?.reconcile()
    }
    reconcile()
  }

  deinit {
    AlarmEventEmitter.shared.detach()
  }

  @objc override func scheduleAlarm(
    _ config: [String: Any],
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    permissionManager.hasRequiredPermissions { granted in
      guard granted else {
        self.reject(reject, .init(.permissionDenied, "Required permissions not granted"))
        return
      }

      AlarmKitQueue.shared.async {
        do {
          let recurrence = try AlarmMapper.parseRecurrenceRule(config["recurrenceRule"])
          let title = config["title"] as? String ?? ""
          if title.isEmpty {
            self.reject(reject, .init(.invalidConfig, "title is required"))
            return
          }

          AlarmEngine.shared.scheduleFromConfig(
            triggerAtMillis: config["triggerAtMillis"] as? Double ?? 0,
            recurrenceRule: recurrence,
            title: title,
            payload: config["payload"] as? String,
            soundRef: config["soundRef"] as? String,
            snoozeConfig: AlarmMapper.parseSnoozeConfig(config["snoozeConfig"])
          ) { result in
            switch result {
            case .success(let record):
              resolve(AlarmMapper.toDictionary(record))
            case .failure(let error):
              self.reject(reject, error)
            }
          }
        } catch let error as AlarmKitError {
          self.reject(reject, error)
        } catch {
          self.reject(reject, .init(.internal, error.localizedDescription))
        }
      }
    }
  }

  @objc override func updateAlarm(
    _ id: String,
    partialConfig: [String: Any],
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    AlarmKitQueue.shared.async {
      do {
        let recurrence = try AlarmMapper.parseRecurrenceRule(partialConfig["recurrenceRule"])
        AlarmEngine.shared.updateAlarm(
          id: id,
          triggerAtMillis: partialConfig["triggerAtMillis"] as? Double,
          recurrenceRule: recurrence,
          title: partialConfig["title"] as? String,
          payload: partialConfig["payload"] as? String,
          soundRef: partialConfig["soundRef"] as? String,
          snoozeConfig: AlarmMapper.parseSnoozeConfig(partialConfig["snoozeConfig"])
        ) { result in
          switch result {
          case .success(let record):
            resolve(AlarmMapper.toDictionary(record))
          case .failure(let error):
            self.reject(reject, error)
          }
        }
      } catch let error as AlarmKitError {
        self.reject(reject, error)
      } catch {
        self.reject(reject, .init(.internal, error.localizedDescription))
      }
    }
  }

  @objc override func cancelAlarm(
    _ id: String,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    AlarmKitQueue.shared.async {
      AlarmEngine.shared.cancelAlarm(id: id) { result in
        switch result {
        case .success:
          resolve(nil)
        case .failure(let error):
          self.reject(reject, error)
        }
      }
    }
  }

  @objc override func cancelAllAlarms(
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    AlarmKitQueue.shared.async {
      AlarmEngine.shared.cancelAllAlarms { result in
        switch result {
        case .success:
          resolve(nil)
        case .failure(let error):
          self.reject(reject, error)
        }
      }
    }
  }

  @objc override func getAlarm(
    _ id: String,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    AlarmKitQueue.shared.async {
      if let record = AlarmEngine.shared.getAlarm(id: id) {
        resolve(AlarmMapper.toDictionary(record))
      } else {
        resolve(nil)
      }
    }
  }

  @objc override func getAllAlarms(
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    AlarmKitQueue.shared.async {
      resolve(AlarmEngine.shared.getAllAlarms().map { AlarmMapper.toDictionary($0) })
    }
  }

  @objc override func snoozeAlarm(
    _ id: String,
    overrideMinutes: NSNumber?,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    AlarmKitQueue.shared.async {
      AlarmEngine.shared.snoozeAlarm(id: id, overrideMinutes: overrideMinutes?.intValue) { result in
        switch result {
        case .success:
          resolve(nil)
        case .failure(let error):
          self.reject(reject, error)
        }
      }
    }
  }

  @objc override func dismissAlarm(
    _ id: String,
    resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    AlarmKitQueue.shared.async {
      AlarmEngine.shared.dismissAlarm(id: id) { result in
        switch result {
        case .success:
          resolve(nil)
        case .failure(let error):
          self.reject(reject, error)
        }
      }
    }
  }

  @objc override func checkPermissions(
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    permissionManager.checkPermissions { status in
      resolve(status)
    }
  }

  @objc override func requestPermissions(
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    permissionManager.requestPermissions { result in
      switch result {
      case .success(let status):
        resolve(status)
      case .failure(let error):
        self.reject(reject, error)
      }
    }
  }

  @objc override func getCapabilities(
    _ resolve: @escaping RCTPromiseResolveBlock,
    reject: @escaping RCTPromiseRejectBlock
  ) {
    capabilityProvider.getCapabilities { capabilities in
      resolve(capabilities)
    }
  }

  private func reconcile() {
    AlarmKitQueue.shared.async {
      AlarmEngine.shared.reconcilePendingNotifications { _ in }
    }
  }

  private func reject(_ reject: @escaping RCTPromiseRejectBlock, _ error: AlarmKitError) {
    var userInfo: [String: Any] = ["code": error.code.rawValue]
    userInfo["alarmId"] = error.alarmId ?? NSNull()
    userInfo["platformErrorCode"] = error.platformErrorCode ?? NSNull()
    reject(error.code.rawValue, error.message, NSError(domain: "AlarmKit", code: 0, userInfo: userInfo))
  }
}
