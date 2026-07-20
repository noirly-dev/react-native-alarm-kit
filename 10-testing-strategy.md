# Section 10: Testing Strategy

**Library:** `@noirly/react-native-alarm-kit`

---

## 10.1 Testing Pyramid Overview

| Layer | Scope | Location (Section 8.2) |
|---|---|---|
| Native unit tests | `AlarmScheduler`, `AlarmRepository`, error mapping, recurrence math — pure logic, no OS integration | `android/src/test`, `ios/Tests/` |
| Native instrumented/integration tests | Real `AlarmManager`/`UNUserNotificationCenter` interaction, receiver/delegate behavior | `android/src/androidTest`, `ios/Tests/` (XCTest with real notification center where feasible) |
| JS unit tests | `AlarmKit.ts` wrapper, error type guards, type-level contract checks | `__tests__/` |
| Example-app integration/E2E | Full JS→native→OS round trip, real device/emulator behavior | `example/` + Detox or Maestro (decided 10.5) |

Each layer exists because the layer below it cannot catch everything: pure unit tests can't verify `AlarmManager` actually persists across a simulated reboot; JS unit tests can't verify Codegen's generated bridge marshals types correctly; only example-app E2E proves the full contract from Section 5 holds end-to-end.

## 10.2 Native Unit Tests (Kotlin)

- **`AlarmScheduler`**: mocked `AlarmManager` (via a thin interface wrapper so the real Android `AlarmManager` is swappable for a fake in unit tests) — verifies correct trigger-time math, correct `setExactAndAllowWhileIdle` vs `setAlarmClock` selection, recurrence re-arm calculation (including DST-crossing edge cases per Section 5.3 rule 4).
- **`AlarmRepository`**: Room's in-memory database mode — verifies CRUD correctness, schema migration paths (Section 9.3 migration-safety rationale is tested here, not just asserted architecturally).
- **Error mapping** (Section 7.4): every native exception type has a corresponding unit test asserting it maps to the correct `E_ALARMKIT_*` code — this test suite is the enforcement mechanism for Section 7's "no error silently swallowed" rule.
- **`CapabilityProvider`**: parameterized tests across simulated OS version/permission-state combinations, asserting correct `PlatformCapabilities` output per Section 5.2's table.

## 10.3 Native Unit Tests (Swift)

- **`AlarmScheduler`**: mocked `UNUserNotificationCenter` (protocol-based wrapper, same pattern as Android's `AlarmManager` interface wrapper, for architectural symmetry) — verifies `UNCalendarNotificationTrigger` construction, recurrence re-arm logic.
- **`AlarmRepository`**: Core Data in-memory store — same CRUD/migration coverage goal as Android.
- **Reconciliation logic** (Section 4.4): unit tests simulate a mismatch between `AlarmRepository`'s expected set and a fake `getPendingNotificationRequests()` response, asserting correct re-scheduling and correct `onAlarmsReconciled` emission.
- **Error mapping**: same 1:1 exception-to-code coverage goal as Android (10.2), keeping both platforms' error taxonomy tests structurally parallel so a reviewer can diff them.

## 10.4 Native Instrumented/Integration Tests

- **Android (`androidTest`)**: runs on emulator/device, exercises real `AlarmManager` + `BroadcastReceiver` delivery (using short test-only intervals, not real-world alarm durations, to keep CI fast), verifies `BootReceiver` re-registration logic via Android's instrumented broadcast-simulation APIs.
- **iOS (XCTest)**: exercises real `UNUserNotificationCenter` scheduling/delivery where the simulator/CI environment allows (some notification delivery behaviors are simulator-limited — documented as a known CI gap in `CONTRIBUTING.md`, not silently ignored).
- These tests are **not** part of the fast per-PR gate (Section 9.4's `android-build.yml`/`ios-build.yml` run unit tests only) — instrumented tests run on a slower, separate scheduled/nightly CI job, since emulator/device boot time makes them unsuitable for blocking every PR.

## 10.5 Example-App Integration/E2E

- **Tool decision: Maestro**, over Detox — Maestro's simpler YAML-flow authoring and lower native-build coupling reduces maintenance burden for a library-focused (not app-focused) test suite, and its black-box approach is a better fit for verifying *observable behavior* (does a notification appear, does tapping snooze update state) rather than deep component internals.
- E2E flows to cover, each mapped to a specific architectural guarantee:
  - Schedule an alarm → verify `AlarmHandle` returned matches request (Section 2.4 contract)
  - Force-kill app → wait for trigger → verify alarm still fires (Sections 3/4 core promise)
  - Simulate device reboot (Android only, via emulator reboot) → verify `onAlarmsReconciled` fires with the alarm still scheduled (Section 3.6/4.4)
  - Deny permission → attempt schedule → verify `E_ALARMKIT_PERMISSION_DENIED` surfaces correctly (Section 7)
  - Snooze from notification action → verify JS receives `onSnoozed` with updated `AlarmHandle` (Section 6.2)

## 10.6 Type-Contract Tests (JS)

A dedicated `__tests__/types.test-d.ts` (using `tsd` or `expect-type`) asserts the **public TypeScript surface itself** doesn't regress — e.g., `AlarmHandle.recurrenceRule` stays nullable, event payload types stay assignable to documented shapes. This directly enforces Section 5.4's backward-compatibility versioning rule at the type level, catching an accidental breaking type change before it ships, not just before it's used incorrectly at runtime.

## 10.7 Coverage Philosophy

No blanket numeric coverage threshold is enforced as a vanity metric. Instead, coverage requirements are **tied to the architecture's own risk surface**:
- 100% of the Section 7.2 error taxonomy must have an asserting test (enforced via a small custom lint/script checking every enum value appears in at least one test file — not just an aspirational rule).
- Every public method in the Codegen spec (Section 2.2) must have at least one JS-level and one native-level test.
- Every event in Section 6.2 must have at least one E2E assertion.

---

**Status:** Approved
