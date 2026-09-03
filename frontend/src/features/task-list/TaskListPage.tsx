import styles from '@/shared/ui/feature.module.css';
import { PriorityBadge } from '@/shared/ui/priority-badge';

export interface TaskListItemModel {
  readonly id: string;
  readonly title: string;
  readonly priority: 'LOW' | 'MEDIUM' | 'HIGH';
  readonly estimateMinutes: number;
  readonly status: 'TODO' | 'COMPLETED';
  readonly dueDate?: string;
}

const seoulToday = (): string => {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date());
  const value = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((part) => part.type === type)?.value ?? '';
  return `${value('year')}-${value('month')}-${value('day')}`;
};

const groups = (tasks: readonly TaskListItemModel[]) => {
  const today = seoulToday();
  return [
    {
      label: '오늘',
      tasks: tasks.filter((task) => task.status === 'TODO' && task.dueDate === today),
    },
    {
      label: '이번 주',
      tasks: tasks.filter((task) => task.status === 'TODO' && task.dueDate !== today),
    },
    { label: '완료', tasks: tasks.filter((task) => task.status === 'COMPLETED') },
  ] as const;
};

const updateSearch = (updates: Record<string, string>): string => {
  const params = new URLSearchParams(window.location.search);
  Object.entries(updates).forEach(([key, value]) => params.set(key, value));
  const search = `?${params.toString()}`;
  window.history.replaceState({}, '', `${window.location.pathname}${search}`);
  return search;
};

export function TaskListPage({
  tasks,
  page,
  totalPages,
  filtered = false,
  onOpenTask,
  onToggleCompletion,
  onSearchChange,
}: {
  readonly tasks: readonly TaskListItemModel[];
  readonly page: number;
  readonly totalPages: number;
  readonly filtered?: boolean;
  readonly onOpenTask: (taskId: string) => void;
  readonly onToggleCompletion?: (taskId: string, completed: boolean) => void;
  readonly onSearchChange?: (search: string) => void;
}) {
  const params = new URLSearchParams(window.location.search);
  const status = params.get('status');
  const selectedStatus = status === 'TODO' || status === 'COMPLETED' ? status : '';
  const changeSearch = (updates: Record<string, string>) => {
    const search = updateSearch(updates);
    onSearchChange?.(search);
  };
  return (
    <section className={styles.panel} aria-labelledby="task-list-title">
      <div className={styles.panelHeader}>
        <h1 id="task-list-title">할 일 목록</h1>
      </div>
      <div className={styles.filters}>
        <label>
          상태
          <select
            value={selectedStatus}
            onChange={(event) => changeSearch({ status: event.currentTarget.value, page: '0' })}
          >
            <option value="">전체</option>
            <option value="TODO">할 일</option>
            <option value="COMPLETED">완료</option>
          </select>
        </label>
      </div>
      {tasks.length === 0 ? (
        <p className={styles.empty}>
          {filtered ? '조건에 맞는 할 일이 없습니다' : '아직 할 일이 없습니다'}
        </p>
      ) : (
        <div className={styles.taskGroups}>
          {groups(tasks).map((group) => (
            <section key={group.label} aria-labelledby={`task-group-${group.label}`}>
              <h2 id={`task-group-${group.label}`}>{group.label}</h2>
              {group.tasks.length === 0 ? (
                <p className={styles.muted}>해당하는 할 일이 없습니다</p>
              ) : (
                <table className={styles.table}>
                  <thead>
                    <tr>
                      <th>제목</th>
                      <th>상태</th>
                      <th>우선순위</th>
                      <th>예상</th>
                    </tr>
                  </thead>
                  <tbody>
                    {group.tasks.map((task) => (
                      <tr key={task.id}>
                        <td>
                          <button
                            className={styles.secondary}
                            type="button"
                            onClick={() => onOpenTask(task.id)}
                          >
                            {task.title}
                          </button>
                        </td>
                        <td>
                          <label>
                            <input
                              type="checkbox"
                              checked={task.status === 'COMPLETED'}
                              onChange={(event) =>
                                onToggleCompletion?.(task.id, event.currentTarget.checked)
                              }
                            />
                            {task.status === 'TODO' ? '할 일' : '완료'}
                          </label>
                        </td>
                        <td>
                          <PriorityBadge priority={task.priority} />
                        </td>
                        <td>{task.estimateMinutes}분</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </section>
          ))}
        </div>
      )}
      <div className={styles.pagination}>
        <button
          type="button"
          disabled={page <= 0}
          onClick={() => changeSearch({ page: String(Math.max(0, page - 1)) })}
        >
          이전 페이지
        </button>
        <span>
          {page + 1} / {Math.max(1, totalPages)}
        </span>
        <button
          type="button"
          disabled={page + 1 >= totalPages}
          onClick={() => changeSearch({ page: String(page + 1) })}
        >
          다음 페이지
        </button>
      </div>
    </section>
  );
}
