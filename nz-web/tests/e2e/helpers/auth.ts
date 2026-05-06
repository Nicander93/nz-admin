import { expect, Page } from '@playwright/test'

function getRequiredEnv(name: string): string {
  const value = process.env[name]
  if (!value) {
    throw new Error(`缺少环境变量 ${name}，请先配置后再执行 E2E`)
  }
  return value
}

export async function loginByUi(page: Page) {
  const username = getRequiredEnv('E2E_USERNAME')
  const password = getRequiredEnv('E2E_PASSWORD')

  await page.goto('/login')
  await expect(page.getByRole('heading', { name: '登录后台控制台' })).toBeVisible()
  await page.getByPlaceholder('请输入用户名').fill(username)
  await page.getByPlaceholder('请输入密码').fill(password)
  await page.getByRole('button', { name: '登录' }).click()

  try {
    await expect(page).not.toHaveURL(/\/login$/, { timeout: 15_000 })
  } catch {
    throw new Error(
      '登录失败，页面仍停留在 /login。请确认后端服务可用，并检查 E2E_USERNAME / E2E_PASSWORD 是否正确。',
    )
  }
}
