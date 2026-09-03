import { useQuery } from '@tanstack/react-query';
import { cacheKeys } from '@/shared/api/cache';
import { planningApi } from '@/shared/api/planning-api';

export const useTaskList = (serializedQuery: string) =>
  useQuery({
    queryKey: cacheKeys.tasks(serializedQuery),
    queryFn: () => planningApi.listTasks(serializedQuery),
  });
