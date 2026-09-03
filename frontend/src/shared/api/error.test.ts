import { describe, expect, it } from 'vitest';
import { normalizeApiError } from './error';

const apiResponse = (
  status: number,
  body: unknown,
  headers: Record<string, string> = {},
): Response =>
  new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', ...headers },
  });

describe('safe API error normalization', () => {
  it.each([
    [400, 'VALIDATION_FAILED', 'validation'],
    [404, 'TASK_NOT_FOUND', 'not-found'],
    [409, 'SCHEDULE_CONFLICT', 'conflict'],
    [409, 'STALE_TASK', 'stale'],
  ] as const)('F_N05_maps_HTTP_%i_%s_to_%s', async (status, code, kind) => {
    const error = await normalizeApiError(
      apiResponse(status, {
        code,
        message: '처리할 수 없습니다.',
        requestId: 'TMP-AAAA-BBBB-CCCC',
      }),
    );

    expect(error).toMatchObject({ kind, code, requestId: 'TMP-AAAA-BBBB-CCCC' });
  });

  it('F_N05_extracts_Retry_After_for_rate_limited_errors', async () => {
    const error = await normalizeApiError(
      apiResponse(
        429,
        {
          code: 'RATE_LIMITED',
          message: '잠시 후 다시 시도하세요.',
          requestId: 'TMP-RATE-LIMIT-0001',
        },
        { 'Retry-After': '7' },
      ),
    );

    expect(error).toMatchObject({ kind: 'rate-limited', retryAfterSeconds: 7 });
  });

  it('F_N05_maps_fetch_rejection_to_network_without_leaking_the_original_message', async () => {
    const error = await normalizeApiError(new TypeError('secret internal connection detail'));

    expect(error).toMatchObject({
      kind: 'network',
      code: 'NETWORK_ERROR',
      message: '백엔드에 연결할 수 없습니다.',
    });
    expect(error.message).not.toContain('secret');
  });

  it('SECURITY_05_degrades_unknown_payloads_and_filters_field_names', async () => {
    const error = await normalizeApiError(
      apiResponse(400, {
        code: 'BRAND_NEW_CODE',
        message: '안전한 안내',
        requestId: 'TMP-UNKN-OWN0-0001',
        stack: '/private/internal/path',
        fieldErrors: [
          { field: 'title', code: 'INVALID', message: '제목 오류' },
          { field: 'admin', code: 'LEAK', message: '숨겨야 함' },
        ],
      }),
    );

    expect(error.kind).toBe('unknown');
    expect(error.fieldErrors).toEqual([{ field: 'title', code: 'INVALID', message: '제목 오류' }]);
    expect(JSON.stringify(error)).not.toContain('/private/internal/path');
    expect(JSON.stringify(error)).not.toContain('admin');
  });

  it('SECURITY_05_degrades_every_unregistered_400_code_to_unknown', async () => {
    const error = await normalizeApiError(
      apiResponse(400, {
        code: 'A_DIFFERENT_FUTURE_CODE',
        message: '새 오류',
        requestId: 'TMP-FUTU-RE00-0001',
      }),
    );

    expect(error.kind).toBe('unknown');
  });
});
