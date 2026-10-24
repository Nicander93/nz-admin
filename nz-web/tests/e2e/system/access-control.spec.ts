import { randomUUID } from 'node:crypto'

import { expect, loginByUi, test } from '@nz/test/e2e'

const userPagePermission = 'system:user:list'
const userPageEndpoint = '/api/system/user/page?pageNum=1&pageSize=10'
const testPassword = 'Access-control-E2E!2026'

test.describe('系统授权与撤权', () => {
  test.use({ actionTimeout: 15_000 })
  test.setTimeout(120_000)
  test('角色和菜单状态变化会影响已登录用户的真实请求', async ({
    authenticatedPage: admin,
    browser,
  }) => {
    const suffix = randomUUID().replaceAll('-', '').slice(0, 12)
    const roleName = `E2E授权角色${suffix}`
    const roleKey = `e2e_access_${suffix}`
    const menuName = `E2E授权菜单${suffix}`
    const username = `e2eaccess${suffix}`
    const nickname = `E2E授权用户${suffix}`
    let roleId: number | undefined
    let menuId: number | undefined
    let userId: number | undefined
    let userContext: Awaited<ReturnType<typeof browser.newContext>> | undefined
    let userToken: string | undefined

    const adminToken = await admin.evaluate(() => localStorage.getItem('token'))
    expect(adminToken).toBeTruthy()
    const apiBase = new URL(admin.url()).origin
    const adminRequest = async (
      path: string,
      method: string,
      data?: unknown,
    ) => {
      const response = await admin.request.fetch(`${apiBase}${path}`, {
        method,
        headers: { Authorization: adminToken! },
        data,
        timeout: 10_000,
      })
      expect(response.ok()).toBeTruthy()
      return response.json() as Promise<{
        code: number
        data: unknown
        msg?: string
      }>
    }
    const userRequest = async () => {
      expect(userToken).toBeTruthy()
      return admin.request.get(`${apiBase}${userPageEndpoint}`, {
        headers: { Authorization: userToken! },
      })
    }
    const assertGranted = async () => {
      const response = await userRequest()
      expect(response.status()).toBe(200)
      const body = (await response.json()) as {
        code: number
        data: { records: Array<{ username: string }> }
        msg?: string
      }
      expect(body.code).toBe(200)
      expect(body.data.records.map((item) => item.username)).toEqual([username])
    }
    const assertDenied = async () => {
      const response = await userRequest()
      const body = (await response.json()) as { code: number; msg?: string }
      expect([401, 403]).toContain(body.code)
    }

    try {
      await admin.goto('/system/menu')
      await admin
        .getByRole('button', { name: '新增', exact: true })
        .first()
        .click()
      const menuDialog = admin.getByRole('dialog')
      await menuDialog.getByText('按钮', { exact: true }).click()
      await menuDialog
        .getByLabel('菜单名称')
        .locator('..')
        .locator('input')
        .fill(menuName)
      await menuDialog
        .getByLabel('权限标识')
        .locator('..')
        .locator('input')
        .fill(userPagePermission)
      await menuDialog
        .getByRole('button', { name: '确定', exact: true })
        .click()
      await expect(menuDialog).toBeHidden()
      const menuResult = (await adminRequest(
        `/api/system/menu/list`,
        'GET',
      )) as { data: Array<{ id: number; name: string; perm?: string }> }
      const createdMenu = menuResult.data.find((item) => item.name === menuName)
      expect(createdMenu).toBeTruthy()
      menuId = createdMenu!.id
      expect(createdMenu!.perm).toBe(userPagePermission)

      await admin.goto('/system/role')
      await admin
        .getByRole('button', { name: '新增', exact: true })
        .first()
        .click()
      const roleDialog = admin.getByRole('dialog')
      await roleDialog
        .getByLabel('角色名称')
        .locator('..')
        .locator('input')
        .fill(roleName)
      await roleDialog
        .getByLabel('权限字符')
        .locator('..')
        .locator('input')
        .fill(roleKey)
      await roleDialog
        .getByRole('button', { name: '确定', exact: true })
        .click()
      await expect(roleDialog).toBeHidden()
      const roleResult = (await adminRequest(
        `/api/system/role/page?pageNum=1&pageSize=100`,
        'GET',
      )) as {
        data: { records: Array<{ id: number; name: string; roleKey: string }> }
      }
      const createdRole = roleResult.data.records.find(
        (item) => item.roleKey === roleKey,
      )
      expect(createdRole).toBeTruthy()
      roleId = createdRole!.id

      await admin
        .getByRole('textbox', { name: '角色名称', exact: true })
        .fill(roleName)
      await admin.getByRole('button', { name: '查询', exact: true }).click()
      const roleRow = admin.getByRole('row').filter({ hasText: roleName })
      await roleRow.getByRole('button', { name: '菜单权限' }).click()
      const permissionDialog = admin.getByRole('dialog')
      const menuNode = permissionDialog.getByRole('treeitem', {
        name: menuName,
        exact: true,
      })
      await menuNode
        .locator(':scope > .el-tree-node__content .el-checkbox')
        .click()
      await expect(menuNode.getByRole('checkbox')).toBeChecked()
      await permissionDialog
        .getByRole('button', { name: '确定', exact: true })
        .click()
      await expect(permissionDialog).toBeHidden()
      const roleMenus = (await adminRequest(
        `/api/system/role/${roleId}/menuIds`,
        'GET',
      )) as { data: number[] }
      expect(roleMenus.data).toContain(menuId)

      await admin.goto('/system/user')
      await admin
        .getByRole('button', { name: '新增', exact: true })
        .first()
        .click()
      const userDialog = admin.getByRole('dialog')
      await userDialog
        .getByLabel('用户名')
        .locator('..')
        .locator('input')
        .fill(username)
      await userDialog
        .getByLabel('密码')
        .locator('..')
        .locator('input')
        .fill(testPassword)
      await userDialog
        .getByLabel('昵称')
        .locator('..')
        .locator('input')
        .fill(nickname)
      const deptSelect = userDialog.getByRole('combobox', {
        name: '部门',
        exact: true,
      })
      await deptSelect
        .locator(
          'xpath=ancestor::div[contains(@class, "el-select__wrapper")][1]',
        )
        .click()
      const firstDept = admin
        .locator('.el-select-dropdown:visible .el-tree-node__content')
        .first()
      await expect(firstDept).toBeVisible()
      await firstDept.click()
      await userDialog
        .getByRole('button', { name: '确定', exact: true })
        .click()
      await expect(userDialog).toBeHidden()
      const userResult = (await adminRequest(
        `/api/system/user/page?pageNum=1&pageSize=100&username=${encodeURIComponent(username)}`,
        'GET',
      )) as { data: { records: Array<{ id: number; username: string }> } }
      const createdUser = userResult.data.records.find(
        (item) => item.username === username,
      )
      expect(createdUser).toBeTruthy()
      userId = createdUser!.id

      const userRow = admin.getByRole('row').filter({ hasText: username })
      await userRow.getByRole('button', { name: '分配角色' }).click()
      const assignDialog = admin.getByRole('dialog')
      await assignDialog.getByText(roleName, { exact: true }).click()
      await expect(assignDialog.getByLabel(roleName)).toBeChecked()
      await assignDialog
        .getByRole('button', { name: '确定', exact: true })
        .click()
      await expect(assignDialog).toBeHidden()
      const assignedRoleIds = (await adminRequest(
        `/api/system/user/${userId}/roleIds`,
        'GET',
      )) as { data: number[] }
      expect(assignedRoleIds.data).toEqual([roleId])

      userContext = await browser.newContext({
        baseURL: new URL(admin.url()).origin,
      })
      const userPage = await userContext.newPage()
      await loginByUi(userPage, {
        username,
        password: testPassword,
        tenantCode: process.env.E2E_TENANT_CODE ?? 'default',
      })
      await expect(userPage).toHaveURL(/\/profile$/)
      userToken =
        (await userPage.evaluate(() => localStorage.getItem('token'))) ??
        undefined
      expect(userToken).toBeTruthy()
      const userInfoResponse = await userPage.request.get(
        `${apiBase}/api/auth/info`,
        {
          headers: { Authorization: userToken! },
        },
      )
      const userInfo = (await userInfoResponse.json()) as {
        code: number
        data: { roles: string[]; permissions: string[] }
      }
      expect(userInfo.code).toBe(200)
      expect(userInfo.data.roles).toContain(roleKey)
      expect(userInfo.data.permissions).toContain(userPagePermission)
      await assertGranted()

      await admin.goto('/system/role')
      await admin
        .getByRole('textbox', { name: '角色名称', exact: true })
        .fill(roleName)
      await admin.getByRole('button', { name: '查询', exact: true }).click()
      const roleRowNow = admin.getByRole('row').filter({ hasText: roleName })
      await roleRowNow
        .getByRole('button', { name: '编辑', exact: true })
        .click()
      const editRoleDialog = admin.getByRole('dialog')
      await editRoleDialog.getByText('禁用', { exact: true }).click()
      await editRoleDialog
        .getByRole('button', { name: '确定', exact: true })
        .click()
      await expect(editRoleDialog).toBeHidden()
      await assertDenied()

      await roleRowNow
        .getByRole('button', { name: '编辑', exact: true })
        .click()
      const enableRoleDialog = admin.getByRole('dialog')
      await enableRoleDialog.getByText('正常', { exact: true }).click()
      await enableRoleDialog
        .getByRole('button', { name: '确定', exact: true })
        .click()
      await expect(enableRoleDialog).toBeHidden()
      await assertGranted()

      await admin.goto('/system/menu')
      const menuRow = admin.getByRole('row').filter({ hasText: menuName })
      await menuRow.getByRole('button', { name: '编辑', exact: true }).click()
      const disableMenuDialog = admin.getByRole('dialog')
      await disableMenuDialog.getByText('停用', { exact: true }).click()
      await disableMenuDialog
        .getByRole('button', { name: '确定', exact: true })
        .click()
      await expect(disableMenuDialog).toBeHidden()
      await assertDenied()

      await menuRow.getByRole('button', { name: '编辑', exact: true }).click()
      const enableMenuDialog = admin.getByRole('dialog')
      await enableMenuDialog.getByText('正常', { exact: true }).click()
      await enableMenuDialog
        .getByRole('button', { name: '确定', exact: true })
        .click()
      await expect(enableMenuDialog).toBeHidden()
      await assertGranted()
    } finally {
      await userContext?.close()
      const users = (await adminRequest(
        `/api/system/user/page?pageNum=1&pageSize=100&username=${username}`,
        'GET',
      )) as { data: { records: Array<{ id: number; username: string }> } }
      userId ??= users.data.records.find(
        (item) => item.username === username,
      )?.id
      const roles = (await adminRequest(
        `/api/system/role/page?pageNum=1&pageSize=100&roleKey=${roleKey}`,
        'GET',
      )) as { data: { records: Array<{ id: number; roleKey: string }> } }
      roleId ??= roles.data.records.find((item) => item.roleKey === roleKey)?.id
      const menus = (await adminRequest('/api/system/menu/list', 'GET')) as {
        data: Array<{ id: number; name: string }>
      }
      menuId ??= menus.data.find((item) => item.name === menuName)?.id
      if (userId) {
        expect(
          (await adminRequest(`/api/system/user/${userId}/roles`, 'PUT', []))
            .code,
        ).toBe(200)
        expect(
          (await adminRequest(`/api/system/user/${userId}`, 'DELETE')).code,
        ).toBe(200)
      }
      if (roleId)
        expect(
          (await adminRequest(`/api/system/role/${roleId}`, 'DELETE')).code,
        ).toBe(200)
      if (menuId)
        expect(
          (await adminRequest(`/api/system/menu/${menuId}`, 'DELETE')).code,
        ).toBe(200)
    }
  })
})
