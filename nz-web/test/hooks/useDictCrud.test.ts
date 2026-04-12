import { describe, it, expect, vi } from 'vitest'
import { useDictCrud } from '@/hooks/useDictCrud'

vi.mock('@/api/system/dict', () => ({
  pageDictTypes: vi.fn().mockResolvedValue({
    code: 200,
    data: { records: [{ id: 1, name: '用户性别', type: 'sys_user_sex', status: 0 }], total: 1 },
  }),
  addDictType: vi.fn().mockResolvedValue({ code: 200 }),
  updateDictType: vi.fn().mockResolvedValue({ code: 200 }),
  deleteDictType: vi.fn().mockResolvedValue({ code: 200 }),
  listDictDataByType: vi.fn().mockResolvedValue({
    code: 200,
    data: [
      { id: 1, dictType: 'sys_user_sex', label: '男', value: '0', sort: 0, status: 0 },
      { id: 2, dictType: 'sys_user_sex', label: '女', value: '1', sort: 1, status: 0 },
    ],
  }),
  addDictData: vi.fn().mockResolvedValue({ code: 200 }),
  updateDictData: vi.fn().mockResolvedValue({ code: 200 }),
  deleteDictData: vi.fn().mockResolvedValue({ code: 200 }),
}))

describe('useDictCrud', () => {
  it('openDataPanel 加载字典数据', async () => {
    const { openDataPanel, dataDrawerVisible, dataList, currentDictType } = useDictCrud()

    await openDataPanel({ id: 1, name: '用户性别', type: 'sys_user_sex', status: 0 } as any)

    expect(dataDrawerVisible.value).toBe(true)
    expect(currentDictType.value).toBe('sys_user_sex')
    expect(dataList.value).toHaveLength(2)
    expect(dataList.value[0].label).toBe('男')
  })

  it('openDataAdd 设置 dictType', async () => {
    const { openDataPanel, openDataAdd, dataForm } = useDictCrud()

    await openDataPanel({ id: 1, name: '用户性别', type: 'sys_user_sex', status: 0 } as any)
    openDataAdd()

    expect(dataForm.visible.value).toBe(true)
    expect(dataForm.form.dictType).toBe('sys_user_sex')
  })

  it('handleTypeResetQuery 重置查询', () => {
    const { typeCrud, handleTypeResetQuery } = useDictCrud()

    typeCrud.query.name = 'test'
    typeCrud.pagination.current = 3

    handleTypeResetQuery()
    expect(typeCrud.pagination.current).toBe(1)
  })
})
