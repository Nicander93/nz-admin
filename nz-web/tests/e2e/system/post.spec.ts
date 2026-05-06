import { expect, test } from '@playwright/test'
import { loginByUi } from '../helpers/auth'

test.describe('岗位管理冒烟', () => {
  test('打开新增弹窗并显示关键字段', async ({ page }) => {
    await loginByUi(page)
    await page.goto('/system/post')

    await page.getByRole('button', { name: '新增' }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.getByText('岗位编码', { exact: true })).toBeVisible()
    await expect(page.getByText('岗位名称', { exact: true })).toBeVisible()
  })
})
