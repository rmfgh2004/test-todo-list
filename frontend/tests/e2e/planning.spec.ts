import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';

const uniqueTitle = (prefix: string) => `${prefix}-${Date.now()}`;

const pointerDrag = async (
  page: Page,
  source: ReturnType<Page['locator']>,
  target: ReturnType<Page['locator']>,
) => {
  const sourceBox = await source.boundingBox();
  const targetBox = await target.boundingBox();
  if (sourceBox === null || targetBox === null) throw new Error('Drag endpoints must be visible');
  await page.mouse.move(sourceBox.x + sourceBox.width / 2, sourceBox.y + sourceBox.height / 2);
  await page.mouse.down();
  await page.mouse.move(
    sourceBox.x + sourceBox.width / 2 + 12,
    sourceBox.y + sourceBox.height / 2,
    { steps: 2 },
  );
  await page.mouse.move(targetBox.x + targetBox.width / 2, targetBox.y + targetBox.height / 2, {
    steps: 12,
  });
  await page.mouse.up();
};

const expectNoSeriousAxeFindings = async (page: Page) => {
  const results = await new AxeBuilder({ page }).analyze();
  expect(
    results.violations.filter(({ impact }) => impact === 'serious' || impact === 'critical'),
  ).toEqual([]);
};

const createTask = async (page: Page, title: string) => {
  await page.getByRole('button', { name: '새 할 일' }).click();
  await expectNoSeriousAxeFindings(page);
  await page.getByLabel('제목').fill(title);
  await page.getByLabel('예상 시간').fill('60');
  await page.getByRole('button', { name: '만들기' }).click();
  await expect(page.getByRole('button', { name: `${title} 열기` })).toBeVisible();
};

test.beforeEach(async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: '미배치 할 일' })).toBeVisible();
});

test('CP_02_CP_03 creates, drags, unschedules and keyboard-schedules against real U1', async ({
  page,
}) => {
  const title = uniqueTitle('e2e-drag');
  await createTask(page, title);

  await pointerDrag(
    page,
    page.getByRole('listitem').filter({ hasText: title }),
    page.locator('[data-slot="0-4"]'),
  );
  await expect(page.getByRole('button', { name: `${title} 09:00–10:00` })).toBeVisible();
  // dnd-kit removes its document-level click suppressor on the next task after a drop.
  await page.waitForTimeout(100);

  await page.getByRole('button', { name: `${title} 09:00–10:00` }).click();
  await expectNoSeriousAxeFindings(page);
  await page.getByRole('button', { name: '시간 배치 해제' }).click();
  await expect(page.getByRole('button', { name: `${title} 열기` })).toBeVisible();

  await page.getByRole('button', { name: `${title} 시간 배치` }).click();
  await expectNoSeriousAxeFindings(page);
  const keyboard = page.getByRole('button', { name: `${title} 키보드로 배치` });
  await keyboard.press('Space');
  await keyboard.press('ArrowDown');
  await keyboard.press('Enter');
  await expect(page.getByRole('button', { name: `${title} 09:15–10:15` })).toBeVisible();
  await page.getByRole('button', { name: `${title} 09:15–10:15` }).click();
  await page.getByRole('button', { name: '삭제' }).click();
  await expectNoSeriousAxeFindings(page);
  await page.getByRole('button', { name: '삭제 확인' }).click();
  await expect(page.getByRole('button', { name: `${title} 열기` })).toHaveCount(0);
});

test('CP_04 resolves a real conflict, completes and filters the list', async ({ page }) => {
  const first = uniqueTitle('e2e-first');
  const second = uniqueTitle('e2e-second');
  await createTask(page, first);
  await page.getByRole('button', { name: `${first} 시간 배치` }).click();
  await page.getByRole('button', { name: '시간 폼으로 배치' }).click();
  await expect(page.getByRole('button', { name: `${first} 09:00–10:00` })).toBeVisible();

  await createTask(page, second);
  await page.getByRole('button', { name: `${second} 시간 배치` }).click();
  await page.getByRole('button', { name: '시간 폼으로 배치' }).click();
  const dialog = page.getByRole('dialog', { name: `${second} 배치 충돌` });
  await expect(dialog).toBeVisible();
  await expectNoSeriousAxeFindings(page);
  await dialog.getByRole('button', { name: /으로 이동/ }).click();
  await expect(page.getByRole('button', { name: `${second} 10:00–11:00` })).toBeVisible();

  await page.goto('/tasks?status=TODO&page=0&size=25');
  await expectNoSeriousAxeFindings(page);
  const row = page.getByRole('row').filter({ hasText: second });
  await row.getByRole('checkbox').click();
  await expect(row.getByRole('checkbox')).toBeChecked();
  await page.getByLabel('상태').selectOption('COMPLETED');
  await expect(page).toHaveURL(/status=COMPLETED/);
  await expect(page.getByRole('row').filter({ hasText: second })).toBeVisible();

  await page.goto('/');
  for (const [title, time] of [
    [second, '10:00–11:00'],
    [first, '09:00–10:00'],
  ] as const) {
    await page.getByRole('button', { name: `${title} ${time}` }).click();
    await page.getByRole('button', { name: '삭제' }).click();
    await page.getByRole('button', { name: '삭제 확인' }).click();
  }
});

test('CP_05 desktop and 320px views have no serious accessibility findings', async ({ page }) => {
  await expectNoSeriousAxeFindings(page);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(
    true,
  );
});

test('NFR_005 loads one weekly request and becomes operable within two seconds', async ({
  page,
}) => {
  let weeklyRequests = 0;
  page.on('request', (request) => {
    if (request.url().includes('/api/v1/planning/weeks/')) weeklyRequests += 1;
  });
  const started = Date.now();
  await page.reload();
  await expect(page.getByRole('region', { name: '주간 시간표' })).toBeVisible();

  expect(Date.now() - started).toBeLessThan(2_000);
  expect(weeklyRequests).toBe(1);
});

test('CP_05 reports transport loss and recovers after the backend returns', async ({ page }) => {
  await page.route('**/api/v1/planning/weeks/**', (route) => route.abort('connectionrefused'));
  await page.reload();
  const banner = page.getByText(/백엔드 연결을 확인하고 있습니다/);
  await expect(banner).toBeVisible();

  await page.unroute('**/api/v1/planning/weeks/**');
  await expect(banner).toHaveCount(0, { timeout: 7_000 });
  await expect(page.getByRole('heading', { name: '미배치 할 일' })).toBeVisible();
});
