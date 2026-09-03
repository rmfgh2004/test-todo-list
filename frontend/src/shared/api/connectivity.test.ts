import { afterEach, describe, expect, it, vi } from 'vitest';
import { ConnectivityMonitor } from './connectivity';

afterEach(() => vi.useRealTimers());

describe('transport-only connectivity monitor', () => {
  it('F_N03_polls_queries_every_five_seconds_and_stops_after_24_attempts', () => {
    vi.useFakeTimers();
    const poll = vi.fn();
    const monitor = new ConnectivityMonitor({ poll });

    monitor.recordTransportFailure();
    expect(monitor.state).toBe('disconnected');
    expect(monitor.isMutationDisabled).toBe(true);

    vi.advanceTimersByTime(5_000 * 24);
    expect(poll).toHaveBeenCalledTimes(24);
    expect(monitor.pollAttempts).toBe(24);
    expect(monitor.isPolling).toBe(false);
  });

  it.each([400, 404, 409, 429, 500])('F_N03_HTTP_%i_never_enters_disconnected', (status) => {
    const monitor = new ConnectivityMonitor({ poll: vi.fn() });

    monitor.recordHttpResponse(status);

    expect(monitor.state).toBe('connected');
    expect(monitor.isMutationDisabled).toBe(false);
  });

  it('F_N03_the_first_HTTP_response_clears_disconnected_and_stops_polling', () => {
    vi.useFakeTimers();
    const monitor = new ConnectivityMonitor({ poll: vi.fn() });
    monitor.recordTransportFailure();

    monitor.recordHttpResponse(503);

    expect(monitor.state).toBe('connected');
    expect(monitor.isPolling).toBe(false);
  });

  it('F_N03_manual_retry_polls_once_and_dispose_stops_the_timer', () => {
    vi.useFakeTimers();
    const poll = vi.fn();
    const monitor = new ConnectivityMonitor({ poll, intervalMs: 10, maxAttempts: 2 });
    monitor.recordTransportFailure();

    monitor.manualRetry();
    monitor.dispose();
    vi.advanceTimersByTime(100);

    expect(poll).toHaveBeenCalledOnce();
    expect(monitor.isPolling).toBe(false);
  });

  it('F_N03_publishes_only_real_connectivity_transitions', () => {
    const onStateChange = vi.fn();
    const monitor = new ConnectivityMonitor({ poll: vi.fn(), onStateChange });

    monitor.recordHttpResponse(200);
    monitor.recordTransportFailure();
    monitor.recordTransportFailure();
    monitor.recordHttpResponse(500);

    expect(onStateChange.mock.calls.map(([state]) => state)).toEqual(['disconnected', 'connected']);
    monitor.dispose();
  });

  it.each([{ intervalMs: 0 }, { maxAttempts: 0 }, { maxAttempts: 1.5 }])(
    'F_N03_rejects_invalid_polling_limits_%#',
    (options) => {
      expect(() => new ConnectivityMonitor({ poll: vi.fn(), ...options })).toThrow(
        'Connectivity polling limits must be positive',
      );
    },
  );
});
