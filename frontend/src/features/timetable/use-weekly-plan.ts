import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { cacheKeys } from '@/shared/api/cache';
import { planningApi } from '@/shared/api/planning-api';

/** F-C02: one bounded weekly query serves both timetable and backlog. */
export const useWeeklyPlan = (weekStart: string) =>
  useQuery({
    queryKey: cacheKeys.week(weekStart),
    queryFn: () => planningApi.getWeek(weekStart),
    placeholderData: keepPreviousData,
  });
