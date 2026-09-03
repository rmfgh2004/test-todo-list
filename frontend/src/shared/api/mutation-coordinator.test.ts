import type { components } from '@/shared/api/generated/planning-api';
import { describe, expect, it, vi } from 'vitest';
import type { SafeApiError } from './error';
import { MutationCoordinator, type MutationState } from './mutation-coordinator';

type TaskView = components['schemas']['TaskView'];

const task = {
  id: '00000000-0000-4000-8000-000000000001',
  title: '원래 제목',
  priority: 'MEDIUM',
  estimateMinutes: 30,
  status: 'TODO',
  version: 7,
  createdAt: '2026-09-01T00:00:00Z',
  updatedAt: '2026-09-01T00:00:00Z',
} satisfies TaskView;

const conflictError: SafeApiError = {
  kind: 'conflict',
  code: 'SCHEDULE_CONFLICT',
  message: '충돌',
  requestId: 'TMP-CONF-LICT-0001',
  fieldErrors: [],
};

describe('snapshot mutation coordinator', () => {
  it('F_N04_snapshots_before_optimistic_write_and_replaces_with_the_server_payload', async () => {
    const events: string[] = [];
    const serverTask = { ...task, title: '서버 제목', version: 8 };
    const coordinator = new MutationCoordinator();

    await coordinator.execute<{ title: string }, TaskView>({
      task,
      captureSnapshot: () => {
        events.push('snapshot');
        return { title: task.title };
      },
      applyOptimistic: () => events.push('optimistic'),
      request: async ({ expectedVersion }) => {
        events.push(`request:${expectedVersion}`);
        return serverTask;
      },
      replaceFromServer: (payload) => {
        events.push(`replace:${payload.title}:${payload.version}`);
      },
      rollback: () => {
        events.push('rollback');
      },
    });

    expect(events).toEqual(['snapshot', 'optimistic', 'request:7', 'replace:서버 제목:8']);
  });

  it('F_N04_completes_rollback_before_the_conflict_state_is_published', async () => {
    const events: string[] = [];
    const states: MutationState[] = [];
    const coordinator = new MutationCoordinator({
      onStateChange: (state) => {
        states.push(state);
        if (state === 'conflict') events.push('dialog');
      },
    });

    await expect(
      coordinator.execute({
        task,
        captureSnapshot: () => ({ title: task.title }),
        applyOptimistic: () => undefined,
        request: () => Promise.reject(conflictError),
        replaceFromServer: () => undefined,
        rollback: async () => {
          await Promise.resolve();
          events.push('rollback');
        },
      }),
    ).rejects.toBe(conflictError);

    expect(events).toEqual(['rollback', 'dialog']);
    expect(states).toEqual(['saving', 'conflict']);
  });

  it('F_N04_delete_waits_for_the_server_and_never_applies_an_optimistic_write', async () => {
    const optimistic = vi.fn();
    const coordinator = new MutationCoordinator();

    await coordinator.execute({
      task,
      optimistic: false,
      captureSnapshot: vi.fn(),
      applyOptimistic: optimistic,
      request: async ({ expectedVersion }) => {
        expect(expectedVersion).toBe(7);
      },
      replaceFromServer: vi.fn(),
      rollback: vi.fn(),
    });

    expect(optimistic).not.toHaveBeenCalled();
  });

  it('F_N04_allows_only_one_in_flight_mutation_per_task', async () => {
    let release: (() => void) | undefined;
    const pending = new Promise<void>((resolve) => {
      release = resolve;
    });
    const request = vi.fn(() => pending);
    const coordinator = new MutationCoordinator();
    const options = {
      task,
      captureSnapshot: () => task,
      applyOptimistic: () => undefined,
      request,
      replaceFromServer: () => undefined,
      rollback: () => undefined,
    };

    const first = coordinator.execute(options);
    await expect(coordinator.execute(options)).resolves.toBe(false);
    expect(request).toHaveBeenCalledOnce();
    release?.();
    await first;
    await coordinator.execute(options);
    expect(request).toHaveBeenCalledTimes(2);
  });

  it('F_N04_exposes_the_S_F03_proposal_cancel_and_terminal_transitions', async () => {
    const states: MutationState[] = [];
    const coordinator = new MutationCoordinator({ onStateChange: (state) => states.push(state) });

    coordinator.beginProposal();
    coordinator.cancelProposal();
    await coordinator.execute({
      task,
      captureSnapshot: () => task,
      applyOptimistic: () => undefined,
      request: async () => task,
      replaceFromServer: () => undefined,
      rollback: () => undefined,
    });

    expect(states).toEqual(['proposing', 'idle', 'saving', 'scheduled', 'idle']);
  });
});
