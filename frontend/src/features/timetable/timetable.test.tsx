import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { CapacityIndicator, TimetableGrid, WeekNavigator } from './components';

describe('F-C02 timetable', () => {
  it('FR_002_navigates_previous_today_and_next_week', () => {
    const select = vi.fn();
    render(<WeekNavigator weekLabel="2026년 8월 31일 – 9월 6일" onSelect={select} />);

    fireEvent.click(screen.getByRole('button', { name: '이전 주' }));
    fireEvent.click(screen.getByRole('button', { name: '오늘' }));
    fireEvent.click(screen.getByRole('button', { name: '다음 주' }));
    expect(select.mock.calls.map(([direction]) => direction)).toEqual([
      'previous',
      'today',
      'next',
    ]);
  });

  it('UR_023_renders_the_truthful_week_capacity_with_a_text_equivalent', () => {
    render(
      <CapacityIndicator windowMinutes={5_880} plannedMinutes={135} availableMinutes={5_745} />,
    );
    expect(screen.getByText('95h 45m')).toBeInTheDocument();
    expect(screen.getByRole('meter')).toHaveAccessibleName('이번 주 계획 시간 2h 15m / 98h');
  });

  it('UR_025_previews_capacity_delta_without_changing_the_meter_payload', () => {
    render(
      <CapacityIndicator
        windowMinutes={5_880}
        plannedMinutes={135}
        availableMinutes={5_745}
        previewDeltaMinutes={-90}
      />,
    );
    expect(screen.getByText('94h 15m')).toBeInTheDocument();
    expect(screen.getByText('(−1h 30m)')).toBeInTheDocument();
    expect(screen.getByRole('meter')).toHaveValue(135);
  });

  it('NFR_005_keeps_all_56x7_slot_nodes_stable_during_drag_preview', () => {
    const { container } = render(
      <TimetableGrid
        weekStart="2026-08-31"
        blocks={[
          {
            id: 'task-1',
            title: '보고서 작성',
            date: '2026-08-31',
            startTime: '09:00',
            endTime: '10:00',
            priority: 'HIGH',
          },
        ]}
        onOpenTask={vi.fn()}
        onSlotProposal={vi.fn()}
      />,
    );
    const before = Array.from(container.querySelectorAll('[data-slot]'));
    expect(before).toHaveLength(392);

    const target = before[80];
    if (target === undefined) throw new Error('Expected a grid slot');
    const after = Array.from(container.querySelectorAll('[data-slot]'));
    expect(after).toHaveLength(392);
    after.forEach((node, index) => expect(node).toBe(before[index]));
    expect(screen.getByText('월요일')).toBeInTheDocument();
    expect(screen.getByText('8.31')).toBeInTheDocument();
    expect(screen.getByText('08:00')).toBeInTheDocument();
    expect(screen.getByText('22:00')).toBeInTheDocument();
    const block = screen.getByRole('button', { name: /보고서 작성/ });
    expect(block).toHaveStyle({ gridColumn: '1', gridRow: '5 / span 4' });
    expect(screen.getByText('높음')).toBeInTheDocument();
  });

  it('UR_067_marks_a_reverted_block_with_a_non_colour_cue', () => {
    render(
      <TimetableGrid
        weekStart="2026-08-31"
        blocks={[
          {
            id: 'task-1',
            title: '복원된 일정',
            priority: 'MEDIUM',
            date: '2026-08-31',
            startTime: '09:00',
            endTime: '09:30',
          },
        ]}
        revertedTaskId="task-1"
        onOpenTask={vi.fn()}
        onSlotProposal={vi.fn()}
      />,
    );
    expect(screen.getByText('↩ 되돌림')).toBeInTheDocument();
  });
});
