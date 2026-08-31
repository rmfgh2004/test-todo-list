import { rm } from 'node:fs/promises';
import { fileURLToPath, URL } from 'node:url';
import { defineConfig, type Plugin } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * SECURITY-09: `public/mockServiceWorker.js` must be served in development and must never ship.
 * Vite copies `public/` verbatim into `dist/`, so the worker is removed from the production output
 * here and `scripts/check-bundle-budget.mjs` fails the build if it survives anyway.
 */
function excludeMockWorkerFromBuild(): Plugin {
  return {
    name: 'exclude-mock-worker-from-build',
    apply: 'build',
    async closeBundle() {
      await rm(fileURLToPath(new URL('./dist/mockServiceWorker.js', import.meta.url)), {
        force: true,
      });
    },
  };
}

/**
 * U2 build configuration.
 *
 * NFR-001: the dev server binds to loopback only and loads no third-party origin.
 * SECURITY-09: the MSW mock transport is development-only; `scripts/check-bundle-budget.mjs`
 * fails the build if it is reachable from the shipped bundle.
 */
export default defineConfig({
  plugins: [react(), excludeMockWorkerFromBuild()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
  },
  preview: {
    host: '127.0.0.1',
    port: 4173,
    strictPort: true,
  },
  build: {
    target: 'es2022',
    sourcemap: false,
    reportCompressedSize: true,
  },
});
