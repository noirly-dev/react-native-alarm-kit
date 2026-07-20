# Section 11: Versioning & Release Strategy

**Library:** `@noirly/react-native-alarm-kit`

---

## 11.1 Semver Policy

Standard SemVer (`MAJOR.MINOR.PATCH`), with the compatibility surface explicitly scoped to what Section 5.4 already committed to:

- **MAJOR** — any breaking change to: the public TS API (Section 1.2), the `AlarmHandle`/`RecurrenceRule`/`PermissionStatus`/`PlatformCapabilities` shapes (Section 5.2) in a non-additive way, the event set/payloads (Section 6.2), the error code taxonomy (Section 7.2 — removing or repurposing a code is breaking; adding one is not), or minimum supported RN/OS versions (Section 9.3).
- **MINOR** — additive, backward-compatible changes: new optional fields on existing types (per Section 5.4's additive-nullable rule), new methods, new error codes, new events, new platform capability flags.
- **PATCH** — bug fixes with no API surface change: scheduling accuracy fixes, permission-flow corrections, native crash fixes, dependency bumps that don't change the public contract.

**Enforcement mechanism, not just policy:** the type-contract tests from Section 10.6 are the automated gate — a PR that breaks `types.test-d.ts` assertions is a strong signal the change is MAJOR, catching accidental breaking changes before a human has to notice them in review.

## 11.2 Changesets-Based Release Flow

**Tool decision: Changesets** (`@changesets/cli`), not manual `npm version` or semantic-release's commit-message-parsing approach. Rationale: Changesets requires an explicit, human-written changeset file per PR describing the change and bump type — this is a better fit than commit-message parsing for a native library where "is this breaking" often requires judgment Section 11.1's rules can't fully automate (e.g., a native-only behavior change might not touch the TS API at all but still be user-facing breaking).

Flow:
1. Contributor adds a changeset file (`.changeset/*.md`) alongside their PR, declaring bump type (major/minor/patch) and a human-readable summary.
2. Merged PRs accumulate changesets on the default branch.
3. A `release.yml` CI workflow (Section 9.4) runs Changesets' "Version Packages" bot-PR flow — opens/updates a standing PR that bundles all pending changesets into a version bump + auto-generated `CHANGELOG.md` entry.
4. Merging that bot PR triggers actual `npm publish` (npm provenance/2FA-protected publish token stored as a CI secret) and a corresponding Git tag + GitHub Release.

This keeps `CHANGELOG.md` (Section 8.2) always accurate and human-readable, generated from the same source of truth used for the version bump — no separate manual changelog-writing step to forget.

## 11.3 Pre-1.0 Considerations

Initial development starts at `0.x.y`. Per SemVer convention, `0.x` releases may contain breaking changes in MINOR bumps rather than requiring a MAJOR bump — this is documented explicitly in `CONTRIBUTING.md`/`README.md` (Section 12) so early adopters have correct expectations. The architecture and contracts defined in Sections 1–10 are the **target v1.0 contract**; `0.x` is the stabilization runway toward them, not a license to design carelessly now.

**Criteria for cutting `1.0.0`** (recorded here as an architectural commitment, not just a release-notes afterthought):
- Full Section 10 test coverage philosophy satisfied (10.7)
- Both platforms' example app E2E suite (10.5) green on CI for at least one stabilization period
- Public API (Section 1.2) and type contract (Section 5) have had no breaking changes for a defined stabilization window
- Section 12 documentation complete (README, API docs, migration guide skeleton)

## 11.4 Native Dependency Version Policy

- **Kotlin/Android**: library declares a minimum supported RN version (New Architecture floor) and minimum `compileSdk`/`minSdk`, bumped only in a MINOR (if additive/non-breaking for existing consumers) or MAJOR (if it drops support for a previously-supported RN/SDK version) release — never silently in a PATCH.
- **Swift/iOS**: same policy, keyed to minimum iOS version and minimum RN version.
- A compatibility table (RN version ↔ library version) is maintained in `README.md` (Section 12), updated as part of the same PR that bumps a minimum — not a separately-remembered task.

## 11.5 Deprecation Policy

Before any MAJOR removal (method, event, error code, type field per 11.1), the item is marked `@deprecated` in TSDoc for at least one MINOR release cycle, with the deprecation notice naming the replacement and the planned removal version — giving consumers a visible, tooling-surfaced (IDE strikethrough) warning window rather than a silent breaking jump.

---

**Status:** Approved
