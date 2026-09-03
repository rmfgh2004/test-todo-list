import type { operations } from '@/shared/api/generated/planning-api';

type ContractTaskListQuery = NonNullable<operations['listTasks']['parameters']['query']>;

export type NormalizedTaskListQuery = ContractTaskListQuery & {
  readonly sort: NonNullable<ContractTaskListQuery['sort']>;
  readonly direction: NonNullable<ContractTaskListQuery['direction']>;
  readonly page: number;
  readonly size: number;
};

const STATUS = ['TODO', 'COMPLETED'] as const;
const PRIORITY = ['LOW', 'MEDIUM', 'HIGH'] as const;
const SORT = ['CREATED_AT', 'DUE_DATE', 'PRIORITY', 'TITLE'] as const;
const DIRECTION = ['ASC', 'DESC'] as const;

const oneOf = <T extends string>(value: string | null, values: readonly T[]): T | undefined =>
  value === null ? undefined : values.find((candidate) => candidate === value);

const boundedInteger = (
  value: string | null,
  minimum: number,
  maximum: number,
  fallback: number,
): number => {
  if (value === null || !/^\d+$/.test(value)) return fallback;
  const parsed = Number(value);
  return Number.isSafeInteger(parsed) && parsed >= minimum && parsed <= maximum ? parsed : fallback;
};

/** FR-010, SECURITY-05: parses only allowlisted contract query keys and bounded values. */
export const parseTaskListQuery = (params: URLSearchParams): NormalizedTaskListQuery => {
  const status = oneOf(params.get('status'), STATUS);
  const priority = oneOf(params.get('priority'), PRIORITY);
  const sort = oneOf(params.get('sort'), SORT) ?? 'CREATED_AT';
  const direction = oneOf(params.get('direction'), DIRECTION) ?? 'DESC';
  const scheduledText = params.get('scheduled');
  const scheduled = scheduledText === 'true' ? true : scheduledText === 'false' ? false : undefined;

  return {
    ...(status === undefined ? {} : { status }),
    ...(scheduled === undefined ? {} : { scheduled }),
    ...(priority === undefined ? {} : { priority }),
    sort,
    direction,
    page: boundedInteger(params.get('page'), 0, Number.MAX_SAFE_INTEGER, 0),
    size: boundedInteger(params.get('size'), 1, 100, 25),
  };
};

/** FR-010, SECURITY-05: serializes query fields in a stable allowlisted order. */
export const serializeTaskListQuery = (query: NormalizedTaskListQuery): string => {
  const params = new URLSearchParams();
  if (query.status !== undefined) params.set('status', query.status);
  if (query.scheduled !== undefined) params.set('scheduled', String(query.scheduled));
  if (query.priority !== undefined) params.set('priority', query.priority);
  params.set('sort', query.sort);
  params.set('direction', query.direction);
  params.set('page', String(query.page));
  params.set('size', String(query.size));
  return params.toString();
};
