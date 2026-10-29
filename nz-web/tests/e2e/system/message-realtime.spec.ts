import { randomUUID } from 'node:crypto'

import { expect, loginByUi, test } from '@nz/test/e2e'

type ApiResult<T> = { code: number; msg?: string; data: T }
type MessageRow = { id: number; title: string; readStatus: number }
type MenuRow = { id: number; parentId?: number | null; perm?: string }
type UserRow = { id: number; username: string }
type RoleRow = { id: number; roleKey: string }
type StreamEvent = { data: string }

const userPassword = 'Message-scope!2026'
const messagePermissions = [
  'system:message:list',
  'system:message:query',
  'system:message:read',
  'system:message:remove',
]

test.describe('消息实时通知闭环', () => {
  test.setTimeout(240_000)
  test.use({ actionTimeout: 10_000 })

  test('普通消息权限用户可收实时通知、隔离其他用户、跨标签同步并在撤权后停止', async ({
    authenticatedPage: admin,
    browser,
  }) => {
    const suffix = randomUUID().replaceAll('-', '').slice(0, 16)
    const roleKey = `e2e_message_${suffix}`
    const roleName = `E2E消息收件人${suffix}`
    const usernames = [`e2emsg_r_${suffix}`, `e2emsg_n_${suffix}`] as const
    const titles = [1, 2, 3].map((index) => `E2E消息闭环_${suffix}_${index}`)
    const controlTitle = `E2E消息控制_${suffix}`
    const trackedTitles = [...titles, controlTitle]
    const token = await admin.evaluate(() => localStorage.getItem('token') ?? '')
    expect(token).toBeTruthy()

    let roleId: number | undefined
    const userIds = new Map<string, number>()
    const contexts: Array<Awaited<ReturnType<typeof browser.newContext>>> = []
    const cdpSessions: Array<{ detach(): Promise<void> }> = []
    let recipientToken: string | undefined
    let otherToken: string | undefined
    let primaryError: unknown
    let cleanupError: unknown

    async function request<T>(
      page: import('@playwright/test').Page,
      bearer: string,
      method: string,
      path: string,
      data?: unknown,
    ): Promise<{ response: import('@playwright/test').APIResponse; result: ApiResult<T> }> {
      const response = await page.request.fetch(path, {
        method,
        headers: { Authorization: bearer },
        data,
        timeout: 10_000,
      })
      const result = (await response.json()) as ApiResult<T>
      return { response, result }
    }

    async function adminApi<T>(method: string, path: string, data?: unknown) {
      const { response, result } = await request<T>(admin, token, method, path, data)
      expect(response.ok()).toBeTruthy()
      expect(result.code, result.msg).toBe(200)
      return result.data
    }

    async function setRoleMenus(menuIds: number[]) {
      expect(roleId).toBeTruthy()
      await adminApi<void>('PUT', `/api/system/role/${roleId}/menus`, menuIds)
    }

    async function findMessages(page: import('@playwright/test').Page, bearer: string) {
      const found: MessageRow[] = []
      for (const title of trackedTitles) {
        const { response, result } = await request<{ records: MessageRow[] }>(
          page,
          bearer,
          'GET',
          `/api/system/message/page?pageNum=1&pageSize=100&title=${encodeURIComponent(title)}`,
        )
        expect(response.ok()).toBeTruthy()
        if (result.code !== 200) continue
        found.push(...result.data.records.filter((row) => row.title === title))
      }
      return found
    }

    async function cleanup() {
      for (const session of cdpSessions) await session.detach()
      for (const context of contexts) await context.close()
      if (!roleId) {
        const roles = await adminApi<{ records: RoleRow[] }>(
          'GET', `/api/system/role/page?pageNum=1&pageSize=100&roleKey=${encodeURIComponent(roleKey)}`,
        )
        roleId = roles.records.find((role) => role.roleKey === roleKey)?.id
      }
      if (roleId) {
        const permissions = await adminApi<MenuRow[]>('GET', '/api/system/menu/list')
        const fullMenuIds = menuIdsForPermissions(permissions, messagePermissions)
        await setRoleMenus(fullMenuIds)
      }
      for (const bearer of [recipientToken, otherToken]) {
        if (!bearer) continue
        const ownMessages = await findMessages(admin, bearer)
        for (const message of ownMessages) {
          const { response, result } = await request<void>(
            admin, bearer, 'DELETE', `/api/system/message/${message.id}`,
          )
          expect(response.ok()).toBeTruthy()
          expect(result.code, result.msg).toBe(200)
        }
      }
      for (const username of usernames) {
        const users = await adminApi<{ records: UserRow[] }>(
          'GET',
          `/api/system/user/page?pageNum=1&pageSize=100&username=${encodeURIComponent(username)}`,
        )
        const user = users.records.find((item) => item.username === username)
        if (!user) continue
        await adminApi<void>('PUT', `/api/system/user/${user.id}/roles`, [])
        await adminApi<void>('DELETE', `/api/system/user/${user.id}`)
      }
      if (roleId) await adminApi<void>('DELETE', `/api/system/role/${roleId}`)
    }

    try {
      const adminInfo = await adminApi<{ user: { id: number; tenantId: number; deptId: number } }>(
        'GET', '/api/auth/info',
      )
      const menus = await adminApi<MenuRow[]>('GET', '/api/system/menu/list')
      const grantedMenuIds = menuIdsForPermissions(menus, messagePermissions)
      expect(grantedMenuIds.length).toBeGreaterThan(messagePermissions.length - 1)
      await adminApi<void>('POST', '/api/system/role', {
        name: roleName,
        roleKey,
        sort: 0,
        status: 0,
        dataScope: 1,
      })
      const roles = await adminApi<{ records: RoleRow[] }>(
        'GET', `/api/system/role/page?pageNum=1&pageSize=100&roleKey=${encodeURIComponent(roleKey)}`,
      )
      roleId = roles.records.find((role) => role.roleKey === roleKey)?.id
      expect(roleId).toBeTruthy()
      await setRoleMenus(grantedMenuIds)

      for (const username of usernames) {
        await adminApi<void>('POST', '/api/system/user', {
          tenantId: adminInfo.user.tenantId,
          deptId: adminInfo.user.deptId,
          username,
          password: userPassword,
          nickname: username,
          status: 0,
          gender: '0',
        })
        const users = await adminApi<{ records: UserRow[] }>(
          'GET', `/api/system/user/page?pageNum=1&pageSize=100&username=${encodeURIComponent(username)}`,
        )
        const user = users.records.find((item) => item.username === username)
        expect(user).toBeTruthy()
        userIds.set(username, user!.id)
        await adminApi<void>('PUT', `/api/system/user/${user!.id}/roles`, [roleId])
      }

      const apiBase = new URL(admin.url()).origin
      const recipientContext = await browser.newContext({ baseURL: apiBase })
      contexts.push(recipientContext)
      const recipient = await recipientContext.newPage()
      await loginByUi(recipient, { username: usernames[0], password: userPassword })
      recipientToken = (await recipient.evaluate(() => localStorage.getItem('token'))) ?? undefined
      expect(recipientToken).toBeTruthy()
      const otherContext = await browser.newContext({ baseURL: apiBase })
      contexts.push(otherContext)
      const other = await otherContext.newPage()
      await loginByUi(other, { username: usernames[1], password: userPassword })
      otherToken = (await other.evaluate(() => localStorage.getItem('token'))) ?? ''
      expect(otherToken).toBeTruthy()

      const rInfo = await request<{ user: { id: number }; permissions: string[] }>(recipient, recipientToken!, 'GET', '/api/auth/info')
      expect(rInfo.result.code).toBe(200)
      expect(rInfo.result.data.permissions).toEqual(expect.arrayContaining(messagePermissions))
      expect(rInfo.result.data.permissions).not.toContain('system:realtime:view')
      expect(rInfo.result.data.permissions).not.toContain('system:message:send')

      const recipientEvents: string[] = []
      const otherEvents: string[] = []
      const recipientCdp = await recipientContext.newCDPSession(recipient)
      const otherCdp = await otherContext.newCDPSession(other)
      cdpSessions.push(recipientCdp, otherCdp)
      await recipientCdp.send('Network.enable')
      await otherCdp.send('Network.enable')
      captureSystemMessageTitles(recipientCdp, recipientEvents)
      captureSystemMessageTitles(otherCdp, otherEvents)
      const recipientTab = await recipientContext.newPage()
      const ticketResponse = recipient.waitForResponse((response) =>
        new URL(response.url()).pathname === '/api/system/realtime/notifications/ticket',
        { timeout: 10_000 },
      )
      const recipientTabTicket = recipientTab.waitForResponse((response) =>
        new URL(response.url()).pathname === '/api/system/realtime/notifications/ticket',
        { timeout: 10_000 },
      )
      const otherTicketResponse = other.waitForResponse((response) =>
        new URL(response.url()).pathname === '/api/system/realtime/notifications/ticket',
        { timeout: 10_000 },
      )
      const rCount = (await request<number>(recipient, recipientToken!, 'GET', '/api/system/message/unread-count')).result.data
      const nCount = (await request<number>(other, otherToken!, 'GET', '/api/system/message/unread-count')).result.data
      await recipient.goto('/system/message')
      await recipientTab.goto('/system/message')
      await other.goto('/system/message')
      const initialTicket = await ticketResponse
      const initialRecipientTabTicket = await recipientTabTicket
      const initialOtherTicket = await otherTicketResponse
      expect(initialTicket.ok()).toBeTruthy()
      expect(initialRecipientTabTicket.ok()).toBeTruthy()
      expect(initialOtherTicket.ok()).toBeTruthy()
      const initialTicketBody = (await initialTicket.json()) as ApiResult<{ ticket: string }>
      expect(initialTicketBody.code).toBe(200)
      expect((await initialRecipientTabTicket.json() as ApiResult<{ ticket: string }>).code).toBe(200)
      expect((await initialOtherTicket.json() as ApiResult<{ ticket: string }>).code).toBe(200)

      const sendMessage = async (title: string, userId: number) =>
        adminApi<number>('POST', '/api/system/message/send', {
          category: 'notice',
          type: 'message',
          source: 'E2E',
          title,
          content: '消息实时权限和持久化验证',
          targetType: 'USERS',
          userIds: [userId],
        })

      expect(await sendMessage(titles[0], userIds.get(usernames[0])!)).toBe(1)
      const recipientRow = recipient.getByRole('row').filter({ hasText: titles[0] })
      await expect(recipientRow).toBeVisible({ timeout: 10_000 })
      await expect(recipient.locator('.app-header__actions .el-badge__content')).toHaveText(String(rCount + 1), { timeout: 10_000 })
      await expect.poll(() => recipientEvents).toContain(titles[0])
      await expect.poll(async () =>
        (await request<number>(other, otherToken!, 'GET', '/api/system/message/unread-count')).result.data,
        { timeout: 10_000 },
      ).toBe(nCount)
      await expect(other.getByRole('row').filter({ hasText: titles[0] })).toHaveCount(0)
      expect(otherEvents).not.toContain(titles[0])
      expect(await sendMessage(controlTitle, userIds.get(usernames[1])!)).toBe(1)
      await expect(other.getByRole('row').filter({ hasText: controlTitle })).toBeVisible({ timeout: 10_000 })
      await expect(other.locator('.app-header__actions .el-badge__content')).toHaveText(String(nCount + 1), { timeout: 10_000 })
      await expect.poll(() => otherEvents).toContain(controlTitle)
      expect(otherEvents).not.toContain(titles[0])
      expect(recipientEvents).not.toContain(controlTitle)
      await expect.poll(async () =>
        (await request<number>(recipient, recipientToken!, 'GET', '/api/system/message/unread-count')).result.data,
        { timeout: 10_000 },
      ).toBe(rCount + 1)
      const recipientMessages = await findMessages(recipient, recipientToken!)
      const firstMessage = recipientMessages.find((row) => row.title === titles[0])
      expect(firstMessage?.readStatus).toBe(0)
      expect(firstMessage).toBeTruthy()

      for (const [method, path] of [
        ['GET', `/api/system/message/${firstMessage!.id}`],
        ['PUT', `/api/system/message/${firstMessage!.id}/read`],
        ['DELETE', `/api/system/message/${firstMessage!.id}`],
      ] as const) {
        const { result } = await request<unknown>(other, otherToken, method, path)
        expect(result.code, `${method} ${path} must not access another user's message`).not.toBe(200)
      }
      expect((await findMessages(recipient, recipientToken!)).find((row) => row.id === firstMessage!.id)?.readStatus).toBe(0)

      await recipientRow.getByRole('button', { name: '查看', exact: true }).click()
      await expect(recipient.getByRole('dialog', { name: '消息详情' })).toContainText(titles[0])
      await expect.poll(async () =>
        (await request<number>(recipient, recipientToken!, 'GET', '/api/system/message/unread-count')).result.data,
        { timeout: 10_000 },
      ).toBe(rCount)
      await expect(recipientTab.locator('.app-header__actions .el-badge__content')).toBeHidden({ timeout: 10_000 })
      await recipient.reload()
      await expect(recipient.getByRole('row').filter({ hasText: titles[0] })).toBeVisible()
      expect((await findMessages(recipient, recipientToken!)).find((row) => row.id === firstMessage!.id)?.readStatus).toBe(1)

      const reconnectDiagnostics: string[] = []
      recipient.on('request', (request) => {
        if (new URL(request.url()).pathname === '/api/system/realtime/notifications/ticket') {
          reconnectDiagnostics.push(`ticket-request:${Date.now()}`)
        }
      })
      recipient.on('requestfailed', (request) => {
        if (new URL(request.url()).pathname === '/api/system/realtime/notifications/ticket') {
          reconnectDiagnostics.push(`ticket-failed:${request.failure()?.errorText ?? 'unknown'}`)
        }
      })
      recipient.on('response', (response) => {
        if (new URL(response.url()).pathname === '/api/system/realtime/notifications/ticket') {
          reconnectDiagnostics.push(`ticket-response:${response.status()}`)
        }
      })
      await recipient.evaluate(() => {
        const target = window as typeof window & { __connectionEvents?: string[] }
        target.__connectionEvents = []
        window.addEventListener('offline', () => target.__connectionEvents?.push('offline'))
        window.addEventListener('online', () => target.__connectionEvents?.push('online'))
      })
      await recipientContext.setOffline(true)
      await recipient.waitForTimeout(1500)
      expect(await sendMessage(titles[1], userIds.get(usernames[0])!)).toBe(1)
      const reconnectPromise = recipient.waitForResponse((response) =>
        new URL(response.url()).pathname === '/api/system/realtime/notifications/ticket',
        { timeout: 45_000 },
      ).catch(async (error) => {
        const browserEvents = await recipient.evaluate(() =>
          (window as typeof window & { __connectionEvents?: string[] }).__connectionEvents ?? [],
        )
        throw new Error(`${String(error)}; ticket=${JSON.stringify(reconnectDiagnostics)}; browser=${JSON.stringify(browserEvents)}`)
      })
      await recipientContext.setOffline(false)
      const reconnect = await reconnectPromise
      expect(reconnect.ok()).toBeTruthy()
      const reconnectBody = (await reconnect.json()) as ApiResult<{ ticket: string }>
      expect(reconnectBody.code).toBe(200)
      expect(reconnectBody.data.ticket).not.toBe(initialTicketBody.data.ticket)
      await expect(recipient.getByRole('row').filter({ hasText: titles[1] })).toBeVisible({ timeout: 10_000 })
      expect((await findMessages(recipient, recipientToken!)).find((row) => row.title === titles[1])?.readStatus).toBe(0)
      expect(otherEvents).not.toContain(titles[1])
      await expect(recipient.locator('.app-header__actions .el-badge__content')).toHaveText(String(rCount + 1), { timeout: 10_000 })

      await recipient.evaluate(() => {
        const target = window as typeof window & { __messageInboxEvents?: number }
        target.__messageInboxEvents = 0
        window.addEventListener('message-inbox-changed', () => { target.__messageInboxEvents = (target.__messageInboxEvents ?? 0) + 1 })
      })
      const deniedTicket = recipient.waitForResponse(async (response) => {
        if (new URL(response.url()).pathname !== '/api/system/realtime/notifications/ticket') return false
        if (!response.ok()) return true
        try { return ((await response.json()) as ApiResult<unknown>).code !== 200 } catch { return false }
      }, { timeout: 45_000 })
      const listMenuIds = new Set(
        menus.filter((menu) => menu.perm === 'system:message:list').map((menu) => menu.id),
      )
      const remainingMenuIds = grantedMenuIds.filter((id) => !listMenuIds.has(id))
      await setRoleMenus(remainingMenuIds)
      const revokedInfo = await request<{ permissions: string[] }>(
        recipient, recipientToken!, 'GET', '/api/auth/info',
      )
      expect(revokedInfo.response.ok()).toBeTruthy()
      expect(revokedInfo.result.code).toBe(200)
      expect(revokedInfo.result.data.permissions).not.toContain('system:message:list')
      const denied = await deniedTicket
      const denial = (await denied.json()) as ApiResult<unknown>
      expect(denial.code).not.toBe(200)
      const beforeRevokedSend = await recipient.evaluate(() => (window as typeof window & { __messageInboxEvents?: number }).__messageInboxEvents ?? 0)
      expect(await sendMessage(titles[2], userIds.get(usernames[0])!)).toBe(1)
      await recipient.waitForTimeout(1500)
      expect(await recipient.evaluate(() => (window as typeof window & { __messageInboxEvents?: number }).__messageInboxEvents ?? 0)).toBe(beforeRevokedSend)
    } catch (error) {
      primaryError = error
      throw error
    } finally {
      try {
        await cleanup()
      } catch (error) {
        cleanupError = error
        if (primaryError) console.error('Message E2E cleanup failed after primary failure:', error)
      }
    }
    if (cleanupError && !primaryError) throw cleanupError
  })
})

function captureSystemMessageTitles(
  session: import('@playwright/test').CDPSession,
  titles: string[],
) {
  session.on('Network.eventSourceMessageReceived', (event: StreamEvent) => {
    try {
      const envelope = JSON.parse(event.data) as {
        type?: string
        payload?: { title?: string }
      }
      if (envelope.type === 'system-message' && envelope.payload?.title) {
        titles.push(envelope.payload.title)
      }
    } catch {
      // Ignore SSE comments and non-JSON transport frames.
    }
  })
}

function menuIdsForPermissions(menus: MenuRow[], permissions: string[]) {
  const selected = new Set(
    menus.filter((menu) => menu.perm && permissions.includes(menu.perm)).map((menu) => menu.id),
  )
  const byId = new Map(menus.map((menu) => [menu.id, menu]))
  for (const id of [...selected]) {
    let parentId = byId.get(id)?.parentId
    while (parentId) {
      selected.add(parentId)
      parentId = byId.get(parentId)?.parentId
    }
  }
  return [...selected]
}
