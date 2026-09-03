import {
  DndContext,
  KeyboardSensor,
  PointerSensor,
  pointerWithin,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragOverEvent,
  type DragStartEvent,
} from '@dnd-kit/core';
import type { ReactNode } from 'react';
import type { SlotProposalModel } from './components';

const taskIdFrom = (event: DragStartEvent | DragEndEvent): string =>
  String(event.active.data.current?.taskId ?? event.active.id);

const proposalFrom = (event: DragOverEvent | DragEndEvent): SlotProposalModel | undefined => {
  const value = event.over?.data.current?.proposal;
  if (typeof value !== 'object' || value === null) return undefined;
  if (!('date' in value) || !('startTime' in value)) return undefined;
  return { date: String(value.date), startTime: String(value.startTime) };
};

export const createSchedulingDndHandlers = ({
  onDragStart,
  onPreview,
  onDrop,
  onCancel,
}: {
  readonly onDragStart: (taskId: string) => void;
  readonly onPreview: (proposal: SlotProposalModel | undefined) => void;
  readonly onDrop: (taskId: string, proposal: SlotProposalModel) => void;
  readonly onCancel: () => void;
}) => ({
  onDragStart: (event: DragStartEvent) => onDragStart(taskIdFrom(event)),
  onDragOver: (event: DragOverEvent) => onPreview(proposalFrom(event)),
  onDragCancel: () => {
    onPreview(undefined);
    onCancel();
  },
  onDragEnd: (event: DragEndEvent) => {
    const proposal = proposalFrom(event);
    onPreview(undefined);
    if (proposal === undefined) onCancel();
    else onDrop(taskIdFrom(event), proposal);
  },
});

/** UR-030: shared pointer and keyboard sensors for scheduling drag interactions. */
export function SchedulingDndContext({
  children,
  onDragStart,
  onPreview,
  onDrop,
  onCancel,
}: {
  readonly children: ReactNode;
  readonly onDragStart: (taskId: string) => void;
  readonly onPreview: (proposal: SlotProposalModel | undefined) => void;
  readonly onDrop: (taskId: string, proposal: SlotProposalModel) => void;
  readonly onCancel: () => void;
}) {
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
    useSensor(KeyboardSensor),
  );
  const handlers = createSchedulingDndHandlers({ onDragStart, onPreview, onDrop, onCancel });
  return (
    <DndContext sensors={sensors} collisionDetection={pointerWithin} {...handlers}>
      {children}
    </DndContext>
  );
}
