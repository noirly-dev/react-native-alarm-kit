/**
 * Canonical alarm state as persisted and returned by native.
 */
export type AlarmState =
  | 'scheduled'
  | 'ringing'
  | 'snoozed'
  | 'dismissed'
  | 'missed_fired';

/**
 * Recurrence frequency discriminant.
 */
export type RecurrenceFrequency = 'none' | 'daily' | 'weekly' | 'custom';

/**
 * Discriminated flat recurrence rule (Codegen-safe).
 */
export interface RecurrenceRule {
  frequency: RecurrenceFrequency;
  daysOfWeek?: number[] | null;
  intervalDays?: number | null;
}

/**
 * Default snooze behavior for an alarm.
 */
export interface SnoozeConfig {
  defaultMinutes: number;
  maxSnoozeCount: number;
}

/**
 * Input config for scheduling a new alarm.
 */
export interface AlarmConfig {
  triggerAtMillis: number;
  recurrenceRule?: RecurrenceRule | null;
  title: string;
  payload?: string | null;
  soundRef?: string | null;
  snoozeConfig?: SnoozeConfig | null;
}

/**
 * Partial config for updating an existing alarm.
 */
export interface AlarmUpdateConfig {
  triggerAtMillis?: number;
  recurrenceRule?: RecurrenceRule | null;
  title?: string;
  payload?: string | null;
  soundRef?: string | null;
  snoozeConfig?: SnoozeConfig | null;
}

/**
 * Canonical representation of a scheduled alarm.
 */
export interface AlarmHandle {
  id: string;
  triggerAtMillis: number;
  recurrenceRule?: RecurrenceRule | null;
  title: string;
  payload?: string | null;
  soundRef?: string | null;
  snoozeConfig?: SnoozeConfig | null;
  state: AlarmState;
  createdAtMillis: number;
  updatedAtMillis: number;
}

/**
 * Notification permission status.
 */
export type NotificationPermissionStatus =
  | 'granted'
  | 'denied'
  | 'notDetermined'
  | 'provisional';

/**
 * Exact-alarm permission status (Android 12+).
 */
export type ExactAlarmPermissionStatus = 'granted' | 'denied' | 'notApplicable';

/**
 * Critical-alert permission status (iOS entitlement).
 */
export type CriticalAlertPermissionStatus =
  | 'granted'
  | 'denied'
  | 'notApplicable';

/**
 * Combined permission status across platforms.
 */
export interface PermissionStatus {
  notificationStatus: NotificationPermissionStatus;
  exactAlarmStatus: ExactAlarmPermissionStatus;
  criticalAlertStatus: CriticalAlertPermissionStatus;
}

/**
 * Platform capability introspection result.
 */
export interface PlatformCapabilities {
  supportsExactAlarms: boolean;
  supportsCriticalAlerts: boolean;
  supportsFullScreenRinging: boolean;
  maxPendingAlarms?: number | null;
}

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
