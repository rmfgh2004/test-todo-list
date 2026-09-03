export type Theme = 'light' | 'dark';

/** F-N01: a valid manual override wins; otherwise the operating-system preference is used. */
export const resolveInitialTheme = (stored: string | null, systemDark: boolean): Theme => {
  if (stored === 'light' || stored === 'dark') return stored;
  return systemDark ? 'dark' : 'light';
};
