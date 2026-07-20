# Section 5: Cross-platform Contract & Type Mapping

**Library:** `@noirly/react-native-alarm-kit`

---

## 5.1 Purpose of This Section

Sections 3 and 4 defined symmetric native architectures (Scheduler / Repository / Receiver-or-Delegate / Ringing coordinator / Permission / Capability / EventEmitter). This section defines the **single canonical contract** — the Codegen-generated TypeScript types — that both native sides must produce and consume identically, so `AlarmHandle`, `PermissionStatus`, `PlatformCapabilities`, and event payloads mean exactly the same thing on both platforms from the JS consumer's point of view.

## 5.2 Canonical Type Definitions (Conceptual)

### `AlarmHandle`
The canonical representation of a scheduled alarm, returned by every mutating method and by `getAlarm`/`getAllAlarms`.

| Field | Type (Codegen-safe) | Notes |
|---|---|---|
| `id` | `string` | Generated natively (UUID) on first `scheduleAlarm`, stable across updates. |
| `triggerAtMillis` | `Double` | Epoch millis. No `Date` objects cross the bridge (Section 2.3). |
| `recurrenceRule` | `RecurrenceRule` (nullable) | Discriminated flat object, not a TS union (Section 2.3). |
| `title` | `string` | App-supplied display payload, opaque to native logic. |
| `payload` | `string` (nullable) | Opaque JSON-serialized app data, round-tripped only, never parsed natively. |
| `soundRef` | `string` (nullable) | Platform-specific sound resource identifier — resolved differently per platform (Android raw resource / iOS bundle sound name), library does not validate existence. |
| `snoozeConfig` | `SnoozeConfig` (nullable) | Default snooze minutes, max snooze count. |
| `state` | `AlarmState` (string literal union) | `"scheduled" \| "ringing" \| "snoozed" \| "dismissed" \| "missed_fired"` — the `"missed_fired"` value ties directly to the Section 3.6 catch-up decision and its iOS reconciliation counterpart (4.4). |
| `createdAtMillis` | `Double` | Set natively on first creation, immutable thereafter. |
| `updatedAtMillis` | `Double` | Set natively on every mutation. |

### `RecurrenceRule` (discriminated flat object — Codegen constraint from 2.3)
| Field | Type | Notes |
|---|---|---|
| `frequency` | `"none" \| "daily" \| "weekly" \| "custom"` | Discriminant. |
| `daysOfWeek` | `Array<Int32>` (nullable) | Used only when `frequency = "weekly"`; native validates relevance rather than Codegen enforcing it (Codegen cannot express conditional-required fields). |
| `intervalDays` | `Int32` (nullable) | Used only when `frequency = "custom"`. |

### `PermissionStatus`
| Field | Type | Notes |
|---|---|---|
| `notificationStatus` | `"granted" \| "denied" \| "notDetermined" \| "provisional"` | `"provisional"` is iOS-only (provisional authorization); Android always resolves to `granted`/`denied`. |
| `exactAlarmStatus` | `"granted" \| "denied" \| "notApplicable"` | `"notApplicable"` on iOS (no equivalent permission) and on Android versions below API 31. |
| `criticalAlertStatus` | `"granted" \| "denied" \| "notApplicable"` | `"notApplicable"` on Android always. |

### `PlatformCapabilities`
| Field | Type | Notes |
|---|---|---|
| `supportsExactAlarms` | `boolean` | False on Android if OS restricts and permission not granted; always true on iOS (no equivalent restriction path). |
| `supportsCriticalAlerts` | `boolean` | iOS entitlement-gated; always false on Android. |
| `supportsFullScreenRinging` | `boolean` | True on both, but gated differently (Android: `USE_FULL_SCREEN_INTENT` + OEM restrictions; iOS: foreground-only unless critical alert entitlement present). |
| `maxPendingAlarms` | `Int32` (nullable) | `64` on iOS (`UNUserNotificationCenter` cap, Section 4.4); `null` on Android (no hard OS-enforced cap, though `AlarmManager` has soft practical limits documented separately). |

### Event Payloads (previewed here, finalized in Section 6)
- `onAlarmFired` → `AlarmHandle`
- `onAlarmMissedThenFired` → `AlarmHandle` (Android catch-up path, Section 3.6)
- `onAlarmsReconciled` → `Array<AlarmHandle>` (iOS reconciliation path, Section 4.4 — cross-platform event name chosen deliberately so JS consumers don't need platform branching; Android emits this too, trivially, after boot-reschedule, for symmetry)
- `onSnoozed` / `onDismissed` → `AlarmHandle`

## 5.3 Type Reconciliation Rules

1. **Every nullable field in the Codegen spec must be nullable identically on both native sides** — Kotlin `String?` ↔ Swift `String?` ↔ TS `string | null`. No platform may silently coerce to empty string or sentinel values; absence must be represented as absence.
2. **String literal unions are the only enum mechanism** — no native-side raw Int enums leak into the bridge; Kotlin `sealed class`/`enum class` and Swift `enum` are mapped to/from the string literal at the module boundary, not exposed as bridge types themselves.
3. **Platform-inapplicable fields resolve to an explicit sentinel value, never omitted** — e.g., `criticalAlertStatus: "notApplicable"` on Android, not a missing field — because Codegen requires consistent shape; optionality is for "unknown/not yet determined," not for "doesn't apply on this OS."
4. **Timestamps are always UTC epoch millis** — timezone/locale handling for recurrence (e.g., DST transitions affecting a daily 7am alarm) is a native-side scheduling concern (Sections 3/4), never pushed to JS as a formatting/parsing responsibility.
5. **`payload` is always an opaque string** — the library never parses or validates it natively; this keeps the Codegen contract stable regardless of what shape of app data consumers choose to store, and avoids native-side JSON parsing divergence between Kotlin/Swift JSON handling.

## 5.4 Versioning Implication

Because `AlarmHandle` is the single canonical object returned everywhere, it is the highest-risk type for breaking changes. Any future field addition must be additive-nullable (never a required new field) to preserve backward compatibility for consumers who serialize/cache `AlarmHandle` objects across library upgrades — formalized further in Section 11 (Versioning & Release Strategy).

---

**Status:** Approved
