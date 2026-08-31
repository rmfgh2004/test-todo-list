import type { components } from '@/shared/api/generated/planning-api';
import { formatIsoTime, parseIsoTime, weekRangeFor } from '@/shared/time/calendar';

type ScheduleView = components['schemas']['ScheduleView'];

export interface GridPosition {
  readonly dayIndex: number;
  readonly startSlotIndex: number;
  readonly slotSpan: number;
}

export interface DropWallTime {
  readonly hour: number;
  readonly minute: number;
  readonly second: number;
}

export const DAY_START_MINUTES = 8 * 60;
export const DAY_END_MINUTES = 22 * 60;
export const SLOT_MINUTES = 15;
export const SLOTS_PER_DAY = (DAY_END_MINUTES - DAY_START_MINUTES) / SLOT_MINUTES;

const toMinutes = (value: string): number => {
  const { hour, minute } = parseIsoTime(value);
  return hour * 60 + minute;
};

/** UR-010, PBT-03: identifies exact 15-minute wall-time boundaries. */
export const isQuarterHourAligned = (value: string): boolean => {
  try {
    return parseIsoTime(value).minute % SLOT_MINUTES === 0;
  } catch {
    return false;
  }
};

/** UR-010, PBT-03: maps an aligned start inside 08:00~22:00 to its grid index. */
export const timeToSlotIndex = (value: string): number => {
  const minutes = toMinutes(value);
  if (!isQuarterHourAligned(value) || minutes < DAY_START_MINUTES || minutes >= DAY_END_MINUTES) {
    throw new Error('Start time must be a 15-minute boundary inside 08:00~22:00');
  }
  return (minutes - DAY_START_MINUTES) / SLOT_MINUTES;
};

/** UR-011, PBT-03: derives the block span from a valid task estimate. */
export const estimateToSlotSpan = (estimateMinutes: number): number => {
  if (
    !Number.isInteger(estimateMinutes) ||
    estimateMinutes < SLOT_MINUTES ||
    estimateMinutes > 840 ||
    estimateMinutes % SLOT_MINUTES !== 0
  ) {
    throw new Error('Estimate must use a 15-minute increment between 15 and 840 minutes');
  }
  return estimateMinutes / SLOT_MINUTES;
};

/** FR-006, UR-012: snaps to the nearest quarter, resolving an exact midpoint upward. */
export const snapDropToQuarter = ({ hour, minute, second }: DropWallTime): string => {
  if (
    !Number.isInteger(hour) ||
    !Number.isInteger(minute) ||
    !Number.isInteger(second) ||
    hour < 0 ||
    hour > 23 ||
    minute < 0 ||
    minute > 59 ||
    second < 0 ||
    second > 59
  ) {
    throw new Error('Invalid drop wall time');
  }

  const seconds = (hour * 60 + minute) * 60 + second;
  if (seconds < DAY_START_MINUTES * 60 || seconds >= DAY_END_MINUTES * 60) {
    throw new Error('Drop time must be inside 08:00~22:00');
  }
  const roundedMinutes =
    Math.floor((seconds + (SLOT_MINUTES * 60) / 2) / (SLOT_MINUTES * 60)) * SLOT_MINUTES;
  const boundedMinutes = Math.min(DAY_END_MINUTES, Math.max(DAY_START_MINUTES, roundedMinutes));
  return formatIsoTime({ hour: Math.floor(boundedMinutes / 60), minute: boundedMinutes % 60 });
};

/** FR-001, UR-010, UR-011: maps a server schedule to its seven-day grid geometry. */
export const toGridPosition = (schedule: ScheduleView, weekStart: string): GridPosition => {
  const range = weekRangeFor(weekStart);
  if (range.start !== weekStart) throw new Error('Week start must be a Monday');

  const dayIndex = range.days.indexOf(schedule.date);
  if (dayIndex < 0) throw new Error('Schedule date must be inside the selected week');

  const startMinutes = toMinutes(schedule.startTime);
  const endMinutes = toMinutes(schedule.endTime);
  const startSlotIndex = timeToSlotIndex(schedule.startTime);
  const slotSpan = estimateToSlotSpan(endMinutes - startMinutes);
  if (endMinutes > DAY_END_MINUTES || startSlotIndex + slotSpan > SLOTS_PER_DAY) {
    throw new Error('Schedule must stay inside 08:00~22:00');
  }

  return { dayIndex, startSlotIndex, slotSpan };
};
