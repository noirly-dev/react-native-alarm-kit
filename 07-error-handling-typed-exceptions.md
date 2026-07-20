# Section 7: Error Handling & Typed Exceptions

**Library:** `@noirly/react-native-alarm-kit`

---

## 7.1 Design Goals

- Every failure surfaced to JS must be **programmatically distinguishable** (error code), not just a human-readable message — so consuming apps can branch logic (e.g., "permission denied" → prompt settings deep link; "alarm not found" → silently no-op) without string-matching messages.
- Error shape must be **identical across Kotlin and Swift** — same code taxonomy, same payload shape — consistent with the Section 5 cross-platform contract philosophy.
- Codegen's `Promise.reject` only carries a `code`, `message`, and optional `userInfo`/`Error` cause on native — the library defines a **fixed, versioned error code enum** as the real contract, with `message` treated as debug-only/non-localized.

## 7.2 Error Code Taxonomy

All error codes are string literals in a single namespaced enum, prefixed `E_ALARMKIT_`, exposed as a TypeScript string literal union so consumers get compile-time exhaustiveness checking on `catch` blocks.

| Code | Meaning | Typical Cause |
|---|---|---|
| `E_ALARMKIT_INVALID_CONFIG` | Input validation failed before reaching native scheduling logic. | Malformed `RecurrenceRule` (e.g., `frequency: "weekly"` with empty `daysOfWeek`), negative `triggerAtMillis`. |
| `E_ALARMKIT_ALARM_NOT_FOUND` | Operation referenced an `id` that doesn't exist in `AlarmRepository`. | `updateAlarm`/`cancelAlarm`/`snoozeAlarm`/`dismissAlarm` called with stale/unknown id. |
| `E_ALARMKIT_PERMISSION_DENIED` | Required OS permission not granted. | `SCHEDULE_EXACT_ALARM` denied (Android), notification authorization denied (iOS), attempting to schedule without prior `requestPermissions()` success. |
| `E_ALARMKIT_PLATFORM_LIMIT_EXCEEDED` | OS-enforced scheduling cap reached. | iOS 64-pending-notification cap (Section 4.4); surfaced rather than silently dropped, since this is a JS-invoked `scheduleAlarm` call, not the reconciliation background path (which is drop-and-notify via `onAlarmsReconciled` instead, per Section 6). |
| `E_ALARMKIT_SCHEDULING_FAILED` | Native OS scheduling API call itself failed. | `AlarmManager`/`UNUserNotificationCenter` rejected the request for a reason not covered by the above (OS-internal error, low storage, OEM restriction). |
| `E_ALARMKIT_RINGING_STATE_CONFLICT` | Snooze/dismiss called on an alarm not currently in a ringing-capable state. | `dismissAlarm` called on an alarm whose `state` is `"scheduled"` (never fired) rather than `"ringing"`/`"snoozed"`. |
| `E_ALARMKIT_UNSUPPORTED_OPERATION` | Operation not supported on the current OS/version. | Requesting critical-alert-dependent behavior on Android; calling a capability the `PlatformCapabilities` check should have precluded. |
| `E_ALARMKIT_INTERNAL` | Unexpected/uncategorized native failure — catch-all, never the first choice. | Persistence layer I/O failure, unexpected null state, native crash-adjacent recoverable exceptions caught at the module boundary. |

## 7.3 Error Payload Shape

Beyond the required `code`/`message` that `Promise.reject` carries natively, a structured `userInfo` payload (Codegen-safe object, following Section 2.3 constraints) is attached for programmatic detail without needing to parse `message` strings:

| Field | Type | Notes |
|---|---|---|
| `code` | Error code string literal (7.2) | Primary dispatch key for consumers. |
| `alarmId` | `string` (nullable) | Present when the error relates to a specific alarm (`ALARM_NOT_FOUND`, `RINGING_STATE_CONFLICT`). |
| `platformErrorCode` | `string` (nullable) | Raw underlying OS error identifier (Android exception class name / iOS `NSError` code), for debugging/telemetry — never used for JS-side branching logic, since it's not stable across OS versions. |

## 7.4 Native-to-JS Error Bridging Rules

1. **`NativeAlarmKitModule`/Swift module is the only place native exceptions are caught and translated.** `AlarmScheduler`, `AlarmRepository`, etc. throw typed native exceptions (Kotlin sealed exception hierarchy / Swift `Error` enum mirroring the 7.2 taxonomy) — the module layer catches these and maps 1:1 to the Codegen error code, never lets a raw platform exception (`SecurityException`, `NSError` domain error) leak across the bridge unmapped.
2. **Validation errors (`INVALID_CONFIG`) are checked before any native OS API call** — fail fast, so a malformed request never partially schedules before rejecting.
3. **No error is ever silently swallowed at the module boundary** — every catch block either maps to a specific code or falls through to `E_ALARMKIT_INTERNAL`; there is no bare "return null on failure" path for mutating methods (this would violate the Section 2.4 return-value philosophy and hide real failures as false-successes).
4. **Background-path failures (inside `AlarmReceiver`/`BootReceiver`/notification delegate callbacks) cannot reject a Promise, since no JS call initiated them** — these are logged natively (Section 10 covers native logging/diagnostics strategy) and, where relevant, surfaced via a distinct event rather than an error — e.g., a failed recurrence re-arm could emit `onAlarmsReconciled` on next reconciliation rather than being lost silently.

## 7.5 JS-Side Consumption Pattern

Because `catch` blocks receive the structured `userInfo.code`, the library ships (implemented later) a small typed helper — e.g. a type guard function — so consumers get TypeScript narrowing on the caught error's code without manual casting, keeping error handling as strongly typed as the rest of the public API surface (Section 1.2 / Section 5).

---

**Status:** Approved
