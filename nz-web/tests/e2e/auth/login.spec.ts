import { expect, test } from '@nz/test/e2e'

test.describe('登录冒烟', () => {
  test('账号密码登录成功后离开登录页', async ({ authenticatedPage: page }) => {
    await expect(page).not.toHaveURL(/\/login$/)
  })
})
