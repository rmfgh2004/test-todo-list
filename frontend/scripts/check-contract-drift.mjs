#!/usr/bin/env node
/**
 * NFR-007: the committed contract types must equal a fresh generation.
 *
 * Regenerates `src/shared/api/generated/planning-api.d.ts` into a temporary file and fails on any
 * difference, so a U1 contract change cannot reach the frontend without a reviewed regeneration.
 */
import { execFile } from 'node:child_process';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { promisify } from 'node:util';
import { fileURLToPath } from 'node:url';

const run = promisify(execFile);
const ROOT = fileURLToPath(new URL('..', import.meta.url));
const CONTRACT = join(ROOT, '..', 'backend', 'openapi', 'planning-api.yaml');
const COMMITTED = join(ROOT, 'src', 'shared', 'api', 'generated', 'planning-api.d.ts');

const workDir = await mkdtemp(join(tmpdir(), 'contract-drift-'));
const fresh = join(workDir, 'planning-api.d.ts');

try {
  await run('npx', ['openapi-typescript', CONTRACT, '-o', fresh], { cwd: ROOT });
  const [committed, regenerated] = await Promise.all([
    readFile(COMMITTED, 'utf8'),
    readFile(fresh, 'utf8'),
  ]);

  if (committed !== regenerated) {
    console.error(
      'FAIL contract drift: src/shared/api/generated/planning-api.d.ts is stale.\n' +
        'Run `npm run contract:generate`, review the diff, and commit it.',
    );
    process.exit(1);
  }
  console.log('contract drift: OK');
} finally {
  await rm(workDir, { recursive: true, force: true });
}
