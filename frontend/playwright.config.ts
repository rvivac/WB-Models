import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './playwright/e2e',
  timeout: 45000,
  expect: {
    timeout: 7000
  },
  fullyParallel: false, // Execução sequencial para validação de ciclo completo
  workers: 1,
  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['list']
  ],
  use: {
    baseURL: process.env['E2E_BASE_URL'] || 'http://localhost:4200',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    {
      name: 'Google Chrome Desktop',
      use: { ...devices['Desktop Chrome'] },
    }
  ]
});
