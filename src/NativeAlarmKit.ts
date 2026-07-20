import type {TurboModule} from 'react-native';
import type {EventEmitter} from 'react-native/Libraries/Types/CodegenTypes';
import {TurboModuleRegistry} from 'react-native';

import type {
  AlarmConfig,
  AlarmHandle,
  AlarmUpdateConfig,
  PermissionStatus,
  PlatformCapabilities,
} from './types';

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
