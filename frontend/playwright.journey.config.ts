import { defineConfig, devices } from '@playwright/test';

const backendUrl = 'http://127.0.0.1:18080';
const frontendUrl = 'http://127.0.0.1:5180';

export default defineConfig({
  testDir: './tests/e2e',
  testMatch: 'full-journey.spec.ts',
  outputDir: './test-results/a-z-journey',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 120_000,
  reporter: 'line',
  use: {
    baseURL: frontendUrl,
    video: 'on',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    launchOptions: { slowMo: 250 },
  },
  projects: [{ name: 'desktop-a-z', use: { ...devices['Desktop Chrome'] } }],
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
