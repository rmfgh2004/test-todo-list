import { QueryClient } from '@tanstack/react-query';
import { describe, expect, it, vi } from 'vitest';
import { cacheKeys, createPlanningQueryClient, invalidatePlanningScope } from './cache';

describe('planning query cache policy', () => {
  it('F_N02_builds_stable_week_list_and_detail_keys', () => {
    expect(cacheKeys.week('2026-08-31')).toEqual(['week', '2026-08-31']);
    expect(cacheKeys.tasks('status=TODO&page=0')).toEqual(['tasks', 'status=TODO&page=0']);
    expect(cacheKeys.task('task-1')).toEqual(['task', 'task-1']);
  });

  it('F_N02_retries_queries_at_most_twice_and_never_retries_mutations', () => {
    const client = createPlanningQueryClient();
    const queryRetry = client.getDefaultOptions().queries?.retry;

    expect(typeof queryRetry).toBe('function');
    if (typeof queryRetry !== 'function') throw new Error('query retry policy is missing');
    expect(queryRetry(0, new Error('first'))).toBe(true);
    expect(queryRetry(1, new Error('second'))).toBe(true);
    expect(queryRetry(2, new Error('third'))).toBe(false);
    expect(client.getDefaultOptions().mutations?.retry).toBe(false);
  });

  it('F_N02_invalidates_only_the_task_lists_detail_and_both_cross_week_keys', async () => {
    const client = new QueryClient();
    const invalidate = vi.spyOn(client, 'invalidateQueries').mockResolvedValue();

    await invalidatePlanningScope(client, {
      taskId: 'task-1',
      fromWeek: '2026-08-31',
      toWeek: '2026-09-07',
    });

    expect(invalidate.mock.calls.map(([filters]) => filters?.queryKey)).toEqual([
      ['task', 'task-1'],
      ['tasks'],
      ['week', '2026-08-31'],
      ['week', '2026-09-07'],
    ]);
    expect(invalidate).not.toHaveBeenCalledWith();
  });

  it('F_N02_deduplicates_the_week_key_for_a_same_week_mutation', async () => {
    const client = new QueryClient();
    const invalidate = vi.spyOn(client, 'invalidateQueries').mockResolvedValue();

    await invalidatePlanningScope(client, {
      taskId: 'task-1',
      fromWeek: '2026-08-31',
      toWeek: '2026-08-31',
    });

    expect(invalidate.mock.calls.map(([filters]) => filters?.queryKey)).toEqual([
      ['task', 'task-1'],
      ['tasks'],
      ['week', '2026-08-31'],
    ]);
  });
});
