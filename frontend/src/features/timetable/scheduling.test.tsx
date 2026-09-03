import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { SchedulingInteraction } from './SchedulingInteraction';

describe('F-C05 scheduling interaction', () => {
  it('FR_006_converges_keyboard_and_form_on_the_same_slot_proposal', () => {
    const propose = vi.fn();
    render(
      <SchedulingInteraction
        taskId="task-1"
        taskTitle="보고서"
        estimateMinutes={60}
        expectedVersion={3}
        initialDate="2026-09-01"
        initialTime="09:00"
        onProposal={propose}
      />,
    );

    const pickup = screen.getByRole('button', { name: '보고서 키보드로 배치' });
    fireEvent.keyDown(pickup, { key: ' ' });
    fireEvent.keyDown(pickup, { key: 'ArrowRight' });
    fireEvent.keyDown(pickup, { key: 'ArrowDown' });
    fireEvent.keyDown(pickup, { key: 'Enter' });
    fireEvent.click(screen.getByRole('button', { name: '시간 폼으로 배치' }));

    expect(propose).toHaveBeenCalledTimes(2);
    expect(pickup).toHaveAttribute('data-dnd-draggable', 'true');
    expect(propose.mock.calls[0]?.[0]).toMatchObject({
      date: '2026-09-02',
      startTime: '09:15',
      source: 'keyboard',
    });
    expect(propose).toHaveBeenLastCalledWith({
      taskId: 'task-1',
      date: '2026-09-01',
      startTime: '09:00',
      estimateMinutes: 60,
      expectedVersion: 3,
      source: 'form',
    });
  });

  it('NFR_004_clamps_keyboard_time_and_Escape_aborts_the_pickup', () => {
    const propose = vi.fn();
    render(
      <SchedulingInteraction
        taskId="task-1"
        taskTitle="보고서"
        estimateMinutes={60}
        expectedVersion={3}
        initialDate="2026-09-01"
        initialTime="09:00"
        onProposal={propose}
      />,
    );

    const pickup = screen.getByRole('button', { name: '보고서 키보드로 배치' });
    fireEvent.keyDown(pickup, { key: ' ' });
    for (let index = 0; index < 100; index += 1) fireEvent.keyDown(pickup, { key: 'ArrowUp' });
    fireEvent.keyDown(pickup, { key: 'Enter' });
    fireEvent.keyDown(pickup, { key: ' ' });
    fireEvent.keyDown(pickup, { key: 'Escape' });
    fireEvent.keyDown(pickup, { key: 'Enter' });

    expect(propose).toHaveBeenCalledOnce();
    expect(propose).toHaveBeenCalledWith(expect.objectContaining({ startTime: '08:00' }));
  });
});
