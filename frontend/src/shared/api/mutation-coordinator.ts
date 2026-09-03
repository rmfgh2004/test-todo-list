import type { components } from '@/shared/api/generated/planning-api';
import type { SafeApiErrorKind } from '@/shared/api/error';

type TaskView = components['schemas']['TaskView'];

export type MutationState =
  'idle' | 'proposing' | 'saving' | 'scheduled' | 'conflict' | 'stale' | 'failed';

export interface MutationCoordinatorOptions {
  readonly onStateChange?: (state: MutationState) => void;
}

export interface CoordinatedMutation<TSnapshot, TResult> {
  readonly task: TaskView;
  readonly optimistic?: boolean;
  readonly captureSnapshot: () => TSnapshot;
  readonly applyOptimistic: () => void;
  readonly request: (command: { readonly expectedVersion: number }) => Promise<TResult>;
  readonly replaceFromServer: (payload: TResult) => void;
  readonly rollback: (snapshot: TSnapshot) => void | Promise<void>;
}

const safeErrorKind = (error: unknown): SafeApiErrorKind | undefined => {
  if (typeof error !== 'object' || error === null || !('kind' in error)) return undefined;
  const kind = error.kind;
  return kind === 'validation' ||
    kind === 'not-found' ||
    kind === 'conflict' ||
    kind === 'stale' ||
    kind === 'rate-limited' ||
    kind === 'network' ||
    kind === 'unknown'
    ? kind
    : undefined;
};

/** F-N04, UR-032, UR-050, UR-052: coordinates one truthful command lifecycle per task. */
export class MutationCoordinator {
  readonly #inFlight = new Set<string>();
  readonly #onStateChange: (state: MutationState) => void;
  #state: MutationState = 'idle';

  constructor({ onStateChange = () => undefined }: MutationCoordinatorOptions = {}) {
    this.#onStateChange = onStateChange;
  }

  get state(): MutationState {
    return this.#state;
  }

  beginProposal(): void {
    this.#transition('proposing');
  }

  cancelProposal(): void {
    this.#transition('idle');
  }

  async execute<TSnapshot, TResult>({
    task,
    optimistic = true,
    captureSnapshot,
    applyOptimistic,
    request,
    replaceFromServer,
    rollback,
  }: CoordinatedMutation<TSnapshot, TResult>): Promise<boolean> {
    if (this.#inFlight.has(task.id)) return false;
    this.#inFlight.add(task.id);
    this.#transition('saving');

    const snapshot = optimistic ? { value: captureSnapshot() } : undefined;
    if (snapshot !== undefined) applyOptimistic();

    try {
      const payload = await request({ expectedVersion: task.version });
      replaceFromServer(payload);
      this.#transition('scheduled');
      this.#transition('idle');
      return true;
    } catch (error) {
      if (snapshot !== undefined) await rollback(snapshot.value);
      const kind = safeErrorKind(error);
      if (kind === 'conflict') this.#transition('conflict');
      else if (kind === 'stale') this.#transition('stale');
      else {
        this.#transition('failed');
        this.#transition('idle');
      }
      throw error;
    } finally {
      this.#inFlight.delete(task.id);
    }
  }

  #transition(state: MutationState): void {
    this.#state = state;
    this.#onStateChange(state);
  }
}
