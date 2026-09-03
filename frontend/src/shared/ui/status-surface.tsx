import { useEffect, useState } from 'react';
import styles from './status-surface.module.css';

export function SkeletonBlock({ label = '내용 불러오는 중' }: { readonly label?: string }) {
  return <span className={styles.skeletonBlock} aria-label={label} />;
}

/** UR-066: delays visual skeletons for fast responses and escalates slow loads at 10 seconds. */
export function LoadingSurface({ onTimeout }: { readonly onTimeout?: () => void } = {}) {
  const [visible, setVisible] = useState(false);
  useEffect(() => {
    const showTimer = window.setTimeout(() => setVisible(true), 200);
    const timeoutTimer = window.setTimeout(() => onTimeout?.(), 10_000);
    return () => {
      window.clearTimeout(showTimer);
      window.clearTimeout(timeoutTimer);
    };
  }, [onTimeout]);
  return (
    <section className={styles.loading} aria-label="계획 불러오는 중" role="status">
      {visible ? (
        <>
          <SkeletonBlock label="첫 번째 항목 불러오는 중" />
          <SkeletonBlock label="두 번째 항목 불러오는 중" />
          <SkeletonBlock label="세 번째 항목 불러오는 중" />
        </>
      ) : null}
    </section>
  );
}

export interface ErrorStatusSurfaceProps {
  readonly message: string;
  readonly requestId: string;
  readonly retryAfterSeconds?: number;
  readonly onRetry: () => void;
  readonly onCopy?: (requestId: string) => void | Promise<void>;
}

/** F-C09, UR-061: safe failure surface with copyable correlation and manual gated retry. */
export function ErrorStatusSurface({
  message,
  requestId,
  retryAfterSeconds = 0,
  onRetry,
  onCopy = (value) => navigator.clipboard.writeText(value),
}: ErrorStatusSurfaceProps) {
  const [remaining, setRemaining] = useState(Math.max(0, retryAfterSeconds));

  useEffect(() => {
    if (remaining <= 0) return undefined;
    const timer = window.setTimeout(() => setRemaining((value) => Math.max(0, value - 1)), 1_000);
    return () => window.clearTimeout(timer);
  }, [remaining]);

  return (
    <section className={styles.error} role="alert">
      <p className={styles.eyebrow}>요청 실패</p>
      <h2>{message}</h2>
      <div className={styles.requestId}>
        <span>요청 ID</span>
        <code>{requestId}</code>
        <button type="button" onClick={() => void onCopy(requestId)}>
          복사
        </button>
      </div>
      <button type="button" disabled={remaining > 0} onClick={onRetry}>
        {remaining > 0 ? `${remaining}초 후 다시 시도` : '다시 시도'}
      </button>
    </section>
  );
}
