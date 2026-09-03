import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { BacklogPanel } from './BacklogPanel';

describe('F-C03 backlog', () => {
  it('FR_005_renders_unscheduled_tasks_and_all_three_command_paths', () => {
    const schedule = vi.fn();
    const open = vi.fn();
    render(
      <BacklogPanel
        tasks={[{ id: 'task-1', title: '보고서 검토', priority: 'HIGH', estimateMinutes: 90 }]}
        onOpenTask={open}
        onSchedule={schedule}
      />,
    );

    expect(screen.getByText('1h 30m')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '보고서 검토 열기' }));
    fireEvent.click(screen.getByRole('button', { name: '보고서 검토 시간 배치' }));
    expect(open).toHaveBeenCalledWith('task-1');
    expect(schedule).toHaveBeenCalledWith('task-1');
    expect(screen.getByText('높음')).toBeInTheDocument();
    expect(screen.getByText('▲')).toBeInTheDocument();
    expect(screen.getByTestId('backlog-card-schedule-button').closest('li')).toHaveAttribute(
      'data-dnd-draggable',
      'true',
    );
  });

  it('FR_005_distinguishes_an_empty_backlog', () => {
    render(<BacklogPanel tasks={[]} onOpenTask={vi.fn()} onSchedule={vi.fn()} />);
    expect(screen.getByText('미배치 할 일이 없습니다')).toBeInTheDocument();
  });
});
