import { defineConfig, devices } from '@playwright/test';

const backendUrl = 'http://127.0.0.1:18080';
const frontendUrl = 'http://127.0.0.1:5180';

export default defineConfig({
  testDir: './tests/e2e',
  testIgnore: 'full-journey.spec.ts',
  outputDir: './test-results/regression',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: 'line',
  use: {
    baseURL: frontendUrl,
    video: 'on',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    {
      name: 'mobile-320',
      use: { ...devices['Desktop Chrome'], viewport: { width: 320, height: 800 } },
    },
  ],
  webServer: [
    {
      command:
        './mvnw spring-boot:run -Dspring-boot.run.profiles=test -Dspring-boot.run.arguments="--server.port=18080 --planning.platform.allowed-origins=http://127.0.0.1:5180"',
      cwd: '../backend',
      url: `${backendUrl}/actuator/health`,
      timeout: 120_000,
      reuseExistingServer: false,
    },
    {
      command: 'npm run dev -- --port 5180',
      env: { VITE_API_BASE_URL: backendUrl },
      url: frontendUrl,
      timeout: 30_000,
      reuseExistingServer: false,
    },
  ],
});
