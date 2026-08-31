import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

/**
 * NFR-005 capacity profile: the 1,000-task render measurement only.
 * Separated from `npm run verify` so the default gate stays fast.
 */
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./tests/setup.ts'],
    include: ['tests/capacity/**/*.test.{ts,tsx}'],
    testTimeout: 120_000,
  },
});
