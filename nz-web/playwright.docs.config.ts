import { defineConfig, devices } from '@playwright/test'
const base = process.env.DOCS_BASE ?? '/'
export default defineConfig({
  testDir: './tests/docs',
  fullyParallel: false,
  timeout: 30000,
  expect: { timeout: 10000 },
  retries: process.env.CI ? 1 : 0,
  reporter: 'list',
  use: {
    baseURL: `http://127.0.0.1:4175${base}`,
    colorScheme: 'light',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'pnpm --dir ../docs preview --host 127.0.0.1',
    url: `http://127.0.0.1:4175${base}`,
    reuseExistingServer: false,
  },
})
