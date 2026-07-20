# Section 4: Native Module Architecture (iOS/Swift)

**Library:** `@noirly/react-native-alarm-kit`

---

## 4.1 Class Responsibility Split

| Class | Responsibility |
|---|---|
| `NativeAlarmKitModule` (Swift, conforms to generated `NativeAlarmKitSpec`) | TurboModule spec implementation only — thin adapter layer. Validates input, delegates to services, maps results back to Codegen types. No business logic. |
| `AlarmScheduler` | Owns `UNUserNotificationCenter` interaction for trigger scheduling (`UNCalendarNotificationTrigger` for exact/recurring alarms). iOS has native recurring-trigger support (unlike Android), but recurrence rules beyond simple calendar-component repeats (e.g. "every 3rd day") still require re-arming, so the re-arm-on-fire pattern is used uniformly for parity with Android. |
| `AlarmRepository` | Persistence layer — source of truth for scheduled alarms, mirrors Android's repository role. Backed by a lightweight embedded store (decided in Section 9). Needed because `UNUserNotificationCenter`'s own pending-request list is not a sufficient source of truth for `getAllAlarms()` (it doesn't carry all app-domain fields, and delivered/consumed notifications drop off it). |
| `AlarmNotificationDelegate` (`UNUserNotificationCenterDelegate`) | Handles foreground notification presentation (`willPresent`) and user interaction (`didReceive response:`) — snooze/dismiss actions from notification action buttons, opening the ringing UI. |
| `AlarmRingingCoordinator` | Owns the "ringing" state/session when the app is in foreground or launched via notification tap — coordinates presenting the full-screen ringing experience, exposes snooze/dismiss actions callable both from the notification delegate AND from JS via the TurboModule. |
| `PermissionManager` | Requests/checks `UNAuthorizationStatus`, requests `criticalAlert` entitlement usage (if configured), checks time-sensitive notification authorization (iOS 15+). |
| `CapabilityProvider` | Computes `PlatformCapabilities` — iOS version gates, critical-alert entitlement presence, time-sensitive authorization status. |
| `AlarmEventEmitter` | Wraps the New Architecture event-emitting mechanism (Section 6). Single choke point — no other class talks to JS directly. |

**Key platform contrast vs. Android:** iOS has no `BootReceiver` equivalent — the OS itself persists `UNUserNotificationCenter` scheduled requests across reboots natively. This removes an entire class from the iOS side, but shifts responsibility onto `AlarmRepository` reconciliation logic (4.4) to keep the app's own repository state in sync with what the OS actually still has scheduled, since iOS may silently drop notification requests (e.g., pending-request limit of 64 per app).

## 4.2 Threading Model

- `NativeAlarmKitModule` methods run on the JS thread for argument marshalling only, then dispatch actual work to a dedicated `DispatchQueue` (serial, background QoS) — no `UNUserNotificationCenter` calls, disk I/O, or repository access on the main/JS thread.
- Promises resolve via completion handlers bridged back through the TurboModule's `Promise` resolve/reject, dispatched back appropriately per Codegen's generated threading contract.
- `AlarmNotificationDelegate` callbacks (`willPresent`, `didReceive response:`) are invoked by the system on an arbitrary background queue — work is dispatched onto the shared `AlarmKitQueue` before touching `AlarmRepository`, and the system-provided completion handler is always called (required by `UNUserNotificationCenterDelegate` contract, or the OS will kill the process on timeout).
- `AlarmRingingCoordinator` UI-facing calls run on the main thread (`DispatchQueue.main`); its state/business logic delegates to the shared background queue.
- A single `AlarmKitQueue` (serial `DispatchQueue`, background QoS) is the iOS analog of Android's `AlarmKitScope` — all repository/scheduler mutations are serialized through it to avoid race conditions between JS-triggered calls and system-triggered notification delegate callbacks.

## 4.3 Lifecycle Management

- `NativeAlarmKitModule` on `invalidate()` detaches event emitter listeners; `AlarmRepository`/`AlarmScheduler` are singletons scoped to the app process, not the module instance, so they survive RN instance teardown/reload (e.g., Fast Refresh in dev).
- `AlarmNotificationDelegate` is set once as `UNUserNotificationCenter.current().delegate` at module init (or app launch, whichever is first — see 4.5) and persists independently of the TurboModule/bridge lifecycle, since notification delivery must work even if the RN bridge is not currently initialized (e.g., app launched directly into background via notification tap before JS loads).
- `AlarmRingingCoordinator` is only meaningfully active while the app is foregrounded or being launched in response to a notification interaction — it does not run as a persistent background service (iOS has no foreground-service equivalent); the "ringing" experience while backgrounded is delivered via the notification itself (critical alert sound + full-screen-capable presentation where entitlements allow).

## 4.4 Repository Reconciliation (iOS-Specific Concern)

Because `UNUserNotificationCenter` can silently drop pending requests (64-request cap, OS-level pruning), `AlarmRepository` cannot blindly trust its own persisted state matches what will actually fire. A reconciliation pass runs:
- On module init (app launch)
- On `applicationDidBecomeActive`-equivalent lifecycle signal (surfaced to the module via `ReactAppLifecycleFacade` — see 4.6)

Reconciliation: fetch `UNUserNotificationCenter.getPendingNotificationRequests()`, diff against `AlarmRepository`'s expected set, re-schedule anything missing (up to the OS cap), and emit an event if alarms had to be silently re-registered (parity with Android's boot-reschedule event, so JS-side consumers get one consistent "alarms were reconciled" signal cross-platform — formalized in Section 6).

## 4.5 Notification Delegate Registration Timing

`UNUserNotificationCenterDelegate` must be set **before** the app finishes launching, or early notification interactions (cold launch via notification tap) can be missed. Since a TurboModule's Swift class isn't guaranteed to initialize before `application(_:didFinishLaunchingWithOptions:)` completes, the library ships an `AppDelegate` integration point (a documented one-line call the consuming app adds to its own `AppDelegate`, OR — preferred — a Swift `load()`-based auto-registration hook to keep the "zero manual native edits" promise consistent with Android's autolinked-manifest approach). Final mechanism choice is deferred to Section 9 (Build/Autolinking Pipeline), but the constraint is recorded here since it affects `AlarmNotificationDelegate`'s design (must be safely initializable as a singleton independent of the TurboModule's own init timing).

## 4.6 App Lifecycle Bridging

Since `AlarmRingingCoordinator` and repository reconciliation (4.4) need app-foreground signals independent of any JS-side listener being attached, a small internal `ReactAppLifecycleFacade` observes `UIApplication` lifecycle notifications (`didBecomeActiveNotification`, etc.) directly via `NotificationCenter`, decoupled from React Native's own `AppState` module — this keeps native reconciliation logic functional even before JS has finished loading.

---

**Cross-platform note:** Sections 3 and 4 are intentionally structured with matching responsibility categories (Scheduler / Repository / Delegate-or-Receiver / Ringing coordinator / Permission / Capability / EventEmitter) even though the concrete OS mechanisms differ — this symmetry is what makes Section 5 (Cross-platform Contract & Type Mapping) tractable.

---

**Status:** Approved (4.5 delegate-registration mechanism deferred to Section 9)
