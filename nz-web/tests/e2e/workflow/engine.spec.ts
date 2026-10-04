import { expect, test, loginByUi } from '@nz/test/e2e'

async function authHeaders(page: import('@playwright/test').Page) {
  return {
    Authorization:
      (await page.evaluate(() => localStorage.getItem('token'))) ?? '',
  }
}

test('官方设计器拖拽、保存、重载与发布', async ({
  authenticatedPage: page,
}) => {
  test.skip(process.env.E2E_WARM_FLOW_ENABLED !== 'true', '需要启用新引擎')
  const code = `browser_designer_${Date.now()}`
  const name = `浏览器设计 ${Date.now()}`
  await page.goto('/workflow/engine')
  await page.getByRole('tab', { name: '流程定义', exact: true }).click()
  await page.getByRole('button', { name: '创建流程', exact: true }).click()
  await page.getByLabel('流程名称', { exact: true }).fill(name)
  await page.getByLabel('流程编码', { exact: true }).fill(code)
  await page.getByRole('button', { name: '创建并设计', exact: true }).click()
  const iframe = page.locator('iframe[title="Warm-Flow 官方流程设计器"]')
  await expect(iframe).toBeVisible()
  const src = await iframe.getAttribute('src')
  expect(src).not.toMatch(/token|Authorization/i)
  const id = new URL(src!, 'http://localhost').searchParams.get('id')!
  const frame = page.frameLocator('iframe[title="Warm-Flow 官方流程设计器"]')
  const node = frame
    .locator('svg text')
    .filter({ hasText: /^审批$/ })
    .first()
  await expect(node).toBeVisible()
  const shape = frame
    .locator('.lf-node')
    .filter({ hasText: /^审批$/ })
    .locator('rect')
    .first()
  const before = await shape.boundingBox()
  expect(before).not.toBeNull()
  await page.mouse.move(
    before!.x + before!.width / 2,
    before!.y + before!.height / 2,
  )
  await page.mouse.down()
  await page.mouse.move(
    before!.x + before!.width / 2 + 80,
    before!.y + before!.height / 2 + 60,
    { steps: 12 },
  )
  await page.mouse.up()
  const properties = frame.getByRole('dialog', {
    name: '设置中间属性',
    exact: true,
  })
  if (!(await properties.isVisible())) await shape.click()
  await expect(properties).toBeVisible()
  await frame.getByText('办理人设置', { exact: true }).click()
  await frame.getByRole('button', { name: '选择', exact: true }).click()
  const picker = frame.getByRole('dialog', { name: '人员选择', exact: true })
  const administrator = picker.getByRole('row').filter({ hasText: '管理员' })
  await expect(administrator).toBeVisible()
  await administrator.click()
  await picker.getByRole('button', { name: '确 定', exact: true }).click()
  await frame
    .getByRole('button', { name: 'el.drawer.close', exact: true })
    .click()
  await frame.locator('button.toolbar-save-btn').click()
  await expect(iframe).toHaveCount(0)
  const headers = await authHeaders(page)
  const saved = await (
    await page.request.get(`/api/workflow/designer/warm-flow/query-def/${id}`, {
      headers,
    })
  ).json()
  expect(saved.code).toBe(200)
  const coordinate = saved.data.nodeList.find(
    (item: { nodeCode: string }) => item.nodeCode === 'review',
  ).coordinate
  expect(coordinate.split('|')[0]).not.toBe('420,240')
  const row = page.getByRole('row').filter({ hasText: name }).first()
  await row.getByRole('button', { name: '设计', exact: true }).click()
  await expect(
    frame
      .locator('svg text')
      .filter({ hasText: /^审批$/ })
      .first(),
  ).toBeVisible()
  const reloaded = await (
    await page.request.get(`/api/workflow/designer/warm-flow/query-def/${id}`, {
      headers,
    })
  ).json()
  expect(
    reloaded.data.nodeList.find(
      (item: { nodeCode: string }) => item.nodeCode === 'review',
    ).coordinate,
  ).toBe(coordinate)
  await page
    .locator('.el-dialog')
    .filter({ has: iframe })
    .getByRole('button', { name: 'Close this dialog', exact: true })
    .click()
  await row.getByRole('button', { name: '发布', exact: true }).click()
  await page.getByRole('button', { name: '确定', exact: true }).click()
  await expect(row.getByText('已发布', { exact: true })).toBeVisible()
  expect(
    (
      await (
        await page.request.delete(`/api/workflow/engine/definitions/${id}`, {
          headers,
        })
      ).json()
    ).code,
  ).toBe(200)
})

