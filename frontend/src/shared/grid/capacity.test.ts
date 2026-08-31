import fc from 'fast-check';
import { describe, expect, it } from 'vitest';
import { deriveWeekCapacity, WEEK_WINDOW_MINUTES } from './capacity';
import { assertProperty, taskViewArbitrary } from '../../../tests/generators/planning';

describe('week capacity', () => {
  it('UR_023_uses_the_fixed_seven_day_planning_window', () => {
    expect(WEEK_WINDOW_MINUTES).toBe(5_880);
  });

  it('UR_023_counts_only_incomplete_scheduled_tasks_and_floors_at_zero', () => {
    assertProperty(
      fc.property(fc.array(taskViewArbitrary, { maxLength: 20 }), (tasks) => {
        const expectedPlanned = tasks
          .filter((task) => task.status === 'TODO' && task.schedule !== undefined)
          .reduce((total, task) => total + task.estimateMinutes, 0);

        expect(deriveWeekCapacity(tasks)).toEqual({
          windowMinutes: WEEK_WINDOW_MINUTES,
          plannedMinutes: expectedPlanned,
          availableMinutes: Math.max(0, WEEK_WINDOW_MINUTES - expectedPlanned),
        });
      }),
    );
  });
});
