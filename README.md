# @noirly-forge/react-native-alarm-kit

Local-only native scheduling engine for alarms, tasks, and reminders in React Native apps.

[![CI](https://github.com/noirly-forge/react-native-alarm-kit/actions/workflows/lint-and-typecheck.yml/badge.svg)](https://github.com/noirly-forge/react-native-alarm-kit/actions)

## Why this library

Headless scheduling layer — no UI components, no cloud sync, no remote push. The library guarantees native trigger delivery (including after app kill and device reboot) and exposes ringing hooks; your app owns the visual experience.

**In scope:** exact-time alarms, recurrence, snooze/dismiss, CRUD, permissions, capability introspection, boot persistence (Android), notification reconciliation (iOS).

**Out of scope:** alarm UI, ringtone asset management, cloud sync, remote push.

## Installation

This package is published to [GitHub Packages](https://github.com/noirly-forge/react-native-alarm-kit/packages), not npmjs.com.

Create or update `.npmrc` in your app:

```ini
@noirly-forge:registry=https://npm.pkg.github.com
//npm.pkg.github.com/:_authToken=YOUR_GITHUB_TOKEN
```

Use a GitHub personal access token with `read:packages` scope. For private repos, the token also needs `repo`.

```bash
npm install @noirly-forge/react-native-alarm-kit
# or
yarn add @noirly-forge/react-native-alarm-kit
```

Autolinking handles native setup on both platforms. Run `pod install` in your iOS project after installing.

**Requirements:** React Native CLI (no Expo), New Architecture enabled, React Native 0.76+.

## Required native setup

The library autolinks receivers/services, but consumers must declare permission intent:

### Android (`AndroidManifest.xml` in your app)

Permissions are merged from the library manifest. On Android 12+, users must grant exact-alarm permission via system settings (the library guides this through `requestPermissions()`).

### iOS (`Info.plist`)

```xml
<key>NSUserNotificationsUsageDescription</key>
<string>This app schedules local alarms and reminders.</string>
```

For critical alerts, apply for the Apple entitlement separately and configure your provisioning profile.

## Compatibility

| Library | React Native | Android min | iOS min |
|---------|--------------|-------------|---------|
| 0.1.x   | 0.76+        | 24          | 15.1    |

Pre-1.0 releases (`0.x`) may include breaking changes in minor versions.

## Quick start

```typescript
import {AlarmKit, isAlarmKitError} from '@noirly-forge/react-native-alarm-kit';

await AlarmKit.requestPermissions();

const alarm = await AlarmKit.scheduleAlarm({
  triggerAtMillis: Date.now() + 60_000,
  title: 'Wake up',
  recurrenceRule: {frequency: 'daily'},
  snoozeConfig: {defaultMinutes: 5, maxSnoozeCount: 3},
});

const unsubscribe = AlarmKit.addListener('onAlarmFired', handle => {
  console.log('Alarm fired', handle.title);
});

// later
await AlarmKit.dismissAlarm(alarm.id);
unsubscribe();
```

See the [example app](./example/App.tsx) for a full working demo.

## API reference

Public exports live in `src/index.ts`. Generated TypeDoc docs are published from CI (see `docs/`).

Core groups:

- **Scheduling:** `scheduleAlarm`, `updateAlarm`, `cancelAlarm`, `cancelAllAlarms`, `getAlarm`, `getAllAlarms`
- **Ringing control:** `snoozeAlarm`, `dismissAlarm`
- **Permissions:** `checkPermissions`, `requestPermissions`
- **Capabilities:** `getCapabilities`
- **Events:** `AlarmKit.addListener(eventName, callback)`

## Core concepts

### Recurrence rules

Use a discriminated object with `frequency`: `"none" | "daily" | "weekly" | "custom"`. Weekly requires `daysOfWeek`; custom requires `intervalDays`.

### Permissions

Check `PermissionStatus` before scheduling. Android exposes `exactAlarmStatus`; iOS exposes `criticalAlertStatus` when entitled.

### Capability introspection

Call `getCapabilities()` to learn about exact-alarm restrictions, critical-alert support, full-screen ringing, and iOS pending-alarm caps.

### Error handling

All failures reject with typed codes (`E_ALARMKIT_*`). Use `isAlarmKitError()` and `isAlarmKitErrorCode()` for branching.

## Platform behavior differences

| Topic | Android | iOS |
|-------|---------|-----|
| Reboot persistence | `BootReceiver` re-registers alarms | OS persists notifications; library reconciles on foreground |
| Recurrence | Re-armed on each fire | Re-armed on each fire for parity |
| Exact alarms | Requires `SCHEDULE_EXACT_ALARM` (API 31+) | N/A (`supportsExactAlarms` always true) |
| Pending cap | Soft practical limits | Hard 64-notification cap |
| Missed alarms | Catch-up fire on boot | Reconciliation on app active |
| Critical alerts | Not supported | Entitlement-gated |

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md).

## License

MIT — see [LICENSE](./LICENSE).