test('普通用户请假提交、审批人退回、重新提交与通过', async ({
  authenticatedPage: page,
  browser,
}) => {
  test.skip(process.env.E2E_WARM_FLOW_ENABLED !== 'true', '需要启用新引擎')
  test.setTimeout(120_000)
  const suffix = Date.now()
  const code = `browser_leave_${suffix}`
  const username = `applicant_${suffix}`
  const roleKey = `applicant_${suffix}`
  const password = 'Browser-test-password-123'
  const headers = await authHeaders(page)
  async function api(method: string, path: string, data?: unknown) {
    const response = await page.request.fetch(path, {
      timeout: 10000,
      method,
      headers: { ...headers, 'Idempotency-Key': crypto.randomUUID() },
      data,
    })
    const body = await response.json()
    expect(body.code, body.msg).toBe(200)
    return body.data
  }
  const definition = await api('POST', '/api/workflow/designer/definitions', {
    flowCode: code,
    flowName: code,
    businessType: 'leave',
  })
  await api('POST', `/api/workflow/engine/definitions/${definition}/publish`)
  const menus = (await api('GET', '/api/system/menu/list')) as {
    id: number
    path: string
    perm?: string
  }[]
  await api('POST', '/api/system/role', {
    name: '浏览器普通申请人',
    roleKey,
    status: 0,
    sort: 0,
    dataScope: 1,
  })
  const roles = (await api('GET', '/api/system/role/listAll')) as {
    id: number
    roleKey: string
  }[]
  const roleId = roles.find((item) => item.roleKey === roleKey)!.id
  await api(
    'PUT',
    `/api/system/role/${roleId}/menus`,
    menus
      .filter(
        (item) =>
          ['/demo', '/workflow'].includes(item.path) ||
          item.perm?.startsWith('demo:leave:') ||
          ['workflow:engine:query', 'workflow:engine:start'].includes(
            item.perm ?? '',
          ),
      )
      .map((item) => item.id),
  )
  await api('POST', '/api/system/user', {
    username,
    nickname: '浏览器申请人',
    password,
    deptId: 1,
    status: 0,
  })
  const users = (await api(
    'GET',
    `/api/system/user/page?username=${username}`,
  )) as { records: { id: number }[] }
  const userId = users.records[0]!.id
  await api('PUT', `/api/system/user/${userId}/roles`, [roleId])
  const context = await browser.newContext({
    baseURL: process.env.E2E_BASE_URL ?? 'http://127.0.0.1:4173',
  })
  const applicant = await context.newPage()
  applicant.setDefaultTimeout(10000)
  try {
    await loginByUi(applicant, { username, password })
    await applicant.goto('/demo/leave')
    await applicant
      .getByRole('button', { name: '新建申请', exact: true })
      .click()
    await applicant
      .getByRole('combobox', { name: '审批流程', exact: true })
      .press('ArrowDown')
    await applicant.getByRole('option', { name: code, exact: true }).click()
    await applicant.getByLabel('开始日期', { exact: true }).fill('2026-10-10')
    await applicant.getByLabel('开始日期', { exact: true }).press('Tab')
    await applicant.getByLabel('结束日期', { exact: true }).fill('2026-10-12')
    await applicant.getByLabel('结束日期', { exact: true }).press('Tab')
    await applicant.getByLabel('申请原因', { exact: true }).fill(code)
    await applicant
      .getByRole('button', { name: '保存草稿', exact: true })
      .click()
    const application = applicant.getByRole('row').filter({ hasText: code })
    await expect(application.getByText('草稿', { exact: true })).toBeVisible()
    await application
      .getByRole('button', { name: '提交审批', exact: true })
      .click()
    await expect(application.getByText('审批中', { exact: true })).toBeVisible()
    await page.goto('/workflow/engine')
    await page
      .getByRole('row')
      .filter({ hasText: code })
      .getByRole('button', { name: '办理', exact: true })
      .click()
    await expect(
      page.locator('.el-descriptions').getByText(code, { exact: true }),
    ).toBeVisible()
    await page.getByRole('button', { name: '退回', exact: true }).click()
    await expect(page.getByText('状态：已退回', { exact: true })).toBeVisible()
    await expect
      .poll(async () => {
        await applicant
          .getByRole('button', { name: '刷新', exact: true })
          .click()
        return application.innerText()
      })
      .toContain('已退回')
    await application.getByRole('button', { name: '编辑', exact: true }).click()
    await expect(applicant.getByLabel('开始日期', { exact: true })).toHaveValue(
      '2026-10-10',
    )
    await expect(applicant.getByLabel('结束日期', { exact: true })).toHaveValue(
      '2026-10-12',
    )
    await applicant
      .getByLabel('申请原因', { exact: true })
      .fill(`${code} 补充说明`)
    await applicant
      .getByRole('button', { name: '保存草稿', exact: true })
      .click()
    await expect(application).toContainText('补充说明')

    await application
      .getByRole('button', { name: '提交审批', exact: true })
      .click()
    await page.reload()
    await page
      .getByRole('row')
      .filter({ hasText: code })
      .getByRole('button', { name: '办理', exact: true })
      .click()
    await expect(page.locator('.el-descriptions')).toContainText('补充说明')
    await page.getByRole('button', { name: '通过', exact: true }).click()
    await expect(page.getByText('状态：已完成', { exact: true })).toBeVisible()
    await expect
      .poll(async () => {
        await applicant
          .getByRole('button', { name: '刷新', exact: true })
          .click()
        return application.innerText()
      })
      .toContain('已通过')
    await applicant.goto('/workflow/engine')
    await expect(
      applicant.getByRole('tab', { name: '流程定义', exact: true }),
    ).toHaveCount(0)
    await applicant.getByRole('tab', { name: '我的申请', exact: true }).click()
    await expect(
      applicant.getByRole('row').filter({ hasText: code }),
    ).toHaveCount(2)
  } finally {
    await context.close().catch(() => {})
    await api('PUT', `/api/system/user/${userId}/roles`, [])
    await api('DELETE', `/api/system/user/${userId}`)
    await api('PUT', `/api/system/role/${roleId}/menus`, [])
    await api('DELETE', `/api/system/role/${roleId}`)
  }
})
