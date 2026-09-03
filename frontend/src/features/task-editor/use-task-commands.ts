import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import type { components } from '@/shared/api/generated/planning-api';
import { invalidatePlanningScope } from '@/shared/api/cache';
import { MutationCoordinator } from '@/shared/api/mutation-coordinator';
import { planningApi } from '@/shared/api/planning-api';
import { captureTaskCache, replaceCachedTask, restoreTaskCache } from '@/shared/api/task-cache';

type TaskView = components['schemas']['TaskView'];
type TaskContentRequest = components['schemas']['TaskContentRequest'];
type UpdateTaskRequest = components['schemas']['UpdateTaskRequest'];

const optimisticContent = (
  task: TaskView,
  content: Omit<UpdateTaskRequest, 'expectedVersion'>,
): TaskView => ({
  id: task.id,
  title: content.title,
  priority: content.priority,
  estimateMinutes: content.estimateMinutes,
  status: task.status,
  version: task.version,
  createdAt: task.createdAt,
  updatedAt: task.updatedAt,
  ...(content.description === null || content.description === undefined
    ? {}
    : { description: content.description }),
  ...(content.dueDate === null || content.dueDate === undefined
    ? {}
    : { dueDate: content.dueDate }),
  ...(task.schedule === undefined ? {} : { schedule: task.schedule }),
});

/** F-C04, F-C07: create/update/delete/completion commands with zero automatic retry. */
export const useTaskCommands = (weekStart: string) => {
  const client = useQueryClient();
  const [coordinator] = useState(() => new MutationCoordinator());
  const create = useMutation({
    retry: false,
    mutationFn: (body: TaskContentRequest) => planningApi.createTask(body),
    onSuccess: (task) => invalidatePlanningScope(client, { taskId: task.id, fromWeek: weekStart }),
  });
  const update = useMutation({
    retry: false,
    mutationFn: ({
      task,
      content,
    }: {
      readonly task: TaskView;
      readonly content: Omit<UpdateTaskRequest, 'expectedVersion'>;
    }) =>
      coordinator.execute({
        task,
        captureSnapshot: () => captureTaskCache(client, task),
        applyOptimistic: () => replaceCachedTask(client, optimisticContent(task, content)),
        request: ({ expectedVersion }) =>
          planningApi.updateTask(task.id, { ...content, expectedVersion }),
        replaceFromServer: (serverTask) => replaceCachedTask(client, serverTask),
        rollback: (snapshot) => restoreTaskCache(client, task.id, snapshot),
      }),
  });
  const remove = useMutation({
    retry: false,
    mutationFn: (task: TaskView) =>
      coordinator.execute({
        task,
        optimistic: false,
        captureSnapshot: () => undefined,
        applyOptimistic: () => undefined,
        request: ({ expectedVersion }) => planningApi.deleteTask(task.id, expectedVersion),
        replaceFromServer: () => undefined,
        rollback: () => undefined,
      }),
    onSuccess: (_result, task) =>
      invalidatePlanningScope(client, { taskId: task.id, fromWeek: weekStart }),
  });
  const completion = useMutation({
    retry: false,
    mutationFn: ({ task, completed }: { readonly task: TaskView; readonly completed: boolean }) =>
      coordinator.execute({
        task,
        captureSnapshot: () => captureTaskCache(client, task),
        applyOptimistic: () =>
          replaceCachedTask(client, { ...task, status: completed ? 'COMPLETED' : 'TODO' }),
        request: ({ expectedVersion }) =>
          planningApi.setCompletion(task.id, { completed, expectedVersion }),
        replaceFromServer: (serverTask) => replaceCachedTask(client, serverTask),
        rollback: (snapshot) => restoreTaskCache(client, task.id, snapshot),
      }),
  });
  return { create, update, remove, completion } as const;
};
