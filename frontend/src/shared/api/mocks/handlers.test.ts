import { setupServer } from 'msw/node';
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { createPlanningHandlers } from './handlers';

const server = setupServer();
const endpoint = 'http://127.0.0.1:8080/api/v1/tasks/11111111-1111-4111-8111-111111111111';

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('generated-contract MSW handlers', () => {
  it.each([
    ['validation', 400, 'VALIDATION_FAILED'],
    ['not-found', 404, 'TASK_NOT_FOUND'],
    ['conflict', 409, 'SCHEDULE_CONFLICT'],
    ['stale', 409, 'STALE_TASK'],
    ['rate-limited', 429, 'RATE_LIMITED'],
    ['server-error', 500, 'INTERNAL_ERROR'],
  ] as const)('F_N08_serves_%s_as_HTTP_%i', async (scenario, status, code) => {
    server.use(...createPlanningHandlers(scenario));

    const response = await fetch(endpoint);
    expect(response.status).toBe(status);
    await expect(response.json()).resolves.toMatchObject({ code });
    if (status === 429) expect(response.headers.get('Retry-After')).toBe('5');
  });

  it('F_N08_models_transport_loss_without_an_HTTP_response', async () => {
    server.use(...createPlanningHandlers('transport-loss'));

    await expect(fetch(endpoint)).rejects.toThrow();
  });
});
