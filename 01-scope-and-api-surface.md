# Section 1: Scope & API Surface

**Library:** `@noirly/react-native-alarm-kit`
**Purpose:** Local-only native scheduling engine for alarms, tasks, and reminders (clock-app-style functionality) in React Native apps.

**Standing constraints:** React Native CLI only, No Expo, New Architecture, TurboModules, Codegen, Kotlin, Swift, TypeScript, npm package, production-ready, open-source quality.

---

## 1.1 Domain

Local-only native scheduling engine. Headless (no UI components). No remote/push involvement.

### In Scope
- Exact-time alarm scheduling (survives app kill + device reboot)
- Recurring alarms (daily/weekly/custom recurrence rules)
- Task/reminder scheduling with optional notification firing
- Native-side snooze handling
- CRUD operations: schedule, update, cancel, query alarms
- Permission handling (Android 12+ exact alarm, Android 13+/iOS notification permissions)
- Foreground "ringing" UX hooks (Android full-screen intent, iOS time-sensitive/critical alerts)
- Boot persistence (Android `BootReceiver` re-registration)
- Doze-mode / battery-optimization resilience (Android)

### Out of Scope
- UI (screens, pickers, alarm-list rendering)
- Ringtone/sound asset management (accepts a reference only)
- Cloud sync / database abstraction
- Remote push notifications

## 1.2 Public API Surface (Conceptual)

Four functional groups, all backed by a single TurboModule:

1. **Scheduling API** — create/update/cancel/query alarms and reminders
2. **Permissions API** — check/request platform-specific permissions
3. **Event Stream** — native → JS events (alarm fired, snoozed, dismissed, boot-rescheduled)
4. **Platform Capability Introspection** — query what the current OS/version supports (e.g., exact alarms restricted, critical alerts entitlement missing) so JS can degrade gracefully

## 1.3 Consumer Mental Model

From the app developer's perspective:
- They schedule an "alarm entity" (id, trigger time, recurrence rule, payload, sound ref, snooze config)
- The library owns firing it natively — even if JS/app is dead
- The library emits events back to JS when the app *is* alive, so app state (e.g., a Redux store of alarms) can stay in sync
- The library does NOT own what happens when the alarm fires visually — that's the app's job; this just guarantees the *trigger* and gives *native ringing infrastructure hooks*

---

**Status:** Approved
