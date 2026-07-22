# @noirly-dev/react-native-alarm-kit

Local-only native scheduling engine for alarms, tasks, and reminders in React Native apps.

[![CI](https://github.com/noirly-dev/react-native-alarm-kit/actions/workflows/lint-and-typecheck.yml/badge.svg)](https://github.com/noirly-dev/react-native-alarm-kit/actions)

## Why this library

Headless scheduling layer — no UI components, no cloud sync, no remote push. The library guarantees native trigger delivery (including after app kill and device reboot) and exposes ringing hooks; your app owns the visual experience.

**In scope:** exact-time alarms, recurrence, snooze/dismiss, CRUD, permissions, capability introspection, boot persistence (Android), notification reconciliation (iOS).

**Out of scope:** alarm UI, ringtone asset management, cloud sync, remote push.

## Requirements

- React Native CLI (no Expo)
- New Architecture enabled
- React Native 0.76+

| Library | React Native | Android min | iOS min |
|---------|--------------|-------------|---------|
| 0.1.x   | 0.76+        | 24          | 15.1    |

Pre-1.0 releases (`0.x`) may include breaking changes in minor versions.

## Installation

This package is published to [GitHub Packages](https://github.com/noirly-dev/react-native-alarm-kit/packages) under the `@noirly-dev` scope — not npmjs.com.

### 1. Create a GitHub personal access token

1. Go to [GitHub → Settings → Developer settings → Personal access tokens](https://github.com/settings/tokens).
2. Generate a **classic** token (or a fine-grained token with package read access).
3. Enable **`read:packages`**. If the repository is private, also enable **`repo`**.
4. Copy the token — you will not be able to see it again.

### 2. Configure npm for GitHub Packages

Copy [`.npmrc.example`](./.npmrc.example) to `.npmrc` in your app root (not needed when developing this library repo itself):

```ini
@noirly-dev:registry=https://npm.pkg.github.com
//npm.pkg.github.com/:_authToken=${GITHUB_TOKEN}
```

Then set the token in your shell before installing:

```bash
# macOS / Linux
export GITHUB_TOKEN=ghp_your_token_here

# Windows PowerShell
$env:GITHUB_TOKEN = "ghp_your_token_here"
```

Alternatively, replace `${GITHUB_TOKEN}` with the token directly in `.npmrc` for local use only — **do not commit a raw token**.

### 3. Install the package

```bash
npm install @noirly-dev/react-native-alarm-kit@0.1.2
# or
yarn add @noirly-dev/react-native-alarm-kit@0.1.2
```

Autolinking handles native setup on both platforms. Run `pod install` in your iOS project after installing.

## Required native setup

The library autolinks receivers and services, but consumers must declare permission intent.

### Android

Permissions are merged from the library manifest. On Android 12+, users must grant exact-alarm permission via system settings. The library guides this through `requestPermissions()`.

### iOS (`Info.plist`)

```xml
<key>NSUserNotificationsUsageDescription</key>
<string>This app schedules local alarms and reminders.</string>
```

For critical alerts, apply for the Apple entitlement separately and configure your provisioning profile.

## Quick start

```typescript
import {AlarmKit, isAlarmKitError} from '@noirly-dev/react-native-alarm-kit';

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

Public exports live in `src/index.ts`.

| Group | Methods |
|-------|---------|
| Scheduling | `scheduleAlarm`, `updateAlarm`, `cancelAlarm`, `cancelAllAlarms`, `getAlarm`, `getAllAlarms` |
| Ringing control | `snoozeAlarm`, `dismissAlarm` |
| Permissions | `checkPermissions`, `requestPermissions` |
| Capabilities | `getCapabilities` |
| Events | `AlarmKit.addListener(eventName, callback)` |

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

## Local development

```bash
yarn install
yarn bootstrap:example
yarn example start
yarn example android   # or ios
```

See [CONTRIBUTING.md](./CONTRIBUTING.md) for the full workflow.

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md).

## License

MIT — see [LICENSE](./LICENSE).
