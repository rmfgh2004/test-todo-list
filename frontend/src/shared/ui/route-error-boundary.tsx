import { Component, type ErrorInfo, type ReactNode } from 'react';
import styles from './status-surface.module.css';

interface Props {
  readonly children: ReactNode;
  readonly requestId: string;
}

interface State {
  readonly failed: boolean;
}

/** F-C09, SECURITY-15: contains route failures and exposes no stack or component detail. */
export class RouteErrorBoundary extends Component<Props, State> {
  override state: State = { failed: false };

  static getDerivedStateFromError(): State {
    return { failed: true };
  }

  override componentDidCatch(_error: Error, _info: ErrorInfo): void {
    // A future local telemetry adapter may record the correlation ID, never the rendered stack.
  }

  override render() {
    if (!this.state.failed) return this.props.children;
    return (
      <section className={styles.error} role="alert">
        <p className={styles.eyebrow}>화면 오류</p>
        <h2>화면을 불러오지 못했습니다</h2>
        <p>다시 시도해 주세요. 문제가 계속되면 아래 요청 ID를 확인해 주세요.</p>
        <code>{this.props.requestId}</code>
        <button type="button" onClick={() => window.location.reload()}>
          다시 불러오기
        </button>
      </section>
    );
  }
}
