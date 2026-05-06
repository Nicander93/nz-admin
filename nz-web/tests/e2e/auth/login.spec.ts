import { expect, test } from '@playwright/test'
import { loginByUi } from '../helpers/auth'

test.describe('登录冒烟', () => {
  test('账号密码登录成功后离开登录页', async ({ page }) => {
    await loginByUi(page)
    await expect(page).not.toHaveURL(/\/login$/)
  })
})
