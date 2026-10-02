import { expect, test } from '@nz/test/e2e'

test.describe('系统参数冒烟', () => {
  test('参数管理页面可访问并看到关键元素', async ({ authenticatedPage: page }) => {
    await page.goto('/system/config')

    await expect(page.getByRole('button', { name: '新增' })).toBeVisible()
    await expect(page.getByText('参数名称', { exact: true })).toBeVisible()
    await expect(page.getByText('参数键名', { exact: true })).toBeVisible()
  })
})
