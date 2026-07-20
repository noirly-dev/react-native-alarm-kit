# Section 2: TurboModule Spec Design

**Library:** `@noirly/react-native-alarm-kit`

---

## 2.1 Module Structure Decision

One primary TurboModule (`NativeAlarmKit`) rather than multiple split modules.

**Rationale:** Alarms/permissions/introspection are tightly coupled (permission state gates scheduling), and Codegen works best with a cohesive spec rather than fragmented cross-talking modules. Internally, native code will still be organized into separate classes (Sections 3/4) — this is purely about the JS-facing Codegen contract.

A separate TurboModule is **not** used for events — events ride on the same module via the New Architecture's native event emitter pattern (not a legacy `NativeEventEmitter` bridge). Detailed in Section 6.

## 2.2 Codegen Spec Categories

The `.ts` spec (interface extending `TurboModule`) defines methods in four groups, matching Section 1.3:

### A. Scheduling methods
- `scheduleAlarm(config) → Promise<AlarmHandle>`
- `updateAlarm(id, partialConfig) → Promise<AlarmHandle>`
- `cancelAlarm(id) → Promise<void>`
- `cancelAllAlarms() → Promise<void>`
- `getAlarm(id) → Promise<AlarmHandle | null>`
- `getAllAlarms() → Promise<AlarmHandle[]>`

### B. Snooze/dismiss control
Native ringing state must be controllable from JS when app is foregrounded.
- `snoozeAlarm(id, overrideMinutes?) → Promise<void>`
- `dismissAlarm(id) → Promise<void>`

### C. Permissions
- `checkPermissions() → Promise<PermissionStatus>`
- `requestPermissions() → Promise<PermissionStatus>`

### D. Platform capability introspection
- `getCapabilities() → Promise<PlatformCapabilities>`

## 2.3 Codegen Type Constraints

New Architecture Codegen has strict type restrictions for TurboModule specs — this drives downstream design decisions:

- No arbitrary nested unions — recurrence rules modeled as a **discriminated object shape** Codegen can parse (flat-ish objects with optional fields), not a TS union of variant shapes
- No `Date` objects across the bridge — all timestamps as `Double` (epoch millis)
- Enums modeled as **string literal unions** (Codegen supports these) — e.g., permission status, capability flags
- Complex/optional nested config objects defined as named Codegen-safe object types, not inline anonymous types, so both Kotlin and Swift codegen output stays clean and reusable

## 2.4 Return Value Philosophy

Every mutating method returns the **resulting canonical alarm state** (`AlarmHandle`), not `void` or just an ID.

**Rationale:** Avoids the JS layer needing a follow-up `getAlarm()` call after every write, and prevents state drift between what JS *thinks* it scheduled and what native actually persisted (e.g., native may adjust a trigger time due to OS constraints).

## 2.5 Deliberately Not a TurboModule Method

- Alarm **firing** itself — this is an event, not a method response (Section 6)
- Boot-reschedule confirmation — emitted as an event, not queried synchronously

---

**Status:** Approved
