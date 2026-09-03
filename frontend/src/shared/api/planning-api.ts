import type { components } from '@/shared/api/generated/planning-api';
import { createApiClient } from '@/shared/api/client';
import { connectivityRuntime } from '@/shared/api/connectivity';
import { isTaskPageView, isTaskView, isWeeklyPlanView } from '@/shared/api/validation';

type TaskContentRequest = components['schemas']['TaskContentRequest'];
type UpdateTaskRequest = components['schemas']['UpdateTaskRequest'];
type ScheduleTaskRequest = components['schemas']['ScheduleTaskRequest'];
type SetCompletionRequest = components['schemas']['SetCompletionRequest'];

const api = createApiClient({
  baseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://127.0.0.1:8080',
  observer: connectivityRuntime,
});
const idPath = (id: string): string => `/api/v1/tasks/${encodeURIComponent(id)}`;

/** F-C08: generated-contract-only operations; every command is sent exactly once by ApiClient. */
export const planningApi = {
  getWeek: (weekStart: string) =>
    api.request({
      method: 'GET',
      path: `/api/v1/planning/weeks/${encodeURIComponent(weekStart)}`,
      validate: isWeeklyPlanView,
    }),
  listTasks: (query: string) =>
    api.request({
      method: 'GET',
      path: `/api/v1/tasks${query === '' ? '' : `?${query}`}`,
      validate: isTaskPageView,
    }),
  createTask: (body: TaskContentRequest) =>
    api.request({ method: 'POST', path: '/api/v1/tasks', body, validate: isTaskView }),
  updateTask: (id: string, body: UpdateTaskRequest) =>
    api.request({ method: 'PATCH', path: idPath(id), body, validate: isTaskView }),
  deleteTask: (id: string, expectedVersion: number) => {
    const query = new URLSearchParams({
      expectedVersion: String(expectedVersion),
      confirmed: 'true',
    });
    return api.requestVoid({ method: 'DELETE', path: `${idPath(id)}?${query.toString()}` });
  },
  scheduleTask: (id: string, body: ScheduleTaskRequest) =>
    api.request({ method: 'PUT', path: `${idPath(id)}/schedule`, body, validate: isTaskView }),
  unscheduleTask: (id: string, expectedVersion: number) => {
    const query = new URLSearchParams({ expectedVersion: String(expectedVersion) });
    return api.request({
      method: 'DELETE',
      path: `${idPath(id)}/schedule?${query.toString()}`,
      validate: isTaskView,
    });
  },
  setCompletion: (id: string, body: SetCompletionRequest) =>
    api.request({ method: 'PUT', path: `${idPath(id)}/completion`, body, validate: isTaskView }),
};
