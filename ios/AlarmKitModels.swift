import Foundation

enum AlarmKitErrorCode: String {
  case invalidConfig = "E_ALARMKIT_INVALID_CONFIG"
  case alarmNotFound = "E_ALARMKIT_ALARM_NOT_FOUND"
  case permissionDenied = "E_ALARMKIT_PERMISSION_DENIED"
  case platformLimitExceeded = "E_ALARMKIT_PLATFORM_LIMIT_EXCEEDED"
  case schedulingFailed = "E_ALARMKIT_SCHEDULING_FAILED"
  case ringingStateConflict = "E_ALARMKIT_RINGING_STATE_CONFLICT"
  case unsupportedOperation = "E_ALARMKIT_UNSUPPORTED_OPERATION"
  case `internal` = "E_ALARMKIT_INTERNAL"
}

struct AlarmKitError: Error {
  let code: AlarmKitErrorCode
  let message: String
  let alarmId: String?
  let platformErrorCode: String?

  init(
    _ code: AlarmKitErrorCode,
    _ message: String,
    alarmId: String? = nil,
    platformErrorCode: String? = nil
  ) {
    self.code = code
    self.message = message
    self.alarmId = alarmId
    self.platformErrorCode = platformErrorCode
  }
}

struct RecurrenceRuleRecord: Codable {
  let frequency: String
  let daysOfWeek: [Int]?
  let intervalDays: Int?
}

struct SnoozeConfigRecord: Codable {
  let defaultMinutes: Int
  let maxSnoozeCount: Int
}

struct AlarmRecord: Codable {
  var id: String
  var triggerAtMillis: Double
  var recurrenceRule: RecurrenceRuleRecord?
  var title: String
  var payload: String?
  var soundRef: String?
  var snoozeConfig: SnoozeConfigRecord?
  var state: String
  var createdAtMillis: Double
  var updatedAtMillis: Double
  var lastFiredAtMillis: Double?
  var snoozeCount: Int
}

enum AlarmMapper {
  static func newId() -> String {
    UUID().uuidString
  }

  static func validateRecurrenceRule(_ rule: RecurrenceRuleRecord) throws {
    switch rule.frequency {
    case "none", "daily":
      return
    case "weekly":
      if rule.daysOfWeek?.isEmpty != false {
        throw AlarmKitError(.invalidConfig, "Weekly recurrence requires daysOfWeek")
      }
    case "custom":
      if rule.intervalDays == nil || rule.intervalDays! <= 0 {
        throw AlarmKitError(.invalidConfig, "Custom recurrence requires positive intervalDays")
      }
    default:
      throw AlarmKitError(.invalidConfig, "Unknown recurrence frequency: \(rule.frequency)")
    }
  }

  static func toDictionary(_ record: AlarmRecord) -> [String: Any] {
    var map: [String: Any] = [
      "id": record.id,
      "triggerAtMillis": record.triggerAtMillis,
      "title": record.title,
      "state": record.state,
      "createdAtMillis": record.createdAtMillis,
      "updatedAtMillis": record.updatedAtMillis,
    ]

    map["payload"] = record.payload ?? NSNull()
    map["soundRef"] = record.soundRef ?? NSNull()

    if let rule = record.recurrenceRule {
      var ruleMap: [String: Any] = ["frequency": rule.frequency]
      ruleMap["daysOfWeek"] = rule.daysOfWeek ?? NSNull()
      ruleMap["intervalDays"] = rule.intervalDays ?? NSNull()
      map["recurrenceRule"] = ruleMap
    } else {
      map["recurrenceRule"] = NSNull()
    }

    if let snooze = record.snoozeConfig {
      map["snoozeConfig"] = [
        "defaultMinutes": snooze.defaultMinutes,
        "maxSnoozeCount": snooze.maxSnoozeCount,
      ]
    } else {
      map["snoozeConfig"] = NSNull()
    }

    return map
  }

  static func parseRecurrenceRule(_ value: Any?) throws -> RecurrenceRuleRecord? {
    guard let dict = value as? [String: Any], let frequency = dict["frequency"] as? String else {
      return nil
    }
    let days = dict["daysOfWeek"] as? [Int]
    let interval = dict["intervalDays"] as? Int
    let rule = RecurrenceRuleRecord(frequency: frequency, daysOfWeek: days, intervalDays: interval)
    try validateRecurrenceRule(rule)
    return rule
  }

  static func parseSnoozeConfig(_ value: Any?) -> SnoozeConfigRecord? {
    guard let dict = value as? [String: Any],
          let defaultMinutes = dict["defaultMinutes"] as? Int,
          let maxSnoozeCount = dict["maxSnoozeCount"] as? Int else {
      return nil
    }
    return SnoozeConfigRecord(defaultMinutes: defaultMinutes, maxSnoozeCount: maxSnoozeCount)
  }
}
