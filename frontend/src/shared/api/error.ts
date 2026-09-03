import type { components } from '@/shared/api/generated/planning-api';

type FieldError = components['schemas']['FieldError'];
type ConflictView = components['schemas']['ConflictView'];
type ScheduleView = components['schemas']['ScheduleView'];

export type SafeApiErrorKind =
  'validation' | 'not-found' | 'conflict' | 'stale' | 'rate-limited' | 'network' | 'unknown';

export interface SafeApiError {
  readonly kind: SafeApiErrorKind;
  readonly code: string;
  readonly message: string;
  readonly requestId: string;
  readonly fieldErrors: readonly FieldError[];
  readonly currentVersion?: number;
  readonly conflict?: ConflictView;
  readonly retryAfterSeconds?: number;
}

const FIELD_ALLOWLIST = new Set([
  'title',
  'description',
  'priority',
  'estimateMinutes',
  'dueDate',
  'date',
  'startTime',
  'expectedVersion',
  'completed',
  'confirmed',
  'resolutionMode',
  'page',
  'size',
  'status',
  'scheduled',
  'sort',
  'direction',
]);

const REQUEST_ID = /^[A-Za-z0-9._-]{8,64}$/;

const VALIDATION_CODES = new Set([
  'VALIDATION_FAILED',
  'MALFORMED_REQUEST',
  'DELETION_NOT_CONFIRMED',
  'TASK_TITLE_INVALID',
  'TASK_DESCRIPTION_TOO_LONG',
  'TASK_ESTIMATE_INVALID',
  'TASK_TIME_INVALID',
  'SCHEDULE_ALIGNMENT_INVALID',
  'SCHEDULE_OUT_OF_WINDOW',
  'SCHEDULE_DURATION_INVALID',
  'WEEK_START_INVALID',
  'PAGE_INVALID',
  'PAGE_SIZE_INVALID',
  'BACKLOG_LIMIT_INVALID',
]);

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null && !Array.isArray(value);

const safeRequestId = (value: unknown, fallback: string): string =>
  typeof value === 'string' && REQUEST_ID.test(value) ? value : fallback;

const safeMessage = (value: unknown, fallback: string): string =>
  typeof value === 'string' && value.length > 0 && value.length <= 500 ? value : fallback;

const safeFieldErrors = (value: unknown): readonly FieldError[] => {
  if (!Array.isArray(value)) return [];
  return value.flatMap((item) => {
    if (
      !isRecord(item) ||
      typeof item.field !== 'string' ||
      !FIELD_ALLOWLIST.has(item.field) ||
      typeof item.code !== 'string'
    ) {
      return [];
    }
    return [
      {
        field: item.field,
        code: item.code,
        ...(typeof item.message === 'string' ? { message: item.message } : {}),
      },
    ];
  });
};

const safeSchedule = (value: unknown): ScheduleView | undefined => {
  if (
    !isRecord(value) ||
    typeof value.date !== 'string' ||
    typeof value.startTime !== 'string' ||
    typeof value.endTime !== 'string'
  ) {
    return undefined;
  }
  return { date: value.date, startTime: value.startTime, endTime: value.endTime };
};

const safeConflict = (value: unknown): ConflictView | undefined => {
  if (!isRecord(value)) return undefined;
  const proposed = safeSchedule(value.proposed);
  const conflicting = safeSchedule(value.conflicting);
  const nextCandidate = safeSchedule(value.nextCandidate);
  if (proposed === undefined || conflicting === undefined) return undefined;
  if (value.nextCandidate !== undefined && nextCandidate === undefined) return undefined;
  return {
    proposed,
    conflicting,
    ...(nextCandidate === undefined ? {} : { nextCandidate }),
  };
};

const kindFor = (status: number, code: string): SafeApiErrorKind => {
  if (status === 400 && VALIDATION_CODES.has(code)) return 'validation';
  if (status === 404 && code === 'TASK_NOT_FOUND') return 'not-found';
  if (status === 409 && code === 'SCHEDULE_CONFLICT') return 'conflict';
  if (status === 409 && code === 'STALE_TASK') return 'stale';
  if (status === 429 && code === 'RATE_LIMITED') return 'rate-limited';
  return 'unknown';
};

/** F-N05, SECURITY-05: reduces every failure to the seven-field-safe UI error model. */
export const normalizeApiError = async (
  input: unknown,
  fallbackRequestId = 'TMP-UNKNOWN-0000',
): Promise<SafeApiError> => {
  if (!(input instanceof Response)) {
    return {
      kind: 'network',
      code: 'NETWORK_ERROR',
      message: '백엔드에 연결할 수 없습니다.',
      requestId: fallbackRequestId,
      fieldErrors: [],
    };
  }

  let body: unknown;
  try {
    body = await input.json();
  } catch {
    body = undefined;
  }
  const payload = isRecord(body) ? body : {};
  const code = typeof payload.code === 'string' ? payload.code : 'UNKNOWN';
  const requestId = safeRequestId(
    payload.requestId,
    safeRequestId(input.headers.get('X-Request-Id'), fallbackRequestId),
  );
  const currentVersion =
    Number.isSafeInteger(payload.currentVersion) && typeof payload.currentVersion === 'number'
      ? payload.currentVersion
      : undefined;
  const conflict = safeConflict(payload.conflict);
  const retryHeader = input.headers.get('Retry-After');
  const retryAfterSeconds =
    retryHeader !== null && /^\d+$/.test(retryHeader) ? Number(retryHeader) : undefined;

  return {
    kind: kindFor(input.status, code),
    code,
    message: safeMessage(payload.message, '요청을 처리할 수 없습니다.'),
    requestId,
    fieldErrors: safeFieldErrors(payload.fieldErrors),
    ...(currentVersion === undefined ? {} : { currentVersion }),
    ...(conflict === undefined ? {} : { conflict }),
    ...(retryAfterSeconds === undefined ? {} : { retryAfterSeconds }),
  };
};
