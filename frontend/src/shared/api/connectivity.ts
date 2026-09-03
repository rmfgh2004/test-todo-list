export type ConnectivityState = 'connected' | 'disconnected';

export interface ConnectivityMonitorOptions {
  readonly poll: () => void | Promise<void>;
  readonly intervalMs?: number;
  readonly maxAttempts?: number;
  readonly onStateChange?: (state: ConnectivityState) => void;
}

/** F-N03, NFR-008: tracks transport reachability without hiding HTTP errors behind a breaker. */
export class ConnectivityMonitor {
  readonly #poll: () => void | Promise<void>;
  readonly #intervalMs: number;
  readonly #maxAttempts: number;
  readonly #onStateChange: (state: ConnectivityState) => void;
  #timer: ReturnType<typeof setInterval> | undefined;
  #state: ConnectivityState = 'connected';
  #pollAttempts = 0;

  constructor({
    poll,
    intervalMs = 5_000,
    maxAttempts = 24,
    onStateChange = () => undefined,
  }: ConnectivityMonitorOptions) {
    if (intervalMs <= 0 || !Number.isInteger(maxAttempts) || maxAttempts <= 0) {
      throw new Error('Connectivity polling limits must be positive');
    }
    this.#poll = poll;
    this.#intervalMs = intervalMs;
    this.#maxAttempts = maxAttempts;
    this.#onStateChange = onStateChange;
  }

  get state(): ConnectivityState {
    return this.#state;
  }

  get pollAttempts(): number {
    return this.#pollAttempts;
  }

  get isPolling(): boolean {
    return this.#timer !== undefined;
  }

  get isMutationDisabled(): boolean {
    return this.#state === 'disconnected';
  }

  recordTransportFailure(): void {
    this.#transition('disconnected');
    if (this.#timer !== undefined || this.#pollAttempts >= this.#maxAttempts) return;
    this.#timer = setInterval(() => {
      this.#pollAttempts += 1;
      void this.#poll();
      if (this.#pollAttempts >= this.#maxAttempts) this.#stopPolling();
    }, this.#intervalMs);
  }

  recordHttpResponse(_status: number): void {
    this.#transition('connected');
    this.#pollAttempts = 0;
    this.#stopPolling();
  }

  manualRetry(): void {
    this.#pollAttempts = 0;
    void this.#poll();
  }

  dispose(): void {
    this.#stopPolling();
  }

  #stopPolling(): void {
    if (this.#timer === undefined) return;
    clearInterval(this.#timer);
    this.#timer = undefined;
  }

  #transition(state: ConnectivityState): void {
    if (state === this.#state) return;
    this.#state = state;
    this.#onStateChange(state);
  }
}

type ConnectivityListener = () => void;

let runtimePoll: () => void | Promise<void> = () => undefined;
let runtimeState: ConnectivityState = 'connected';
const runtimeListeners = new Set<ConnectivityListener>();
const runtimeMonitor = new ConnectivityMonitor({
  poll: () => runtimePoll(),
  onStateChange: (state) => {
    runtimeState = state;
    runtimeListeners.forEach((listener) => listener());
  },
});

/** F-N03: one browser-wide transport observer drives the shell and active-query recovery polling. */
export const connectivityRuntime = {
  recordTransportFailure: () => runtimeMonitor.recordTransportFailure(),
  recordHttpResponse: (status: number) => runtimeMonitor.recordHttpResponse(status),
  configurePoll: (poll: () => void | Promise<void>) => {
    runtimePoll = poll;
  },
  subscribe: (listener: ConnectivityListener) => {
    runtimeListeners.add(listener);
    return () => runtimeListeners.delete(listener);
  },
  getSnapshot: () => runtimeState,
} as const;
