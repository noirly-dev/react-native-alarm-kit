# Section 9: Build & Codegen Pipeline

**Library:** `@noirly/react-native-alarm-kit`

---

## 9.1 Codegen Configuration

- `codegenConfig` in `package.json` declares:
  - `name`: `NativeAlarmKitSpec` (the generated spec name, distinct from the module name `NativeAlarmKit` to follow RN codegen convention of `Native<Module>Spec`).
  - `type`: `modules` (this library has no Fabric component/view, only a TurboModule — no `components` entry needed, per Section 1's headless-library scope).
  - `jsSrcsDir`: `src` — Codegen scans `src/NativeAlarmKit.ts` (Section 8.2) as the sole spec source.
  - `android.javaPackageName`: `com.noirly.alarmkit` — generated Java/Kotlin interfaces land in this package, matching the native folder layout from Section 8.2.
- Codegen runs at **consumer build time** (standard RN CLI New Architecture behavior — generated code is not committed to the library's own repo, consistent with the "no build output committed" rule in Section 8.3), invoked automatically by the RN CLI's autolinking + codegen Gradle/CocoaPods hooks. The library does not ship a custom codegen invocation script; it relies entirely on RN CLI's standard mechanism to keep consumer setup at zero extra steps.
- A `scripts/generate-codegen-artifacts.sh` (or npm script `codegen:check`) exists **for library-internal CI/dev use only** — runs codegen against the `example/` app to validate the spec compiles cleanly on both platforms before every native code change is trusted, but this is a dev-loop tool, not something consumers ever invoke.

## 9.2 Autolinking

- Standard RN CLI autolinking via `react-native.config.js` at the repo root — declares platform roots (`android`, `ios`) explicitly rather than relying on default inference, to keep the config self-documenting for contributors.
- **Android:** `android/build.gradle` is a standard RN library module gradle file — no custom Gradle plugin required. `AndroidManifest.xml`'s receiver/service declarations (Section 3.5/8.3) are picked up automatically via Gradle manifest merging once autolinking includes the module — zero manual `MainApplication.kt`/`settings.gradle` edits for consumers.
- **iOS:** `NoirlyAlarmKit.podspec` declares the library as a standard CocoaPods pod, autolinked via RN CLI's `react-native.config.js` + standard `use_native_modules!` in the consumer's `Podfile` (unchanged, no consumer edits required). The Section 4.5 notification-delegate registration-timing concern is resolved here: the podspec ships a `+load()`-based Swift/ObjC registration (via a small `AlarmKitBootstrap` class using Objective-C `+load` or Swift's equivalent static-init pattern) rather than requiring an `AppDelegate` edit — preserving the zero-manual-native-edit promise symmetric with Android.

## 9.3 Native Build Toolchain

- **Android:** Kotlin via the standard Android Gradle Plugin, target/min SDK aligned to current RN New Architecture minimums (verified against the RN version matrix at implementation time, not hardcoded here since RN's minimums shift release to release). `AlarmRepository`'s embedded store (Sections 3.1/4.1) uses **Room** (not raw SQLite) — chosen over raw SQLite for compile-time query verification and migration tooling, which matters for a library that must gracefully evolve `AlarmHandle`'s schema (Section 5.4) across versions without corrupting already-scheduled user alarms.
- **iOS:** Swift via standard Xcode toolchain / CocoaPods, minimum iOS version pinned to whatever the current RN New Architecture floor requires. `AlarmRepository`'s embedded store uses **a lightweight `Codable`-backed flat-file store or Core Data**, decided in favor of **Core Data** for the same migration-safety reasoning as Android's Room choice — cross-platform symmetry in *capability* (safe schema evolution), not identical technology.

## 9.4 CI Pipeline (GitHub Actions, per `.github/workflows/` in Section 8.2)

Four required workflows, run on every PR:
1. **`lint-and-typecheck.yml`** — ESLint + TypeScript `tsc --noEmit` on `src/`, plus `ktlint`/SwiftLint on native code.
2. **`android-build.yml`** — Builds `example/android` in both Old-disabled/New-Architecture-enabled configuration (New Arch is the only supported mode per the standing rules, but the build is still validated end-to-end against a real Gradle assemble, not just a syntax check) and runs Kotlin unit tests (Section 10).
3. **`ios-build.yml`** — Builds `example/ios` via `xcodebuild`, runs Swift unit tests (Section 10).
4. **`codegen-verify.yml`** — Runs the `codegen:check` script (9.1) to catch spec/type drift before it reaches native build steps, fast-failing before the slower native builds run.

A fifth workflow, **`release.yml`**, is gated separately (manual trigger or tag-push only) and covers npm publish — detailed in Section 11, not duplicated here.

## 9.5 Local Developer Loop

- `yarn example android` / `yarn example ios` (root-level scripts, per Section 8's workspace setup) build and launch the example app against live `src/`/native source — no manual `pod install`/link step beyond what CocoaPods itself requires on native dependency changes.
- Native code changes require `yarn example ios --clean`-equivalent guidance documented in `CONTRIBUTING.md` (Section 12) for cases where CocoaPods/Gradle caching masks a native change — this is a documentation commitment, not a pipeline architecture item, flagged here so Section 12 doesn't drop it.

---

**Status:** Approved
