import { describe, it, expect, vi } from 'vitest'

vi.mock('@/api/system/dict', () => ({
  pageDictTypes: vi.fn().mockResolvedValue({
    code: 200,
    data: { records: [{ id: 1, name: '用户性别', type: 'sys_user_sex', status: 0 }], total: 1 },
  }),
  addDictType: vi.fn().mockResolvedValue({ code: 200 }),
  updateDictType: vi.fn().mockResolvedValue({ code: 200 }),
  deleteDictType: vi.fn().mockResolvedValue({ code: 200 }),
}))

import { pageDictTypes } from '@/api/system/dict'
import { useDictCrud } from '@/views/system/dict/hooks'

describe('useDictCrud', () => {
  it('refresh 后表格有数据', async () => {
    const { table } = useDictCrud()
    await table.refresh()

    await vi.waitFor(() => {
      expect(table.data.length).toBeGreaterThan(0)
    })
    expect(table.data[0].type).toBe('sys_user_sex')
    expect(pageDictTypes).toHaveBeenCalled()
  })

  it('handleResetQuery 将分页回到第一页', () => {
    const { table } = useDictCrud()
    table.query.name = 'test'
    table.pagination.current = 3

    table.handleResetQuery()

    expect(table.pagination.current).toBe(1)
  })

  it('openAdd 打开新增弹窗', () => {
    const { form } = useDictCrud()
    form.openAdd()

    expect(form.visible).toBe(true)
    expect(form.mode).toBe('add')
    expect(form.model.type).toBe('')
  })
})
