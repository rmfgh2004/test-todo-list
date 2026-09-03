import { render } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { TimetableGrid, type TimetableBlockModel } from '@/features/timetable/components';

const blocks: readonly TimetableBlockModel[] = Array.from({ length: 1_000 }, (_, index) => {
  const weekDates = [
    '2026-08-31',
    '2026-09-01',
    '2026-09-02',
    '2026-09-03',
    '2026-09-04',
    '2026-09-05',
    '2026-09-06',
  ] as const;
  const slot = index % 56;
  const startMinutes = 8 * 60 + slot * 15;
  const endMinutes = startMinutes + 15;
  const time = (minutes: number) =>
    `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`;
  return {
    id: `capacity-${index}`,
    title: `용량 할 일 ${index}`,
    date: weekDates[index % weekDates.length] ?? '2026-08-31',
    startTime: time(startMinutes),
    endTime: time(endMinutes),
    priority: 'MEDIUM',
  };
});

describe('NFR-005 1,000-task capacity profile', () => {
  it('renders a resolved week update within 300ms', () => {
    const view = render(
      <TimetableGrid
        weekStart="2026-08-31"
        blocks={[]}
        onOpenTask={vi.fn()}
        onSlotProposal={vi.fn()}
      />,
    );
    const started = performance.now();
    view.rerender(
      <TimetableGrid
        weekStart="2026-08-31"
        blocks={blocks}
        onOpenTask={vi.fn()}
        onSlotProposal={vi.fn()}
      />,
    );
    expect(performance.now() - started).toBeLessThan(300);
    expect(view.container.querySelectorAll('button').length).toBe(1_000);
  });
});
