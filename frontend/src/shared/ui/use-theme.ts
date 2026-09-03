import { useEffect, useState } from 'react';
import { resolveInitialTheme, type Theme } from './theme';

const STORAGE_KEY = 'tempo-theme';

const storedTheme = (): string | null => {
  try {
    return window.localStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
};

const systemDark = (): boolean =>
  typeof window.matchMedia === 'function' &&
  window.matchMedia('(prefers-color-scheme: dark)').matches;

/** F-N01: owns the sole runtime theme read; feature components consume CSS tokens only. */
export const useTheme = () => {
  const [theme, setTheme] = useState<Theme>(() => resolveInitialTheme(storedTheme(), systemDark()));

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  const toggleTheme = () => {
    const next = theme === 'light' ? 'dark' : 'light';
    try {
      window.localStorage.setItem(STORAGE_KEY, next);
    } catch {
      // The visual mode still changes when browser storage is unavailable.
    }
    setTheme(next);
  };

  return { theme, toggleTheme } as const;
};
