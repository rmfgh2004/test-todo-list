import { HttpResponse, http } from 'msw';
import { setupServer } from 'msw/node';
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import { createApiClient, generateRequestId } from './client';
import { isTaskPageView } from './validation';

const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('generated-contract fetch wrapper', () => {
  it('NFR_008_generates_the_TMP_request_ID_shape', () => {
    expect(generateRequestId(() => new Uint8Array([1, 2, 3, 4, 5, 6]))).toBe('TMP-0102-0304-0506');
  });

  it('NFR_008_propagates_a_fresh_request_ID_and_validates_the_response', async () => {
    server.use(
      http.get('http://127.0.0.1:8080/api/v1/tasks', ({ request }) => {
        expect(request.headers.get('X-Request-Id')).toBe('TMP-TEST-TEST-0001');
        return HttpResponse.json({
          content: [],
          page: 0,
          size: 25,
          totalElements: 0,
          totalPages: 0,
        });
      }),
    );
    const client = createApiClient({ requestIdFactory: () => 'TMP-TEST-TEST-0001' });

    await expect(
      client.request({ method: 'GET', path: '/api/v1/tasks', validate: isTaskPageView }),
    ).resolves.toMatchObject({ content: [], page: 0 });
  });

  it('SECURITY_13_rejects_a_malformed_success_payload_before_cache_entry', async () => {
    server.use(
      http.get('http://127.0.0.1:8080/api/v1/tasks', () =>
        HttpResponse.json({ content: 'not-an-array' }),
      ),
    );
    const client = createApiClient({ requestIdFactory: () => 'TMP-TEST-TEST-0002' });

    await expect(
      client.request({ method: 'GET', path: '/api/v1/tasks', validate: isTaskPageView }),
    ).rejects.toMatchObject({ kind: 'unknown', code: 'INVALID_RESPONSE' });
  });

  it('F_N03_reports_transport_failure_but_treats_HTTP_5xx_as_a_server_response', async () => {
    const observer = {
      recordTransportFailure: vi.fn(),
      recordHttpResponse: vi.fn(),
    };
    const client = createApiClient({ observer });
    server.use(
      http.get('http://127.0.0.1:8080/api/v1/tasks', () =>
        HttpResponse.json(
          { code: 'INTERNAL_ERROR', message: '실패', requestId: 'TMP-SERV-ER00-0001' },
          { status: 500 },
        ),
      ),
    );

    await expect(
      client.request({ method: 'GET', path: '/api/v1/tasks', validate: isTaskPageView }),
    ).rejects.toMatchObject({ kind: 'unknown' });
    expect(observer.recordHttpResponse).toHaveBeenCalledOnce();
    expect(observer.recordTransportFailure).not.toHaveBeenCalled();

    server.use(http.get('http://127.0.0.1:8080/api/v1/tasks', () => HttpResponse.error()));
    await expect(
      client.request({ method: 'GET', path: '/api/v1/tasks', validate: isTaskPageView }),
    ).rejects.toMatchObject({ kind: 'network' });
    expect(observer.recordTransportFailure).toHaveBeenCalledOnce();
  });

  it('F_C08_accepts_a_204_mutation_response_without_attempting_JSON_parsing', async () => {
    server.use(
      http.delete(
        'http://127.0.0.1:8080/api/v1/tasks/task-1',
        () => new HttpResponse(null, { status: 204 }),
      ),
    );
    const client = createApiClient({ requestIdFactory: () => 'TMP-TEST-TEST-0003' });

    await expect(
      client.requestVoid({ method: 'DELETE', path: '/api/v1/tasks/task-1?confirmed=true' }),
    ).resolves.toBeUndefined();
  });
});
