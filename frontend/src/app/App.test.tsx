import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';
import axe from 'axe-core';
import { App } from './App';
import { layoutModeForWidth } from './layout';

beforeEach(() => window.localStorage.clear());

describe('F-C01 application shell', () => {
  it('FR_001_renders_only_the_approved_week_and_list_navigation', () => {
    render(<App />);

    expect(screen.getByRole('banner')).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: '보기 전환' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '주간' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '목록' })).toBeInTheDocument();
    expect(screen.queryByText('보드')).not.toBeInTheDocument();
    expect(screen.queryByText('태그')).not.toBeInTheDocument();
    expect(screen.queryByText('반복')).not.toBeInTheDocument();
  });

  it('NFR_004_exposes_skip_navigation_and_real_week_controls', () => {
    render(<App />);

    expect(screen.getByRole('link', { name: '본문으로 건너뛰기' })).toHaveAttribute(
      'href',
      '#main-content',
    );
    expect(screen.getByRole('button', { name: '이전 주' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '오늘' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '다음 주' })).toBeInTheDocument();
  });

  it('F_N03_displays_the_connectivity_banner_and_disables_mutating_controls', () => {
    render(<App connectivity="disconnected" />);

    expect(screen.getByRole('status')).toHaveTextContent('백엔드 연결을 확인하고 있습니다');
    expect(screen.getByRole('button', { name: '새 할 일' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '첫 할 일 만들기' })).toBeDisabled();
  });

  it('F_N01_switches_theme_and_persists_the_manual_override', () => {
    render(<App />);
    const toggle = screen.getByRole('button', { name: '다크 모드 사용' });

    fireEvent.click(toggle);

    expect(document.documentElement).toHaveAttribute('data-theme', 'dark');
    expect(window.localStorage.getItem('tempo-theme')).toBe('dark');
    expect(toggle).toHaveAccessibleName('라이트 모드 사용');
  });

  it('FR_011_uses_one_day_below_768px_and_seven_days_at_the_breakpoint', () => {
    expect(layoutModeForWidth(320)).toEqual({ mode: 'day', visibleDays: 1 });
    expect(layoutModeForWidth(767)).toEqual({ mode: 'day', visibleDays: 1 });
    expect(layoutModeForWidth(768)).toEqual({ mode: 'week', visibleDays: 7 });
  });

  it.each(['light', 'dark'] as const)(
    'NFR_004_has_no_serious_axe_violations_in_%s_theme',
    async (theme) => {
      document.documentElement.dataset.theme = theme;
      const { container } = render(<App />);
      const result = await axe.run(container, {
        rules: { 'color-contrast': { enabled: false } },
      });

      expect(
        result.violations.filter((violation) =>
          ['serious', 'critical'].includes(violation.impact ?? ''),
        ),
      ).toEqual([]);
    },
  );
});
