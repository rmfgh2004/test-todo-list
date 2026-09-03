import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';

const chapter = async (page: Page, text: string) => {
  await page.evaluate((caption) => {
    document.querySelector('#playwright-journey-caption')?.remove();
    const element = document.createElement('div');
    element.id = 'playwright-journey-caption';
    element.textContent = caption;
    Object.assign(element.style, {
      position: 'fixed',
      top: '16px',
      left: '50%',
      transform: 'translateX(-50%)',
      zIndex: '2147483647',
      padding: '10px 18px',
      borderRadius: '999px',
      color: '#fff',
      background: 'rgba(15, 23, 42, 0.92)',
      font: '600 16px/1.4 system-ui, sans-serif',
      boxShadow: '0 8px 24px rgba(0, 0, 0, 0.28)',
      pointerEvents: 'none',
    });
    document.body.append(element);
  }, text);
  await page.waitForTimeout(800);
};

const pointerDrag = async (
  page: Page,
  source: ReturnType<Page['locator']>,
  target: ReturnType<Page['locator']>,
  onHover?: () => Promise<void>,
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
  await onHover?.();
  await page.mouse.up();
};

const seoulToday = () =>
  new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date());

const createTask = async (page: Page, title: string) => {
  await page.getByRole('button', { name: '새 할 일' }).click();
  await page.getByLabel('제목').fill(title);
  await page.getByLabel('예상 시간').fill('60');
  await page.getByRole('button', { name: '만들기' }).click();
  await expect(page.getByRole('button', { name: `${title} 열기` })).toBeVisible();
};

test('A_Z 실제 사용 전체 여정', async ({ page }) => {
  const suffix = Date.now();
  const draftTitle = `A-Z 계획-${suffix}`;
  const plannedTitle = `A-Z 집중 계획-${suffix}`;
  const conflictTitle = `A-Z 충돌 해결-${suffix}`;

  await page.goto('/');
  await expect(page.getByRole('heading', { name: '미배치 할 일' })).toBeVisible();
  await expect(page.getByText('월요일')).toBeVisible();
  await expect(page.getByText('08:00')).toBeVisible();
  await expect(page.getByText('22:00')).toBeVisible();
  await chapter(page, '1. Tempo 시작 — 비어 있는 주간 계획 확인');

  await createTask(page, draftTitle);
  await chapter(page, '2. 첫 할 일 생성');

  await page.getByRole('button', { name: `${draftTitle} 열기` }).click();
  await page.getByLabel('제목').fill(plannedTitle);
  await page.getByLabel('설명').fill('A-Z 영상에서 확인하는 실제 집중 작업');
  await page.getByLabel('우선순위').selectOption('HIGH');
  await page.getByLabel('마감일').fill(seoulToday());
  await page.getByRole('button', { name: '저장' }).click();
  const plannedCard = page.getByRole('listitem').filter({ hasText: plannedTitle });
  await expect(plannedCard.getByText('▲')).toBeVisible();
  await expect(plannedCard.getByText('높음')).toBeVisible();
  await chapter(page, '3. 제목·설명·마감일 수정 — 우선순위 3중 표현 확인');

  await pointerDrag(page, plannedCard, page.locator('[data-slot="0-4"]'), async () => {
    await expect(page.getByText('(−1h)')).toBeVisible();
    await expect(page.getByText(/배치 미리보기 .* 09:00/)).toBeVisible();
  });
  const plannedBlock = page.getByRole('button', { name: `${plannedTitle} 09:00–10:00` });
  await expect(plannedBlock).toBeVisible();
  await expect(plannedBlock.getByText('높음')).toBeVisible();
  await chapter(page, '4. 가용시간 미리보기 후 드래그로 월요일 09:00 배치');

  await createTask(page, conflictTitle);
  await chapter(page, '5. 두 번째 할 일 생성');

  await page.getByRole('button', { name: `${conflictTitle} 시간 배치` }).click();
  const keyboard = page.getByRole('button', { name: `${conflictTitle} 키보드로 배치` });
  await keyboard.press('Space');
  await chapter(page, '6. 키보드로 같은 시간 선택');
  await keyboard.press('Enter');

  const conflict = page.getByRole('dialog', { name: `${conflictTitle} 배치 충돌` });
  await expect(conflict).toBeVisible();
  await expect(page.getByText('복원 위치: 미배치 목록')).toBeVisible();
  await expect(page.getByText(/실패 위치: .* 09:00/)).toBeVisible();
  await expect(page.getByRole('button', { name: '다른 시간 선택' })).toBeVisible();
  await chapter(page, '7. 실제 충돌 감지 — 구조화 롤백 확인 후 다음 시간 선택');
  await conflict.getByRole('button', { name: /으로 이동/ }).click();
  await expect(page.getByRole('button', { name: `${conflictTitle} 10:00–11:00` })).toBeVisible();

  await page.getByRole('link', { name: '목록' }).click();
  await expect(page.getByRole('heading', { name: '할 일 목록' })).toBeVisible();
  await expect(page.getByRole('heading', { name: '오늘' })).toBeVisible();
  await expect(page.getByRole('heading', { name: '이번 주' })).toBeVisible();
  await expect(page.getByRole('heading', { name: '완료' })).toBeVisible();
  await expect(
    page.getByRole('region', { name: '오늘' }).getByRole('row', { name: /A-Z 집중 계획/ }),
  ).toBeVisible();
  await chapter(page, '8. 목록 보기 — 오늘/이번 주/완료 그룹 확인');

  const row = page.getByRole('row').filter({ hasText: conflictTitle });
  await row.getByRole('checkbox').click();
  await expect(row.getByRole('checkbox')).toBeChecked();
  await expect(
    page
      .getByRole('region', { name: '완료' })
      .getByRole('row', { name: new RegExp(conflictTitle) }),
  ).toBeVisible();
  await chapter(page, '9. 할 일 완료 처리');

  await page.getByLabel('상태').selectOption('COMPLETED');
  await expect(page).toHaveURL(/status=COMPLETED/);
  await expect(page.getByRole('row').filter({ hasText: conflictTitle })).toBeVisible();
  await chapter(page, '10. 완료 상태로 필터링');

  await page.getByRole('link', { name: '주간' }).click();
  await page.getByRole('button', { name: `${plannedTitle} 09:00–10:00` }).click();
  await page.getByRole('button', { name: '시간 배치 해제' }).click();
  await expect(page.getByRole('button', { name: `${plannedTitle} 열기` })).toBeVisible();
  await chapter(page, '11. 첫 할 일의 시간 배치 해제');

  await page.getByRole('button', { name: `${plannedTitle} 열기` }).click();
  await page.getByRole('button', { name: '삭제' }).click();
  await page.getByRole('button', { name: '삭제 확인' }).click();
  await expect(page.getByRole('button', { name: `${plannedTitle} 열기` })).toHaveCount(0);
  await chapter(page, '12. 첫 할 일 삭제');

  await page.getByRole('button', { name: `${conflictTitle} 10:00–11:00` }).click();
  await page.getByRole('button', { name: '삭제' }).click();
  await page.getByRole('button', { name: '삭제 확인' }).click();
  await expect(page.getByRole('button', { name: `${conflictTitle} 10:00–11:00` })).toHaveCount(0);
  await chapter(page, '13. 전체 여정 완료 — 다시 비어 있는 계획');
});
