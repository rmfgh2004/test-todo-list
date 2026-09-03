import type { components } from '@/shared/api/generated/planning-api';

type ScheduleView = components['schemas']['ScheduleView'];
type TaskView = components['schemas']['TaskView'];
type TaskPageView = components['schemas']['TaskPageView'];
type WeeklyPlanView = components['schemas']['WeeklyPlanView'];

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null && !Array.isArray(value);

const isInteger = (value: unknown): value is number => Number.isSafeInteger(value);

/** SECURITY-13: validates a generated ScheduleView at the untrusted response boundary. */
export const isScheduleView = (value: unknown): value is ScheduleView =>
  isRecord(value) &&
  typeof value.date === 'string' &&
  typeof value.startTime === 'string' &&
  typeof value.endTime === 'string';

/** SECURITY-13: validates a generated TaskView before it can enter the query cache. */
export const isTaskView = (value: unknown): value is TaskView =>
  isRecord(value) &&
  typeof value.id === 'string' &&
  typeof value.title === 'string' &&
  (value.description === undefined || typeof value.description === 'string') &&
  (value.priority === 'LOW' || value.priority === 'MEDIUM' || value.priority === 'HIGH') &&
  isInteger(value.estimateMinutes) &&
  (value.dueDate === undefined || typeof value.dueDate === 'string') &&
  (value.status === 'TODO' || value.status === 'COMPLETED') &&
  (value.schedule === undefined || isScheduleView(value.schedule)) &&
  isInteger(value.version) &&
  typeof value.createdAt === 'string' &&
  typeof value.updatedAt === 'string';

/** SECURITY-13: validates the bounded generated task page response. */
export const isTaskPageView = (value: unknown): value is TaskPageView =>
  isRecord(value) &&
  Array.isArray(value.content) &&
  value.content.every(isTaskView) &&
  isInteger(value.page) &&
  isInteger(value.size) &&
  isInteger(value.totalElements) &&
  isInteger(value.totalPages);

/** SECURITY-13: validates the generated weekly plan response. */
export const isWeeklyPlanView = (value: unknown): value is WeeklyPlanView =>
  isRecord(value) &&
  typeof value.weekStart === 'string' &&
  typeof value.weekEndExclusive === 'string' &&
  Array.isArray(value.scheduled) &&
  value.scheduled.every(isTaskView) &&
  Array.isArray(value.backlog) &&
  value.backlog.every(isTaskView);
