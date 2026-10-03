import { expect, test } from '@nz/test/e2e'

test.describe('岗位管理冒烟', () => {
  test('打开新增弹窗并显示关键字段', async ({ authenticatedPage: page }) => {
    await page.goto('/system/post')

    await page.getByRole('button', { name: '新增' }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(
      page.getByRole('dialog').getByText('岗位编码', { exact: true }),
    ).toBeVisible()
    await expect(
      page.getByRole('dialog').getByText('岗位名称', { exact: true }),
    ).toBeVisible()
  })
})
