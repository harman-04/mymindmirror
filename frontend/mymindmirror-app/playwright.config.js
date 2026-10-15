// playwright.config.js
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },

  // Run serially — tests share a single Docker Compose stack
  fullyParallel: false,
  workers: 1,

  // Refuse `.only` in CI to prevent accidentally disabling tests
  forbidOnly: !!process.env.CI,

  // Retries: 2 in CI (flake insurance), 0 locally (surface real failures)
  retries: process.env.CI ? 2 : 0,

  // Reporters: HTML for local, list + HTML for CI (artifact upload)
  reporter: process.env.CI
    ? [['list'], ['html', { open: 'never' }]]
    : [['html', { open: 'never' }]],

  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});