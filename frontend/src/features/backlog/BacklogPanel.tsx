import { useDraggable } from '@dnd-kit/core';
import styles from '@/shared/ui/feature.module.css';
import { PriorityBadge } from '@/shared/ui/priority-badge';

export interface BacklogItemModel {
  readonly id: string;
  readonly title: string;
  readonly priority: 'LOW' | 'MEDIUM' | 'HIGH';
  readonly estimateMinutes: number;
}

export interface BacklogPanelProps {
  readonly tasks: readonly BacklogItemModel[];
  readonly onOpenTask: (taskId: string) => void;
  readonly onSchedule: (taskId: string) => void;
  readonly onDragStart?: (taskId: string) => void;
}

const duration = (minutes: number): string => {
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  return [hours > 0 ? `${hours}h` : '', remainder > 0 ? `${remainder}m` : '']
    .filter(Boolean)
    .join(' ');
};

function BacklogCard({
  task,
  onOpenTask,
  onSchedule,
  onDragStart,
}: {
  readonly task: BacklogItemModel;
  readonly onOpenTask: (taskId: string) => void;
  readonly onSchedule: (taskId: string) => void;
  readonly onDragStart?: (taskId: string) => void;
}) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: task.id,
    data: { taskId: task.id },
  });
  return (
    <li
      ref={setNodeRef}
      className={styles.taskCard}
      data-dnd-draggable="true"
      style={{
        opacity: isDragging ? 0.55 : 1,
        transform:
          transform === null
            ? undefined
            : `translate3d(${String(transform.x)}px, ${String(transform.y)}px, 0)`,
      }}
      onPointerDown={() => onDragStart?.(task.id)}
      {...listeners}
    >
      <button type="button" onClick={() => onOpenTask(task.id)} aria-label={`${task.title} 열기`}>
        {task.title}
      </button>
      <div className={styles.taskMeta}>
        <PriorityBadge priority={task.priority} />
        <span>{duration(task.estimateMinutes)}</span>
        <button
          type="button"
          onClick={() => onSchedule(task.id)}
          aria-label={`${task.title} 시간 배치`}
          data-testid="backlog-card-schedule-button"
          {...attributes}
        >
          시간 배치
        </button>
      </div>
    </li>
  );
}

/** F-C03, FR-005: renders only unscheduled task commands and no out-of-scope metadata. */
export function BacklogPanel({ tasks, onOpenTask, onSchedule, onDragStart }: BacklogPanelProps) {
  return (
    <section className={styles.panel} aria-labelledby="backlog-panel-title">
      <div className={styles.panelHeader}>
        <h2 id="backlog-panel-title">미배치 할 일</h2>
        <span className={styles.muted}>{tasks.length}개</span>
      </div>
      {tasks.length === 0 ? (
        <p className={styles.empty}>미배치 할 일이 없습니다</p>
      ) : (
        <ul className={styles.taskList}>
          {tasks.map((task) => (
            <BacklogCard
              key={task.id}
              task={task}
              onOpenTask={onOpenTask}
              onSchedule={onSchedule}
              {...(onDragStart === undefined ? {} : { onDragStart })}
            />
          ))}
        </ul>
      )}
    </section>
  );
}
