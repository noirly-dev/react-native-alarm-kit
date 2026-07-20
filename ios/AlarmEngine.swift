import Foundation

final class AlarmEngine {
  static let shared = AlarmEngine()

  private let repository = AlarmRepository.shared
  private let scheduler = AlarmScheduler.shared
  private let eventEmitter = AlarmEventEmitter.shared
  private let ringingCoordinator = AlarmRingingCoordinator.shared

  func scheduleFromConfig(
    triggerAtMillis: Double,
    recurrenceRule: RecurrenceRuleRecord?,
    title: String,
    payload: String?,
    soundRef: String?,
    snoozeConfig: SnoozeConfigRecord?,
    completion: @escaping (Result<AlarmRecord, AlarmKitError>) -> Void
  ) {
    if triggerAtMillis <= 0 {
      completion(.failure(.init(.invalidConfig, "triggerAtMillis must be positive")))
      return
    }
    if let rule = recurrenceRule {
      do {
        try AlarmMapper.validateRecurrenceRule(rule)
      } catch let error as AlarmKitError {
        completion(.failure(error))
        return
      } catch {
        completion(.failure(.init(.internal, error.localizedDescription)))
        return
      }
    }

    if repository.count() >= scheduler.pendingLimit {
      completion(.failure(.init(.platformLimitExceeded, "iOS pending notification limit reached")))
      return
    }

    let now = Date().timeIntervalSince1970 * 1000
    var record = AlarmRecord(
      id: AlarmMapper.newId(),
      triggerAtMillis: triggerAtMillis,
      recurrenceRule: recurrenceRule,
      title: title,
      payload: payload,
      soundRef: soundRef,
      snoozeConfig: snoozeConfig,
      state: "scheduled",
      createdAtMillis: now,
      updatedAtMillis: now,
      lastFiredAtMillis: nil,
      snoozeCount: 0
    )

    scheduler.schedule(record) { result in
      switch result {
      case .success:
        self.repository.insert(record)
        completion(.success(record))
      case .failure(let error):
        completion(.failure(error))
      }
    }
  }

  func updateAlarm(
    id: String,
    triggerAtMillis: Double?,
    recurrenceRule: RecurrenceRuleRecord?,
    title: String?,
    payload: String?,
    soundRef: String?,
    snoozeConfig: SnoozeConfigRecord?,
    completion: @escaping (Result<AlarmRecord, AlarmKitError>) -> Void
  ) {
    do {
      var existing = try repository.requireById(id)
      if let rule = recurrenceRule {
        try AlarmMapper.validateRecurrenceRule(rule)
        existing.recurrenceRule = rule
      }
      if let triggerAtMillis = triggerAtMillis { existing.triggerAtMillis = triggerAtMillis }
      if let title = title { existing.title = title }
      if payload != nil { existing.payload = payload }
      if soundRef != nil { existing.soundRef = soundRef }
      if let snoozeConfig = snoozeConfig { existing.snoozeConfig = snoozeConfig }
      existing.state = "scheduled"
      existing.updatedAtMillis = Date().timeIntervalSince1970 * 1000

      scheduler.cancel(id)
      scheduler.schedule(existing) { result in
        switch result {
        case .success:
          self.repository.update(existing)
          completion(.success(existing))
        case .failure(let error):
          completion(.failure(error))
        }
      }
    } catch let error as AlarmKitError {
      completion(.failure(error))
    } catch {
      completion(.failure(.init(.internal, error.localizedDescription)))
    }
  }

  func cancelAlarm(id: String, completion: @escaping (Result<Void, AlarmKitError>) -> Void) {
    do {
      _ = try repository.requireById(id)
      scheduler.cancel(id)
      repository.deleteById(id)
      completion(.success(()))
    } catch let error as AlarmKitError {
      completion(.failure(error))
    } catch {
      completion(.failure(.init(.internal, error.localizedDescription)))
    }
  }

  func cancelAllAlarms(completion: @escaping (Result<Void, AlarmKitError>) -> Void) {
    repository.getAll().forEach { scheduler.cancel($0.id) }
    repository.deleteAll()
    completion(.success(()))
  }

