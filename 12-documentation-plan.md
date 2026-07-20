# Section 12: Documentation Plan

**Library:** `@noirly/react-native-alarm-kit`

---

## 12.1 Documentation Philosophy

Documentation is treated as part of the public contract, not an afterthought — several earlier sections already deferred specific commitments here (Section 9.5's clean-build guidance, Section 11.3/11.4's compatibility table, Section 5's type contract). This section collects and finalizes those, plus defines the overall doc structure.

## 12.2 README.md Structure

1. **Header** — name, one-line purpose, badges (CI status, npm version, license).
2. **Why this library** — scope statement lifted directly from Section 1.1 (local-only, headless, in/out-of-scope list) so a prospective consumer can self-select out quickly if they need remote/push or UI components.
3. **Installation** — `npm install @noirly/react-native-alarm-kit`, autolinking note (zero manual native edits, per Sections 3.5/9.2), CocoaPods `pod install` reminder.
4. **Required native setup** — despite the zero-manual-edit design goal, permission-related manifest/Info.plist entries the *consumer* must still declare themselves are documented explicitly here (e.g., `POST_NOTIFICATIONS` usage description, `NSUserNotificationsUsageDescription`-equivalent, critical-alert entitlement request process if the consumer wants that capability) — the library cannot autolink permission *declarations* that express consumer intent.
5. **Compatibility table** — RN version ↔ library version ↔ min OS versions (Section 11.4 commitment lands here).
6. **Quick start** — minimal schedule/listen/cancel example, using the actual public API from Section 1.2/6.5.
7. **API Reference** — link out to generated API docs (12.4), not inlined in full in the README.
8. **Core concepts** — short explainer sections for: recurrence rules, permission model, capability introspection, error handling — each cross-referencing the deeper architecture doc sections conceptually (without exposing this internal architecture doc set to end users).
9. **Platform behavior differences** — an explicit table surfacing the Section 5 platform-divergence points that matter to app developers (e.g., iOS 64-alarm cap, Android exact-alarm permission flow) — this is the single most important trust-building doc section for a native library, since silent platform divergence is the top source of native-library bug reports.
10. **Contributing** — link to `CONTRIBUTING.md`.
11. **License** — link to `LICENSE`.

## 12.3 CONTRIBUTING.md

- Local dev loop instructions (Section 9.5), including the clean-build guidance flagged as deferred there.
- Repo structure orientation, linking conceptually to Section 8.2's layout so a new contributor can navigate by responsibility folder.
- Test-writing expectations (Section 10.7's coverage philosophy stated in human terms — "every error code needs a test," "every event needs an E2E assertion").
- Changeset-writing instructions (Section 11.2) — how to add a `.changeset` file, how bump-type judgment calls should be made for native-only behavior changes.
- Code style/lint requirements (referencing the `.eslintrc`/`ktlint`/SwiftLint configs from Section 8.2/9.4).

## 12.4 Generated API Docs

- **Tool decision: TypeDoc**, run against `src/` (the hand-authored + Codegen-generated types), published to GitHub Pages via a CI step. Chosen over hand-maintained API markdown because the public surface (Section 1.2/5.2) is already strongly typed — TypeDoc keeps docs mechanically in sync with the actual type contract, directly reinforcing Section 10.6's type-contract-test philosophy (types and docs can't silently drift apart).
- TSDoc comments are a **required** part of any PR touching `src/` public exports — enforced via an ESLint rule (`eslint-plugin-tsdoc` or equivalent), not just a style convention, so `12.4`'s generated docs stay complete rather than accumulating undocumented exports.

## 12.5 Migration Guides

- A `docs/migrations/` folder (per Section 8.2's `docs/` entry), with one file per MAJOR version bump, created as part of the same PR that introduces a MAJOR changeset (Section 11.2) — never written retroactively after release.
- Each migration guide follows a fixed template: what changed, why (linking the architectural rationale, e.g., "Section 5.4 additive-only rule was violated because X"), before/after code snippets, and a checklist of consumer-side steps.

## 12.6 Example App as Living Documentation

Per Section 8.3's design intent, `example/` is not just a test harness — it is referenced directly from the README's Quick Start section as "see the full working example here," and its source is written with documentation-quality comments (not just functional test code), since it's the fastest way for a new consumer to see every public method/event in realistic context (Section 10.5's E2E flows double as documented usage patterns).

## 12.7 Documentation Definition of Done (Ties to Section 11.3's 1.0 Criteria)

The `1.0.0` release gate from Section 11.3 explicitly requires:
- README sections 12.2.1–12.2.9 complete and accurate against the actual shipped API
- Zero undocumented public exports (enforced per 12.4)
- At least one populated `docs/migrations/` entry is not required pre-1.0 (no MAJOR bump exists yet), but the folder structure and template must exist and be validated against a dry-run
- Platform-behavior-differences table (12.2.9) reviewed against the final Section 5 contract for accuracy

---

**Status:** Pending confirmation

---

## Architecture Document Set — Complete

All 12 sections are approved-pending-this-final-confirmation and saved as individual `.md` files in `noirly-alarm-kit-architecture/`. Once you confirm this section, the full architecture for `@noirly/react-native-alarm-kit` is locked and ready to move into implementation planning whenever you choose to start a new project or request code.
