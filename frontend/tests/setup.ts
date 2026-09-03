import '@testing-library/jest-dom/vitest';

const storedValues = new Map<string, string>();
const testStorage = {
  get length() {
    return storedValues.size;
  },
  clear: () => storedValues.clear(),
  getItem: (key: string) => storedValues.get(key) ?? null,
  key: (index: number) => Array.from(storedValues.keys())[index] ?? null,
  removeItem: (key: string) => {
    storedValues.delete(key);
  },
  setItem: (key: string, value: string) => {
    storedValues.set(key, value);
  },
} satisfies Storage;

Object.defineProperty(window, 'localStorage', { configurable: true, value: testStorage });