  func getAlarm(id: String) -> AlarmRecord? {
    repository.getById(id)
  }

  func getAllAlarms() -> [AlarmRecord] {
    repository.getAll()
  }

  func snoozeAlarm(id: String, overrideMinutes: Int?, completion: @escaping (Result<Void, AlarmKitError>) -> Void) {
    do {
      var existing = try repository.requireById(id)
      guard existing.state == "ringing" || existing.state == "snoozed" else {
        throw AlarmKitError(.ringingStateConflict, "Alarm must be ringing to snooze", alarmId: id)
      }
      let minutes = overrideMinutes ?? existing.snoozeConfig?.defaultMinutes ?? 5
      let maxCount = existing.snoozeConfig?.maxSnoozeCount ?? Int.max
      if existing.snoozeCount >= maxCount {
        throw AlarmKitError(.ringingStateConflict, "Maximum snooze count reached", alarmId: id)
      }
      let nextTrigger = Date().timeIntervalSince1970 * 1000 + Double(minutes * 60_000)
      existing.triggerAtMillis = nextTrigger
      existing.state = "snoozed"
      existing.snoozeCount += 1
      existing.updatedAtMillis = Date().timeIntervalSince1970 * 1000

      scheduler.cancel(id)
      scheduler.schedule(existing) { result in
        switch result {
        case .success:
          self.repository.update(existing)
          self.ringingCoordinator.endRinging()
          self.eventEmitter.emitSnoozed(existing)
          completion(.success(()))
        case .failure(let error):
          completion(.failure(error))
        }
      }
    } catch let error as AlarmKitError {
      completion(.failure(error))
    } catch {
      completion(.failure(.init(.internal, error.localizedDescription)))
    }
  }

  func dismissAlarm(id: String, completion: @escaping (Result<Void, AlarmKitError>) -> Void) {
    do {
      var existing = try repository.requireById(id)
      guard existing.state == "ringing" || existing.state == "snoozed" else {
        throw AlarmKitError(.ringingStateConflict, "Alarm must be ringing to dismiss", alarmId: id)
      }
      existing.state = "dismissed"
      existing.updatedAtMillis = Date().timeIntervalSince1970 * 1000
      scheduler.cancel(id)
      repository.update(existing)
      ringingCoordinator.endRinging()
      eventEmitter.emitDismissed(existing)
      completion(.success(()))
    } catch let error as AlarmKitError {
      completion(.failure(error))
    } catch {
      completion(.failure(.init(.internal, error.localizedDescription)))
    }
  }

  func handleAlarmFire(alarmId: String, missedFire: Bool) {
    guard var existing = repository.getById(alarmId) else { return }
    let now = Date().timeIntervalSince1970 * 1000

    if let nextTrigger = scheduler.computeNextTrigger(for: existing) {
      var rearmed = existing
      rearmed.triggerAtMillis = nextTrigger
      rearmed.state = "scheduled"
      rearmed.lastFiredAtMillis = now
      rearmed.updatedAtMillis = now
      repository.update(rearmed)
      scheduler.schedule(rearmed) { _ in }
    }

    existing.state = missedFire ? "missed_fired" : "ringing"
    existing.lastFiredAtMillis = now
    existing.updatedAtMillis = now
    repository.update(existing)
    ringingCoordinator.beginRinging(alarmId: alarmId)

    if missedFire {
      eventEmitter.emitAlarmMissedThenFired(existing)
    } else {
      eventEmitter.emitAlarmFired(existing)
    }
  }

  func reconcilePendingNotifications(completion: @escaping ([AlarmRecord]) -> Void) {
    scheduler.getPendingNotificationRequests { pending in
      let pendingIds = Set(pending.map { $0.identifier })
      let expected = self.repository.getAll().filter { $0.state == "scheduled" || $0.state == "snoozed" }
      var reconciled: [AlarmRecord] = []

      for record in expected {
        if !pendingIds.contains(record.id) {
          self.scheduler.schedule(record) { _ in }
          reconciled.append(record)
        } else {
          reconciled.append(record)
        }
      }

      if !reconciled.isEmpty {
        self.eventEmitter.emitAlarmsReconciled(reconciled)
      }
      completion(reconciled)
    }
  }
}
