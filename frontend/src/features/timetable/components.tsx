import { useDroppable } from '@dnd-kit/core';
import { memo, useEffect, useMemo, useState } from 'react';
import { addDays } from '@/shared/time/calendar';
import { SLOTS_PER_DAY, toGridPosition } from '@/shared/grid/geometry';
import { PriorityBadge, type Priority } from '@/shared/ui/priority-badge';
import styles from './timetable.module.css';

export type WeekDirection = 'previous' | 'today' | 'next';

export function WeekNavigator({
  weekLabel,
  onSelect,
}: {
  readonly weekLabel: string;
  readonly onSelect: (direction: WeekDirection) => void;
}) {
  return (
    <div className={styles.weekNavigator}>
      <button type="button" aria-label="이전 주" onClick={() => onSelect('previous')}>
        ‹
      </button>
      <button type="button" onClick={() => onSelect('today')}>
        오늘
      </button>
      <button type="button" aria-label="다음 주" onClick={() => onSelect('next')}>
        ›
      </button>
      <strong>{weekLabel}</strong>
    </div>
  );
}

const duration = (minutes: number): string => {
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  return [hours > 0 ? `${hours}h` : '', remainder > 0 ? `${remainder}m` : '']
    .filter(Boolean)
    .join(' ');
};

export function CapacityIndicator({
  windowMinutes,
  plannedMinutes,
  availableMinutes,
  previewDeltaMinutes,
}: {
  readonly windowMinutes: number;
  readonly plannedMinutes: number;
  readonly availableMinutes: number;
  readonly previewDeltaMinutes?: number;
}) {
  return (
    <div className={styles.capacity}>
      <span>이번 주 가용 시간</span>
      <strong>{duration(Math.max(0, availableMinutes + (previewDeltaMinutes ?? 0)))}</strong>
      {previewDeltaMinutes === undefined ? null : (
        <span className={styles.capacityDelta}>
          ({previewDeltaMinutes < 0 ? '−' : '+'}
          {duration(Math.abs(previewDeltaMinutes))})
        </span>
      )}
      <meter
        aria-label={`이번 주 계획 시간 ${duration(plannedMinutes)} / ${duration(windowMinutes)}`}
        min={0}
        max={windowMinutes}
        value={plannedMinutes}
      />
    </div>
  );
}

export interface TimetableBlockModel {
  readonly id: string;
  readonly title: string;
  readonly date: string;
  readonly startTime: string;
  readonly endTime: string;
  readonly priority: Priority;
}

export interface SlotProposalModel {
  readonly date: string;
  readonly startTime: string;
}

const slotTime = (index: number): string => {
  const minutes = 8 * 60 + index * 15;
  return `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`;
};

const weekdays = ['월', '화', '수', '목', '금', '토', '일'] as const;
const hourLabels = Array.from(
  { length: 15 },
  (_, index) => `${String(index + 8).padStart(2, '0')}:00`,
);

const shortDate = (date: string): string => {
  const [, month = '', day = ''] = date.split('-');
  return `${Number(month)}.${Number(day)}`;
};

const Slot = memo(function Slot({
  dayIndex,
  slotIndex,
  weekStart,
  onPreview,
  onProposal,
}: {
  readonly dayIndex: number;
  readonly slotIndex: number;
  readonly weekStart: string;
  readonly onPreview: (proposal: SlotProposalModel) => void;
  readonly onProposal: (proposal: SlotProposalModel) => void;
}) {
  const proposal = useMemo(
    () => ({ date: addDays(weekStart, dayIndex), startTime: slotTime(slotIndex) }),
    [dayIndex, slotIndex, weekStart],
  );
  const { isOver, setNodeRef } = useDroppable({
    id: `slot-${String(dayIndex)}-${String(slotIndex)}`,
    data: { proposal, onProposal },
  });
  useEffect(() => {
    if (isOver) onPreview(proposal);
  }, [isOver, onPreview, proposal]);
  return (
    <div
      ref={setNodeRef}
      className={`${styles.slot} ${isOver ? styles.slotOver : ''}`}
      data-slot={`${dayIndex}-${slotIndex}`}
      style={{ gridColumn: dayIndex + 1, gridRow: slotIndex + 1 }}
    />
  );
});

/** F-C02, NFR-005: one stable 56x7 CSS grid with a separate preview layer. */
export function TimetableGrid({
  weekStart,
  blocks,
  onOpenTask,
  onSlotProposal,
  onPreviewChange,
  revertedTaskId,
}: {
  readonly weekStart: string;
  readonly blocks: readonly TimetableBlockModel[];
  readonly onOpenTask: (taskId: string) => void;
  readonly onSlotProposal: (proposal: SlotProposalModel) => void;
  readonly onPreviewChange?: (proposal: SlotProposalModel | undefined) => void;
  readonly revertedTaskId?: string;
}) {
  const [preview, setPreview] = useState<SlotProposalModel>();
  const updatePreview = (proposal: SlotProposalModel) => {
    setPreview(proposal);
    onPreviewChange?.(proposal);
  };
  return (
    <div
      className={styles.timetable}
      role="region"
      aria-label="주간 시간표"
      tabIndex={-1}
      data-testid="timetable-grid"
    >
      <div className={styles.corner} aria-hidden="true">
        KST
      </div>
      <div className={styles.dayHeaders}>
        {weekdays.map((weekday, dayIndex) => {
          const date = addDays(weekStart, dayIndex);
          return (
            <div className={styles.dayHeader} data-day={dayIndex} key={weekday}>
              <span>{weekday}요일</span>
              <strong>{shortDate(date)}</strong>
            </div>
          );
        })}
      </div>
      <div className={styles.timeAxis} aria-label="시간대">
        {hourLabels.map((label, index) => (
          <span style={{ top: `${(index / 14) * 100}%` }} key={label}>
            {label}
          </span>
        ))}
      </div>
      <div className={styles.grid}>
        {Array.from({ length: 7 }, (_, dayIndex) =>
          Array.from({ length: SLOTS_PER_DAY }, (_, slotIndex) => (
            <Slot
              dayIndex={dayIndex}
              slotIndex={slotIndex}
              weekStart={weekStart}
              onPreview={updatePreview}
              onProposal={onSlotProposal}
              key={`${dayIndex}-${slotIndex}`}
            />
          )),
        )}
        {blocks.map((block) => {
          const position = toGridPosition(
            { date: block.date, startTime: block.startTime, endTime: block.endTime },
            weekStart,
          );
          return (
            <button
              className={styles.block}
              type="button"
              data-day={position.dayIndex}
              style={{
                gridColumn: position.dayIndex + 1,
                gridRow: `${position.startSlotIndex + 1} / span ${position.slotSpan}`,
              }}
              aria-label={`${block.title} ${block.startTime}–${block.endTime} ${block.priority}`}
              onClick={() => onOpenTask(block.id)}
              key={block.id}
            >
              <strong>{block.title}</strong>
              <span>
                {block.startTime}–{block.endTime}
              </span>
              <PriorityBadge priority={block.priority} />
              {block.id === revertedTaskId ? (
                <span className={styles.reverted}>↩ 되돌림</span>
              ) : null}
            </button>
          );
        })}
        {preview === undefined ? null : (
          <output className={styles.preview}>
            배치 미리보기 {preview.date} {preview.startTime}
          </output>
        )}
      </div>
    </div>
  );
}
