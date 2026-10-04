import {
  expect,
  test as base,
  type Page,
  type APIRequestContext,
  type BrowserContext,
} from '@playwright/test'

export interface LoginOptions {
  username: string
  password: string
  tenantCode?: string
}

export function loginOptions(): LoginOptions {
  const username = process.env.E2E_USERNAME
  const password = process.env.E2E_PASSWORD
  if (!username || !password)
    throw new Error('请配置 E2E_USERNAME 和 E2E_PASSWORD')
  return {
    username,
    password,
    tenantCode: process.env.E2E_TENANT_CODE ?? 'default',
  }
}

export async function loginByUi(page: Page, options = loginOptions()) {
  await page.goto('/login')
  await expect(
    page.getByRole('heading', { name: '登录后台控制台' }),
  ).toBeVisible()
  const tenantInput = page.getByPlaceholder('请输入租户编码')
  if (await tenantInput.count())
    await tenantInput.fill(options.tenantCode ?? 'default')
  await page.getByPlaceholder('请输入用户名').fill(options.username)
  await page.getByPlaceholder('请输入密码').fill(options.password)
  await page.getByRole('button', { name: '登录', exact: true }).click()
  await expect(page).not.toHaveURL(/\/login(?:[?#].*)?$/, { timeout: 15_000 })
}

export async function loginByApi(
  request: APIRequestContext,
  baseURL: string,
  options = loginOptions(),
) {
  const response = await request.post(`${baseURL}/api/auth/login`, {
    data: {
      ...options,
      tenantCode: options.tenantCode ?? 'default',
      clientId: 'nz-web-account',
    },
  })
  if (!response.ok()) throw new Error(`登录 HTTP 错误：${response.status()}`)
  const body = await response.json()
  if (body.code !== 200 || typeof body.data !== 'string')
    throw new Error(`登录失败：${body.msg ?? body.code}`)
  return body.data as string
}

type AuthenticationState = Awaited<ReturnType<BrowserContext['storageState']>>

export const test = base.extend<
  { authenticatedPage: Page },
  { authenticationState: AuthenticationState }
>({
  authenticationState: [
    async ({ browser }, use, workerInfo) => {
      const context = await browser.newContext({
        baseURL: workerInfo.project.use.baseURL,
      })
      try {
        const page = await context.newPage()
        await loginByUi(page)
        await use(await context.storageState())
      } finally {
        await context.close()
      }
    },
    { scope: 'worker' },
  ],
  authenticatedPage: async ({ page, authenticationState }, use) => {
    await page.context().addCookies(authenticationState.cookies)
    await page.context().addInitScript((origins) => {
      for (const entry of origins)
        if (entry.origin === location.origin) {
          for (const item of entry.localStorage)
            localStorage.setItem(item.name, item.value)
        }
    }, authenticationState.origins)
    await page.goto('/')
    await expect(page).not.toHaveURL(/\/login(?:[?#].*)?$/)
    await use(page)
  },
})

export { expect }
