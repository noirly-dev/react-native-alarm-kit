# Contributing

Thanks for helping improve `@noirly-forge/react-native-alarm-kit`.

## Local development

```bash
yarn install
yarn bootstrap:example   # creates example/android + example/ios if missing
yarn example start
yarn example android     # or ios
```

If native changes are not picked up, clean rebuild:

```bash
cd example/android && ./gradlew clean
cd example/ios && pod install
```

## Repository layout

- `src/` — TypeScript public API and TurboModule spec
- `android/` — Kotlin native module (scheduler, repository, receivers, service)
- `ios/` — Swift native module (scheduler, repository, delegate, coordinator)
- `example/` — RN CLI demo app linked via workspaces

## Tests

```bash
yarn test
yarn typescript
yarn lint
```

Coverage expectations:

- Every `E_ALARMKIT_*` error code must have at least one test
- Every TurboModule method needs JS + native test coverage
- Every event needs an E2E assertion (Maestro flows in `example/`)

## Changesets

Add a changeset for user-facing changes:

```bash
yarn changeset
```

Choose patch/minor/major using semver rules in the architecture docs. Native-only behavior changes may still be user-facing breaking — use judgment.

## Code style

- TypeScript: ESLint + Prettier
- Kotlin: ktlint
- Swift: SwiftLint

Public exports in `src/` require TSDoc comments.
