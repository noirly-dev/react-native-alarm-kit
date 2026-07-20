import type {AlarmKitErrorCode, AlarmKitErrorUserInfo} from './types';

/** All known error codes for exhaustiveness checks. */
export const ALARMKIT_ERROR_CODES: readonly AlarmKitErrorCode[] = [
  'E_ALARMKIT_INVALID_CONFIG',
  'E_ALARMKIT_ALARM_NOT_FOUND',
  'E_ALARMKIT_PERMISSION_DENIED',
  'E_ALARMKIT_PLATFORM_LIMIT_EXCEEDED',
  'E_ALARMKIT_SCHEDULING_FAILED',
  'E_ALARMKIT_RINGING_STATE_CONFLICT',
  'E_ALARMKIT_UNSUPPORTED_OPERATION',
  'E_ALARMKIT_INTERNAL',
] as const;

/**
 * Typed error shape surfaced to JS consumers via Promise rejection.
 */
export interface AlarmKitError extends Error {
  code: AlarmKitErrorCode;
  userInfo?: AlarmKitErrorUserInfo;
}

/**
 * Type guard for distinguishing AlarmKit errors from generic failures.
 */
export function isAlarmKitError(error: unknown): error is AlarmKitError {
  if (typeof error !== 'object' || error === null) {
    return false;
  }
  const candidate = error as Partial<AlarmKitError>;
  return (
    typeof candidate.code === 'string' &&
    ALARMKIT_ERROR_CODES.includes(candidate.code as AlarmKitErrorCode)
  );
}

/**
 * Narrows a caught error to a specific AlarmKit error code.
 */
export function isAlarmKitErrorCode<C extends AlarmKitErrorCode>(
  error: unknown,
  code: C,
): error is AlarmKitError & {code: C} {
  return isAlarmKitError(error) && error.code === code;
}
