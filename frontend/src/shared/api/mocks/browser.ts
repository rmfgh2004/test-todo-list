import { setupWorker } from 'msw/browser';
import { createPlanningHandlers } from '@/shared/api/mocks/handlers';

export const planningWorker = setupWorker(...createPlanningHandlers());
