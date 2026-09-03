import { HttpResponse, http, type HttpHandler } from 'msw';
import type { components } from '@/shared/api/generated/planning-api';

type ApiError = components['schemas']['ApiError'];
type TaskView = components['schemas']['TaskView'];
type TaskPageView = components['schemas']['TaskPageView'];
type WeeklyPlanView = components['schemas']['WeeklyPlanView'];

export type MockScenario =
  | 'success'
  | 'validation'
  | 'not-found'
  | 'conflict'
  | 'stale'
  | 'rate-limited'
  | 'server-error'
  | 'transport-loss';

const BASE_URL = 'http://127.0.0.1:8080';

const task = {
  id: '11111111-1111-4111-8111-111111111111',
  title: '계획할 작업',
  priority: 'MEDIUM',
  estimateMinutes: 30,
  status: 'TODO',
  version: 0,
  createdAt: '2026-09-01T09:00:00+09:00',
  updatedAt: '2026-09-01T09:00:00+09:00',
} satisfies TaskView;

const errorFor = (scenario: Exclude<MockScenario, 'success' | 'transport-loss'>): ApiError => {
  const common = { message: '요청을 처리할 수 없습니다.', requestId: 'TMP-MOCK-0000-0001' };
  switch (scenario) {
    case 'validation':
      return {
        ...common,
        code: 'VALIDATION_FAILED',
        fieldErrors: [{ field: 'title', code: 'REQUIRED', message: '제목을 입력하세요.' }],
      };
    case 'not-found':
      return { ...common, code: 'TASK_NOT_FOUND' };
    case 'conflict':
      return {
        ...common,
        code: 'SCHEDULE_CONFLICT',
        conflict: {
          proposed: { date: '2026-09-01', startTime: '09:00', endTime: '09:30' },
          conflicting: { date: '2026-09-01', startTime: '09:15', endTime: '09:45' },
          nextCandidate: { date: '2026-09-01', startTime: '09:45', endTime: '10:15' },
        },
      };
    case 'stale':
      return { ...common, code: 'STALE_TASK', currentVersion: 2 };
    case 'rate-limited':
      return { ...common, code: 'RATE_LIMITED' };
    case 'server-error':
      return { ...common, code: 'INTERNAL_ERROR' };
  }
};

const statusFor = (scenario: Exclude<MockScenario, 'success' | 'transport-loss'>): number => {
  if (scenario === 'validation') return 400;
  if (scenario === 'not-found') return 404;
  if (scenario === 'conflict' || scenario === 'stale') return 409;
  if (scenario === 'rate-limited') return 429;
  return 500;
};

const successHandlers = (): HttpHandler[] => {
  const page = {
    content: [task],
    page: 0,
    size: 25,
    totalElements: 1,
    totalPages: 1,
  } satisfies TaskPageView;
  const week = {
    weekStart: '2026-09-01',
    weekEndExclusive: '2026-09-08',
    scheduled: [],
    backlog: [task],
  } satisfies WeeklyPlanView;
  return [
    http.get(`${BASE_URL}/api/v1/planning/weeks/:weekStart`, () => HttpResponse.json(week)),
    http.get(`${BASE_URL}/api/v1/tasks`, () => HttpResponse.json(page)),
    http.post(`${BASE_URL}/api/v1/tasks`, () => HttpResponse.json(task, { status: 201 })),
    http.get(`${BASE_URL}/api/v1/tasks/:id`, () => HttpResponse.json(task)),
    http.patch(`${BASE_URL}/api/v1/tasks/:id`, () => HttpResponse.json(task)),
    http.delete(`${BASE_URL}/api/v1/tasks/:id`, () => new HttpResponse(null, { status: 204 })),
    http.put(`${BASE_URL}/api/v1/tasks/:id/schedule`, () => HttpResponse.json(task)),
    http.delete(`${BASE_URL}/api/v1/tasks/:id/schedule`, () => HttpResponse.json(task)),
    http.put(`${BASE_URL}/api/v1/tasks/:id/completion`, () => HttpResponse.json(task)),
  ];
};

/** F-N08: creates generated-contract-shaped handlers for success and every approved failure path. */
export const createPlanningHandlers = (scenario: MockScenario = 'success'): HttpHandler[] => {
  if (scenario === 'success') return successHandlers();
  if (scenario === 'transport-loss') {
    return [http.get(`${BASE_URL}/api/v1/tasks/:id`, () => HttpResponse.error())];
  }
  const responseOptions =
    scenario === 'rate-limited'
      ? { status: statusFor(scenario), headers: { 'Retry-After': '5' } }
      : { status: statusFor(scenario) };
  return [
    http.get(`${BASE_URL}/api/v1/tasks/:id`, () =>
      HttpResponse.json(errorFor(scenario), responseOptions),
    ),
  ];
};
