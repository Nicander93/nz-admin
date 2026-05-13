import { describe, it, expect, vi } from 'vitest'

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
  },
}))

import { useConfigCrud } from '@/views/system/config/hooks'

function mockListApi(data: any[] = []) {
  return vi.fn().mockResolvedValue({ code: 200, data })
}

function mockVoidApi() {
  return vi.fn().mockResolvedValue({ code: 200, data: null })
}

describe('useConfigCrud', () => {
  it('loadData 填充表格', async () => {
    const listApi = mockListApi([
      { id: 1, configName: '主框架', configKey: 'sys.index.skinName', configValue: 'blue', configType: 2, status: 0 },
    ])

    const { table } = useConfigCrud({
      listApi: listApi as any,
    })

    await table.loadData()
    expect(table.loading).toBe(false)
    expect(table.data).toHaveLength(1)
    expect(table.data[0].configKey).toBe('sys.index.skinName')
  })

  it('submit 新增成功后刷新列表', async () => {
    const addApi = mockVoidApi()
    const listApi = mockListApi([])

    const { form, actions } = useConfigCrud({
      listApi: listApi as any,
      addApi: addApi as any,
    })

    form.openAdd()
    form.model.configKey = 'k'
    form.model.configName = 'n'
    form.model.configValue = 'v'
    await actions.submit()

    expect(addApi).toHaveBeenCalled()
    expect(listApi).toHaveBeenCalled()
  })

  it('remove 删除后刷新列表', async () => {
    const deleteApi = mockVoidApi()
    const listApi = mockListApi([])

    const { actions } = useConfigCrud({
      listApi: listApi as any,
      deleteApi: deleteApi as any,
    })

    await actions.remove(1)
    expect(deleteApi).toHaveBeenCalledWith(1)
    expect(listApi).toHaveBeenCalled()
  })
})
