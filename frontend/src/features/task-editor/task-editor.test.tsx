import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { DeleteConfirmDialog, TaskEditorDialog } from './components';

describe('F-C04 task editor', () => {
  it('UR_001_validates_locally_then_submits_the_complete_create_content', () => {
    const submit = vi.fn();
    render(<TaskEditorDialog mode="create" onClose={vi.fn()} onSubmit={submit} />);

    fireEvent.click(screen.getByRole('button', { name: '만들기' }));
    expect(screen.getByText('제목을 입력해 주세요')).toBeInTheDocument();
    expect(submit).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText('제목'), { target: { value: '주간 보고서' } });
    fireEvent.change(screen.getByLabelText('예상 시간'), { target: { value: '60' } });
    fireEvent.click(screen.getByRole('button', { name: '만들기' }));
    expect(submit).toHaveBeenCalledWith({
      title: '주간 보고서',
      description: null,
      priority: 'MEDIUM',
      estimateMinutes: 60,
      dueDate: null,
    });
  });

  it('UR_053_requires_named_confirmation_before_delete', () => {
    const confirm = vi.fn();
    const trigger = document.createElement('button');
    document.body.append(trigger);
    trigger.focus();
    render(
      <DeleteConfirmDialog
        taskTitle="주간 보고서"
        invoker={trigger}
        onCancel={vi.fn()}
        onConfirm={confirm}
      />,
    );
    expect(screen.getByText(/주간 보고서/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '삭제 확인' }));
    expect(confirm).toHaveBeenCalledOnce();
  });

  it('NFR_004_traps_focus_closes_on_Escape_and_restores_the_invoker', () => {
    const close = vi.fn();
    const trigger = document.createElement('button');
    document.body.append(trigger);
    trigger.focus();
    render(<TaskEditorDialog mode="create" onClose={close} onSubmit={vi.fn()} />);

    const dialog = screen.getByRole('dialog', { name: '새 할 일' });
    screen.getByRole('button', { name: '만들기' }).focus();
    fireEvent.keyDown(dialog, { key: 'Tab' });
    expect(screen.getByLabelText('제목')).toHaveFocus();
    fireEvent.keyDown(dialog, { key: 'Escape' });
    expect(close).toHaveBeenCalledOnce();
    expect(trigger).toHaveFocus();
  });
});
