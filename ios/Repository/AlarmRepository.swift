import Foundation

final class AlarmKitQueue {
  static let shared = AlarmKitQueue()
  private let queue = DispatchQueue(label: "com.noirly.alarmkit.queue", qos: .utility)

  func async(_ work: @escaping () -> Void) {
    queue.async(execute: work)
  }

  func sync<T>(_ work: () throws -> T) rethrows -> T {
    try queue.sync(execute: work)
  }
}

final class AlarmRepository {
  static let shared = AlarmRepository()

  private let fileURL: URL
  private var cache: [String: AlarmRecord] = [:]

  private init() {
    let directory = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first!
    let folder = directory.appendingPathComponent("NoirlyAlarmKit", isDirectory: true)
    try? FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
    fileURL = folder.appendingPathComponent("alarms.json")
    load()
  }

  func insert(_ record: AlarmRecord) {
    cache[record.id] = record
    persist()
  }

  func update(_ record: AlarmRecord) {
    cache[record.id] = record
    persist()
  }

  func getById(_ id: String) -> AlarmRecord? {
    cache[id]
  }

  func requireById(_ id: String) throws -> AlarmRecord {
    guard let record = cache[id] else {
      throw AlarmKitError(.alarmNotFound, "Alarm not found: \(id)", alarmId: id)
    }
    return record
  }

  func getAll() -> [AlarmRecord] {
    Array(cache.values)
  }

  func deleteById(_ id: String) {
    cache.removeValue(forKey: id)
    persist()
  }

  func deleteAll() {
    cache.removeAll()
    persist()
  }

  func count() -> Int {
    cache.count
  }

  private func load() {
    guard let data = try? Data(contentsOf: fileURL),
          let records = try? JSONDecoder().decode([AlarmRecord].self, from: data) else {
      return
    }
    cache = Dictionary(uniqueKeysWithValues: records.map { ($0.id, $0) })
  }

  private func persist() {
    let records = Array(cache.values)
    guard let data = try? JSONEncoder().encode(records) else { return }
    try? data.write(to: fileURL, options: .atomic)
  }
}
