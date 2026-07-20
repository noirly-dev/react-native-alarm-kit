import type {EventSubscription} from 'react-native';

import NativeAlarmKit from './NativeAlarmKit';
import type {AlarmKitEventMap, AlarmKitEventName} from './types';

/**
 * Thin JS convenience layer over the generated TurboModule.
 * Provides typed listener helpers and re-exports scheduling APIs.
 */
class AlarmKitImpl {
  /**
   * Subscribe to a native event with full payload type inference.
   *
   * @returns Unsubscribe function — call to remove the listener.
   */
  addListener<E extends AlarmKitEventName>(
    eventName: E,
    callback: (payload: AlarmKitEventMap[E]) => void,
  ): () => void {
    const emitter = NativeAlarmKit[eventName] as {
      (listener: (payload: AlarmKitEventMap[E]) => void): EventSubscription;
    };
    const subscription = emitter(callback);
    return () => subscription.remove();
  }

  scheduleAlarm = NativeAlarmKit.scheduleAlarm.bind(NativeAlarmKit);
  updateAlarm = NativeAlarmKit.updateAlarm.bind(NativeAlarmKit);
  cancelAlarm = NativeAlarmKit.cancelAlarm.bind(NativeAlarmKit);
  cancelAllAlarms = NativeAlarmKit.cancelAllAlarms.bind(NativeAlarmKit);
  getAlarm = NativeAlarmKit.getAlarm.bind(NativeAlarmKit);
  getAllAlarms = NativeAlarmKit.getAllAlarms.bind(NativeAlarmKit);
  snoozeAlarm = NativeAlarmKit.snoozeAlarm.bind(NativeAlarmKit);
  dismissAlarm = NativeAlarmKit.dismissAlarm.bind(NativeAlarmKit);
  checkPermissions = NativeAlarmKit.checkPermissions.bind(NativeAlarmKit);
  requestPermissions = NativeAlarmKit.requestPermissions.bind(NativeAlarmKit);
  getCapabilities = NativeAlarmKit.getCapabilities.bind(NativeAlarmKit);
}

/** Public singleton instance. */
export const AlarmKit = new AlarmKitImpl();
