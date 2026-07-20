# Section 8: Package/Repo Structure

**Library:** `@noirly/react-native-alarm-kit`

---

## 8.1 Repository Shape Decision

**Single-package repo with an internal example app**, not a monorepo with multiple published packages. Rationale: this library has one cohesive TurboModule surface (Section 2.1 decision), no sub-packages to independently version, and a monorepo (Lerna/Nx/Turborepo-managed multi-package workspace) would be unjustified complexity for a v1 single-purpose library. The example app lives in-repo as a workspace member (via npm/yarn workspaces), not a separately published package.

## 8.2 Top-Level Layout

```
@noirly-react-native-alarm-kit/
├── src/                        # TypeScript public API (Section 1.2 / Section 6.5 wrappers)
│   ├── index.ts                 # Public entry point — re-exports only, no logic
│   ├── NativeAlarmKit.ts        # Codegen TurboModule spec (extends TurboModule)
│   ├── AlarmKit.ts               # Thin JS convenience layer (listener wrapper, error type guard)
│   ├── types.ts                  # Hand-authored TS types re-exported alongside generated ones
│   └── errors.ts                 # Error code enum + type guard (Section 7.5)
├── android/
│   ├── build.gradle
│   ├── src/main/java/com/noirly/alarmkit/
│   │   ├── NativeAlarmKitModule.kt
│   │   ├── NativeAlarmKitPackage.kt
│   │   ├── scheduler/AlarmScheduler.kt
│   │   ├── repository/AlarmRepository.kt
│   │   ├── receivers/AlarmReceiver.kt
│   │   ├── receivers/BootReceiver.kt
│   │   ├── service/AlarmRingingService.kt
│   │   ├── permissions/PermissionManager.kt
│   │   ├── capabilities/CapabilityProvider.kt
│   │   └── events/AlarmEventEmitter.kt
│   └── src/main/AndroidManifest.xml   # Declares receivers/service (Section 3.5)
├── ios/
│   ├── NoirlyAlarmKit.podspec
│   ├── NativeAlarmKitModule.swift
│   ├── Scheduler/AlarmScheduler.swift
│   ├── Repository/AlarmRepository.swift
│   ├── Delegate/AlarmNotificationDelegate.swift
│   ├── Ringing/AlarmRingingCoordinator.swift
│   ├── Permissions/PermissionManager.swift
│   ├── Capabilities/CapabilityProvider.swift
│   ├── Events/AlarmEventEmitter.swift
│   └── Lifecycle/ReactAppLifecycleFacade.swift
├── example/                      # RN CLI app, workspace member, consumes library via workspace link
│   ├── android/
│   ├── ios/
│   ├── src/
│   ├── package.json
│   └── metro.config.js            # Configured to resolve library from ../src, not node_modules copy
├── android-example-manifest-notes.md   # (if needed) documents any consumer-side manifest notes
├── nitrogen/ or codegen/          # Codegen output directory conventions — finalized in Section 9
├── docs/                          # Section 12 documentation plan output lives here
├── __tests__/                     # JS unit tests (Section 10)
├── android/src/test + androidTest # Kotlin unit + instrumented tests (Section 10)
├── ios/Tests/                     # Swift unit tests (Section 10)
├── .github/workflows/             # CI pipelines (Section 9)
├── package.json
├── tsconfig.json
├── react-native.config.js         # Autolinking config, codegen config pointer
├── babel.config.js
├── .eslintrc.js / eslint.config.js
├── .prettierrc
├── LICENSE                        # Section 12 — open-source licensing
├── CONTRIBUTING.md
├── CODE_OF_CONDUCT.md
├── CHANGELOG.md                   # Section 11 — versioning
└── README.md
```

## 8.3 Key Structural Decisions

- **`src/index.ts` is a pure re-export barrel** — no logic lives at the entry point, keeping tree-shaking clean and making the public surface auditable at a glance (matches Section 1.2's intent of a deliberately curated API).
- **Native code is organized by responsibility folder** (`scheduler/`, `repository/`, `receivers/`, etc.) mirroring the class tables in Sections 3.1/4.1 exactly — a contributor reading Section 3/4 of this architecture doc can navigate the repo without a mapping step.
- **`example/` is a real RN CLI app**, not a minimal smoke-test shell — it must exercise every public API method and event (used later for manual QA and as the target for E2E tests, Section 10) and serves as living documentation (Section 12 links to it).
- **Workspace-based linking** (npm/yarn workspaces at the repo root) ensures `example/` always builds against the current `src/`/`android/`/`ios/` — not a stale `npm install`ed copy — critical for a native library where native code changes need immediate example-app verification.
- **`android/AndroidManifest.xml` at the library level** carries the receiver/service declarations (Section 3.5) — Gradle's manifest merger handles combining this into the consuming app's final manifest automatically; no separate "consumer setup" manifest step needed.
- **No `lib/` or `dist/` committed to source control** — build output is generated at publish time only (Section 9), keeping the repo diff-clean and avoiding stale-build-artifact bugs.

## 8.4 Package.json Shape (Conceptual, Not Code)

Key fields to note as architectural commitments (full file content is implementation, not architecture):
- `main`/`module`/`types` point to built output (`lib/commonjs`, `lib/module`, `lib/typescript`), never `src/` directly, so consumers get compiled JS + declaration files, not raw TS requiring their own transpilation config for this package.
- `react-native` field points to `src/index.ts` — enables Metro to resolve source directly in monorepo/workspace dev scenarios (like this repo's own `example/` app) while published consumers still get the compiled `main` entry.
- `codegenConfig` block declares the TurboModule spec name/path (New Architecture requirement) — Section 9 finalizes exact values.
- `peerDependencies` on `react` and `react-native` (never `dependencies`) — standard for RN libraries, avoids duplicate RN copies in consumer node_modules.

---

**Status:** Approved
