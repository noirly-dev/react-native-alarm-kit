import {expectType} from 'tsd';
import type {
  AlarmHandle,
  AlarmKitEventMap,
  PermissionStatus,
  RecurrenceRule,
} from '../src';

declare const handle: AlarmHandle;

expectType<string>(handle.id);
expectType<number>(handle.triggerAtMillis);
expectType<RecurrenceRule | null | undefined>(handle.recurrenceRule);

declare const permissions: PermissionStatus;
expectType<'granted' | 'denied' | 'notApplicable'>(permissions.exactAlarmStatus);

declare const fired: AlarmKitEventMap['onAlarmFired'];
expectType<AlarmHandle>(fired);

declare const reconciled: AlarmKitEventMap['onAlarmsReconciled'];
expectType<AlarmHandle[]>(reconciled);
