import { normalizeApiError, type SafeApiError } from '@/shared/api/error';

export interface ConnectivityObserver {
  recordTransportFailure(): void;
  recordHttpResponse(status: number): void;
}

export interface ApiClientOptions {
  readonly baseUrl?: string;
  readonly fetchImpl?: typeof fetch;
  readonly requestIdFactory?: () => string;
  readonly observer?: ConnectivityObserver;
}

export interface ApiRequest<T> {
  readonly method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  readonly path: string;
  readonly body?: unknown;
  readonly validate: (value: unknown) => value is T;
}

export interface ApiClient {
  request<T>(request: ApiRequest<T>): Promise<T>;
  requestVoid(request: Omit<ApiRequest<never>, 'validate'>): Promise<void>;
}

const bytesFromCrypto = (): Uint8Array => crypto.getRandomValues(new Uint8Array(6));

/** NFR-008: generates one correlation ID per request in the approved TMP shape. */
export const generateRequestId = (bytesFactory = bytesFromCrypto): string => {
  const bytes = bytesFactory();
  if (bytes.length !== 6) throw new Error('Request ID entropy must contain exactly six bytes');
  const hex = Array.from(bytes, (value) => value.toString(16).padStart(2, '0').toUpperCase()).join(
    '',
  );
  return `TMP-${hex.slice(0, 4)}-${hex.slice(4, 8)}-${hex.slice(8, 12)}`;
};

const invalidResponse = (requestId: string): SafeApiError => ({
  kind: 'unknown',
  code: 'INVALID_RESPONSE',
  message: '백엔드 응답 형식을 확인할 수 없습니다.',
  requestId,
  fieldErrors: [],
});

/** F-C08, NFR-008, SECURITY-13: fetches once, correlates and validates before cache entry. */
export const createApiClient = ({
  baseUrl = 'http://127.0.0.1:8080',
  fetchImpl,
  requestIdFactory = generateRequestId,
  observer,
}: ApiClientOptions = {}): ApiClient => {
  const send = async ({
    method,
    path,
    body,
  }: Omit<ApiRequest<never>, 'validate'>): Promise<{ response: Response; requestId: string }> => {
    const requestId = requestIdFactory();
    let response: Response;
    try {
      response = await (fetchImpl ?? fetch)(`${baseUrl}${path}`, {
        method,
        headers: {
          Accept: 'application/json',
          'X-Request-Id': requestId,
          ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
        },
        ...(body === undefined ? {} : { body: JSON.stringify(body) }),
      });
    } catch (error) {
      observer?.recordTransportFailure();
      throw await normalizeApiError(error, requestId);
    }

    observer?.recordHttpResponse(response.status);
    if (!response.ok) throw await normalizeApiError(response, requestId);
    return { response, requestId };
  };

  return {
    async request<T>({ method, path, body, validate }: ApiRequest<T>): Promise<T> {
      const { response, requestId } = await send({
        method,
        path,
        ...(body === undefined ? {} : { body }),
      });

      let payload: unknown;
      try {
        payload = await response.json();
      } catch {
        throw invalidResponse(requestId);
      }
      if (!validate(payload)) throw invalidResponse(requestId);
      return payload;
    },
    async requestVoid({ method, path, body }): Promise<void> {
      const { response, requestId } = await send({
        method,
        path,
        ...(body === undefined ? {} : { body }),
      });
      if (response.status !== 204) throw invalidResponse(requestId);
    },
  };
};
