import { expect, test } from '@nz/test/e2e'

test.describe('工作台', () => {
  test('登录后进入工作台可见快捷入口与表格区域', async ({ authenticatedPage: page }) => {
    await page.goto('/workbench')
    await expect(page.getByRole('heading', { name: '工作台' })).toBeVisible()
    await expect(page.getByText('快捷入口')).toBeVisible()
    await expect(page.getByText('最近登录')).toBeVisible()
    await expect(page.getByText('最近操作')).toBeVisible()
  })
})
