# Section 3: Native Module Architecture (Android/Kotlin)

**Library:** `@noirly/react-native-alarm-kit`

---

## 3.1 Class Responsibility Split

| Class | Responsibility |
|---|---|
| `NativeAlarmKitModule` | TurboModule spec implementation only — thin adapter layer. Validates input, delegates to services, maps results back to Codegen types. No business logic. |
| `AlarmScheduler` | Owns `AlarmManager` interaction — setting exact alarms (`setExactAndAllowWhileIdle` / `setAlarmClock`), cancelling, recurrence re-arming after each fire (Android has no native recurring exact alarm, so recurrence is re-armed on each fire). |
| `AlarmRepository` | Persistence layer — source of truth for scheduled alarms. Internal persistence detail needed to survive process death and support `getAllAlarms()`. Backed by a lightweight embedded store (decided in Section 9), never exposed as a public dependency. |
| `AlarmReceiver` (`BroadcastReceiver`) | Fires when `AlarmManager` triggers. Reads alarm from repository, decides ring vs. reminder-fire behavior, starts `AlarmRingingService`, re-arms recurrence via `AlarmScheduler`, emits fired event if JS is alive. |
| `BootReceiver` (`BroadcastReceiver`) | Listens for `ACTION_BOOT_COMPLETED`, reads all persisted alarms from `AlarmRepository`, re-registers them via `AlarmScheduler`. Emits a boot-reschedule event if a listener is attached. |
| `AlarmRingingService` (foreground `Service`) | Owns the "ringing" lifecycle — full-screen intent / high-priority notification, holds wake lock, exposes snooze/dismiss actions (from notification action buttons AND from JS via the TurboModule), stops itself on dismiss/snooze/timeout. |
| `PermissionManager` | Checks/requests `SCHEDULE_EXACT_ALARM` (Android 12+), `POST_NOTIFICATIONS` (Android 13+), battery-optimization exemption status. Wraps Activity-dependent permission flows behind a clean interface. |
| `CapabilityProvider` | Computes `PlatformCapabilities` — OS version gates, whether exact alarms are currently restricted, whether the app is battery-optimized, etc. |
| `AlarmEventEmitter` | Wraps the New Architecture event-emitting mechanism (Section 6). Single choke point so no other class talks to JS directly — keeps native business logic testable without a JS runtime. |

**Why this split matters:** `AlarmReceiver` and `BootReceiver` run in contexts where the TurboModule instance may not exist. All alarm-critical logic (`AlarmScheduler`, `AlarmRepository`, `AlarmRingingService`) must work **independently of the TurboModule being alive** — the TurboModule is a control surface, not the runtime engine.

## 3.2 Threading Model

- `NativeAlarmKitModule` methods run on the JS thread for argument marshalling only, then dispatch actual work to a dedicated background executor (Kotlin coroutine, `Dispatchers.IO`) — no `AlarmManager`, disk I/O, or repository access on the JS thread.
- Promises resolve back on the calling thread per TurboModule convention.
- `AlarmReceiver` / `BootReceiver` use `goAsync()` with a coroutine + explicit `finish()`, since `onReceive` cannot do open-ended async work safely.
- `AlarmRingingService` runs as a foreground service — off the main thread for I/O, main thread only for UI-adjacent calls.
- A single application-scoped `AlarmKitScope` (`SupervisorJob` + `Dispatchers.IO`) is shared across `AlarmScheduler`/`AlarmRepository` so work survives the TurboModule's own lifecycle.

## 3.3 Lifecycle Management

- `NativeAlarmKitModule` on `invalidate()` detaches event emitter listeners but does **not** tear down `AlarmScheduler`/`AlarmRepository` state (application-scoped, not module-scoped).
- `PermissionManager`'s Activity-dependent flows register via `ReactApplicationContext`, unregister on `invalidate()`.
- `AlarmRingingService` lifecycle is independent of React Native entirely — standard foreground `Service`, started via `Context.startForegroundService`, stopped via `stopSelf()`. Guarantees ringing survives RN instance destruction/recreation.

## 3.4 Data Flow: Alarm Firing (Critical Path)

1. `AlarmManager` fires → `AlarmReceiver.onReceive()`
2. Receiver calls `goAsync()`, launches coroutine on `AlarmKitScope`
3. Reads alarm config from `AlarmRepository`
4. If recurring → `AlarmScheduler` re-arms next occurrence immediately (before anything else, to minimize risk of missed recurrence if later steps fail)
5. Starts `AlarmRingingService` with alarm payload
6. If a JS instance + listener is currently attached → `AlarmEventEmitter` emits `onAlarmFired`
7. Receiver calls `finish()` to release the broadcast wake lock

## 3.5 Module Registration

- Registered via standard New Architecture `ReactPackage` (`TurboReactPackage`), autolinked (Section 9) — no manual `MainApplication.kt` edits required.
- `BootReceiver` and `AlarmReceiver` declared in the library's own `AndroidManifest.xml` (merged into host app manifest at build time) — zero manifest edits required from consumer apps.

## 3.6 Design Decision: Missed-Alarm Recovery (Default Assumption — Flag to Change)

**Default behavior:** Catch-up. If a recurring or one-shot alarm's trigger time has passed while the device was off/Doze-restricted, `BootReceiver`/`AlarmScheduler` fires it immediately on next boot/wake rather than silently skipping to the next occurrence.

**Rationale:** Alarm-clock UX convention (matches stock clock apps) — a missed 7am alarm firing at 8:05 on boot is expected behavior; silent skipping would be surprising and could cause a user to miss a reminder entirely with no indication anything was scheduled.

**Schema implication:** `AlarmRepository` tracks a `lastFiredAt` / `missedFireDetected` marker per alarm so `AlarmEventEmitter` can optionally emit a distinct `onAlarmMissedThenFired` event (vs. a normal `onAlarmFired`), letting consuming apps show "this alarm rang late" UX if desired.

*This is a default assumption, not a confirmed requirement. Revisit and override in this section if a different behavior (silent skip-to-next) is wanted.*

---

**Status:** Approved (3.6 catch-up default accepted)
