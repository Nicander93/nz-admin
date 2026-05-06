import { describe, it, expect, vi } from 'vitest'

vi.mock('@/api/system/user', () => ({
  pageUsers: vi.fn().mockResolvedValue({
    code: 200,
    data: { records: [{ id: 1, username: 'admin', nickname: '管理员' }], total: 1 },
  }),
  addUser: vi.fn().mockResolvedValue({ code: 200 }),
  updateUser: vi.fn().mockResolvedValue({ code: 200 }),
  deleteUser: vi.fn().mockResolvedValue({ code: 200 }),
  getUserRoleIds: vi.fn().mockResolvedValue({ code: 200, data: [1, 2] }),
  assignUserRoles: vi.fn().mockResolvedValue({ code: 200 }),
}))

vi.mock('@/api/system/dept', () => ({
  listDepts: vi.fn().mockResolvedValue({
    code: 200,
    data: [
      { id: 1, parentId: 0, name: '总公司' },
      { id: 2, parentId: 1, name: '技术部' },
    ],
  }),
}))

vi.mock('@/api/system/role', () => ({
  listAllRoles: vi.fn().mockResolvedValue({
    code: 200,
    data: [{ id: 1, name: '管理员', roleKey: 'admin' }],
  }),
}))

import { useUserCrud } from '@/views/system/user/hooks'

describe('useUserCrud', () => {
  it('init 后加载部门和角色数据', async () => {
    const { lifecycle, dept, role } = useUserCrud()
    lifecycle.init()

    await vi.waitFor(() => {
      expect(dept.tree.length).toBeGreaterThan(0)
      expect(role.all.length).toBeGreaterThan(0)
    })
  })

  it('getDeptName 返回正确的部门名称', async () => {
    const { dept } = useUserCrud()
    await dept.load()

    expect(dept.getName(1)).toBe('总公司')
    expect(dept.getName(2)).toBe('技术部')
    expect(dept.getName(999)).toBe('-')
    expect(dept.getName(undefined)).toBe('-')
  })

  it('handleResetQuery 重置分页到第一页', () => {
    const { table } = useUserCrud()

    table.query.username = 'test'
    table.pagination.current = 3

    table.handleResetQuery()
    expect(table.pagination.current).toBe(1)
  })

  it('openRoleDialog 加载用户角色 ID', async () => {
    const { role } = useUserCrud()

    await role.openDialog({ id: 1, username: 'admin', nickname: '', email: '', phone: '', status: 0 })

    expect(role.dialogVisible).toBe(true)
    expect(role.selectedIds).toEqual([1, 2])
  })

  it('toAdd 打开新增弹窗', () => {
    const { form } = useUserCrud()
    form.toAdd()

    expect(form.visible).toBe(true)
    expect(form.mode).toBe('add')
    expect(form.model.username).toBe('')
  })

  it('toEdit 打开编辑弹窗并填充数据', () => {
    const { form } = useUserCrud()

    form.toEdit({ id: 1, username: 'admin', nickname: '管理员', email: 'a@b.com', phone: '123', status: 0 })

    expect(form.visible).toBe(true)
    expect(form.mode).toBe('edit')
    expect(form.model.username).toBe('admin')
    expect(form.model.nickname).toBe('管理员')
  })
})
