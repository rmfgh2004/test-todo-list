import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ConflictDialog } from './ConflictDialog';

describe('F-C06 conflict resolution', () => {
  it('FR_007_compares_slots_and_restores_focus_after_cancel', () => {
    const invoker = document.createElement('button');
    document.body.append(invoker);
    invoker.focus();
    const choose = vi.fn();
    render(
      <ConflictDialog
        taskTitle="보고서"
        proposed={{ date: '2026-09-01', startTime: '09:00', endTime: '10:00' }}
        conflicting={{ date: '2026-09-01', startTime: '09:30', endTime: '10:30' }}
        nextCandidate={{ date: '2026-09-01', startTime: '10:30', endTime: '11:30' }}
        invoker={invoker}
        onChooseCandidate={choose}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.getByText('09:00–10:00')).toBeInTheDocument();
    expect(screen.getByText('09:30–10:30')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '10:30으로 이동' }));
    expect(choose).toHaveBeenCalledWith('2026-09-01', '10:30');
    fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' });
    expect(invoker).toHaveFocus();
  });
});
