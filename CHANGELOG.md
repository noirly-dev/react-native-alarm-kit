# Changelog

## 0.1.2

- Fix Android New Architecture builds: apply `com.facebook.react` Gradle plugin so codegen JNI is generated
- Fix TurboModule event emission with RN 0.86 protected codegen emitters
- Include `react-native.config.js` in the published package
- Remove incorrect self-referential autolinking config from `react-native.config.js`

## 0.1.1

- Renamed package scope from `@noirly-forge` to `@noirly-dev`
- Updated README with GitHub Packages install and token setup instructions

## 0.1.0

- Initial implementation of TurboModule scheduling API
- Android: AlarmManager scheduling, boot receiver, foreground ringing service
- iOS: UNUserNotificationCenter scheduling, reconciliation, notification delegate bootstrap
- Typed error codes and event stream
