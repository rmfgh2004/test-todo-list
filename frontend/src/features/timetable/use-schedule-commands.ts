import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import type { components } from '@/shared/api/generated/planning-api';
import type { SafeApiError } from '@/shared/api/error';
import { MutationCoordinator } from '@/shared/api/mutation-coordinator';
import { planningApi } from '@/shared/api/planning-api';
import {
  captureTaskCache,
  replaceCachedTask,
  restoreTaskCache,
  withoutTaskSchedule,
} from '@/shared/api/task-cache';
import { formatIsoTime, parseIsoTime } from '@/shared/time/calendar';
import { useFeedback } from '@/shared/ui/feedback';

type TaskView = components['schemas']['TaskView'];

const previewEndTime = (startTime: string, estimateMinutes: number): string => {
  const start = parseIsoTime(startTime);
  const endMinutes = start.hour * 60 + start.minute + estimateMinutes;
  if (endMinutes > 22 * 60) throw new Error('Schedule preview must end by 22:00');
  return formatIsoTime({ hour: Math.floor(endMinutes / 60), minute: endMinutes % 60 });
};

export interface ScheduleCommand {
  readonly task: TaskView;
  readonly date: string;
  readonly startTime: string;
  readonly resolutionMode?: components['schemas']['ScheduleResolutionMode'];
}

/** F-C05, F-C06, F-N04: optimistic placement/unschedule through one per-task coordinator. */
export const useScheduleCommands = () => {
  const client = useQueryClient();
  const feedback = useFeedback();
  const [revertedTaskId, setRevertedTaskId] = useState<string>();
  const [coordinator] = useState(() => new MutationCoordinator());
  const schedule = useMutation<boolean, SafeApiError, ScheduleCommand>({
    retry: false,
    mutationFn: ({ task, date, startTime, resolutionMode }) =>
      coordinator.execute({
        task,
        captureSnapshot: () => captureTaskCache(client, task),
        applyOptimistic: () =>
          replaceCachedTask(client, {
            ...task,
            schedule: { date, startTime, endTime: previewEndTime(startTime, task.estimateMinutes) },
          }),
        request: ({ expectedVersion }) =>
          planningApi.scheduleTask(task.id, {
            date,
            startTime,
            expectedVersion,
            ...(resolutionMode === undefined ? {} : { resolutionMode }),
          }),
        replaceFromServer: (serverTask) => replaceCachedTask(client, serverTask),
        rollback: (snapshot) => restoreTaskCache(client, task.id, snapshot),
      }),
    onSuccess: (changed, command) => {
      if (!changed) return;
      setRevertedTaskId((current) => (current === command.task.id ? undefined : current));
      feedback.announceOutcome(`${command.date} ${command.startTime}에 배치했습니다`);
    },
    onError: (error, command) => {
      setRevertedTaskId(command.task.id);
      feedback.announceFailure({
        restoredPosition:
          command.task.schedule === undefined
            ? '미배치 목록'
            : `${command.task.schedule.date} ${command.task.schedule.startTime}`,
        failedPosition: `${command.date} ${command.startTime}`,
        reason: error.message,
        actionLabel: '다른 시간 선택',
        onAction: () =>
          document.querySelector<HTMLElement>('[data-testid="schedule-form-button"]')?.focus(),
      });
    },
  });
  const unschedule = useMutation<boolean, SafeApiError, TaskView>({
    retry: false,
    mutationFn: (task) =>
      coordinator.execute({
        task,
        captureSnapshot: () => captureTaskCache(client, task),
        applyOptimistic: () => replaceCachedTask(client, withoutTaskSchedule(task)),
        request: ({ expectedVersion }) => planningApi.unscheduleTask(task.id, expectedVersion),
        replaceFromServer: (serverTask) => replaceCachedTask(client, serverTask),
        rollback: (snapshot) => restoreTaskCache(client, task.id, snapshot),
      }),
    onSuccess: (changed, task) => {
      if (!changed) return;
      setRevertedTaskId((current) => (current === task.id ? undefined : current));
      feedback.announceOutcome('미배치 목록으로 이동했습니다');
    },
    onError: (error, task) => {
      setRevertedTaskId(task.id);
      feedback.announceFailure({
        restoredPosition:
          task.schedule === undefined
            ? '미배치 목록'
            : `${task.schedule.date} ${task.schedule.startTime}`,
        failedPosition: '미배치 목록',
        reason: error.message,
        actionLabel: '일정 다시 확인',
        onAction: () =>
          document.querySelector<HTMLElement>('[data-testid="timetable-grid"]')?.focus(),
      });
    },
  });
  return { schedule, unschedule, revertedTaskId } as const;
};
