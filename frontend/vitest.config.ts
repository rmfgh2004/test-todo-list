import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

/**
 * Unit and component test configuration.
 *
 * NFR-001: coverage thresholds are blocking (80% statements/functions/lines, 75% branches).
 * The capacity profile lives in `vitest.capacity.config.ts` so the default run stays fast,
 * mirroring U1's `-Pcapacity` split.
 */
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./tests/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}', 'tests/unit/**/*.test.{ts,tsx}'],
    exclude: ['tests/e2e/**', 'tests/capacity/**', 'node_modules/**'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json-summary'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: [
        'src/**/*.test.{ts,tsx}',
        'src/main.tsx',
        'src/vite-env.d.ts',
        'src/shared/api/generated/**',
        'src/shared/api/mocks/**',
      ],
      thresholds: {
        statements: 80,
        functions: 80,
        lines: 80,
        branches: 75,
      },
    },
  },
});
