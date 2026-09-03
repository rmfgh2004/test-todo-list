import type { components } from '@/shared/api/generated/planning-api';
import { QueryClientProvider } from '@tanstack/react-query';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { setupServer } from 'msw/node';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { createPlanningQueryClient } from '@/shared/api/cache';
import { App } from './App';

type TaskView = components['schemas']['TaskView'];

const makeTask = (overrides: Partial<TaskView> = {}): TaskView => ({
  id: '00000000-0000-4000-8000-000000000001',
  title: '보고서 검토',
  priority: 'HIGH',
  estimateMinutes: 60,
  status: 'TODO',
  version: 1,
  createdAt: '2026-09-01T00:00:00Z',
  updatedAt: '2026-09-01T00:00:00Z',
  ...overrides,
});

const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
beforeEach(() => {
  window.history.replaceState({}, '', '/');
  server.resetHandlers();
});
afterAll(() => server.close());
afterEach(() => cleanup());

const renderLive = () => {
  const client = createPlanningQueryClient();
  return render(
    <QueryClientProvider client={client}>
      <App live />
    </QueryClientProvider>,
  );
};

const installWeek = (readTasks: () => readonly TaskView[]) => {
  server.use(
    http.get('http://127.0.0.1:8080/api/v1/planning/weeks/:weekStart', ({ params }) => {
      const tasks = readTasks();
      return HttpResponse.json({
        weekStart: params.weekStart,
        weekEndExclusive: '2026-09-07',
        scheduled: tasks.filter((task) => task.schedule !== undefined),
        backlog: tasks.filter((task) => task.schedule === undefined),
      });
    }),
  );
};

describe('real U1 feature hook integration', () => {
  it('FR_003_FR_004_creates_updates_and_confirm_deletes_without_mutation_retry', async () => {
    let tasks: TaskView[] = [];
    let deletedVersion: string | null = null;
    installWeek(() => tasks);
    server.use(
      http.post('http://127.0.0.1:8080/api/v1/tasks', async ({ request }) => {
        const body = await request.json();
        expect(body).toMatchObject({ title: '주간 보고서', estimateMinutes: 60 });
        const created = makeTask({ title: '주간 보고서' });
        tasks = [created];
        return HttpResponse.json(created, { status: 201 });
      }),
      http.patch('http://127.0.0.1:8080/api/v1/tasks/:id', async ({ request }) => {
        const body = await request.json();
        expect(body).toMatchObject({ title: '수정 보고서', expectedVersion: 1 });
        const updated = makeTask({ title: '수정 보고서', version: 2 });
        tasks = [updated];
        return HttpResponse.json(updated);
      }),
      http.delete('http://127.0.0.1:8080/api/v1/tasks/:id', ({ request }) => {
        expect(new URL(request.url).searchParams.get('confirmed')).toBe('true');
        deletedVersion = new URL(request.url).searchParams.get('expectedVersion');
        tasks = [];
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderLive();

    await screen.findByText('미배치 할 일이 없습니다');
    fireEvent.click(screen.getByRole('button', { name: '새 할 일' }));
    fireEvent.change(screen.getByLabelText('제목'), { target: { value: '주간 보고서' } });
    fireEvent.change(screen.getByLabelText('예상 시간'), { target: { value: '60' } });
    fireEvent.click(screen.getByRole('button', { name: '만들기' }));
    await screen.findByRole('button', { name: '주간 보고서 열기' });

    fireEvent.click(screen.getByRole('button', { name: '주간 보고서 열기' }));
    fireEvent.change(screen.getByLabelText('제목'), { target: { value: '수정 보고서' } });
    fireEvent.click(screen.getByRole('button', { name: '저장' }));
    await screen.findByRole('button', { name: '수정 보고서 열기' });

    fireEvent.click(screen.getByRole('button', { name: '수정 보고서 열기' }));
    fireEvent.click(screen.getByRole('button', { name: '삭제' }));
    fireEvent.click(screen.getByRole('button', { name: '삭제 확인' }));
    await screen.findByText('미배치 할 일이 없습니다');
    expect(deletedVersion).toBe('2');
  });

  it('FR_006_FR_007_rolls_back_conflict_then_accepts_the_server_candidate', async () => {
    let task = makeTask();
    let attempts = 0;
    installWeek(() => [task]);
    server.use(
      http.put('http://127.0.0.1:8080/api/v1/tasks/:id/schedule', async ({ request }) => {
        attempts += 1;
        const body = await request.json();
        if (attempts === 1) {
          expect(body).toMatchObject({ startTime: '09:00', expectedVersion: 1 });
          return HttpResponse.json(
            {
              code: 'SCHEDULE_CONFLICT',
              message: '시간이 겹칩니다.',
              requestId: 'TMP-CONF-LICT-0001',
              conflict: {
                proposed: { date: '2026-08-31', startTime: '09:00', endTime: '10:00' },
                conflicting: { date: '2026-08-31', startTime: '09:30', endTime: '10:30' },
                nextCandidate: { date: '2026-08-31', startTime: '10:30', endTime: '11:30' },
              },
            },
            { status: 409 },
          );
        }
        expect(body).toMatchObject({ startTime: '10:30', resolutionMode: 'ACCEPT_CANDIDATE' });
        task = makeTask({
          version: 2,
          schedule: { date: '2026-08-31', startTime: '10:30', endTime: '11:30' },
        });
        return HttpResponse.json(task);
      }),
    );
    renderLive();

    await screen.findByRole('button', { name: '보고서 검토 시간 배치' });
    fireEvent.click(screen.getByRole('button', { name: '보고서 검토 시간 배치' }));
    fireEvent.click(screen.getByRole('button', { name: '시간 폼으로 배치' }));
    await screen.findByRole('dialog', { name: '보고서 검토 배치 충돌' });
    expect(screen.getByText('복원 위치: 미배치 목록')).toBeInTheDocument();
    expect(screen.getByText('실패 위치: 2026-08-31 09:00')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '보고서 검토 열기' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '10:30으로 이동' }));
    await screen.findByRole('button', { name: /보고서 검토 10:30–11:30/ });
    expect(screen.getByText('2026-08-31 10:30에 배치했습니다')).toBeInTheDocument();
    expect(attempts).toBe(2);
  });

  it('FR_009_FR_010_lists_URL_filtered_tasks_and_sets_completion', async () => {
    window.history.replaceState({}, '', '/tasks?status=TODO&page=0&size=25');
    let task = makeTask();
    server.use(
      http.get('http://127.0.0.1:8080/api/v1/tasks', () =>
        HttpResponse.json({ content: [task], page: 0, size: 25, totalElements: 1, totalPages: 1 }),
      ),
      http.put('http://127.0.0.1:8080/api/v1/tasks/:id/completion', async ({ request }) => {
        const body = await request.json();
        expect(body).toEqual({ completed: true, expectedVersion: 1 });
        task = makeTask({ status: 'COMPLETED', version: 2 });
        return HttpResponse.json(task);
      }),
    );
    renderLive();

    const checkbox = await screen.findByRole('checkbox');
    fireEvent.click(checkbox);
    await waitFor(() => expect(screen.getByRole('checkbox')).toBeChecked());
  });
});
