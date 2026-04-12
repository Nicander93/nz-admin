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

import { useUserCrud } from '@/hooks/useUserCrud'

describe('useUserCrud', () => {
  it('init 后加载部门和角色数据', async () => {
    const { init, deptTree, allRoles } = useUserCrud()
    init()

    await vi.waitFor(() => {
      expect(deptTree.value.length).toBeGreaterThan(0)
      expect(allRoles.value.length).toBeGreaterThan(0)
    })
  })

  it('getDeptName 返回正确的部门名称', async () => {
    const { loadDepts, getDeptName } = useUserCrud()
    await loadDepts()

    expect(getDeptName(1)).toBe('总公司')
    expect(getDeptName(2)).toBe('技术部')
    expect(getDeptName(999)).toBe('-')
    expect(getDeptName(undefined)).toBe('-')
  })

  it('handleResetQuery 重置分页到第一页', () => {
    const { query, handleResetQuery, pagination } = useUserCrud()

    query.username = 'test'
    pagination.current = 3

    handleResetQuery()
    expect(pagination.current).toBe(1)
  })

  it('openRoleDialog 加载用户角色 ID', async () => {
    const { openRoleDialog, roleDialogVisible, selectedRoleIds } = useUserCrud()

    await openRoleDialog({ id: 1, username: 'admin', nickname: '', email: '', phone: '', status: 0 })

    expect(roleDialogVisible.value).toBe(true)
    expect(selectedRoleIds.value).toEqual([1, 2])
  })

  it('toAdd 打开新增弹窗', () => {
    const { toAdd, visible, mode, form } = useUserCrud()
    toAdd()

    expect(visible.value).toBe(true)
    expect(mode.value).toBe('add')
    expect(form.username).toBe('')
  })

  it('toEdit 打开编辑弹窗并填充数据', () => {
    const { toEdit, visible, mode, form } = useUserCrud()

    toEdit({ id: 1, username: 'admin', nickname: '管理员', email: 'a@b.com', phone: '123', status: 0 })

    expect(visible.value).toBe(true)
    expect(mode.value).toBe('edit')
    expect(form.username).toBe('admin')
    expect(form.nickname).toBe('管理员')
  })
})
