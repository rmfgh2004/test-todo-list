import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { App } from './App';

/**
 * Step 1 scaffold smoke test.
 *
 * Proves the toolchain renders a React tree under jsdom before any feature exists.
 * FR-001 is only stubbed here; the real weekly planner arrives in Step 12.
 */
describe('App scaffold', () => {
  it('renders the application landmark and title', () => {
    render(<App />);

    expect(screen.getByRole('banner')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: '주간 계획' })).toBeInTheDocument();
    expect(screen.getByRole('main')).toBeInTheDocument();
  });
});
