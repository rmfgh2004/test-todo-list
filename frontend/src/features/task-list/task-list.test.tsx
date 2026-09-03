import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TaskListPage } from './TaskListPage';

beforeEach(() => window.history.replaceState({}, '', '/tasks?page=0&size=25'));
afterEach(() => vi.useRealTimers());

describe('F-C07 task list', () => {
  it('FR_010_keeps_allowlisted_filters_and_paging_in_the_URL', () => {
    render(
      <TaskListPage
        tasks={[
          { id: 'task-1', title: '보고서', priority: 'HIGH', estimateMinutes: 60, status: 'TODO' },
        ]}
        page={0}
        totalPages={2}
        onOpenTask={vi.fn()}
      />,
    );
    fireEvent.change(screen.getByLabelText('상태'), { target: { value: 'COMPLETED' } });
    expect(window.location.search).toContain('status=COMPLETED');
    fireEvent.click(screen.getByRole('button', { name: '다음 페이지' }));
    expect(window.location.search).toContain('page=1');
  });

  it('FR_010_distinguishes_no_results_from_an_empty_dataset', () => {
    render(<TaskListPage tasks={[]} page={0} totalPages={0} filtered onOpenTask={vi.fn()} />);
    expect(screen.getByText('조건에 맞는 할 일이 없습니다')).toBeInTheDocument();
  });

  it('UR_070_groups_the_existing_page_without_adding_query_parameters', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-09-03T03:00:00Z'));
    render(
      <TaskListPage
        tasks={[
          {
            id: 'today',
            title: '오늘 일',
            priority: 'HIGH',
            estimateMinutes: 30,
            status: 'TODO',
            dueDate: '2026-09-03',
          },
          {
            id: 'week',
            title: '주간 일',
            priority: 'MEDIUM',
            estimateMinutes: 60,
            status: 'TODO',
            dueDate: '2026-09-05',
          },
          {
            id: 'done',
            title: '완료 일',
            priority: 'LOW',
            estimateMinutes: 15,
            status: 'COMPLETED',
          },
        ]}
        page={0}
        totalPages={1}
        onOpenTask={vi.fn()}
      />,
    );
    expect(screen.getByRole('heading', { name: '오늘' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '이번 주' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '완료' })).toBeInTheDocument();
    expect(screen.getAllByRole('table')).toHaveLength(3);
    expect(window.location.search).toBe('?page=0&size=25');
  });
});
