import type { components } from '@/shared/api/generated/planning-api';

type TaskView = components['schemas']['TaskView'];

export interface WeekCapacity {
  readonly windowMinutes: number;
  readonly plannedMinutes: number;
  readonly availableMinutes: number;
}

export const WEEK_WINDOW_MINUTES = 7 * 14 * 60;

/** FR-001, UR-023, UR-024: derives display-only capacity from the rendered server payload. */
export const deriveWeekCapacity = (tasks: readonly TaskView[]): WeekCapacity => {
  const plannedMinutes = tasks.reduce(
    (total, task) =>
      task.status === 'TODO' && task.schedule !== undefined ? total + task.estimateMinutes : total,
    0,
  );
  return {
    windowMinutes: WEEK_WINDOW_MINUTES,
    plannedMinutes,
    availableMinutes: Math.max(0, WEEK_WINDOW_MINUTES - plannedMinutes),
  };
};
