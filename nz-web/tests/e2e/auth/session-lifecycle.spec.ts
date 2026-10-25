import { randomUUID } from 'node:crypto'

import { expect, loginByUi, test } from '@nz/test/e2e'

const initialPassword = 'Session-lifecycle!2026'
const changedPassword = 'Session-lifecycle!2027'

test.describe('会话生命周期', () => {
  test.setTimeout(90_000)

  test('个人中心修改密码后旧会话失效，新密码重新登录', async ({
    authenticatedPage: admin,
    browser,
  }) => {
    const suffix = randomUUID().replaceAll('-', '').slice(0, 12)
    const username = `e2esession${suffix}`
    const nickname = `会话生命周期${suffix}`
    const apiBase = new URL(admin.url()).origin
    const adminToken = await admin.evaluate(() => localStorage.getItem('token'))
    expect(adminToken).toBeTruthy()

    let userId: number | undefined
    let userContext: Awaited<ReturnType<typeof browser.newContext>> | undefined
    const adminRequest = async (path: string, method = 'GET', data?: unknown) =>
      admin.request.fetch(`${apiBase}${path}`, {
        method,
        headers: { Authorization: adminToken! },
        data,
        timeout: 10_000,
      })

    try {
      await admin.goto('/system/user')
      await admin
        .getByRole('button', { name: '新增', exact: true })
        .first()
        .click()
      const dialog = admin.getByRole('dialog')
      await dialog
        .getByLabel('用户名')
        .locator('..')
        .locator('input')
        .fill(username)
      await dialog
        .getByLabel('密码')
        .locator('..')
        .locator('input')
        .fill(initialPassword)
      await dialog
        .getByLabel('昵称')
        .locator('..')
        .locator('input')
        .fill(nickname)
      const department = dialog.getByRole('combobox', {
        name: '部门',
        exact: true,
      })
      await department
        .locator(
          'xpath=ancestor::div[contains(@class, "el-select__wrapper")][1]',
        )
        .click()
      await admin
        .locator('.el-select-dropdown:visible .el-tree-node__content')
        .first()
        .click()
      await dialog.getByRole('button', { name: '确定', exact: true }).click()
      await expect(dialog).toBeHidden()

      const usersResponse = await adminRequest(
        `/api/system/user/page?pageNum=1&pageSize=100&username=${encodeURIComponent(username)}`,
      )
      expect(usersResponse.ok()).toBeTruthy()
      const usersBody = (await usersResponse.json()) as {
        data: { records: Array<{ id: number; username: string }> }
      }
      userId = usersBody.data.records.find(
        (item) => item.username === username,
      )?.id
      expect(userId).toBeTruthy()

      userContext = await browser.newContext({ baseURL: apiBase })
      const userPage = await userContext.newPage()
      await loginByUi(userPage, {
        username,
        password: initialPassword,
        tenantCode: process.env.E2E_TENANT_CODE ?? 'default',
      })
      await userPage.goto('/profile')
      await expect(
        userPage.getByRole('heading', { name: nickname }),
      ).toBeVisible()
      const oldToken = await userPage.evaluate(() =>
        localStorage.getItem('token'),
      )
      expect(oldToken).toBeTruthy()
      const secondTab = await userContext.newPage()
      await secondTab.goto('/profile')
      await expect(
        secondTab.getByRole('heading', { name: nickname }),
      ).toBeVisible()

      const passwordCard = userPage.locator('.profile-content .el-card').nth(1)
      await passwordCard
        .getByLabel('当前密码', { exact: true })
        .fill(initialPassword)
      await passwordCard
        .getByLabel('新密码', { exact: true })
        .fill(changedPassword)
      await passwordCard
        .getByLabel('确认新密码', { exact: true })
        .fill(changedPassword)
      await passwordCard
        .getByRole('button', { name: '修改密码', exact: true })
        .click()

      await expect(userPage).toHaveURL(/\/login(?:[?#].*)?$/)
      expect(
        await userPage.evaluate(() => localStorage.getItem('token')),
      ).toBeNull()
      const oldSessionResponse = await userPage.request.get(
        `${apiBase}/api/auth/info`,
        {
          headers: { Authorization: oldToken! },
        },
      )
      const oldSessionBody = (await oldSessionResponse.json()) as {
        code: number
      }
      expect(oldSessionBody.code).toBe(401)

      await userPage.getByPlaceholder('请输入用户名').fill(username)
      await userPage.getByPlaceholder('请输入密码').fill(initialPassword)
      const rejectedLogin = userPage.waitForResponse(
        (response) => new URL(response.url()).pathname === '/api/auth/login',
      )
      await userPage.getByRole('button', { name: '登录', exact: true }).click()
      expect(
        ((await (await rejectedLogin).json()) as { code: number }).code,
      ).not.toBe(200)
      await expect(userPage).toHaveURL(/\/login(?:[?#].*)?$/)

      await userPage.getByPlaceholder('请输入密码').fill(changedPassword)
      const acceptedLogin = userPage.waitForResponse(
        (response) => new URL(response.url()).pathname === '/api/auth/login',
      )
      await userPage.getByRole('button', { name: '登录', exact: true }).click()
      expect(
        ((await (await acceptedLogin).json()) as { code: number }).code,
      ).toBe(200)
      await expect(userPage).not.toHaveURL(/\/login(?:[?#].*)?$/)
      const newToken = await userPage.evaluate(() =>
        localStorage.getItem('token'),
      )
      expect(newToken).toBeTruthy()
      expect(newToken).not.toBe(oldToken)
      await expect(secondTab).not.toHaveURL(/\/login(?:[?#].*)?$/)
      await secondTab.goto('/profile')
      await expect(
        secondTab.getByRole('heading', { name: nickname }),
      ).toBeVisible()
      expect(
        await secondTab.evaluate(() => localStorage.getItem('token')),
      ).toBe(newToken)
      const stillRevokedResponse = await userPage.request.get(
        `${apiBase}/api/auth/info`,
        {
          headers: { Authorization: oldToken! },
        },
      )
      expect(
        ((await stillRevokedResponse.json()) as { code: number }).code,
      ).toBe(401)
    } finally {
      await userContext?.close()
      const response = await adminRequest(
        `/api/system/user/page?pageNum=1&pageSize=100&username=${encodeURIComponent(username)}`,
      )
      if (response.ok()) {
        const body = (await response.json()) as {
          data: { records: Array<{ id: number; username: string }> }
        }
        userId ??= body.data.records.find(
          (item) => item.username === username,
        )?.id
      }
      if (userId) {
        const deleted = await adminRequest(
          `/api/system/user/${userId}`,
          'DELETE',
        )
        expect(deleted.ok()).toBeTruthy()
        expect((await deleted.json()).code).toBe(200)
      }
    }
  })
})
