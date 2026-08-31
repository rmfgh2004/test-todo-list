#!/usr/bin/env node
/**
 * NFR-005: blocking bundle budget.
 * SECURITY-09: proves the MSW mock transport is not reachable from the shipped bundle.
 *
 * Runs after `vite build`. Exits non-zero on a violation so the build fails.
 */
import { readdir, readFile, stat } from 'node:fs/promises';
import { gzipSync } from 'node:zlib';
import { join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const BUDGET_BYTES = 250 * 1024; // 250KB gzip, total of the initially loaded assets
const DIST = fileURLToPath(new URL('../dist', import.meta.url));
const MOCK_MARKERS = ['msw/browser', 'mockServiceWorker', 'setupWorker'];
/** Files that must never exist in a production build, whatever their contents. */
const FORBIDDEN_FILES = ['mockServiceWorker.js'];

async function walk(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const full = join(dir, entry.name);
    if (entry.isDirectory()) files.push(...(await walk(full)));
    else files.push(full);
  }
  return files;
}

const distExists = await stat(DIST).catch(() => null);
if (distExists === null) {
  console.error('bundle budget: dist/ is missing — run `vite build` first.');
  process.exit(1);
}

const files = await walk(DIST);
const failures = [];
let totalGzip = 0;

for (const file of files) {
  const name = relative(DIST, file);
  if (FORBIDDEN_FILES.some((forbidden) => name.endsWith(forbidden))) {
    failures.push(`SECURITY-09: ${name} must never be part of a production build`);
  }

  const contents = await readFile(file);
  if (file.endsWith('.js') || file.endsWith('.css')) {
    totalGzip += gzipSync(contents).length;
  }
  if (file.endsWith('.js')) {
    const text = contents.toString('utf8');
    for (const marker of MOCK_MARKERS) {
      if (text.includes(marker)) {
        failures.push(
          `SECURITY-09: mock transport marker "${marker}" is reachable from ${relative(DIST, file)}`,
        );
      }
    }
  }
}

const kb = (bytes) => `${(bytes / 1024).toFixed(1)}KB`;
console.log(`bundle budget: ${kb(totalGzip)} gzip of ${kb(BUDGET_BYTES)} allowed`);

if (totalGzip > BUDGET_BYTES) {
  failures.push(`NFR-005: bundle is ${kb(totalGzip)} gzip, over the ${kb(BUDGET_BYTES)} ceiling`);
}

if (failures.length > 0) {
  for (const failure of failures) console.error(`FAIL ${failure}`);
  process.exit(1);
}

console.log('bundle budget: OK');
