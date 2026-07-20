# Section 6: Event Emission Strategy

**Library:** `@noirly/react-native-alarm-kit`

---

## 6.1 Mechanism Choice

New Architecture TurboModules support native event emission via the generated `EventEmitter` base (spec methods typed as `(payload: X) => void` in the Codegen `.ts` spec, exposed as `onAlarmFired`-style callback properties), rather than the legacy bridge's `RCTEventEmitter` + `NativeEventEmitter(NativeModules.X)` pattern.

**Decision:** Use the Codegen-generated typed event emitter exclusively. No legacy `sendEvent`/`RCTDeviceEventEmitter` path, and no dependency on `NativeEventEmitter` from consumer-side JS — the generated spec's event methods are strongly typed end-to-end, which is the whole point of adopting New Architecture for this library.

This single choke point is `AlarmEventEmitter` on both native sides (named identically in Sections 3.1 / 4.1) — no other native class calls into JS directly.

## 6.2 Canonical Event Set

Matches the payload preview from Section 5.2, finalized here:

| Event | Payload | Emitted When |
|---|---|---|
| `onAlarmFired` | `AlarmHandle` | Alarm triggers normally, JS runtime is alive and listener attached. |
| `onAlarmMissedThenFired` | `AlarmHandle` | Android catch-up fire after boot/Doze delay (Section 3.6). |
| `onAlarmsReconciled` | `Array<AlarmHandle>` | Emitted on both platforms after boot-reschedule (Android, 3.1) or notification-cap reconciliation (iOS, 4.4) — unified name so JS never branches on platform. |
| `onSnoozed` | `AlarmHandle` | Snooze triggered from either native UI (notification action) or JS-invoked `snoozeAlarm`. |
| `onDismissed` | `AlarmHandle` | Dismiss triggered from either native UI or JS-invoked `dismissAlarm`. |
| `onPermissionsChanged` | `PermissionStatus` | OS-level permission state changes detected outside of an explicit `requestPermissions()` call (e.g., user revokes notification permission from system Settings while app is backgrounded, detected on next foreground reconciliation pass). |

**Design rule:** every event that represents a state transition emits the **full resulting `AlarmHandle`**, mirroring the Section 2.4 return-value philosophy for methods — JS never needs to re-fetch state after receiving an event.

## 6.3 Listener Lifecycle & Memory Safety

- The generated TurboModule event emitter tracks listener add/remove counts internally (New Architecture handles this at the codegen layer, not manually as in legacy bridge's `addListener`/`removeListeners` convention) — but the library still exposes a JS-level convenience wrapper (thin TS class, not a TurboModule concern) that returns an unsubscribe function per call, so app code never needs to manually track listener counts.
- **Native side never assumes a listener is attached.** Every emit call is guarded — if no JS listener is currently registered (app backgrounded, JS not yet loaded, bridge torn down), the native event is silently dropped, **never queued**. State is not lost because `AlarmRepository` remains the source of truth; a dropped `onAlarmFired` event just means JS didn't get a push notification of a state change it can re-derive via `getAlarm`/`getAllAlarms` next time it's active.
- This "drop, don't queue" rule is a deliberate simplicity choice — an event queue/replay mechanism would add significant complexity (ordering, staleness, unbounded growth if JS never reconnects) for a scenario the repository-as-source-of-truth pattern already handles safely.
- On JS reload (Fast Refresh, bridge reinitialization), `AlarmEventEmitter`'s internal listener-count tracking resets cleanly since it's owned by the generated TurboModule infrastructure tied to the current bridge instance — no manual cleanup required in `NativeAlarmKitModule`/native Swift module beyond what `invalidate()` already handles (Sections 3.3 / 4.3).

## 6.4 Cross-Platform Symmetry Note

`onAlarmsReconciled` is the clearest example of Section 5's design goal: two structurally different native mechanisms (Android boot broadcast vs. iOS foreground reconciliation pass) collapse into one JS-visible event shape. Consuming app code writes reconciliation-handling logic once, with no platform branching required.

## 6.5 Recommended JS-Side Consumption Pattern (documented here for API-design consistency, implemented later)

A single `AlarmKit.addListener(eventName, callback)` wrapper (thin TS, not native) is the only documented way apps attach listeners — even though it's a straightforward pass-through to the generated emitter — so that:
- The public API surface (Section 1.2) stays stable even if the underlying New Architecture event-emitter generation mechanics change in future RN versions.
- TypeScript overloads on `eventName` give full payload-type inference per event without consumers needing to import individual payload types manually.

---

**Status:** Approved
