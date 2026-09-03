import { describe, expect, it } from 'vitest';
import { isScheduleView, isTaskPageView, isTaskView, isWeeklyPlanView } from './validation';

const task = {
  id: '00000000-0000-4000-8000-000000000001',
  title: '계획 검증',
  priority: 'MEDIUM',
  estimateMinutes: 30,
  status: 'TODO',
  version: 0,
  createdAt: '2026-09-01T00:00:00Z',
  updatedAt: '2026-09-01T00:00:00Z',
};

describe('generated response boundary validators', () => {
  it('SECURITY_13_accepts_contract_shaped_task_page_and_week_payloads', () => {
    expect(
      isTaskPageView({ content: [task], page: 0, size: 25, totalElements: 1, totalPages: 1 }),
    ).toBe(true);
    expect(
      isWeeklyPlanView({
        weekStart: '2026-08-31',
        weekEndExclusive: '2026-09-07',
        scheduled: [
          { ...task, schedule: { date: '2026-09-01', startTime: '09:00', endTime: '09:30' } },
        ],
        backlog: [task],
      }),
    ).toBe(true);
  });

  it.each([
    null,
    [],
    { ...task, id: 1 },
    { ...task, priority: 'URGENT' },
    { ...task, estimateMinutes: 30.5 },
    { ...task, status: 'DELETED' },
    { ...task, schedule: { date: '2026-09-01', startTime: 900, endTime: '09:30' } },
  ])('SECURITY_13_rejects_malformed_task_payload_%#', (value) => {
    expect(isTaskView(value)).toBe(false);
  });

  it('SECURITY_13_rejects_malformed_collections_and_schedule_values', () => {
    expect(isScheduleView({ date: '2026-09-01', startTime: '09:00' })).toBe(false);
    expect(
      isTaskPageView({ content: [null], page: 0, size: 25, totalElements: 1, totalPages: 1 }),
    ).toBe(false);
    expect(
      isWeeklyPlanView({
        weekStart: '2026-08-31',
        weekEndExclusive: '2026-09-07',
        scheduled: 'not-an-array',
        backlog: [],
      }),
    ).toBe(false);
  });
});
