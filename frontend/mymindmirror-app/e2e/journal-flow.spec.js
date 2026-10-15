// e2e/journal-flow.spec.js
import { test, expect } from '@playwright/test';

const API_URL = process.env.PLAYWRIGHT_API_URL || 'http://localhost:8080/api';
const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost';

test.describe('Journal Flow — Smoke Test', () => {
  const uniqueSuffix = `${Date.now()}-${Math.random().toString(36).substring(2, 8)}`;
  const username = `e2euser_${uniqueSuffix}`;
  const email = `${username}@test.com`;
  const password = 'Password123!';
  let authToken;

  test.beforeAll(async ({ request }) => {
    const registerRes = await request.post(`${API_URL}/auth/register`, {
      data: { username, email, password },
    });
    expect(registerRes.ok(), `Register failed: ${registerRes.status()}`).toBeTruthy();

    const loginRes = await request.post(`${API_URL}/auth/login`, {
      data: { username, password },
    });
    expect(loginRes.ok(), `Login failed: ${loginRes.status()}`).toBeTruthy();

    const loginData = await loginRes.json();
    authToken = loginData.token;
    expect(authToken, 'Login response missing token').toBeTruthy();
  });

  test('User can write a journal entry and see it appear in history', async ({ page }) => {
    await page.addInitScript((token) => {
      localStorage.setItem('jwtToken', token);
    }, authToken);

    await page.goto(`${BASE_URL}/journal`, { waitUntil: 'domcontentloaded' });

    const form = page.getByTestId('journal-input-form');
    await expect(form).toBeVisible({ timeout: 20_000 });

    const textarea = page.getByTestId('journal-input-textarea');
    await expect(textarea).toBeVisible({ timeout: 10_000 });

    const saveButton = page.getByTestId('journal-input-submit');
    await expect(saveButton).toBeVisible();
    await expect(saveButton).toBeDisabled();

    const journalText = `Playwright smoke test entry ${Date.now()}`;
    await textarea.fill(journalText);
    await expect(saveButton).toBeEnabled();

    await saveButton.click();

    // Success signal: the textarea clears only in the mutation's onSuccess
    // handler. We deliberately avoid toast assertions — Sonner's two racing
    // toasts make them unreliable.
    await expect(textarea).toHaveValue('', { timeout: 20_000 });

    // Durability signal: the entry appears in today's history list.
    const entryPreview = page.getByText(journalText.substring(0, 40), { exact: false }).first();
    await expect(entryPreview).toBeVisible({ timeout: 20_000 });
  });

  test('Submit button stays disabled while typing whitespace only', async ({ page }) => {
    await page.addInitScript((token) => {
      localStorage.setItem('jwtToken', token);
    }, authToken);

    await page.goto(`${BASE_URL}/journal`, { waitUntil: 'domcontentloaded' });

    const textarea = page.getByTestId('journal-input-textarea');
    await textarea.waitFor({ state: 'visible', timeout: 20_000 });

    const saveButton = page.getByTestId('journal-input-submit');

    await textarea.fill('    \n   ');
    await expect(saveButton).toBeDisabled();

    await textarea.fill('real content');
    await expect(saveButton).toBeEnabled();
  });
});