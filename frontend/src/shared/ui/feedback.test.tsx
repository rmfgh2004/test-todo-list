import { act, fireEvent, render, screen } from '@testing-library/react';
import { Component, type ReactNode } from 'react';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it, vi } from 'vitest';
import { FeedbackProvider, useFeedback } from './feedback';
import { RouteErrorBoundary } from './route-error-boundary';
import { ErrorStatusSurface, LoadingSurface } from './status-surface';

function FeedbackHarness() {
  const feedback = useFeedback();
  return (
    <>
      <button type="button" onClick={() => feedback.announceOutcome('화요일 10:00에 배치했습니다')}>
        성공 알림
      </button>
      <button
        type="button"
        onClick={() => feedback.announceFailure('충돌이 있어 배치하지 않았습니다')}
      >
        실패 알림
      </button>
      <button
        type="button"
        onClick={() =>
          feedback.announceFailure({
            restoredPosition: '2026-09-03 09:00',
            failedPosition: '2026-09-04 10:30',
            reason: '네트워크 오류',
            actionLabel: '다른 시간 선택',
            onAction: vi.fn(),
          })
        }
      >
        구조화 실패 알림
      </button>
    </>
  );
}

class ThrowingChild extends Component<{ children?: ReactNode }> {
  override render(): ReactNode {
    throw new Error('secret stack and component detail');
  }
}

describe('F-C09 accessible feedback', () => {
  it('UR_035_owns_exactly_one_polite_and_one_assertive_live_region', () => {
    render(
      <FeedbackProvider>
        <FeedbackHarness />
      </FeedbackProvider>,
    );

    expect(document.querySelectorAll('[aria-live="polite"]')).toHaveLength(1);
    expect(document.querySelectorAll('[aria-live="assertive"]')).toHaveLength(1);
    fireEvent.click(screen.getByRole('button', { name: '성공 알림' }));
    expect(screen.getByText('화요일 10:00에 배치했습니다')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '실패 알림' }));
    expect(screen.getByText('충돌이 있어 배치하지 않았습니다')).toBeInTheDocument();
  });

  it('SECURITY_15_route_error_fallback_shows_the_request_ID_but_never_internal_detail', () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    render(
      <RouteErrorBoundary requestId="TMP-ROUT-ER00-0001">
        <ThrowingChild />
      </RouteErrorBoundary>,
    );

    expect(screen.getByRole('alert')).toHaveTextContent('화면을 불러오지 못했습니다');
    expect(screen.getByText('TMP-ROUT-ER00-0001')).toBeInTheDocument();
    expect(document.body).not.toHaveTextContent('secret stack');
    expect(screen.getByRole('button', { name: '다시 불러오기' })).toBeInTheDocument();
  });

  it('UR_061_gates_manual_retry_for_Retry_After_and_copies_only_the_request_ID', () => {
    vi.useFakeTimers();
    const retry = vi.fn();
    const copy = vi.fn();
    render(
      <ErrorStatusSurface
        message="잠시 후 다시 시도해 주세요"
        requestId="TMP-RATE-LIMI-0001"
        retryAfterSeconds={2}
        onRetry={retry}
        onCopy={copy}
      />,
    );

    const retryButton = screen.getByRole('button', { name: '2초 후 다시 시도' });
    expect(retryButton).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: '복사' }));
    expect(copy).toHaveBeenCalledWith('TMP-RATE-LIMI-0001');
    act(() => vi.advanceTimersByTime(1_000));
    expect(screen.getByRole('button', { name: '1초 후 다시 시도' })).toBeDisabled();
    act(() => vi.advanceTimersByTime(1_000));
    expect(screen.getByRole('button', { name: '다시 시도' })).toBeEnabled();
    fireEvent.click(screen.getByRole('button', { name: '다시 시도' }));
    expect(retry).toHaveBeenCalledOnce();
    vi.useRealTimers();
  });

  it('F_C09_exposes_a_named_loading_skeleton', () => {
    vi.useFakeTimers();
    const timeout = vi.fn();
    render(<LoadingSurface onTimeout={timeout} />);
    expect(screen.getByRole('status', { name: '계획 불러오는 중' })).toBeInTheDocument();
    expect(screen.queryByLabelText('첫 번째 항목 불러오는 중')).not.toBeInTheDocument();
    act(() => vi.advanceTimersByTime(200));
    expect(screen.getByLabelText('첫 번째 항목 불러오는 중')).toBeInTheDocument();
    act(() => vi.advanceTimersByTime(9_800));
    expect(timeout).toHaveBeenCalledOnce();
    vi.useRealTimers();
  });

  it('UR_067_persists_structured_rollback_feedback_until_explicit_dismissal', () => {
    render(
      <FeedbackProvider>
        <FeedbackHarness />
      </FeedbackProvider>,
    );
    fireEvent.click(screen.getByRole('button', { name: '구조화 실패 알림' }));
    expect(screen.getByText('복원 위치: 2026-09-03 09:00')).toBeInTheDocument();
    expect(screen.getByText('실패 위치: 2026-09-04 10:30')).toBeInTheDocument();
    expect(screen.getByText('이유: 네트워크 오류')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '다른 시간 선택' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '실패 알림 닫기' }));
    expect(screen.queryByText('복원 위치: 2026-09-03 09:00')).not.toBeInTheDocument();
  });

  it('UR_066_disables_skeleton_shimmer_for_reduced_motion', () => {
    const css = readFileSync(
      resolve(process.cwd(), 'src/shared/ui/status-surface.module.css'),
      'utf8',
    );
    expect(css).toMatch(/prefers-reduced-motion:\s*reduce/);
    expect(css).toMatch(/\.skeletonBlock\s*\{[^}]*animation:\s*none/s);
  });
});
