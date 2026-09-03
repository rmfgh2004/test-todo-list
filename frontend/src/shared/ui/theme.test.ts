import { describe, expect, it } from 'vitest';
import { resolveInitialTheme } from './theme';

describe('token theme root', () => {
  it('F_N01_prefers_a_persisted_manual_override', () => {
    expect(resolveInitialTheme('light', true)).toBe('light');
    expect(resolveInitialTheme('dark', false)).toBe('dark');
  });

  it('F_N01_falls_back_to_the_system_preference_without_an_override', () => {
    expect(resolveInitialTheme(null, true)).toBe('dark');
    expect(resolveInitialTheme(null, false)).toBe('light');
    expect(resolveInitialTheme('invalid', true)).toBe('dark');
  });
});
