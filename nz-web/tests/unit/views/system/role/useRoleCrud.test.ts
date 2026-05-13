import { describe, it, expect, vi } from 'vitest'

vi.mock('@/api/system/role', () => ({
  pageRoles: vi.fn().mockResolvedValue({
    code: 200,
    data: { records: [{ id: 1, name: '管理员', roleKey: 'admin', sort: 0, status: 0 }], total: 1 },
  }),
  addRole: vi.fn().mockResolvedValue({ code: 200 }),
  updateRole: vi.fn().mockResolvedValue({ code: 200 }),
  deleteRole: vi.fn().mockResolvedValue({ code: 200 }),
}))

import { pageRoles } from '@/api/system/role'
import { useRoleCrud } from '@/views/system/role/hooks'

describe('useRoleCrud', () => {
  it('refresh 后表格有数据', async () => {
    const { table } = useRoleCrud()
    await table.refresh()

    await vi.waitFor(() => {
      expect(table.data.length).toBeGreaterThan(0)
    })
    expect(table.data[0].roleKey).toBe('admin')
    expect(pageRoles).toHaveBeenCalled()
  })

  it('handleResetQuery 将分页回到第一页', async () => {
    const { table } = useRoleCrud()
    table.pagination.current = 5
    table.query.name = 'x'

    table.handleResetQuery()

    expect(table.pagination.current).toBe(1)
  })

  it('openAdd 打开新增弹窗', () => {
    const { form } = useRoleCrud()
    form.openAdd()

    expect(form.visible).toBe(true)
    expect(form.mode).toBe('add')
    expect(form.model.roleKey).toBe('')
  })
})
