import { QueryClient, type QueryKey } from '@tanstack/react-query';

export const cacheKeys = {
  week: (weekStart: string) => ['week', weekStart] as const,
  tasks: (serializedQuery: string) => ['tasks', serializedQuery] as const,
  task: (taskId: string) => ['task', taskId] as const,
};

export interface PlanningInvalidationScope {
  readonly taskId: string;
  readonly fromWeek?: string;
  readonly toWeek?: string;
}

/** F-N02, UR-062: GET queries retry at most twice; commands are never replayed automatically. */
export const createPlanningQueryClient = (): QueryClient =>
  new QueryClient({
    defaultOptions: {
      queries: {
        retry: (failureCount) => failureCount < 2,
        retryDelay: (attempt) => Math.min(500 * 2 ** attempt, 5_000),
      },
      mutations: { retry: false },
    },
  });

const uniqueWeekKeys = ({ fromWeek, toWeek }: PlanningInvalidationScope): readonly QueryKey[] => {
  const weeks = new Set([fromWeek, toWeek].filter((week): week is string => week !== undefined));
  return Array.from(weeks, cacheKeys.week);
};

/** F-N02, NFR-005: invalidates only the affected detail, lists and explicit week boundaries. */
export const invalidatePlanningScope = async (
  client: QueryClient,
  scope: PlanningInvalidationScope,
): Promise<void> => {
  const keys: readonly QueryKey[] = [
    cacheKeys.task(scope.taskId),
    ['tasks'],
    ...uniqueWeekKeys(scope),
  ];
  await Promise.all(keys.map((queryKey) => client.invalidateQueries({ queryKey })));
};
