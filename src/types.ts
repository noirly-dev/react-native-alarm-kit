import type {AlarmHandle, PermissionStatus} from './NativeAlarmKit';

export type {
  AlarmConfig,
  AlarmHandle,
  AlarmState,
  AlarmUpdateConfig,
  CriticalAlertPermissionStatus,
  ExactAlarmPermissionStatus,
  NotificationPermissionStatus,
  PermissionStatus,
  PlatformCapabilities,
  RecurrenceFrequency,
  RecurrenceRule,
  SnoozeConfig,
} from './NativeAlarmKit';

/**
 * Versioned error code taxonomy for programmatic error handling.
 */
export type AlarmKitErrorCode =
  | 'E_ALARMKIT_INVALID_CONFIG'
  | 'E_ALARMKIT_ALARM_NOT_FOUND'
  | 'E_ALARMKIT_PERMISSION_DENIED'
  | 'E_ALARMKIT_PLATFORM_LIMIT_EXCEEDED'
  | 'E_ALARMKIT_SCHEDULING_FAILED'
  | 'E_ALARMKIT_RINGING_STATE_CONFLICT'
  | 'E_ALARMKIT_UNSUPPORTED_OPERATION'
  | 'E_ALARMKIT_INTERNAL';

/**
 * Structured error userInfo attached to rejected promises.
 */
export interface AlarmKitErrorUserInfo {
  code: AlarmKitErrorCode;
  alarmId?: string | null;
  platformErrorCode?: string | null;
}

/**
 * Canonical event names exposed by the library.
 */
export type AlarmKitEventName =
  | 'onAlarmFired'
  | 'onAlarmMissedThenFired'
  | 'onAlarmsReconciled'
  | 'onSnoozed'
  | 'onDismissed'
  | 'onPermissionsChanged';

/**
 * Event payload map for type-safe listener registration.
 */
export interface AlarmKitEventMap {
  onAlarmFired: AlarmHandle;
  onAlarmMissedThenFired: AlarmHandle;
  onAlarmsReconciled: AlarmHandle[];
  onSnoozed: AlarmHandle;
  onDismissed: AlarmHandle;
  onPermissionsChanged: PermissionStatus;
}
