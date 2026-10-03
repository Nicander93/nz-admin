import path from 'node:path'

import { defineConfig as defineBrowserConfig, devices } from '@playwright/test'
import vue from '@vitejs/plugin-vue'
import { defineConfig as defineUnitConfig } from 'vitest/config'

export function unitTestConfig(root) {
  return defineUnitConfig({
    root,
    plugins: [vue()],
    resolve: { alias: { '@': path.resolve(root, 'src') } },
    test: { environment: 'happy-dom', include: ['tests/unit/**/*.test.ts'] },
  })
}

export function browserTestConfig(options = {}) {
  const baseURL =
    options.baseURL ?? process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173'
  return defineBrowserConfig({
    testDir: './tests/e2e',
    timeout: 60_000,
    expect: { timeout: 10_000 },
    workers: Number(process.env.E2E_WORKERS ?? 1),
    fullyParallel: false,
    retries: process.env.CI ? 2 : 0,
    reporter: [['list'], ['html', { open: 'never' }]],
    use: {
      baseURL,
      trace: 'on-first-retry',
      screenshot: 'only-on-failure',
      video: 'retain-on-failure',
    },
    projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
    webServer: {
      command: options.serverCommand ?? 'pnpm dev --host 127.0.0.1 --port 4173',
      url: baseURL,
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
  })
}
