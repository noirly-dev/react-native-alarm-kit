import type {TurboModule} from 'react-native';
import type {EventEmitter} from 'react-native/Libraries/Types/CodegenTypes';
import {TurboModuleRegistry} from 'react-native';

export type AlarmState =
  | 'scheduled'
  | 'ringing'
  | 'snoozed'
  | 'dismissed'
  | 'missed_fired';

export type RecurrenceFrequency = 'none' | 'daily' | 'weekly' | 'custom';

export interface RecurrenceRule {
  frequency: RecurrenceFrequency;
  daysOfWeek?: ReadonlyArray<number> | null;
  intervalDays?: number | null;
}

export interface SnoozeConfig {
  defaultMinutes: number;
  maxSnoozeCount: number;
}

export interface AlarmConfig {
  triggerAtMillis: number;
  recurrenceRule?: RecurrenceRule | null;
  title: string;
  payload?: string | null;
  soundRef?: string | null;
  snoozeConfig?: SnoozeConfig | null;
}

export interface AlarmUpdateConfig {
  triggerAtMillis?: number;
  recurrenceRule?: RecurrenceRule | null;
  title?: string;
  payload?: string | null;
  soundRef?: string | null;
  snoozeConfig?: SnoozeConfig | null;
}

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

export type NotificationPermissionStatus =
  | 'granted'
  | 'denied'
  | 'notDetermined'
  | 'provisional';

export type ExactAlarmPermissionStatus = 'granted' | 'denied' | 'notApplicable';

export type CriticalAlertPermissionStatus =
  | 'granted'
  | 'denied'
  | 'notApplicable';

export interface PermissionStatus {
  notificationStatus: NotificationPermissionStatus;
  exactAlarmStatus: ExactAlarmPermissionStatus;
  criticalAlertStatus: CriticalAlertPermissionStatus;
}

export interface PlatformCapabilities {
  supportsExactAlarms: boolean;
  supportsCriticalAlerts: boolean;
  supportsFullScreenRinging: boolean;
  maxPendingAlarms?: number | null;
}

export interface Spec extends TurboModule {
  scheduleAlarm(config: AlarmConfig): Promise<AlarmHandle>;
  updateAlarm(id: string, partialConfig: AlarmUpdateConfig): Promise<AlarmHandle>;
  cancelAlarm(id: string): Promise<void>;
  cancelAllAlarms(): Promise<void>;
  getAlarm(id: string): Promise<AlarmHandle | null>;
  getAllAlarms(): Promise<AlarmHandle[]>;
  snoozeAlarm(id: string, overrideMinutes?: number | null): Promise<void>;
  dismissAlarm(id: string): Promise<void>;
  checkPermissions(): Promise<PermissionStatus>;
  requestPermissions(): Promise<PermissionStatus>;
  getCapabilities(): Promise<PlatformCapabilities>;

  readonly onAlarmFired: EventEmitter<AlarmHandle>;
  readonly onAlarmMissedThenFired: EventEmitter<AlarmHandle>;
  readonly onAlarmsReconciled: EventEmitter<ReadonlyArray<AlarmHandle>>;
  readonly onSnoozed: EventEmitter<AlarmHandle>;
  readonly onDismissed: EventEmitter<AlarmHandle>;
  readonly onPermissionsChanged: EventEmitter<PermissionStatus>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('NativeAlarmKit');
