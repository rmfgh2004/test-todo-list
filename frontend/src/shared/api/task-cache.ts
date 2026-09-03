import type { QueryClient, QueryKey } from '@tanstack/react-query';
import type { components } from '@/shared/api/generated/planning-api';
import { cacheKeys } from '@/shared/api/cache';

type TaskView = components['schemas']['TaskView'];
type WeeklyPlanView = components['schemas']['WeeklyPlanView'];
type TaskPageView = components['schemas']['TaskPageView'];

export interface TaskCacheSnapshot {
  readonly weeks: readonly [QueryKey, WeeklyPlanView | undefined][];
  readonly lists: readonly [QueryKey, TaskPageView | undefined][];
  readonly detail: TaskView | undefined;
}

/** F-N04: server payload replaces every cached appearance of one task. */
export const replaceCachedTask = (client: QueryClient, task: TaskView) => {
  client.setQueryData(cacheKeys.task(task.id), task);
  client.setQueriesData<WeeklyPlanView>({ queryKey: ['week'] }, (week) => {
    if (week === undefined) return undefined;
    const without = (items: readonly TaskView[]) => items.filter((item) => item.id !== task.id);
    const belongsToWeek =
      task.schedule !== undefined &&
      task.schedule.date >= week.weekStart &&
      task.schedule.date < week.weekEndExclusive;
    return {
      ...week,
      scheduled: belongsToWeek ? [...without(week.scheduled), task] : without(week.scheduled),
      backlog:
        task.schedule === undefined ? [...without(week.backlog), task] : without(week.backlog),
    };
  });
  client.setQueriesData<TaskPageView>({ queryKey: ['tasks'] }, (page) =>
    page === undefined
      ? undefined
      : { ...page, content: page.content.map((item) => (item.id === task.id ? task : item)) },
  );
};

export const captureTaskCache = (client: QueryClient, task: TaskView): TaskCacheSnapshot => ({
  weeks: client.getQueriesData<WeeklyPlanView>({ queryKey: ['week'] }),
  lists: client.getQueriesData<TaskPageView>({ queryKey: ['tasks'] }),
  detail: client.getQueryData<TaskView>(cacheKeys.task(task.id)),
});

export const restoreTaskCache = (
  client: QueryClient,
  taskId: string,
  snapshot: TaskCacheSnapshot,
) => {
  snapshot.weeks.forEach(([key, value]) => client.setQueryData(key, value));
  snapshot.lists.forEach(([key, value]) => client.setQueryData(key, value));
  client.setQueryData(cacheKeys.task(taskId), snapshot.detail);
};

/** FR-008: represents an unscheduled generated view by omitting, never undefining, schedule. */
export const withoutTaskSchedule = (task: TaskView): TaskView => ({
  id: task.id,
  title: task.title,
  priority: task.priority,
  estimateMinutes: task.estimateMinutes,
  status: task.status,
  version: task.version,
  createdAt: task.createdAt,
  updatedAt: task.updatedAt,
  ...(task.description === undefined ? {} : { description: task.description }),
  ...(task.dueDate === undefined ? {} : { dueDate: task.dueDate }),
});
