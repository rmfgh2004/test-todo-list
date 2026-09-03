import { useDraggable } from '@dnd-kit/core';
import { useState } from 'react';
import { addDays } from '@/shared/time/calendar';
import styles from '@/shared/ui/feature.module.css';

export interface SlotProposal {
  readonly taskId: string;
  readonly date: string;
  readonly startTime: string;
  readonly estimateMinutes: number;
  readonly expectedVersion: number;
  readonly source: 'pointer' | 'keyboard' | 'form';
}

export function SchedulingInteraction({
  taskId,
  taskTitle,
  estimateMinutes,
  expectedVersion,
  initialDate,
  initialTime,
  onProposal,
}: {
  readonly taskId: string;
  readonly taskTitle: string;
  readonly estimateMinutes: number;
  readonly expectedVersion: number;
  readonly initialDate: string;
  readonly initialTime: string;
  readonly onProposal: (proposal: SlotProposal) => void;
}) {
  const { attributes, listeners, setNodeRef, transform } = useDraggable({
    id: taskId,
    data: { taskId },
  });
  const [pickedUp, setPickedUp] = useState(false);
  const [keyboardDate, setKeyboardDate] = useState(initialDate);
  const [keyboardMinutes, setKeyboardMinutes] = useState(() => {
    const [hours = 8, minutes = 0] = initialTime.split(':').map(Number);
    return hours * 60 + minutes;
  });
  const keyboardTime = `${String(Math.floor(keyboardMinutes / 60)).padStart(2, '0')}:${String(keyboardMinutes % 60).padStart(2, '0')}`;
  const propose = (source: SlotProposal['source']) =>
    onProposal({
      taskId,
      date: source === 'keyboard' ? keyboardDate : initialDate,
      startTime: source === 'keyboard' ? keyboardTime : initialTime,
      estimateMinutes,
      expectedVersion,
      source,
    });
  return (
    <div className={styles.row}>
      <button
        ref={setNodeRef}
        className={styles.secondary}
        type="button"
        aria-label={`${taskTitle} 키보드로 배치`}
        data-dnd-draggable="true"
        style={{
          transform:
            transform === null
              ? undefined
              : `translate3d(${String(transform.x)}px, ${String(transform.y)}px, 0)`,
        }}
        {...attributes}
        {...listeners}
        onKeyDown={(event) => {
          if (event.key === ' ') {
            event.preventDefault();
            setPickedUp(true);
          } else if (event.key === 'Escape') setPickedUp(false);
          else if (pickedUp && event.key === 'ArrowLeft') {
            event.preventDefault();
            setKeyboardDate((current) => addDays(current, -1));
          } else if (pickedUp && event.key === 'ArrowRight') {
            event.preventDefault();
            setKeyboardDate((current) => addDays(current, 1));
          } else if (pickedUp && event.key === 'ArrowUp') {
            event.preventDefault();
            setKeyboardMinutes((current) => Math.max(8 * 60, current - 15));
          } else if (pickedUp && event.key === 'ArrowDown') {
            event.preventDefault();
            setKeyboardMinutes((current) => Math.min(22 * 60 - estimateMinutes, current + 15));
          } else if (event.key === 'Enter' && pickedUp) {
            propose('keyboard');
            setPickedUp(false);
          }
        }}
      >
        {pickedUp ? `배치 위치 ${keyboardDate} ${keyboardTime}` : '키보드 배치'}
      </button>
      <span className={styles.muted}>Space로 집고 방향키로 이동, Enter로 확정</span>
      <button
        className={styles.secondary}
        type="button"
        data-testid="schedule-form-button"
        onClick={() => propose('form')}
      >
        시간 폼으로 배치
      </button>
    </div>
  );
}
