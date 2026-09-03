import type { DragEndEvent, DragOverEvent, DragStartEvent } from '@dnd-kit/core';
import { describe, expect, it, vi } from 'vitest';
import { createSchedulingDndHandlers } from './dnd';

const active = { id: 'task-1', data: { current: { taskId: 'task-1' } } };
const over = {
  id: 'slot-0-4',
  data: { current: { proposal: { date: '2026-08-31', startTime: '09:00' } } },
};

describe('Tempo scheduling dnd transport', () => {
  it('UR_030_routes_start_preview_and_drop_through_dnd_kit_payloads', () => {
    const onDragStart = vi.fn();
    const onPreview = vi.fn();
    const onDrop = vi.fn();
    const onCancel = vi.fn();
    const handlers = createSchedulingDndHandlers({ onDragStart, onPreview, onDrop, onCancel });

    handlers.onDragStart({ active } as unknown as DragStartEvent);
    handlers.onDragOver({ active, over } as unknown as DragOverEvent);
    handlers.onDragEnd({ active, over } as unknown as DragEndEvent);

    expect(onDragStart).toHaveBeenCalledWith('task-1');
    expect(onPreview).toHaveBeenNthCalledWith(1, { date: '2026-08-31', startTime: '09:00' });
    expect(onPreview).toHaveBeenLastCalledWith(undefined);
    expect(onDrop).toHaveBeenCalledWith('task-1', { date: '2026-08-31', startTime: '09:00' });
    expect(onCancel).not.toHaveBeenCalled();
  });

  it('UR_030_cancels_when_drag_is_aborted_or_has_no_drop_slot', () => {
    const onPreview = vi.fn();
    const onCancel = vi.fn();
    const handlers = createSchedulingDndHandlers({
      onDragStart: vi.fn(),
      onPreview,
      onDrop: vi.fn(),
      onCancel,
    });

    handlers.onDragCancel();
    handlers.onDragOver({ active, over: null } as unknown as DragOverEvent);
    handlers.onDragEnd({ active, over: null } as unknown as DragEndEvent);

    expect(onPreview).toHaveBeenCalledWith(undefined);
    expect(onCancel).toHaveBeenCalledTimes(2);
  });
});
