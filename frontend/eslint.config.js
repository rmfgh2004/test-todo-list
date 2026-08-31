import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';
import jsxA11y from 'eslint-plugin-jsx-a11y';
import globals from 'globals';

/**
 * U2 lint policy.
 *
 * UR-007 / SECURITY-09: HTML injection sinks are errors with no allowlist.
 * NFR-007: import boundaries mirror U1's ArchUnit direction —
 *   UI components -> feature hooks -> coordinators -> API client -> generated types,
 *   and no arrow ever points backwards.
 */

const FEATURES = ['timetable', 'backlog', 'task-editor', 'conflict-resolution', 'task-list'];

/** UR-007: no HTML injection sink may appear anywhere in the application. */
const injectionSinks = [
  {
    selector: "MemberExpression[property.name='innerHTML']",
    message: 'innerHTML is forbidden (UR-007). Render user content as a text node.',
  },
  {
    selector: "MemberExpression[property.name='outerHTML']",
    message: 'outerHTML is forbidden (UR-007). Render user content as a text node.',
  },
  {
    selector: "CallExpression[callee.property.name='insertAdjacentHTML']",
    message: 'insertAdjacentHTML is forbidden (UR-007).',
  },
  {
    selector: "JSXAttribute[name.name='dangerouslySetInnerHTML']",
    message: 'dangerouslySetInnerHTML is forbidden (UR-007). There is no allowlist.',
  },
  {
    selector: "NewExpression[callee.name='Function']",
    message: 'The Function constructor is dynamic evaluation and is forbidden (UR-007).',
  },
];

/** The transport surface a UI component may never reach directly (NFR-007). */
const transportModules = [
  '@/shared/api/client',
  '@/shared/api/client/*',
  '@/shared/api/generated',
  '@/shared/api/generated/*',
  '@/shared/api/mocks',
  '@/shared/api/mocks/*',
];

export default tseslint.config(
  {
    ignores: [
      'dist/**',
      'coverage/**',
      'playwright-report/**',
      'test-results/**',
      'node_modules/**',
      'public/mockServiceWorker.js',
      'src/shared/api/generated/**',
      'sbom.json',
    ],
  },

  js.configs.recommended,
  ...tseslint.configs.recommended,

  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 2022,
      globals: { ...globals.browser, ...globals.node },
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    plugins: {
      'react-hooks': reactHooks,
      'jsx-a11y': jsxA11y,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      ...jsxA11y.flatConfigs.recommended.rules,

      'no-eval': 'error',
      'no-implied-eval': 'error',
      'no-script-url': 'error',
      'no-restricted-syntax': ['error', ...injectionSinks],

      // NFR-007: `any` and non-null assertions are errors outside generated files.
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/no-non-null-assertion': 'error',
      '@typescript-eslint/consistent-type-imports': 'error',
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],

      'no-console': ['error', { allow: ['warn', 'error'] }],
      eqeqeq: ['error', 'always'],
    },
  },

  // A UI component never imports the API client (NFR-007, F-N04 seam).
  {
    files: ['src/**/*.tsx'],
    ignores: ['src/**/*.test.tsx'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: transportModules,
              message:
                'UI components must go through a feature hook (NFR-007). Only hooks and coordinators may reach the transport layer.',
            },
          ],
        },
      ],
    },
  },

  // Shared code never depends on a feature or on the app shell.
  {
    files: ['src/shared/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: ['@/features/*', '@/features/*/**', '@/app', '@/app/*', '@/app/**'],
              message: 'shared/ must not depend on a feature or on the app shell (NFR-007).',
            },
          ],
        },
      ],
    },
  },

  // A feature never imports another feature's internals.
  ...FEATURES.map((feature) => ({
    files: [`src/features/${feature}/**/*.{ts,tsx}`],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: [`@/features/*/**`, `!@/features/${feature}/**`],
              message: `Feature "${feature}" must not import another feature's internals (NFR-007). Move the shared piece into shared/.`,
            },
          ],
        },
      ],
    },
  })),

  // Test files may reach the transport layer and the mock handlers directly.
  {
    files: ['src/**/*.test.{ts,tsx}', 'tests/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-imports': 'off',
      '@typescript-eslint/no-non-null-assertion': 'off',
      'no-console': 'off',
    },
  },

  // Build scripts run in Node.
  {
    files: ['scripts/**/*.mjs', '*.config.{ts,js}'],
    languageOptions: { globals: globals.node },
    rules: { 'no-console': 'off' },
  },
);
