import { createContext, type ReactNode, useContext, useMemo, useState } from 'react';
import styles from './feedback.module.css';

export interface FailureFeedback {
  readonly restoredPosition: string;
  readonly failedPosition: string;
  readonly reason: string;
  readonly actionLabel: string;
  readonly onAction: () => void;
}

interface FeedbackApi {
  readonly announceOutcome: (message: string) => void;
  readonly announceFailure: (message: string | FailureFeedback) => void;
}

const FeedbackContext = createContext<FeedbackApi | undefined>(undefined);

/** F-C09, UR-035: owns exactly two application live regions and one message per channel. */
export function FeedbackProvider({ children }: { readonly children: ReactNode }) {
  const [outcome, setOutcome] = useState('');
  const [failure, setFailure] = useState<string | FailureFeedback>('');
  const api = useMemo(() => ({ announceOutcome: setOutcome, announceFailure: setFailure }), []);

  return (
    <FeedbackContext.Provider value={api}>
      {children}
      <div className={styles.toastRack}>
        <div
          className={`${styles.toast} ${outcome === '' ? styles.empty : ''}`}
          aria-live="polite"
          aria-atomic="true"
        >
          {outcome}
        </div>
        <div
          className={`${styles.toast} ${styles.failure} ${failure === '' ? styles.empty : ''}`}
          aria-live="assertive"
          aria-atomic="true"
        >
          {typeof failure === 'string' ? (
            failure
          ) : (
            <>
              <strong>저장하지 못해 되돌렸습니다</strong>
              <span>복원 위치: {failure.restoredPosition}</span>
              <span>실패 위치: {failure.failedPosition}</span>
              <span>이유: {failure.reason}</span>
              <button
                type="button"
                onClick={() => {
                  failure.onAction();
                  setFailure('');
                }}
              >
                {failure.actionLabel}
              </button>
            </>
          )}
          {failure === '' ? null : (
            <button type="button" aria-label="실패 알림 닫기" onClick={() => setFailure('')}>
              닫기
            </button>
          )}
        </div>
      </div>
    </FeedbackContext.Provider>
  );
}

export const useFeedback = (): FeedbackApi => {
  const value = useContext(FeedbackContext);
  if (value === undefined) throw new Error('useFeedback must be used inside FeedbackProvider');
  return value;
};
