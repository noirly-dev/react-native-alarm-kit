import {AlarmKit, isAlarmKitError, isAlarmKitErrorCode, ALARMKIT_ERROR_CODES} from '../src';

describe('errors', () => {
  it('recognizes AlarmKit errors by code', () => {
    const error = Object.assign(new Error('denied'), {
      code: 'E_ALARMKIT_PERMISSION_DENIED',
    });
    expect(isAlarmKitError(error)).toBe(true);
    expect(isAlarmKitErrorCode(error, 'E_ALARMKIT_PERMISSION_DENIED')).toBe(true);
  });

  it('exports all documented error codes', () => {
    expect(ALARMKIT_ERROR_CODES).toContain('E_ALARMKIT_INVALID_CONFIG');
    expect(ALARMKIT_ERROR_CODES).toHaveLength(8);
  });
});

describe('AlarmKit wrapper', () => {
  it('exposes scheduling methods', () => {
    expect(typeof AlarmKit.scheduleAlarm).toBe('function');
    expect(typeof AlarmKit.addListener).toBe('function');
  });
});
